/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.teavm.classlib;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;

/**
 * {@code Character.getDirectionality} agrees with the JDK for representative characters. The same
 * assertions run on the JVM, so every expected value is the JDK's.
 */
@RunWith(TeaVMTestRunner.class)
public class CharacterDirectionalityTest {

  @Test
  public void matchesTheJdkForRepresentativeCharacters() {
    assertDirection('A', Character.DIRECTIONALITY_LEFT_TO_RIGHT);
    assertDirection('5', Character.DIRECTIONALITY_EUROPEAN_NUMBER);
    assertDirection(' ', Character.DIRECTIONALITY_WHITESPACE);
    assertDirection('\n', Character.DIRECTIONALITY_PARAGRAPH_SEPARATOR);
    assertDirection('\t', Character.DIRECTIONALITY_SEGMENT_SEPARATOR);
    assertDirection(',', Character.DIRECTIONALITY_COMMON_NUMBER_SEPARATOR);
    assertDirection('+', Character.DIRECTIONALITY_EUROPEAN_NUMBER_SEPARATOR);
    assertDirection('$', Character.DIRECTIONALITY_EUROPEAN_NUMBER_TERMINATOR);
    assertDirection(0x0301, Character.DIRECTIONALITY_NONSPACING_MARK);
    assertDirection(0x202A, Character.DIRECTIONALITY_LEFT_TO_RIGHT_EMBEDDING);
    assertDirection(0x202E, Character.DIRECTIONALITY_RIGHT_TO_LEFT_OVERRIDE);
  }

  @Test
  public void separatesRightToLeftScriptsFromArabic() {
    assertDirection(0x05D0, Character.DIRECTIONALITY_RIGHT_TO_LEFT);
    assertDirection(0x05EA, Character.DIRECTIONALITY_RIGHT_TO_LEFT);
    assertDirection(0x07C0, Character.DIRECTIONALITY_RIGHT_TO_LEFT);
    assertDirection(0xFB1F, Character.DIRECTIONALITY_RIGHT_TO_LEFT);
    assertDirection(0x0627, Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC);
    assertDirection(0x0710, Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC);
    assertDirection(0xFB50, Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC);
    assertDirection(0xFE70, Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC);
  }

  @Test
  public void explicitSeparatorsTakePrecedenceOverScriptBlocks() {
    assertDirection(0x060C, Character.DIRECTIONALITY_COMMON_NUMBER_SEPARATOR);
    assertDirection(0x066A, Character.DIRECTIONALITY_EUROPEAN_NUMBER_TERMINATOR);
    assertDirection(0xFB29, Character.DIRECTIONALITY_EUROPEAN_NUMBER_SEPARATOR);
    assertEquals(Character.DIRECTIONALITY_COMMON_NUMBER_SEPARATOR,
        Character.getDirectionality('\u060C'));
  }

  @Test
  public void classifiesArabicDigits() {
    assertDirection(0x0660, Character.DIRECTIONALITY_ARABIC_NUMBER);
    assertDirection(0x06F5, Character.DIRECTIONALITY_EUROPEAN_NUMBER);
  }

  @Test
  public void theCharOverloadAgreesWithTheCodePoint() {
    assertEquals(Character.DIRECTIONALITY_RIGHT_TO_LEFT, Character.getDirectionality('א'));
    assertEquals(Character.DIRECTIONALITY_LEFT_TO_RIGHT, Character.getDirectionality('z'));
  }

  @Test
  public void anInvalidCodePointIsUndefined() {
    assertDirection(-1, Character.DIRECTIONALITY_UNDEFINED);
    assertDirection(0x110000, Character.DIRECTIONALITY_UNDEFINED);
  }

  private static void assertDirection(int codePoint, byte expected) {
    assertEquals(
        "codePoint " + Integer.toHexString(codePoint),
        expected,
        Character.getDirectionality(codePoint));
  }
}
