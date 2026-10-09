# Instanto TeaVM

Instanto TeaVM is the shared home for the small additions and fixes that our
Java applications need when TeaVM compiles them for the browser. Keeping these
here means applications can use the same tested implementation instead of
carrying their own copies of TeaVM classes.

It provides four things:

| Module | What it does | When to use it |
| --- | --- | --- |
| `instanto-teavm-classlib` | Supplies missing Java methods and corrects some handling of Unicode characters. | Code compiled by TeaVM needs `String.lines()`, `Character.getDirectionality()`, or the code-point corrections. |
| `instanto-teavm-extensions` | Makes ordinary JUnit rules work in compiled tests, fixes repeated setup/cleanup, and can detect unsafe ThreadLocal access from browser callbacks. | TeaVM tests use JUnit rules or need the lifecycle fix. Enable callback diagnostics separately when wanted. |
| `instanto-teavm-core-patch` | Corrects a TeaVM compiler fault that can leave static fields uninitialised after an exception. | Applied by `instanto-teavm-pom` to every build; opt out with `-Dinstanto.teavm.stockCompiler`. |
| `instanto-teavm-jso-patch` | Corrects a TeaVM compiler fault that turns calls to `@JSTopLevel` functions, such as `Window.setTimeout`, into calls on `null` in optimised JavaScript. | Applied with the core patch. |

The supported baseline is **TeaVM 0.16.0 with Java 21**. Applications keep
using TeaVM's standard Maven plugin. These artifacts have no Sarto runtime
dependency.

## Choose only what you need

The organisation's `io.instanto:instanto-teavm-pom`, maintained in
[instanto-poms](https://github.com/instanto-io/instanto-poms), manages the
versions. Inheriting it does not install these extensions or start a TeaVM
compilation.

Add the classlib artifact to the module whose Java code TeaVM compiles:

```xml
<dependency>
  <groupId>io.instanto</groupId>
  <artifactId>instanto-teavm-classlib</artifactId>
  <scope>provided</scope>
</dependency>
```

Use `test` scope instead when only compiled tests need it. TeaVM discovers
the plugin automatically; it adds methods to TeaVM's own classes rather than
putting replacement `TString` or `TCharacter` classes first on the classpath.
Missing-method additions step aside when upstream provides the method.
The existing-method code-point corrections always apply and need revalidation
on a compiler upgrade. Directionality is an approximation, not a complete
Unicode bidi implementation.

For JUnit rules and the lifecycle fix, declare the extensions **before
`org.teavm:teavm-junit`**, including before a dependency that brings it in
transitively:

```xml
<dependency>
  <groupId>io.instanto</groupId>
  <artifactId>instanto-teavm-extensions</artifactId>
  <scope>test</scope>
</dependency>
```

ThreadLocal diagnostics are **off by default**. They detect access without
a Java execution context; they do not repair or propagate that context.
See [configuration and implementation details](docs/implementation.md) for
enabling them in an application or test suite.

## What is packaged and what is still a proposal

The classlib and extensions modules are normal artifacts. The class-initialisation
and global call patches replace TeaVM compiler classes, and every build that
inherits `instanto-teavm-pom` uses them unless it opts out.
Three additional compiler fixes are retained under `upstream-fixes` as review
proposals. Normal builds do not apply them. Their reproducers and patches are
centralised here so they can be reviewed and submitted upstream independently.

GWT, Elemental and JSInterop adaptation remains in
[teavm-compat](https://github.com/instanto-io/teavm-compat). Sarto-specific code
generation and application runtime libraries remain with Sarto.
The old TeaVM 0.15 suppressed-exception workaround is
[retired reference source](legacy/README.md); TeaVM 0.16 already fixes it.

## Build and adopt

Run the JVM and browser verification with Java 21, Maven and Chromium:

```sh
mvn -Pteavm-browser-tests,teavm-class-init-fix -Dspotless.skip=true \
  -Dinstanto.teavm.threadLocalChecks=true clean verify
```

[The detailed guide](docs/implementation.md) explains the JavaScript/Wasm GC
matrix, standalone consumers, compiler proposals and limitations.
[JUnit support](docs/junit-support.md) describes supported rule types and ordering.

Publish `instanto-poms` first, then this repository, then its consumers.
Moving from `teavm-rule-support`, `teavm-classlib-support` or
`teavm-classlib-compat`? See [the migration guide](docs/consumer-migration.md).
