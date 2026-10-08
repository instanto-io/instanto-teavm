# Consumer migration

## Coordinates

| Former artifact | Shared artifact | Required action |
| --- | --- | --- |
| `io.instanto:teavm-rule-support` | `io.instanto:instanto-teavm-extensions` | Replace the dependency; keep it before `teavm-junit` or anything bringing that jar in transitively. |
| Cucumber Tea's `io.instanto:teavm-classlib-support` | `io.instanto:instanto-teavm-classlib` | Use TeaVM 0.16 from the shared parent; remove the copied classlib module. |
| `io.instanto:teavm-classlib-compat` | `io.instanto:instanto-teavm-classlib` | For TeaVM 0.16 consumers, replace the old artifact. The 0.15 Throwable repair is retired rather than enabled in the new module. |

The shared snapshot version is `0.1.0-SNAPSHOT`, managed by
`instanto-teavm-pom` as `instanto-teavm.version`.
Projects using another parent must manage or declare this version explicitly.
Do not couple the extension version to Mockatcha's version.
Use `test` scope for test suites and `provided` for application compilation.
ThreadLocal diagnostics remain opt-in.

## Repositories included in the migration

| Repository | Changes | Verification |
| --- | --- | --- |
| instanto-teavm | Directionality precedence and targeted regressions; central implementation and retired-source documentation. | Reactor browser matrix and `./verify.sh` consumer checks. |
| cucumber-tea | Shared TeaVM parent and classlib; remove copied class replacements. | `./mvnw clean test`; existing JVM/Chromium suites and `-Pminiflare` Worker suites. |
| teavm-rule-support | Publish a relocation POM; remove duplicate source and tests from the active tree. | Maven `clean verify`, then resolve the relocation against the registry. |
| teavm-compat | Use shared classlib for GWT consumers; relocate the former classlib artifact and retire the 0.15 repair. | Existing GWT/TeaVM shared browser contracts, especially event-bus suppressed exceptions. |
| mockatcha | Replace the old rule dependency; add shared lifecycle support to core and BDD tests; update examples and docs. | Existing JVM and TeaVM browser suites. |
| verrai | Update demo, SSR Worker integration and browser testkit test dependencies. | Existing browser and Worker test profiles. |
| sarto-edge | Update React and CF JUnit tests plus test documentation. | Existing Chromium and Miniflare suites. |
| sarto-async | Add shared lifecycle support before TeaVM JUnit. | Existing continuation browser tests. |
| sarto | Add shared extension and native callback regressions to invocation tests. | Existing invocation JS/Wasm GC suites; callback tests explicitly enable diagnostics. |

Prepared changes and merge status are recorded by the migration pull requests.
This table describes scope; it does not claim all default branches have migrated.

## Publication and merge order

1. Publish the current `instanto-poms` snapshot, including the managed shared coordinates.
2. Merge and publish the shared reactor after its verification passes.
3. Build consumers with a clean or isolated Maven repository and `-U` so cached
   local artifacts cannot conceal missing registry publication.
4. Merge and publish consumer migrations and relocation POMs after those builds pass.

The publication job on commit `c5498fd` failed with **401 Unauthorized** from
`packages.instanto.io`; verification itself passed.
The shared reusable workflow expects `FORGEJO_PACKAGE_TOKEN` (singular),
whereas the working sarto-async caller also accepts `FORGEJO_PACKAGES_TOKEN`
(plural). This migration applies the same explicit mapping here: verification
accepts the plural, read-only, or singular token; publication accepts the plural
or singular publishing token. Both pass `FORGEJO_PACKAGE_USER`.
The successful sarto-async publication on 8 October validates this convention.
The earlier 401 does not establish that this repository lacks tokens.
Validate the next deployment after adopting the mapping. Credentials are not
stored in source.

## Upgrade and retirement

Check missing-method additions against upstream on each TeaVM upgrade.
Reproduce the code-point faults on stock TeaVM before removing their replacements.
Disable the version-bound class-init profile before changing its compiler version,
then run the compiler and browser regressions. Keep draft upstream proposals
separate from packaged fixes until their full verification and consumer packaging
are complete.

## Migration pull requests

| Repository | Review |
| --- | --- |
| instanto-teavm | [Pull request](https://github.com/instanto-io/instanto-teavm/pull/1) |
| mockatcha | [Pull request](https://github.com/instanto-io/mockatcha/pull/2) |
| verrai | [Pull request](https://github.com/instanto-io/verrai/pull/46) |
| sarto-edge | [Pull request](https://github.com/instanto-io/sarto-edge/pull/42) |
| sarto-async | [Pull request](https://github.com/instanto-io/sarto-async/pull/23) |
| sarto | [Pull request](https://github.com/instanto-io/sarto/pull/394) |
| cucumber-tea | [Pull request](https://github.com/instanto-io/cucumber-tea/pull/1) |
| teavm-rule-support | [Pull request](https://github.com/instanto-io/teavm-rule-support/pull/1) |
| teavm-compat | [Pull request](https://github.com/instanto-io/teavm-compat/pull/2) |

These are draft PRs. Consumer adoption awaits shared artifact publication and
successful checks in the relevant repository; queued or skipped jobs are not
verification results.
