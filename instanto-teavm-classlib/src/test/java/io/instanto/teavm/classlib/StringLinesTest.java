/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.teavm.classlib;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;

/** {@code String.lines()} behaves as the JDK's under TeaVM; on the JVM it is the JDK's own. */
@RunWith(TeaVMTestRunner.class)
public class StringLinesTest {

  @Test
  public void splitsOnEveryLineTerminator() {
    assertEquals(List.of("a", "b", "c", "d"), lines("a\nb\rc\r\nd"));
  }

  @Test
  public void aFinalTerminatorAddsNoEmptyLine() {
    assertEquals(List.of("a", "b"), lines("a\nb\n"));
    assertEquals(List.of("a", "b"), lines("a\nb\r\n"));
  }

  @Test
  public void keepsEmptyLinesBetweenTerminators() {
    assertEquals(List.of("", "a", "", "b"), lines("\na\n\r\nb"));
  }

  @Test
  public void anEmptyStringHasNoLines() {
    assertEquals(List.of(), lines(""));
  }

  @Test
  public void aStringWithoutTerminatorsIsOneLine() {
    assertEquals(List.of("text"), lines("text"));
  }

  private static List<String> lines(String text) {
    return text.lines().collect(Collectors.toList());
  }
}
