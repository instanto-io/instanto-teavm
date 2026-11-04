# TeaVM issues

Faults in TeaVM that this repository works around or fixes, with their state
upstream. A draft here has a Java reproducer, where the fault lies and a proposed
fix, ready to file at https://github.com/konsoletyper/teavm/issues. Checked
against TeaVM master `6df39ce` (2026-11-03).

When an issue is filed, add its number here. When a TeaVM release includes a
fix, move it to the last table and remove our workaround once the release is
adopted.

## Not yet addressed upstream

| Fault | Where we fix it | Upstream | Draft |
| --- | --- | --- | --- |
| `@JSTopLevel` calls, such as `Window.setTimeout`, become calls on `null` in optimised JavaScript | `instanto-teavm-jso-patch` | Not reported | [jstoplevel-null-receiver.md](jstoplevel-null-receiver.md) |
| Java callbacks in arrays and varargs reach JavaScript as objects, not functions, and back | `instanto-teavm-jso-patch` | Not reported; related to [#504](https://github.com/konsoletyper/teavm/issues/504) | [functor-arrays-not-converted.md](functor-arrays-not-converted.md) |
| Virtual calls select subclass methods that do not override across package boundaries | `instanto-teavm-core-patch` | Not reported | [virtual-dispatch-across-packages.md](virtual-dispatch-across-packages.md) |
| `String.lines()` is missing | `instanto-teavm-classlib` | Not reported | [string-lines-missing.md](string-lines-missing.md) |
| `codePointCount` and `offsetByCodePoints` give wrong answers for array slices and unchecked indices | `instanto-teavm-classlib` | Not reported; related to [#1327](https://github.com/konsoletyper/teavm/issues/1327) | [character-code-point-slices.md](character-code-point-slices.md) |
| `offsetByCodePoints` mishandles an unpaired low surrogate at the lower bound | `instanto-teavm-classlib` | [#1327](https://github.com/konsoletyper/teavm/issues/1327), open | Filed |
| `ConcurrentHashMap.computeIfAbsent` and `computeIfPresent` keep a mapping when the function returns null | `instanto-teavm-classlib` | [#1328](https://github.com/konsoletyper/teavm/issues/1328), open | Filed |
| `Character.getDirectionality` is missing | `instanto-teavm-classlib`, an approximation | Requested in [#1098](https://github.com/konsoletyper/teavm/issues/1098), open | None: a feature request exists |
| JUnit `@Rule` fields and methods are ignored in compiled tests | `instanto-teavm-extensions` | [PR #1253](https://github.com/konsoletyper/teavm/pull/1253) declined in that form; the maintainer suggested a class transformer | None |

## Fixed only as proposals

Tested patches under [`upstream-fixes`](../upstream-fixes), not packaged for
consumers.

| Fault | Upstream | Patch |
| --- | --- | --- |
| An optimised field read in a catch handler returns a value from the skipped try path | [#1329](https://github.com/konsoletyper/teavm/issues/1329), open | [field.patch](../upstream-fixes/patches/field.patch) |
| The minifier lets a generated JSBody parameter name capture an identifier in the script | Not reported | [minifier.patch](../upstream-fixes/patches/minifier.patch) |
| An invalid Wasm GC local after a promise suspends | Not reported | [wasm.patch](../upstream-fixes/patches/wasm.patch) |

## Fixed upstream, awaiting a release

| Fault | Upstream | Our workaround, to remove on adoption |
| --- | --- | --- |
| Class initialisation removed from exception handlers | [PR #1289](https://github.com/konsoletyper/teavm/pull/1289), merged 2026-10-10 | `ClassInitElimination` in `instanto-teavm-core-patch` |
| Inherited or overridden `@Before` and `@After` run more than once | [PR #1262](https://github.com/konsoletyper/teavm/pull/1262), merged by hand as [4cb1b19](https://github.com/konsoletyper/teavm/commit/4cb1b19bff29770e2df6ced960c293dc3db1a1a5) | Lifecycle backport in `instanto-teavm-extensions` |

## Not TeaVM faults

| Behaviour | Upstream | Here |
| --- | --- | --- |
| A JavaScript callback can see the `ThreadLocal` values of a thread that is suspended | [#1271](https://github.com/konsoletyper/teavm/issues/1271): expected, thread state is undefined in native callbacks | Opt-in ThreadLocal checks in `instanto-teavm-extensions` report such access |
| `Throwable`'s suppressed exceptions were null after some constructors in 0.15 | Fixed in 0.16 by [PR #1252](https://github.com/konsoletyper/teavm/pull/1252) | Retired; reference source in [`legacy`](../legacy/README.md) |
