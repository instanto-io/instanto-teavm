/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.teavm.classlib;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;

/**
 * {@code Character}'s code-point counting and movement agree with the JDK, which the same
 * assertions check on the JVM. Ported from cucumber-tea, whose Gherkin parser depends on them; the
 * title-case and {@code String} checks are correct in TeaVM 0.16 and guard against regressions.
 */
@RunWith(TeaVMTestRunner.class)
public class CodePointsTest {

  @Test
  public void countsPairsAtEveryPositionAndWithinSlices() {
    String[] values = {
      "", "a", "a😀", "😀a", "a😀b😀", "\uD800a\uDC00"
    };
    int[] expected = {0, 1, 2, 2, 4, 3};
    for (int i = 0; i < values.length; i++) {
      char[] chars = values[i].toCharArray();
      assertEquals(expected[i], Character.codePointCount(chars, 0, chars.length));
      char[] padded = ("x" + values[i] + "y").toCharArray();
      assertEquals(expected[i], Character.codePointCount(padded, 1, chars.length));
    }
  }

  @Test
  public void countingRejectsASliceOutsideTheArray() {
    assertOutOfBounds(() -> Character.codePointCount(new char[2], 1, 2));
    assertOutOfBounds(() -> Character.codePointCount(new char[2], -1, 1));
  }

  @Test
  public void forwardMovementHonoursSliceStartAndEnd() {
    char[] chars = "x😀y".toCharArray();
    assertEquals(3, Character.offsetByCodePoints(chars, 1, 2, 1, 1));
    assertEquals(2, Character.offsetByCodePoints(chars, 1, 1, 1, 1));
    assertEquals(4, Character.offsetByCodePoints(chars, 1, 3, 1, 2));
  }

  @Test
  public void backwardMovementHandlesLoneSurrogatesAndSliceBoundaries() {
    assertEquals(0, Character.offsetByCodePoints("\uDC00", 1, -1));
    assertEquals(0, Character.offsetByCodePoints(new char[] {'\uDC00'}, 0, 1, 1, -1));
    char[] pair = "😀".toCharArray();
    assertEquals(1, Character.offsetByCodePoints(pair, 1, 1, 2, -1));
    assertEquals(0, Character.offsetByCodePoints(pair, 0, 2, 2, -1));
    assertEquals(0, Character.offsetByCodePoints("😀", 2, -1));
  }

  @Test
  public void validatesStartingIndicesEvenWithoutMovement() {
    assertOutOfBounds(() -> Character.offsetByCodePoints("a", -1, 0));
    assertOutOfBounds(() -> Character.offsetByCodePoints("a", 2, 0));
    assertEquals(0, Character.offsetByCodePoints("a", 0, 0));
    assertEquals(1, Character.offsetByCodePoints("a", 1, 0));
  }

  @Test
  public void movingPastTheEndIsOutOfBounds() {
    assertOutOfBounds(() -> Character.offsetByCodePoints("ab", 0, 3));
    assertOutOfBounds(() -> Character.offsetByCodePoints("ab".toCharArray(), 0, 2, 2, -3));
  }

  @Test
  public void titleCasePreservesDedicatedMappings() {
    assertEquals(0x01F2, Character.toTitleCase(0x01F3));
    assertEquals(0x01F2, Character.toTitleCase(0x01F2));
    assertEquals(0x01F2, Character.toTitleCase(0x01F1));
    assertEquals('A', Character.toTitleCase('a'));
    assertEquals('A', Character.toTitleCase('A'));
    assertEquals('1', Character.toTitleCase('1'));
    assertEquals(-1, Character.toTitleCase(-1));
    assertEquals(0x110000, Character.toTitleCase(0x110000));
    assertEquals(0x10400, Character.toTitleCase(0x10428));
  }

  @Test
  public void stringReplacementAndIdentity() {
    String text = new String(new char[] {'a', 'b', 'a'});
    CharSequence same = "a";
    assertSame(text, text.replace(same, same));
    assertEquals("xbx", text.replace((CharSequence) "a", (CharSequence) "x"));
    assertEquals("-a-b-a-", text.replace((CharSequence) "", (CharSequence) "-"));
    assertEquals("za", text.replace((CharSequence) "ab", (CharSequence) "z"));
    assertSame(text, text.toString());
    assertEquals("value: 7", "%s: %d".formatted("value", 7));
  }

  private static void assertOutOfBounds(Runnable call) {
    try {
      call.run();
      fail("Expected IndexOutOfBoundsException");
    } catch (IndexOutOfBoundsException expected) {
      // Required by the Character API.
    }
  }
}
