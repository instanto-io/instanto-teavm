/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.teavm.classlib;

/**
 * Corrected bodies for {@code Character}'s code-point counting and movement, which {@link
 * ClasslibAdditions} puts in place of TeaVM's. TeaVM 0.16 tests only the first character of an
 * array slice for a surrogate pair, ignores a slice's start when moving through an array, and
 * accepts an invalid starting index when there is nothing to move. These follow the JDK.
 */
public final class CodePoints {
  private CodePoints() {}

  /** {@code Character.codePointCount(char[], int, int)}. */
  public static int count(char[] a, int offset, int count) {
    if (count < 0 || offset < 0 || offset > a.length - count) {
      throw new IndexOutOfBoundsException();
    }
    int end = offset + count;
    int result = count;
    int i = offset;
    while (i < end - 1) {
      if (Character.isHighSurrogate(a[i]) && Character.isLowSurrogate(a[i + 1])) {
        result--;
        i += 2;
      } else {
        i++;
      }
    }
    return result;
  }

  /** {@code Character.offsetByCodePoints(CharSequence, int, int)}. */
  public static int offset(CharSequence seq, int index, int codePointOffset) {
    int length = seq.length();
    if (index < 0 || index > length) throw new IndexOutOfBoundsException();
    return codePointOffset >= 0
        ? offsetForward(seq, index, length, codePointOffset)
        : offsetBackward(seq, index, codePointOffset);
  }

  private static int offsetForward(CharSequence seq, int index, int length, int codePointOffset) {
    int x = index;
    int i = 0;
    for (; x < length && i < codePointOffset; i++) {
      if (Character.isHighSurrogate(seq.charAt(x++))
          && x < length
          && Character.isLowSurrogate(seq.charAt(x))) {
        x++;
      }
    }
    if (i < codePointOffset) throw new IndexOutOfBoundsException();
    return x;
  }

  private static int offsetBackward(CharSequence seq, int index, int codePointOffset) {
    int x = index;
    int i = codePointOffset;
    for (; x > 0 && i < 0; i++) {
      if (Character.isLowSurrogate(seq.charAt(--x))
          && x > 0
          && Character.isHighSurrogate(seq.charAt(x - 1))) {
        x--;
      }
    }
    if (i < 0) throw new IndexOutOfBoundsException();
    return x;
  }

  /** {@code Character.offsetByCodePoints(char[], int, int, int, int)}. */
  public static int offset(char[] a, int start, int count, int index, int codePointOffset) {
    if (count > a.length - start
        || start < 0
        || count < 0
        || index < start
        || index > start + count) {
      throw new IndexOutOfBoundsException();
    }
    int limit = start + count;
    return codePointOffset >= 0
        ? offsetForward(a, index, limit, codePointOffset)
        : offsetBackward(a, index, start, codePointOffset);
  }

  private static int offsetForward(char[] a, int index, int limit, int codePointOffset) {
    int x = index;
    int i = 0;
    for (; x < limit && i < codePointOffset; i++) {
      if (Character.isHighSurrogate(a[x++]) && x < limit && Character.isLowSurrogate(a[x])) {
        x++;
      }
    }
    if (i < codePointOffset) throw new IndexOutOfBoundsException();
    return x;
  }

  private static int offsetBackward(char[] a, int index, int start, int codePointOffset) {
    int x = index;
    int i = codePointOffset;
    for (; x > start && i < 0; i++) {
      if (Character.isLowSurrogate(a[--x]) && x > start && Character.isHighSurrogate(a[x - 1])) {
        x--;
      }
    }
    if (i < 0) throw new IndexOutOfBoundsException();
    return x;
  }
}
