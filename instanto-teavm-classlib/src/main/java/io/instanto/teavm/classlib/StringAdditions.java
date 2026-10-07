/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.teavm.classlib;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/** The bodies of {@code String} methods TeaVM lacks; {@link ClasslibAdditions} calls them. */
public final class StringAdditions {
  private StringAdditions() {}

  /**
   * {@code String.lines()}: the lines separated by {@code \n}, {@code \r} or {@code \r\n}. An empty
   * string has no lines, and a final line terminator does not add an empty line.
   */
  public static Stream<String> lines(String text) {
    List<String> lines = new ArrayList<>();
    int start = 0;
    int length = text.length();
    for (int i = 0; i < length; i++) {
      char c = text.charAt(i);
      if (c == '\n' || c == '\r') {
        lines.add(text.substring(start, i));
        if (c == '\r' && i + 1 < length && text.charAt(i + 1) == '\n') i++;
        start = i + 1;
      }
    }
    if (start < length) lines.add(text.substring(start));
    return lines.stream();
  }
}
