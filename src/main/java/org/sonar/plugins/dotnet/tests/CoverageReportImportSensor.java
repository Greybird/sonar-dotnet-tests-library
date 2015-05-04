/*
 * SonarQube .NET Tests Library
 * Copyright (C) 2014 SonarSource
 * dev@sonar.codehaus.org
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.config.Settings;
import org.sonar.api.measures.CoverageMeasuresBuilder;
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.Project;

import java.io.File;
import java.util.Map;

public class CoverageReportImportSensor implements Sensor {
  private static final Logger LOG = LoggerFactory.getLogger(CoverageReportImportSensor.class);

  private static Coverage cachedCoverage;

  private final WildcardPatternFileProvider wildcardPatternFileProvider = new WildcardPatternFileProvider(new File("."), File.separator);
  private final CoverageConfiguration coverageConf;
  private final CoverageAggregator coverageAggregator;
  private final Settings settings;

  @VisibleForTesting
  static void clearCache() {
    cachedCoverage = null;
  }

  public CoverageReportImportSensor(Settings settings, CoverageConfiguration coverageConf, CoverageAggregator coverageAggregator) {
    this.coverageConf = coverageConf;
    this.coverageAggregator = coverageAggregator;
    this.settings = settings;
  }

  @Override
  public boolean shouldExecuteOnProject(Project project) {
    return coverageAggregator.hasCoverageProperty();
  }

  @Override
  public void analyse(Project project, SensorContext context) {
    analyze(context, new FileProvider(project, context));
  }

  @VisibleForTesting
  void analyze(SensorContext context, FileProvider fileProvider) {
    analyze(context, fileProvider, new Coverage());
  }

  @VisibleForTesting
  void analyze(SensorContext context, FileProvider fileProvider, Coverage coverage) {
    Coverage usedCoverage = coverage;
    boolean aggregate = true;
    if (settings.getBoolean("coverage.useGlobalCache")) {
      if (cachedCoverage == null) {
        cachedCoverage = usedCoverage;
      } else {
        aggregate = false;
      }
      usedCoverage = cachedCoverage;
    }
    analyze(context, fileProvider, usedCoverage, aggregate);
  }

  private void analyze(SensorContext context, FileProvider fileProvider, Coverage coverage, boolean aggregate) {
    if (aggregate) {
      coverageAggregator.aggregate(wildcardPatternFileProvider, coverage);
    }
    CoverageMeasuresBuilder coverageMeasureBuilder = CoverageMeasuresBuilder.create();

    for (String filePath : coverage.files()) {
      org.sonar.api.resources.File sonarFile = fileProvider.fromPath(filePath);

      if (sonarFile != null) {
        if (coverageConf.languageKey().equals(sonarFile.getLanguage().getKey())) {
          coverageMeasureBuilder.reset();
          for (Map.Entry<Integer, Integer> entry : coverage.hits(filePath).entrySet()) {
            coverageMeasureBuilder.setHits(entry.getKey(), entry.getValue());
          }

          for (Measure measure : coverageMeasureBuilder.createMeasures()) {
            context.saveMeasure(sonarFile, measure);
          }
        }
      } else {
        LOG.debug("Code coverage will not be imported for the following file outside of SonarQube: " + filePath);
      }
    }
  }

}
