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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.config.Settings;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Project;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CoverageReportImportSensorTest {

    @Test
    public void should_execute_on_project() {
        Settings settings = mock(Settings.class);
        CoverageConfiguration coverageConf = new CoverageConfiguration("", "", "", "", "", "");
        Project project = mock(Project.class);

        CoverageAggregator coverageAggregator = mock(CoverageAggregator.class);

        when(coverageAggregator.hasCoverageProperty()).thenReturn(true);
        assertThat(new CoverageReportImportSensor(settings, coverageConf, mock(FileSystem.class), coverageAggregator).shouldExecuteOnProject(project)).isTrue();

        when(coverageAggregator.hasCoverageProperty()).thenReturn(false);
        assertThat(new CoverageReportImportSensor(settings, coverageConf, mock(FileSystem.class), coverageAggregator).shouldExecuteOnProject(project)).isFalse();
    }

    @Test
    public void analyze() {
        Settings settings = mock(Settings.class);

        Coverage coverage = mock(Coverage.class);
        when(coverage.files()).thenReturn(ImmutableSet.of("Foo.cs", "Bar.cs", "Baz.java"));
        when(coverage.hits("Foo.cs")).thenReturn(ImmutableMap.<Integer, Integer>builder()
                .put(24, 1)
                .put(42, 0)
                .build());
        when(coverage.hits("Bar.cs")).thenReturn(ImmutableMap.<Integer, Integer>builder()
                .put(42, 1)
                .build());
        when(coverage.hits("Baz.java")).thenReturn(ImmutableMap.<Integer, Integer>builder()
                .put(42, 1)
                .build());

        CoverageAggregator coverageAggregator = mock(CoverageAggregator.class);

        SensorContext context = mock(SensorContext.class);

        DefaultFileSystem fs = new DefaultFileSystem();
        InputFile inputFile = new DefaultInputFile("Foo.cs").setAbsolutePath("Foo.cs").setLanguage("cs");
        fs.add(inputFile);
        fs.add(new DefaultInputFile("Baz.java").setAbsolutePath("Baz.java").setLanguage("java"));

        CoverageConfiguration coverageConf = new CoverageConfiguration("cs", "", "", "", "", "");

        new CoverageReportImportSensor(settings, coverageConf, fs, coverageAggregator).analyze(context, coverage);

        verify(coverageAggregator).aggregate(Mockito.any(WildcardPatternFileProvider.class), Mockito.eq(coverage));
        verify(context, Mockito.times(3)).saveMeasure(Mockito.any(InputFile.class), Mockito.any(Measure.class));

        ArgumentCaptor<Measure> captor = ArgumentCaptor.forClass(Measure.class);
        verify(context, Mockito.times(3)).saveMeasure(Mockito.eq(inputFile), captor.capture());

        List<Measure> values = captor.getAllValues();
        checkMeasure(values.get(0), CoreMetrics.LINES_TO_COVER, 2.0);
        checkMeasure(values.get(1), CoreMetrics.UNCOVERED_LINES, 1.0);
    }

    @Test
    public void analyseWithCache() {
        Settings settings = mock(Settings.class);
        when(settings.getBoolean("globalCache")).thenReturn(true);

        Project project = mock(Project.class);

        Coverage coverage = mock(Coverage.class);

        CoverageAggregator coverageAggregator = mock(CoverageAggregator.class);
        SensorContext context = mock(SensorContext.class);
        DefaultFileSystem fs = new DefaultFileSystem();
        InputFile inputFile = new DefaultInputFile("Foo.cs").setAbsolutePath("Foo.cs").setLanguage("cs");
        fs.add(inputFile);
        fs.add(new DefaultInputFile("Foo.cs").setAbsolutePath("Foo.cs").setLanguage("cs"));

        CoverageConfiguration coverageConf = new CoverageConfiguration("cs", "globalCache", "", "", "", "");
        CoverageReportImportSensor.clearCache();
        CoverageReportImportSensor sensor = new CoverageReportImportSensor(settings, coverageConf, fs, coverageAggregator);
        sensor.analyze(context, coverage);
        sensor = new CoverageReportImportSensor(settings, coverageConf, fs, coverageAggregator);
        sensor.analyze(context,coverage);
        verify(coverageAggregator).aggregate(Mockito.any(WildcardPatternFileProvider.class), Mockito.any(Coverage.class));
    }

    private static void checkMeasure(Measure measure, Metric metric, Double value) {
        assertThat(measure.getMetric()).isEqualTo(metric);
        assertThat(measure.getValue()).isEqualTo(value);
    }

}
