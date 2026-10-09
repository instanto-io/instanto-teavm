# Moving to the shared artifacts

Three older artifacts are replaced by modules of this repository.

| Former artifact | Replacement | What to change |
| --- | --- | --- |
| `io.instanto:teavm-rule-support` | `io.instanto:instanto-teavm-extensions` | Replace the dependency, and keep it before `teavm-junit` and anything that brings that jar in. |
| Cucumber Tea's `io.instanto:teavm-classlib-support` | `io.instanto:instanto-teavm-classlib` | Build with TeaVM 0.16 and remove the copied `TString` and `TCharacter` classes. |
| `io.instanto:teavm-classlib-compat` | `io.instanto:instanto-teavm-classlib` | Replace the dependency. Its TeaVM 0.15 `Throwable` repair is not needed on 0.16; the source is kept in [`legacy`](../legacy/README.md). |

## Versions

`instanto-teavm-pom` manages both modules through the `instanto-teavm.version`
property, so a project that inherits it declares them without a version. A
project with another parent declares the version itself.

Use `test` scope for test suites and `provided` when a module only needs the
class-library additions while TeaVM compiles an application. The ThreadLocal
checks stay off until a build turns them on.

## TeaVM upgrades

On each TeaVM upgrade:

- check whether TeaVM now provides the methods `instanto-teavm-classlib` adds;
  each addition steps aside when it does;
- reproduce the code-point faults on stock TeaVM before removing their
  corrections, which `CodePointsTest` covers;
- turn off the version-bound `teavm-class-init-fix` profile before changing its
  TeaVM version, then run the compiler and browser regressions.
