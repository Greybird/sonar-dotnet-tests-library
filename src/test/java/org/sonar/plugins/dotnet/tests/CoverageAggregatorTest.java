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

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.collect.ImmutableSet;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.sonar.api.config.Settings;

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.never;

public class CoverageAggregatorTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void hasCoverageProperty() {
    Settings settings = mock(Settings.class);

    CoverageConfiguration coverageConf = new CoverageConfiguration("", "ncover", "opencover", "dotcover", "visualstudio");

    when(settings.hasKey("ncover")).thenReturn(false);
    when(settings.hasKey("opencover")).thenReturn(false);
    when(settings.hasKey("dotcover")).thenReturn(false);
    when(settings.hasKey("visualstudio")).thenReturn(false);
    assertThat(new CoverageAggregator(coverageConf, settings).hasCoverageProperty()).isFalse();

    when(settings.hasKey("ncover")).thenReturn(false);
    when(settings.hasKey("opencover")).thenReturn(true);
    when(settings.hasKey("dotcover")).thenReturn(false);
    when(settings.hasKey("visualstudio")).thenReturn(false);
    assertThat(new CoverageAggregator(coverageConf, settings).hasCoverageProperty()).isTrue();

    when(settings.hasKey("ncover")).thenReturn(false);
    when(settings.hasKey("opencover")).thenReturn(false);
    when(settings.hasKey("dotcover")).thenReturn(true);
    when(settings.hasKey("visualstudio")).thenReturn(false);
    assertThat(new CoverageAggregator(coverageConf, settings).hasCoverageProperty()).isTrue();

    when(settings.hasKey("ncover")).thenReturn(false);
    when(settings.hasKey("opencover")).thenReturn(false);
    when(settings.hasKey("dotcover")).thenReturn(false);
    when(settings.hasKey("visualstudio")).thenReturn(true);
    assertThat(new CoverageAggregator(coverageConf, settings).hasCoverageProperty()).isTrue();

    when(settings.hasKey("ncover")).thenReturn(true);
    when(settings.hasKey("opencover")).thenReturn(true);
    when(settings.hasKey("dotcover")).thenReturn(true);
    when(settings.hasKey("visualstudio")).thenReturn(true);
    assertThat(new CoverageAggregator(coverageConf, settings).hasCoverageProperty()).isTrue();

    coverageConf = new CoverageConfiguration("", "ncover2", "opencover2", "dotcover2", "visualstudio2");
    when(settings.hasKey("ncover")).thenReturn(true);
    when(settings.hasKey("opencover")).thenReturn(true);
    when(settings.hasKey("dotcover")).thenReturn(true);
    when(settings.hasKey("visualstudio")).thenReturn(true);
    assertThat(new CoverageAggregator(coverageConf, settings).hasCoverageProperty()).isFalse();
  }

  @Test
  public void aggregate() {
    WildcardPatternFileProvider wildcardPatternFileProvider = mock(WildcardPatternFileProvider.class);

    CoverageConfiguration coverageConf = new CoverageConfiguration("", "ncover", "opencover", "dotcover", "visualstudio");
    Settings settings = mock(Settings.class);

    when(settings.hasKey("ncover")).thenReturn(true);
    when(settings.getString("ncover")).thenReturn("foo.nccov");
    when(wildcardPatternFileProvider.listFiles("foo.nccov")).thenReturn(ImmutableSet.of(new File("foo.nccov")));
    when(settings.hasKey("opencover")).thenReturn(false);
    when(settings.hasKey("dotcover")).thenReturn(false);
    when(settings.hasKey("visualstudio")).thenReturn(false);
    NCover3ReportParser ncoverParser = mock(NCover3ReportParser.class);
    OpenCoverReportParser openCoverParser = mock(OpenCoverReportParser.class);
    DotCoverReportsAggregator dotCoverParser = mock(DotCoverReportsAggregator.class);
    VisualStudioCoverageXmlReportParser visualStudioCoverageXmlReportParser = mock(VisualStudioCoverageXmlReportParser.class);
    Coverage coverage = mock(Coverage.class);
    new CoverageAggregator(coverageConf, settings, ncoverParser, openCoverParser, dotCoverParser, visualStudioCoverageXmlReportParser)
      .aggregate(wildcardPatternFileProvider, coverage);
    verify(ncoverParser).parse(new File("foo.nccov"), coverage);
    verify(openCoverParser, never()).parse(any(File.class), any(Coverage.class));
    verify(dotCoverParser, never()).parse(any(File.class), any(Coverage.class));
    verify(visualStudioCoverageXmlReportParser, never()).parse(any(File.class), any(Coverage.class));

    when(settings.hasKey("ncover")).thenReturn(false);
    when(settings.hasKey("opencover")).thenReturn(true);
    when(settings.getString("opencover")).thenReturn("bar.xml");
    when(wildcardPatternFileProvider.listFiles("bar.xml")).thenReturn(ImmutableSet.of(new File("bar.xml")));
    when(settings.hasKey("dotcover")).thenReturn(false);
    when(settings.hasKey("visualstudio")).thenReturn(false);
    ncoverParser = mock(NCover3ReportParser.class);
    openCoverParser = mock(OpenCoverReportParser.class);
    dotCoverParser = mock(DotCoverReportsAggregator.class);
    visualStudioCoverageXmlReportParser = mock(VisualStudioCoverageXmlReportParser.class);
    coverage = mock(Coverage.class);
    new CoverageAggregator(coverageConf, settings, ncoverParser, openCoverParser, dotCoverParser, visualStudioCoverageXmlReportParser)
      .aggregate(wildcardPatternFileProvider, coverage);
    verify(ncoverParser, never()).parse(any(File.class), any(Coverage.class));
    verify(openCoverParser).parse(new File("bar.xml"), coverage);
    verify(dotCoverParser, never()).parse(any(File.class), any(Coverage.class));
    verify(visualStudioCoverageXmlReportParser, never()).parse(any(File.class), any(Coverage.class));

    when(settings.hasKey("ncover")).thenReturn(false);
    when(settings.hasKey("opencover")).thenReturn(false);
    when(settings.hasKey("dotcover")).thenReturn(true);
    when(settings.getString("dotcover")).thenReturn("baz.html");
    when(wildcardPatternFileProvider.listFiles("baz.html")).thenReturn(ImmutableSet.of(new File("baz.html")));
    when(settings.hasKey("visualstudio")).thenReturn(false);
    ncoverParser = mock(NCover3ReportParser.class);
    openCoverParser = mock(OpenCoverReportParser.class);
    dotCoverParser = mock(DotCoverReportsAggregator.class);
    visualStudioCoverageXmlReportParser = mock(VisualStudioCoverageXmlReportParser.class);
    coverage = mock(Coverage.class);
    new CoverageAggregator(coverageConf, settings, ncoverParser, openCoverParser, dotCoverParser, visualStudioCoverageXmlReportParser)
      .aggregate(wildcardPatternFileProvider, coverage);
    verify(ncoverParser, never()).parse(any(File.class), any(Coverage.class));
    verify(openCoverParser, never()).parse(any(File.class), any(Coverage.class));
    verify(dotCoverParser).parse(new File("baz.html"), coverage);
    verify(visualStudioCoverageXmlReportParser, never()).parse(any(File.class), any(Coverage.class));

    when(settings.hasKey("ncover")).thenReturn(false);
    when(settings.hasKey("opencover")).thenReturn(false);
    when(settings.hasKey("dotcover")).thenReturn(false);
    when(settings.hasKey("visualstudio")).thenReturn(true);
    when(settings.getString("visualstudio")).thenReturn("qux.coveragexml");
    when(wildcardPatternFileProvider.listFiles("qux.coveragexml")).thenReturn(ImmutableSet.of(new File("qux.coveragexml")));
    ncoverParser = mock(NCover3ReportParser.class);
    openCoverParser = mock(OpenCoverReportParser.class);
    dotCoverParser = mock(DotCoverReportsAggregator.class);
    visualStudioCoverageXmlReportParser = mock(VisualStudioCoverageXmlReportParser.class);
    coverage = mock(Coverage.class);
    new CoverageAggregator(coverageConf, settings, ncoverParser, openCoverParser, dotCoverParser, visualStudioCoverageXmlReportParser)
      .aggregate(wildcardPatternFileProvider, coverage);
    verify(ncoverParser, never()).parse(any(File.class), any(Coverage.class));
    verify(openCoverParser, never()).parse(any(File.class), any(Coverage.class));
    verify(dotCoverParser, never()).parse(any(File.class), any(Coverage.class));
    verify(visualStudioCoverageXmlReportParser).parse(new File("qux.coveragexml"), coverage);

    Mockito.reset(wildcardPatternFileProvider);

    when(settings.hasKey("ncover")).thenReturn(true);
    when(settings.getString("ncover")).thenReturn(",*.nccov  ,bar.nccov");
    when(wildcardPatternFileProvider.listFiles("*.nccov")).thenReturn(ImmutableSet.of(new File("foo.nccov")));
    when(wildcardPatternFileProvider.listFiles("bar.nccov")).thenReturn(ImmutableSet.of(new File("bar.nccov")));
    when(settings.hasKey("opencover")).thenReturn(true);
    when(settings.getString("opencover")).thenReturn("bar.xml");
    when(wildcardPatternFileProvider.listFiles("bar.xml")).thenReturn(ImmutableSet.of(new File("bar.xml")));
    when(settings.hasKey("dotcover")).thenReturn(true);
    when(settings.getString("dotcover")).thenReturn("baz.html");
    when(wildcardPatternFileProvider.listFiles("baz.html")).thenReturn(ImmutableSet.of(new File("baz.html")));
    when(settings.hasKey("visualstudio")).thenReturn(true);
    when(settings.getString("visualstudio")).thenReturn("qux.coveragexml");
    when(wildcardPatternFileProvider.listFiles("qux.coveragexml")).thenReturn(ImmutableSet.of(new File("qux.coveragexml")));
    ncoverParser = mock(NCover3ReportParser.class);
    openCoverParser = mock(OpenCoverReportParser.class);
    dotCoverParser = mock(DotCoverReportsAggregator.class);
    visualStudioCoverageXmlReportParser = mock(VisualStudioCoverageXmlReportParser.class);
    coverage = mock(Coverage.class);

    new CoverageAggregator(coverageConf, settings, ncoverParser, openCoverParser, dotCoverParser, visualStudioCoverageXmlReportParser)
      .aggregate(wildcardPatternFileProvider, coverage);

    verify(wildcardPatternFileProvider).listFiles("*.nccov");
    verify(wildcardPatternFileProvider).listFiles("bar.nccov");
    verify(wildcardPatternFileProvider).listFiles("bar.xml");
    verify(wildcardPatternFileProvider).listFiles("baz.html");
    verify(wildcardPatternFileProvider).listFiles("qux.coveragexml");

    verify(ncoverParser).parse(new File("foo.nccov"), coverage);
    verify(ncoverParser).parse(new File("bar.nccov"), coverage);
    verify(openCoverParser).parse(new File("bar.xml"), coverage);
    verify(dotCoverParser).parse(new File("baz.html"), coverage);
    verify(visualStudioCoverageXmlReportParser).parse(new File("qux.coveragexml"), coverage);
  }

  @Test
  public void aggregateWithCache() {
    CoverageParserCache.clear();
    WildcardPatternFileProvider wildcardPatternFileProvider = mock(WildcardPatternFileProvider.class);

    CoverageConfiguration coverageConf = new CoverageConfiguration("", "ncover", "opencover", "dotcover", "visualstudio");
    Settings settings = mock(Settings.class);

    when(settings.hasKey("ncover")).thenReturn(true);
    when(settings.getString("ncover")).thenReturn("foo.nccov");
    when(settings.getBoolean("ncover.useCache")).thenReturn(true);
    when(wildcardPatternFileProvider.listFiles("foo.nccov")).thenReturn(ImmutableSet.of(new File("foo.nccov")));
    when(settings.hasKey("opencover")).thenReturn(false);
    when(settings.hasKey("dotcover")).thenReturn(false);
    when(settings.hasKey("visualstudio")).thenReturn(false);
    NCover3ReportParser ncoverParser = mock(NCover3ReportParser.class);
    OpenCoverReportParser openCoverParser = mock(OpenCoverReportParser.class);
    DotCoverReportsAggregator dotCoverParser = mock(DotCoverReportsAggregator.class);
    VisualStudioCoverageXmlReportParser visualStudioCoverageXmlReportParser = mock(VisualStudioCoverageXmlReportParser.class);
    Coverage coverage = new Coverage();
    for(int i = 0 ; i < 2 ; i++) {
      new CoverageAggregator(coverageConf, settings, ncoverParser, openCoverParser, dotCoverParser, visualStudioCoverageXmlReportParser)
      .aggregate(wildcardPatternFileProvider, coverage);
    }
    verify(ncoverParser).parse(eq(new File("foo.nccov")), any(Coverage.class));
    verify(openCoverParser, never()).parse(any(File.class), any(Coverage.class));
    verify(dotCoverParser, never()).parse(any(File.class), any(Coverage.class));
    verify(visualStudioCoverageXmlReportParser, never()).parse(any(File.class), any(Coverage.class));

    when(settings.hasKey("ncover")).thenReturn(false);
    when(settings.hasKey("opencover")).thenReturn(true);
    when(settings.getString("opencover")).thenReturn("bar.xml");
    when(settings.getBoolean("opencover.useCache")).thenReturn(true);
    when(wildcardPatternFileProvider.listFiles("bar.xml")).thenReturn(ImmutableSet.of(new File("bar.xml")));
    when(settings.hasKey("dotcover")).thenReturn(false);
    when(settings.hasKey("visualstudio")).thenReturn(false);
    ncoverParser = mock(NCover3ReportParser.class);
    openCoverParser = mock(OpenCoverReportParser.class);
    dotCoverParser = mock(DotCoverReportsAggregator.class);
    visualStudioCoverageXmlReportParser = mock(VisualStudioCoverageXmlReportParser.class);
    coverage = new Coverage();
    for(int i = 0 ; i < 2 ; i++) {
      new CoverageAggregator(coverageConf, settings, ncoverParser, openCoverParser, dotCoverParser, visualStudioCoverageXmlReportParser)
        .aggregate(wildcardPatternFileProvider, coverage);
    }
    verify(ncoverParser, never()).parse(any(File.class), any(Coverage.class));
    verify(openCoverParser).parse(eq(new File("bar.xml")), any(Coverage.class));
    verify(dotCoverParser, never()).parse(any(File.class), any(Coverage.class));
    verify(visualStudioCoverageXmlReportParser, never()).parse(any(File.class), any(Coverage.class));

    when(settings.hasKey("ncover")).thenReturn(false);
    when(settings.hasKey("opencover")).thenReturn(false);
    when(settings.hasKey("dotcover")).thenReturn(true);
    when(settings.getBoolean("dotcover.useCache")).thenReturn(true);
    when(settings.getString("dotcover")).thenReturn("baz.html");
    when(wildcardPatternFileProvider.listFiles("baz.html")).thenReturn(ImmutableSet.of(new File("baz.html")));
    when(settings.hasKey("visualstudio")).thenReturn(false);
    ncoverParser = mock(NCover3ReportParser.class);
    openCoverParser = mock(OpenCoverReportParser.class);
    dotCoverParser = mock(DotCoverReportsAggregator.class);
    visualStudioCoverageXmlReportParser = mock(VisualStudioCoverageXmlReportParser.class);
    coverage = new Coverage();
    for(int i = 0 ; i < 2 ; i++) {
      new CoverageAggregator(coverageConf, settings, ncoverParser, openCoverParser, dotCoverParser, visualStudioCoverageXmlReportParser)
        .aggregate(wildcardPatternFileProvider, coverage);
    }
    verify(ncoverParser, never()).parse(any(File.class), any(Coverage.class));
    verify(openCoverParser, never()).parse(any(File.class), any(Coverage.class));
    verify(dotCoverParser).parse(eq(new File("baz.html")), any(Coverage.class));
    verify(visualStudioCoverageXmlReportParser, never()).parse(any(File.class), any(Coverage.class));

    when(settings.hasKey("ncover")).thenReturn(false);
    when(settings.hasKey("opencover")).thenReturn(false);
    when(settings.hasKey("dotcover")).thenReturn(false);
    when(settings.hasKey("visualstudio")).thenReturn(true);
    when(settings.getString("visualstudio")).thenReturn("qux.coveragexml");
    when(settings.getBoolean("visualstudio.useCache")).thenReturn(true);
    when(wildcardPatternFileProvider.listFiles("qux.coveragexml")).thenReturn(ImmutableSet.of(new File("qux.coveragexml")));
    ncoverParser = mock(NCover3ReportParser.class);
    openCoverParser = mock(OpenCoverReportParser.class);
    dotCoverParser = mock(DotCoverReportsAggregator.class);
    visualStudioCoverageXmlReportParser = mock(VisualStudioCoverageXmlReportParser.class);
    coverage = new Coverage();
    for(int i = 0 ; i < 2 ; i++) {
      new CoverageAggregator(coverageConf, settings, ncoverParser, openCoverParser, dotCoverParser, visualStudioCoverageXmlReportParser)
        .aggregate(wildcardPatternFileProvider, coverage);
    }
    verify(ncoverParser, never()).parse(any(File.class), any(Coverage.class));
    verify(openCoverParser, never()).parse(any(File.class), any(Coverage.class));
    verify(dotCoverParser, never()).parse(any(File.class), any(Coverage.class));
    verify(visualStudioCoverageXmlReportParser).parse(eq(new File("qux.coveragexml")), any(Coverage.class));
  }

  @Test
  public void cachedCoverageIsCorrectlyAggregated() {
    CoverageParserCache.clear();
    WildcardPatternFileProvider wildcardPatternFileProvider = mock(WildcardPatternFileProvider.class);

    CoverageConfiguration coverageConf = new CoverageConfiguration("", "ncover", "opencover", "dotcover", "visualstudio");
    Settings settings = mock(Settings.class);

    when(settings.hasKey("ncover")).thenReturn(false);
    when(settings.hasKey("opencover")).thenReturn(true);
    when(settings.getString("opencover")).thenReturn("*.xml");
    when(settings.getBoolean("opencover.useCache")).thenReturn(true);
    when(wildcardPatternFileProvider.listFiles("*.xml")).thenReturn(ImmutableSet.of(new File("bar.xml"), new File("bar2.xml")));
    when(settings.hasKey("dotcover")).thenReturn(false);
    when(settings.hasKey("visualstudio")).thenReturn(false);

    NCover3ReportParser ncoverParser = mock(NCover3ReportParser.class);
    OpenCoverReportParser openCoverParser = mock(OpenCoverReportParser.class);
    DotCoverReportsAggregator dotCoverParser = mock(DotCoverReportsAggregator.class);
    VisualStudioCoverageXmlReportParser visualStudioCoverageXmlReportParser = mock(VisualStudioCoverageXmlReportParser.class);
    Coverage coverage = new Coverage();

    Mockito.doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        Coverage coverage = (Coverage) invocation.getArguments()[1];
        coverage.addHits("A", 1, 1);
        coverage.addHits("A", 2, 2);
        return null;
      } }).when(openCoverParser).parse(eq(new File("bar2.xml")), any(Coverage.class));
    Mockito.doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        Coverage coverage = (Coverage) invocation.getArguments()[1];
        coverage.addHits("A", 2, 1);
        coverage.addHits("B", 1, 4);
        return null;
      } }).when(openCoverParser).parse(eq(new File("bar.xml")), any(Coverage.class));

    new CoverageAggregator(coverageConf, settings, ncoverParser, openCoverParser, dotCoverParser, visualStudioCoverageXmlReportParser)
    .aggregate(wildcardPatternFileProvider, coverage);

    assertThat(coverage.hits("A").get(1)).isEqualTo(1);
    assertThat(coverage.hits("A").get(2)).isEqualTo(3);
    assertThat(coverage.hits("B").get(1)).isEqualTo(4);
  }


}
