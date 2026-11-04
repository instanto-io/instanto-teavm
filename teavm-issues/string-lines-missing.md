# String.lines() is missing from the class library

**TeaVM version:** 0.16.0, and master as of 2026-11-03 (`6df39ce`).

## Summary

`String.lines()` (Java 11) is not in TeaVM's `TString`, so code that calls it,
directly or through a library, fails to compile with TeaVM. The official Gherkin
parser is one such library.

## Reproducer

```java
@RunWith(TeaVMTestRunner.class)
public class StringLinesTest {
    @Test
    public void splitsOnEveryLineTerminator() {
        assertEquals(List.of("a", "b", "c", "d"), "a\nb\rc\r\nd".lines().toList());
    }

    @Test
    public void aFinalTerminatorAddsNoEmptyLine() {
        assertEquals(List.of("a", "b"), "a\nb\r\n".lines().toList());
    }

    @Test
    public void keepsEmptyLinesBetweenTerminators() {
        assertEquals(List.of("", "a", "", "b"), "\na\n\r\nb".lines().toList());
    }

    @Test
    public void anEmptyStringHasNoLines() {
        assertEquals(List.of(), "".lines().toList());
    }
}
```

The tests pass on the JVM. Under TeaVM the test class does not compile, because
`lines()` cannot be resolved.

## Proposed fix

Add `lines()` to `TString`, following the JDK: lines are separated by `\n`,
`\r` or `\r\n`; an empty string has no lines; a final terminator does not add an
empty line. A straightforward implementation:

```java
public TStream<TString> lines() {
    var result = new ArrayList<TString>();
    int start = 0;
    int length = length();
    for (int i = 0; i < length; ++i) {
        char c = charAt(i);
        if (c == '\n' || c == '\r') {
            result.add(substring(start, i));
            if (c == '\r' && i + 1 < length && charAt(i + 1) == '\n') {
                ++i;
            }
            start = i + 1;
        }
    }
    if (start < length) {
        result.add(substring(start, length));
    }
    return result.stream();
}
```

Until then, `instanto-teavm-classlib` adds the method during compilation:
https://github.com/instanto-io/instanto-teavm/blob/main/instanto-teavm-classlib/src/main/java/io/instanto/teavm/classlib/StringAdditions.java
