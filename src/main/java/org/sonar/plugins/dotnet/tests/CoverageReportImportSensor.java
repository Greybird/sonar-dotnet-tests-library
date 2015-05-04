/*
 * SonarQube .NET Tests Library
 * Copyright (C) 2014 SonarSource
 * sonarqube@googlegroups.com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.dotnet.tests;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.config.Settings;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputFile.Type;
import org.sonar.api.measures.CoverageMeasuresBuilder;
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.Project;

import java.io.File;
import java.util.Map;

public class CoverageReportImportSensor implements Sensor {
  private static final Logger LOG = LoggerFactory.getLogger(CoverageReportImportSensor.class);

  private static final Map<String, Coverage> cachedCoverage = Maps.newHashMap();

  private final WildcardPatternFileProvider wildcardPatternFileProvider = new WildcardPatternFileProvider(new File("."), File.separator);
  private final CoverageConfiguration coverageConf;
  private final CoverageAggregator coverageAggregator;
  private final FileSystem fs;
  private final Settings settings;

  @VisibleForTesting
  static void clearCache() {
    cachedCoverage.clear();;
  }

  public CoverageReportImportSensor(Settings settings, CoverageConfiguration coverageConf, FileSystem fs, CoverageAggregator coverageAggregator) {
    this.coverageConf = coverageConf;
    this.coverageAggregator = coverageAggregator;
    this.fs = fs;
    this.settings = settings;
  }

  @Override
  public boolean shouldExecuteOnProject(Project project) {
    return coverageAggregator.hasCoverageProperty();
  }

  @Override
  public void analyse(Project project, SensorContext context) {
    analyze(context, new Coverage());
  }

  @VisibleForTesting
  void analyze(SensorContext context, Coverage coverage) {
    Coverage usedCoverage = coverage;
    boolean aggregate = true;
    if (settings.getBoolean(coverageConf.globalCacheKey())) {
      Coverage c = cachedCoverage.get(coverageConf.globalCacheKey());
      if (c == null) {
        cachedCoverage.put(coverageConf.globalCacheKey(), usedCoverage);
      } else {
        LOG.info("Reusing global cached coverage");
        aggregate = false;
        usedCoverage = c;
      }
    }
    analyze(context, usedCoverage, aggregate);
  }

  private void analyze(SensorContext context, Coverage coverage, boolean aggregate) {
    if (aggregate) {
      coverageAggregator.aggregate(wildcardPatternFileProvider, coverage);
    }
    CoverageMeasuresBuilder coverageMeasureBuilder = CoverageMeasuresBuilder.create();

    for (String filePath : coverage.files()) {
      InputFile inputFile = fs.inputFile(fs.predicates().and(fs.predicates().hasType(Type.MAIN), fs.predicates().hasAbsolutePath(filePath)));

      if (inputFile != null) {
        if (coverageConf.languageKey().equals(inputFile.language())) {
          coverageMeasureBuilder.reset();
          for (Map.Entry<Integer, Integer> entry : coverage.hits(filePath).entrySet()) {
            coverageMeasureBuilder.setHits(entry.getKey(), entry.getValue());
          }

          for (Measure measure : coverageMeasureBuilder.createMeasures()) {
            context.saveMeasure(inputFile, measure);
          }
        }
      } else {
        LOG.debug("Code coverage will not be imported for the following file outside of SonarQube: " + filePath);
      }
    }
  }

}
