/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.teavm.classlib;

/**
 * The body of {@code Character.getDirectionality}, which TeaVM lacks; {@link ClasslibAdditions}
 * calls it.
 *
 * <p>The Unicode bidirectional class is a property in its own right, and TeaVM ships no table for
 * it. This derives the classes that separate right-to-left text from left-to-right, which is what
 * callers use it for, from the blocks that carry them and from {@link Character#getType(int)}.
 * Weak and neutral classes within a script are approximated by category.
 */
public final class Directionality {
  private Directionality() {}

  public static byte of(char ch) {
    return of((int) ch);
  }

  public static byte of(int codePoint) {
    if (!Character.isValidCodePoint(codePoint)) return Character.DIRECTIONALITY_UNDEFINED;
    // Both digit sets sit inside the Arabic block and are classified before it.
    if (codePoint >= 0x0660 && codePoint <= 0x0669 || codePoint >= 0x066C && codePoint <= 0x066D) {
      return Character.DIRECTIONALITY_ARABIC_NUMBER;
    }
    if (codePoint >= 0x06F0 && codePoint <= 0x06F9) return Character.DIRECTIONALITY_EUROPEAN_NUMBER;
    if (isArabic(codePoint)) return Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC;
    if (isRightToLeft(codePoint)) return Character.DIRECTIONALITY_RIGHT_TO_LEFT;
    switch (codePoint) {
      case 0x000A, 0x000D, 0x001C, 0x001D, 0x001E, 0x0085, 0x2029:
        return Character.DIRECTIONALITY_PARAGRAPH_SEPARATOR;
      case 0x0009, 0x000B, 0x001F:
        return Character.DIRECTIONALITY_SEGMENT_SEPARATOR;
      case 0x002C, 0x003A, 0x00A0, 0x202F, 0x2044, 0xFE50, 0xFE52, 0xFE55, 0xFF0C, 0xFF1A:
        return Character.DIRECTIONALITY_COMMON_NUMBER_SEPARATOR;
      case 0x002B, 0x002D, 0x207A, 0x207B, 0x208A, 0x208B, 0x2212, 0xFB29, 0xFE62, 0xFE63, 0xFF0B,
          0xFF0D:
        return Character.DIRECTIONALITY_EUROPEAN_NUMBER_SEPARATOR;
      case 0x0023, 0x0024, 0x0025, 0x00A2, 0x00A3, 0x00A4, 0x00A5, 0x00B0, 0x00B1, 0x066A, 0x09F2,
          0x09F3, 0x2030, 0x2031, 0x212E:
        return Character.DIRECTIONALITY_EUROPEAN_NUMBER_TERMINATOR;
      case 0x202A:
        return Character.DIRECTIONALITY_LEFT_TO_RIGHT_EMBEDDING;
      case 0x202B:
        return Character.DIRECTIONALITY_RIGHT_TO_LEFT_EMBEDDING;
      case 0x202C:
        return Character.DIRECTIONALITY_POP_DIRECTIONAL_FORMAT;
      case 0x202D:
        return Character.DIRECTIONALITY_LEFT_TO_RIGHT_OVERRIDE;
      case 0x202E:
        return Character.DIRECTIONALITY_RIGHT_TO_LEFT_OVERRIDE;
      default:
        break;
    }
    switch (Character.getType(codePoint)) {
      case Character.DECIMAL_DIGIT_NUMBER:
        return Character.DIRECTIONALITY_EUROPEAN_NUMBER;
      case Character.NON_SPACING_MARK, Character.ENCLOSING_MARK, Character.COMBINING_SPACING_MARK:
        return Character.DIRECTIONALITY_NONSPACING_MARK;
      case Character.SPACE_SEPARATOR:
        return Character.DIRECTIONALITY_WHITESPACE;
      case Character.CONTROL, Character.FORMAT, Character.SURROGATE, Character.PRIVATE_USE:
        return Character.DIRECTIONALITY_BOUNDARY_NEUTRAL;
      case Character.UNASSIGNED:
        return Character.DIRECTIONALITY_UNDEFINED;
      case Character.UPPERCASE_LETTER,
          Character.LOWERCASE_LETTER,
          Character.TITLECASE_LETTER,
          Character.MODIFIER_LETTER,
          Character.OTHER_LETTER,
          Character.LETTER_NUMBER:
        return Character.DIRECTIONALITY_LEFT_TO_RIGHT;
      default:
        return Character.DIRECTIONALITY_OTHER_NEUTRALS;
    }
  }

  /** Arabic, Syriac, Thaana and the Arabic presentation forms carry the Arabic bidi class. */
  private static boolean isArabic(int codePoint) {
    return codePoint >= 0x0600 && codePoint <= 0x07BF
        || codePoint >= 0x0860 && codePoint <= 0x08FF
        || codePoint >= 0xFB50 && codePoint <= 0xFDFF
        || codePoint >= 0xFE70 && codePoint <= 0xFEFF
        || codePoint >= 0x10D00 && codePoint <= 0x10D3F
        || codePoint >= 0x1EC70 && codePoint <= 0x1ECBF
        || codePoint >= 0x1ED00 && codePoint <= 0x1ED4F
        || codePoint >= 0x1EE00 && codePoint <= 0x1EEFF;
  }

  /** Hebrew and the other strong right-to-left scripts. */
  private static boolean isRightToLeft(int codePoint) {
    return codePoint >= 0x0590 && codePoint <= 0x05FF
        || codePoint >= 0x07C0 && codePoint <= 0x085F
        || codePoint >= 0xFB1D && codePoint <= 0xFB4F
        || codePoint >= 0x10800 && codePoint <= 0x10CFF
        || codePoint >= 0x10E80 && codePoint <= 0x10FFF
        || codePoint >= 0x1E800 && codePoint <= 0x1EDFF
        || codePoint >= 0x1EF00 && codePoint <= 0x1EFFF;
  }
}
