# Instanto TeaVM

Class-library additions, compiler extensions and a patched compiler core for TeaVM. Applications
continue to use TeaVM's standard Maven compiler goal. There is no Sarto runtime
dependency and no custom compiler launcher.

| Module | Purpose |
| --- | --- |
| `instanto-teavm-classlib` | Class-library methods TeaVM lacks, `String.lines()` and `Character.getDirectionality`, added while compiling. |
| `instanto-teavm-extensions` | JUnit rule support, the temporary TeaVM lifecycle fix, and opt-in ThreadLocal checks. JVM and compiled browser tests live in this module. |
| `instanto-teavm-core-patch` | Optional TeaVM 0.16.0 compiler patch for class initialization on exception paths. No Sarto or JUnit runtime dependency. |

The root `instanto-teavm-parent` builds these three modules. It inherits
`io.instanto:instanto-teavm-pom`, which lives in
[instanto-poms](https://github.com/instanto-io/instanto-poms) with the other
organisation POMs and manages the versions of the modules here. This repository
holds code only, and its CI publishes every push to `main`.

## Compatibility first

Inheriting the parent does not add an extension dependency or execute TeaVM.
Adding the extension enables JUnit support where relevant; an ordinary application
with no reachable JUnit code gets no JUnit runtime code. The ThreadLocal check
is **disabled by default**, and disabling it installs no
transformer, runtime probe, or JavaScript entry wrapper.

The current compatibility baseline is TeaVM **0.16.0**, tested with Java 21.
The check uses TeaVM runtime and compiler internals; a TeaVM upgrade needs the
browser and consumer tests below. It is a diagnostic option, not an upstream
behaviour change that all consumers must adopt.

## Compiler configuration

The compiler configuration lives in `io.instanto:instanto-teavm-pom`, published
from instanto-poms. An application can inherit it directly or through
a Sarto parent. It declares the standard `org.teavm:teavm-maven-plugin` compile
execution and chooses its main class, target, output path, module format and
optimisation level. The shared parent manages the compiler version and enables
source maps and copied Java sources.

Importing the parent as a BOM carries dependency versions only. Maven does not
import plugin configuration through `dependencyManagement`; projects with a
different parent must configure the compiler explicitly.

Sarto-specific transformations and CDI generation stay in their Sarto modules.
Application and hosting parents continue to own entry points, packaging and
deployment. `sarto-async` remains an independent application library.

## Class initialization compiler patch

TeaVM 0.16.0 can remove a necessary class initializer from a catch handler, or
from code reached after the catch. An exception thrown before the first use in
the try block then leaves static references null and computed primitive fields
at their default values. The bug also occurs at `SIMPLE`.

The optional `teavm-class-init-fix` profile supplies a corrected
[`ClassInitElimination`](instanto-teavm-core-patch/src/main/java/org/teavm/model/optimization/ClassInitElimination.java):

```sh
mvn -Pteavm-class-init-fix -Dspotless.skip=true clean package
# To also run compiled browser tests:
mvn -Pteavm-class-init-fix,teavm-browser-tests -Dspotless.skip=true clean test
```

The pass uses TeaVM's existing `buildControlFlowGraph2` helper. Each basic block
has separate entry and normal-completion nodes; exception edges leave the entry
node. Initialization facts learned inside a block are attached to its completion
node. They therefore cannot incorrectly dominate a handler or a catch/normal
flow join. This is conservative about how much of a protected block completed;
it can retain additional checks on exception paths. Safe duplicate checks are
still eliminated, initialization remains lazy, and the initializer still runs
once. No new runtime instrumentation is added.

This is a local compiler patch, not an upstream backport or a TeaVM plugin hook.
TeaVM directly constructs this optimization pass. The patch artifact contains
the original 0.16.0 core with just `ClassInitElimination` and its inner class
recompiled. Other upstream class files remain byte-for-byte identical. The
upstream Maven artifact is not overwritten.

The profile adds that artifact to the standard Maven compiler plugin's
dependencies. For Surefire it supplies the same artifact with test scope and
excludes the original `teavm-core` jar from the test classpath, avoiding reliance
on project dependency order. The compiler's other dependencies still come from
upstream TeaVM. This does not add JUnit or Sarto to application dependencies,
change the optimization level, or enable ThreadLocal checks. Inheriting the
parent alone leaves the patch off. BOM imports do not import this profile.
The profile rejects a `teavm.version` other than 0.16.0. After an upstream fix,
disable the profile, clean the generated output, and rerun the regressions before
removing the patch module.

Nine direct compiler tests cover catch handlers, joins, nested handlers, retry
loops, shared handlers, and preservation of safe elimination. Nine Java browser
tests cover object and enum values, computed primitives, boxed Boolean, helper
inlining, nested catch/finally, and initialization timing. The standalone Maven
consumer under `instanto-teavm-core-patch/src/it/consumer` verifies both packaged
compiler and JUnit use. Its script checks a failing stock application, the
patched JS application at SIMPLE/ADVANCED/FULL with minification off/on, the
JS/Wasm GC JUnit matrix, and rejection of an incompatible version.

This patch does not fix the separate timer, JSBody minifier, or optimized-Wasm
promise defects. An additional repeated-field-read defect was isolated during
verification: ADVANCED and FULL can reuse a value from a skipped try path in its handler,
even when initialization is present. In the explicitly pre-initialized reproducer,
disabling only `RepeatedFieldReadElimination` restores the correct result.
Neither pass is disabled by this profile.

## Compiler fix proposals for review

The five independent compiler defects now have draft source patches against
TeaVM 0.16.0. Only the class-initialization patch is packaged by the consumer
profile above. The other four proposals are isolated under `upstream-fixes`;
they do not change normal builds, published artifacts or the local upstream
checkout. These are tested candidates, not upstream-approved changes.

| Defect | Proposed correction | Patch |
| --- | --- | --- |
| Class initialization on exception paths | Propagate completed initialization through normal exits, separately from exception entries. | [class-init.patch](upstream-fixes/patches/class-init.patch) |
| Cached field reads in catch handlers | Invalidate the read cache at handler entry, retaining existing join invalidation and normal-path elimination. | [field.patch](upstream-fixes/patches/field.patch) |
| Global timer calls become null member calls | Lower JS global calls explicitly to function calls before optimization, with distinct fixed-arity and varargs operations. Preserve strict `this` semantics; leave module, instance and Wasm lowering on their existing paths. | [timer.patch](upstream-fixes/patches/timer.patch) |
| JSBody parameter name capture | When a generated parameter name collides with a script identifier, copy it into a fresh alias chosen against all script identifiers and generated parameter names. This adds a local assignment, not a wrapper call. | [minifier.patch](upstream-fixes/patches/minifier.patch) |
| Invalid Wasm local after promise suspension | Make the temporary used by the non-native cast check nullable in suspendable methods, matching coroutine restoration requirements. | [wasm.patch](upstream-fixes/patches/wasm.patch) |

The combined fixture passes **32 JUnit tests at each of SIMPLE, ADVANCED and
FULL**. Browser cases run on JS with minification off/on and Wasm GC where
applicable. The count includes TeaVM's ten existing field-read optimizer tests,
a new handler-entry IR regression, nested JS scopes, strict global `this`,
varargs, instance receivers, null promises, repeated suspension and invalid casts.
A fresh stock FULL run reproduces all five failures; its global-call compatibility
controls and ten existing optimizer tests pass.
Upstream `:core:checkstyleMain` and `:jso:impl:checkstyleMain` pass in a separate
source tree containing the proposals; their Java compilation also succeeds.

The merged [PR #1246](https://github.com/konsoletyper/teavm/pull/1246) corrects a
similar temporary in reflection generation. Applying that change alone to
0.16.0 still reproduces our promise validation failure. Our proposal changes
cast generation, which remains unchanged in the current upstream source checked
during this investigation. This is not a claim that the entire current upstream
compiler has been built or tested.

Run the review fixture with Java 21, Maven and Chrome:

```sh
python3 upstream-fixes/verify.py --teavm-source ../teavm --level FULL
python3 upstream-fixes/verify.py --teavm-source ../teavm --level ADVANCED
python3 upstream-fixes/verify.py --teavm-source ../teavm --level SIMPLE
# Expected failures on the stock compiler:
python3 upstream-fixes/verify.py --teavm-source ../teavm --stock --level FULL
```

Use `--case class-init`, `field`, `timer`, `minifier` or `wasm` to apply and test
one proposal independently. The script reads tag 0.16.0 from the supplied Git
checkout, applies patches in a temporary directory, adapts source imports to
Maven's relocated libraries, and places replacement classes on the test
compiler's classpath. It prints the retained fixture and log paths. The runner
configuration selects the requested optimization level; it disables no compiler
pass. Consumer packaging and a complete upstream suite run remain separate
follow-up work before adopting the four new proposals in normal builds.

## Opt in to ThreadLocal checks

Add the extension as a **project dependency** in the TeaVM application module:

```xml
<dependency>
  <groupId>io.instanto</groupId>
  <artifactId>instanto-teavm-extensions</artifactId>
  <scope>provided</scope>
</dependency>
```

The parent supplies its version. `provided` makes it visible to TeaVM's
application classpath without making it a transitive JVM runtime dependency.
JUnit 4.13.2 is a required dependency of this combined artifact so its JUnit
plugin can be loaded even during an ordinary application compilation. TeaVM
compiler dependencies are `provided`. Only reachable runtime code is emitted;
this does not bundle the compiler or all of JUnit into a browser application.

Then enable the check through the parent's compiler-property mapping:

```sh
mvn package -Dinstanto.teavm.threadLocalChecks=true
```

Both the dependency and the property are required. Without this parent, pass
`instanto.teavm.threadLocalChecks=true` in the TeaVM Maven plugin's `<properties>`
configuration. This is a TeaVM compiler property, not a browser runtime switch.
To disable the check, omit the property or set it to `false`, then rebuild.

When enabled, the plugin adds a check before TeaVM's standard `ThreadLocal.get`,
`set`, and `remove` implementations. It throws `UnsupportedOperationException`
when they run without Java execution context, before reading state, changing it,
or invoking `initialValue`. This includes uses inherited by
`ThreadLocal.withInitial` and third-party libraries. Subclasses that replace
these accessors without calling the standard implementation are outside its
coverage. The check does not instrument `Thread.currentThread`, repair thread
identity, propagate context, or make arbitrary native callbacks suspendable.

The same option also checks `AsyncCallback.complete` and `error`. One running
Java thread completing another thread's callback leaves the completing thread
with the wrong identity: `main` on Wasm GC, or the waiting thread on
JavaScript, where TeaVM then also refuses the call. That thread would then
read the wrong thread locals. TeaVM's own class library never does this. It
completes a callback either inside the `@Async` method while the caller
suspends, or later from the event loop. With checks on, completing from
another running Java thread throws `UnsupportedOperationException` before
anything changes. Complete the callback from a native callback, such as
`setTimeout`, instead.

In a raw browser callback, start independent Java work with
`new Thread(task).start()` before using thread locals. Completing TeaVM's
`AsyncCallback` resumes its associated continuation normally. Synchronous
callbacks nested inside an already running Java continuation retain that
continuation's context.

JavaScript checks the current coroutine when TeaVM includes its coroutine
runtime. For a wholly synchronous application, TeaVM has no such marker: the
plugin wraps the Java runner passed to `$rt_startThread` with a small marker,
restoring it in `finally` before native completion callbacks run. Wasm GC uses
the current fiber. Only JavaScript and Wasm GC are supported when checks are on;
requesting the check for another target fails the build. The JVM's ThreadLocal
implementation is never changed.

Valid accesses allocate nothing for the guard and perform no thread switch or
locking. Exact hot-path cost depends on generated code and the JavaScript/Wasm
engine; no percentage overhead is promised. With the option off there is no
guard code to execute.

## Tests

Run the extension's JVM tests and browser tests, including normal, optimized,
and minified JavaScript plus normal and optimized Wasm GC:

```sh
mvn -Pteavm-browser-tests -Dspotless.skip=true \
  -Dinstanto.teavm.threadLocalChecks=true clean verify
```

The profile requires Chrome/Chromium available to TeaVM's `browser-chrome`
runner. Individual test classes opt into checks with TeaVM's test annotation:

```java
@TeaVMProperties(@TeaVMProperty(
    key = "instanto.teavm.threadLocalChecks", value = "true"))
```

For tests in another project, add the same extension version with `test` scope.
TeaVMTestRunner discovers it on the test classpath. Enable checks for a whole
suite with `-Dinstanto.teavm.threadLocalChecks=true`: the extension uses this
compiler JVM system property when no TeaVM compiler property is supplied.
Explicit compiler properties (including `@TeaVMProperties`) take precedence,
so an explicit `false` remains a negative control in an enabled suite.
Maven's application compiler configuration is not automatically applied to the
test runner. Configuration tests and the Maven consumer below verify that
omitting both settings leaves the default behaviour intact.

Declare the extension **before `org.teavm:teavm-junit`** in the test module:

```xml
<!-- Must precede teavm-junit: shadows its entry point and runner. -->
<dependency>
  <groupId>io.instanto</groupId>
  <artifactId>instanto-teavm-extensions</artifactId>
  <scope>test</scope>
</dependency>
```

Do not append it to Surefire's additional classpath: TeaVM's original classes
would win. Replace an existing `teavm-rule-support` dependency with this one;
do not include both artifacts.

JUnit `TestRule` and `MethodRule` support is automatic for TeaVM test compilation.
It includes inherited rules, ordering and reflection metadata. Tests without
rules retain their normal lifecycle. This does not add `@ClassRule` support or
apply rules in TeaVMTestRunner's JVM execution path. See [JUnit support](docs/junit-support.md).

The artifact also backports upstream's fix for repeated inherited/overridden
`@Before` and `@After` execution, on the JVM and in compiled tests. TeaVM 0.16.0
needs this backport. When upgrading to a release with upstream commit
`4cb1b19bff29770e2df6ced960c293dc3db1a1a5`, remove our copied
`TeaVMTestRunner.java`; retain the deduplication in our rule-aware entry-point
transformer while we still shadow it. Run the inherited/overridden setup tests
on all targets before removing the backport.

The tests cover:

- rejecting `get`, `set` and `remove` in a callback, without running
  `initialValue`;
- preserving the waiting thread's state, independent Java threads, resumption,
  starting a Java thread from a callback, and synchronous access;
- rejecting a callback completed from another running Java thread, while
  completion from the event loop, inside the `@Async` method and through a
  monitor hand-over still works.

There is also a small real Maven consumer under
`instanto-teavm-extensions/src/it/consumer`. It has no Sarto dependency and no
coroutines. After installing this reactor, run its checks with Maven and Node:

```sh
./verify.sh
```

`verify.sh` first builds and installs all three modules with the compiler patch
and guard enabled for the browser suites, then invokes both consumer checks.
It needs Java 21, Maven,
Node and Chrome/Chromium. Extra Maven options are forwarded to both builds.
CI runs the reactor browser matrix with the flag enabled; the standalone
consumer comparison is currently a local check.

The consumer check compiles the application without the extension, with it present but
disabled, and with checks enabled. It requires byte-for-byte identical
JavaScript in the first two builds, and tests ordinary main execution, native
exports, completion callbacks, and marker cleanup after an exception.

## Class-library additions

`instanto-teavm-classlib` registers a TeaVM plugin, so adding it as a dependency
of a module TeaVM compiles is enough; `instanto-teavm-pom` manages its version.
It replaces no TeaVM class. It adds the missing methods to TeaVM's own
`String` and `Character` during compilation and delegates them to plain Java
helpers, and each addition steps aside when TeaVM already has the method. Its
tests also check that `Throwable`'s suppressed exceptions work after every
constructor, which TeaVM 0.15 got wrong and 0.16 fixed.

`Character.getDirectionality` derives the bidirectional class from the
right-to-left script blocks and `Character.getType`, since TeaVM ships no bidi
table. It matches the JDK for the strong classes, digits, separators and the
embedding controls; other neutral characters are approximated by category.

## Releases

Publish `instanto-poms` first, then this repository, then consumers such as
`sarto-poms`.


The former `teavm-rule-support` repository now builds only a Maven relocation
POM to `instanto-teavm-extensions`. Existing source files there are retained as
migration history and are no longer compiled. New implementation and tests live
here. The eight existing rule-support consumers have been switched to the new
coordinate; additional Sarto/Mockatcha suites declare it explicitly for checks.
