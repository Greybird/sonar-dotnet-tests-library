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
import com.google.common.base.Splitter;
import org.sonar.api.BatchExtension;
import org.sonar.api.config.Settings;

import java.io.File;

public class CoverageAggregator implements BatchExtension {

  private final CoverageConfiguration coverageConf;
  private final Settings settings;
  private final NCover3ReportParser ncover3ReportParser;
  private final OpenCoverReportParser openCoverReportParser;
  private final DotCoverReportsAggregator dotCoverReportsAggregator;
  private final VisualStudioCoverageXmlReportParser visualStudioCoverageXmlReportParser;

  public CoverageAggregator(CoverageConfiguration coverageConf, Settings settings) {
    this(coverageConf, settings,
      new NCover3ReportParser(),
      new OpenCoverReportParser(),
      new DotCoverReportsAggregator(new DotCoverReportParser()), new VisualStudioCoverageXmlReportParser());
  }

  @VisibleForTesting
  public CoverageAggregator(CoverageConfiguration coverageConf, Settings settings,
    NCover3ReportParser ncover3ReportParser,
    OpenCoverReportParser openCoverReportParser,
    DotCoverReportsAggregator dotCoverReportsAggregator,
    VisualStudioCoverageXmlReportParser visualStudioCoverageXmlReportParser) {

    this.coverageConf = coverageConf;
    this.settings = settings;
    this.ncover3ReportParser = ncover3ReportParser;
    this.openCoverReportParser = openCoverReportParser;
    this.dotCoverReportsAggregator = dotCoverReportsAggregator;
    this.visualStudioCoverageXmlReportParser = visualStudioCoverageXmlReportParser;
  }

  public boolean hasCoverageProperty() {
    return hasNCover3ReportPaths() || hasOpenCoverReportPaths() || hasDotCoverReportPaths() || hasVisualStudioCoverageXmlReportPaths();
  }

  private boolean hasNCover3ReportPaths() {
    return settings.hasKey(coverageConf.ncover3PropertyKey());
  }

  private boolean hasOpenCoverReportPaths() {
    return settings.hasKey(coverageConf.openCoverPropertyKey());
  }

  private boolean hasDotCoverReportPaths() {
    return settings.hasKey(coverageConf.dotCoverPropertyKey());
  }

  private boolean hasVisualStudioCoverageXmlReportPaths() {
    return settings.hasKey(coverageConf.visualStudioCoverageXmlPropertyKey());
  }

  public Coverage aggregate(WildcardPatternFileProvider wildcardPatternFileProvider, Coverage coverage) {
    aggregateIfNeeded(wildcardPatternFileProvider, coverage, coverageConf.ncover3PropertyKey(), ncover3ReportParser);
    aggregateIfNeeded(wildcardPatternFileProvider, coverage, coverageConf.openCoverPropertyKey(), openCoverReportParser);
    aggregateIfNeeded(wildcardPatternFileProvider, coverage, coverageConf.dotCoverPropertyKey(), dotCoverReportsAggregator);
    aggregateIfNeeded(wildcardPatternFileProvider, coverage, coverageConf.visualStudioCoverageXmlPropertyKey(), visualStudioCoverageXmlReportParser);
    return coverage;
  }

  private void aggregateIfNeeded(WildcardPatternFileProvider wildcardPatternFileProvider, Coverage coverage, String toolKey, CoverageParser parser) {
    if (settings.hasKey(toolKey)) {
      boolean useCache = settings.getBoolean(toolKey + ".useCache");
      CoverageParser realParser = useCache ? new CoverageParserCache(parser) : parser;
      aggregate(wildcardPatternFileProvider, settings.getString(toolKey), realParser, coverage);
    }
  }

  private void aggregate(WildcardPatternFileProvider wildcardPatternFileProvider, String reportPaths, CoverageParser parser, Coverage coverage) {
    for (String reportPathPattern : Splitter.on(',').trimResults().omitEmptyStrings().split(reportPaths)) {
      for (File reportFile : wildcardPatternFileProvider.listFiles(reportPathPattern)) {
        parser.parse(reportFile, coverage);
      }
    }
  }

}
