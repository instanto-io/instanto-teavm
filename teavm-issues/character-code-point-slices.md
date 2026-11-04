# Character code-point counting and movement give wrong answers for array slices

**TeaVM version:** 0.16.0, and master as of 2026-11-03 (`6df39ce`).

This is related to #1327, which covers stepping backwards over an unpaired low
surrogate at the lower bound. The three faults below are separate and are not
covered by that report.

## Summary

1. `Character.codePointCount(char[] a, int offset, int count)` tests only the
   slice's first character, `a[offset]`, as the high half of each surrogate pair,
   so it counts pairs wrongly anywhere else in the slice.
2. `Character.offsetByCodePoints(char[] a, int start, int count, int index, int
   codePointOffset)` moving forwards decides whether a surrogate pair fits in the
   slice by comparing `index` with `count - 1` instead of `start + count - 1`. In
   a slice that does not begin at zero, a pair near its end is stepped over as
   two code points.
3. `Character.offsetByCodePoints(CharSequence seq, int index, int
   codePointOffset)` never checks `index`. With `codePointOffset` zero it returns
   an index outside the sequence as given; the JDK throws
   `IndexOutOfBoundsException`.

## Reproducer

```java
@RunWith(TeaVMTestRunner.class)
public class CodePointsTest {
    @Test
    public void countsPairsAtEveryPositionAndWithinSlices() {
        String[] values = {"", "a", "a😀", "😀a", "a😀b😀"};
        int[] expected = {0, 1, 2, 2, 4};
        for (int i = 0; i < values.length; i++) {
            char[] chars = values[i].toCharArray();
            assertEquals(expected[i], Character.codePointCount(chars, 0, chars.length));
            char[] padded = ("x" + values[i] + "y").toCharArray();
            assertEquals(expected[i], Character.codePointCount(padded, 1, chars.length));
        }
    }

    @Test
    public void forwardMovementHonoursSliceStartAndEnd() {
        char[] chars = "x😀y".toCharArray();
        assertEquals(3, Character.offsetByCodePoints(chars, 1, 2, 1, 1));
        assertEquals(2, Character.offsetByCodePoints(chars, 1, 1, 1, 1));
        assertEquals(4, Character.offsetByCodePoints(chars, 1, 3, 1, 2));
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void rejectsANegativeIndexWithoutMovement() {
        Character.offsetByCodePoints("a", -1, 0);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void rejectsAnIndexPastTheEndWithoutMovement() {
        Character.offsetByCodePoints("a", 2, 0);
    }
}
```

The expected values are the JDK's, and the tests pass on the JVM. On TeaVM
0.16.0 they fail on JavaScript and Wasm GC. The full test is at
https://github.com/instanto-io/instanto-teavm/blob/main/instanto-teavm-classlib/src/test/java/io/instanto/teavm/classlib/CodePointsTest.java

## Where the fault lies

In `TCharacter`:

- `codePointCount(char[], int, int)` reads `a[offset]` where it should read
  `a[offset + i]`.
- The array form of `offsetByCodePoints` tests `index < count - 1` before
  pairing surrogates on the way forward; the slice ends at `start + count`.
- The `CharSequence` form has no bounds check on `index`; its loops stop at the
  sequence's ends but never reject a starting index outside them.

## Proposed fix

Follow the JDK's implementations: check the bounds first, then walk the slice
between `start` and `start + count`, pairing a high surrogate with the low
surrogate that follows it inside the slice. Our replacements, applied during
compilation by `instanto-teavm-classlib`, are at
https://github.com/instanto-io/instanto-teavm/blob/main/instanto-teavm-classlib/src/main/java/io/instanto/teavm/classlib/CodePoints.java
