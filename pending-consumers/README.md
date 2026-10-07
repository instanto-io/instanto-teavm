# Waiting for instanto-teavm to be published

These changes make other repositories use this repository's artifacts:
`instanto-teavm-pom` from here rather than from `instanto-poms`, and
`instanto-teavm-extensions` in place of `teavm-rule-support`. They were set
aside on 6 October 2026, uncommitted, because `instanto-teavm` has no GitHub
repository and is not on packages.instanto.io, so consumers built with them
only on this machine.

Apply them once this repository is pushed and its CI has published its
artifacts, each in its own repository, then verify against the registry.

| File | Repository | Change |
|---|---|---|
| `instanto-poms-move-teavm-pom.patch` | instanto-poms | Removes `instanto-teavm-pom`, which this repository then publishes |
| `sarto-poms-readme.patch` | sarto-poms | README: `instanto-teavm-pom` comes from instanto-teavm |
| `sarto-async-teavm-tests.patch` | sarto-async | Browser tests use `instanto-teavm-extensions` |
| `sarto-invoke-teavm.patch` | sarto (monorepo) | `sarto-invoke-teavm` tests use `instanto-teavm-extensions` |
| `NativeCallbackTeaVmTest.java` | sarto (monorepo) | Goes in `sarto-invoke-teavm/src/test/java/io/instanto/sarto/invoke/teavm/`; needs the ThreadLocal checks |
| `verrai-test-modules.patch` | verrai | Demo, SSR and browser testkit tests use `instanto-teavm-extensions` |
| `sarto-edge-test-modules.patch` | sarto-edge | React example and CF JUnit tests use `instanto-teavm-extensions` |
| `teavm-rule-support/` | teavm-rule-support | Turns it into a relocation POM to `instanto-teavm-extensions`, with the copied `TeaVMTestRunner` backport and its tests |
| `mockatcha-teavm-extensions.patch` | mockatcha | Tests use `instanto-teavm-extensions`; README and docs point to it |
| `mockatcha-java-reformat.patch` | mockatcha | Formatter rewrites found alongside the above; not part of the switch, kept only so nothing is lost |
