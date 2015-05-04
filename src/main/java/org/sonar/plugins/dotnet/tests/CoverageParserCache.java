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

import com.google.common.collect.Table.Cell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Map;

import com.google.common.collect.Maps;

public class CoverageParserCache implements CoverageParser {

  private static final Logger LOG = LoggerFactory.getLogger(CoverageParserCache.class);
  private final static Map<String, CachedCoverage> cache = Maps.newHashMap();
  private final CoverageParser wrappedParser;

  public CoverageParserCache(CoverageParser wrappedParser) {
    super();
    this.wrappedParser = wrappedParser;
  }

  @Override
  public void parse(File file, Coverage coverage) {
    String path = file.getAbsolutePath();
    CachedCoverage cachedCoverage = cache.get(path);
    if (cachedCoverage == null) {
      cachedCoverage = new CachedCoverage();
      wrappedParser.parse(file, cachedCoverage);
      cache.put(path, cachedCoverage);
      LOG.info("Caching coverage parsing for " + path);
    } else {
      LOG.info("Reusing cached coverage parsing for " + path);
    }
    cachedCoverage.mergeTo(coverage);
  }

  @VisibleForTesting
  static void clear() {
    cache.clear();
  }

  private class CachedCoverage extends Coverage {

    public void mergeTo(Coverage coverage) {
      //coverage.hitsByLineAndFile.putAll(this.hitsByLineAndFile);
      for (Cell<String, Integer, Integer> cell : this.hitsByLineAndFile.cellSet()) {
        coverage.addHits(cell.getRowKey(), cell.getColumnKey(), cell.getValue());
      }
    }
  }
}
