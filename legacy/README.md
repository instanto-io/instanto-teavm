# Retired TeaVM workarounds

`teavm-0.15/ThrowableInitializationPlugin.java` preserves the repair formerly
compiled by `teavm-compat/teavm-classlib-compat`. It restored the suppressed
exception array in TeaVM 0.15's `Throwable` constructors. TeaVM 0.16 fixes
that defect, so this file is reference source only: no reactor module compiles
or registers it.

The active classlib module retains the suppressed-exception regression tests.
Do not enable the retired plugin for a new compiler version without reproducing
the defect and checking constructor semantics again.
