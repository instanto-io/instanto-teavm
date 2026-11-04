# Virtual calls select methods that do not override across package boundaries

**TeaVM version:** 0.16.0. JavaScript, Wasm GC and C.

## Summary

Java allows a subclass in another package to declare a method with the same
signature as a package-private method in its superclass. The subclass method
does not override it: a call compiled against the superclass must still run the
superclass method. The same holds for a private or static method in a subclass,
which never overrides.

TeaVM 0.16.0 can select the subclass method for such a call. Depending on the
case, the program returns the wrong result, or the superclass method is left out
of the output and the call fails with `TypeError: ... is not a function`.

## Reproducer

```java
package example.base;

public class Base {
    int value() {                    // package-private
        return 1;
    }

    public int readBaseValue() {
        return value();              // must call Base.value()
    }
}
```

```java
package example;

import example.base.Base;

public class Derived extends Base {
    protected int value() {          // a new method, not an override:
        return 2;                    // Base.value() is not visible here
    }

    public int readDerivedValue() {
        return value();
    }
}

public class PrivateDerived extends Base {
    private int value() {            // private: never an override
        return 2;
    }
}

public class Main {
    public static void main(String[] args) {
        Derived derived = new Derived();
        int result = derived.readBaseValue() * 10 + derived.readDerivedValue();
        System.out.println(result);                            // 12 on the JVM

        System.out.println(new PrivateDerived().readBaseValue());  // 1 on the JVM
    }
}
```

On the JVM this prints `12` and `1`. Compiled by TeaVM 0.16.0 the first gives
`22`, because `readBaseValue()` calls `Derived.value()`. In the private and
static variants the base method can be dropped from the generated code, so the
call fails at run time with `is not a function`.

A wider set of scenarios (protected, public and package-private subclass methods,
promotion through an intermediate public or protected override, re-entering the
base package, interface defaults, covariant bridges, mixed receivers) is at
https://github.com/instanto-io/instanto-teavm/tree/main/instanto-teavm-core-patch/src/it/consumer/src/main/java/example/dispatch
and in
https://github.com/instanto-io/instanto-teavm/blob/main/instanto-teavm-core-patch/src/test/java/io/instanto/teavm/VirtualDispatchTeaVmTest.java

## Where the fault lies

The generic implementation lookup used for virtual calls treats any method with
a matching signature in a subclass as an override. It does not apply the
selection rules in JVMS 5.4.5–5.4.6:

- a private or static method does not override;
- a package-private method is overridden only by a method in the same runtime
  package, unless an intermediate public or protected override has promoted it;
- an abstract declaration suppresses an inherited body, and default-method
  selection keeps the maximally specific declaration.

This lookup is shared by dependency analysis (`VirtualCallConsumer`,
`FastVirtualCallConsumer`), devirtualization, inlining and class inference, so a
wrong choice there either removes the base method as unreachable or inlines the
wrong body.

Correct selection is not enough on its own. Two unrelated methods with the same
name and descriptor (`Base.value()` and `Derived.value()` above) currently share
one runtime dispatch slot, so even with the right target the generated code can
call the other method. Package-private methods need dispatch keys that include
their package, in the JavaScript output and in the C, legacy Wasm and Wasm GC
virtual tables.

## Proposed fix

1. A `VirtualMethodResolver` implementing the JVMS selection rules, used by both
   dependency analysers, devirtualization, inlining and class inference. Direct,
   private and static call resolution stays as it is.
2. Separate dispatch slots for package-private methods, keyed by declaring
   package. Public and protected slots keep the normal signature, and a real
   override fills every slot it implements, including a public method that
   overrides two previously independent declarations. The keys are internal to
   dispatch: Java names, signatures, access levels and reflection metadata are
   unchanged.

Our implementation against 0.16.0, as replacement classes:
https://github.com/instanto-io/instanto-teavm/tree/main/instanto-teavm-core-patch/src/main/java/org/teavm
(`model/VirtualMethodResolver.java` and the classes it is used from). It passes
17 dispatch cases on the JVM, on normal, minified and optimised JavaScript, and on
normal and optimised Wasm GC, plus 9 resolver cases against the compiler model;
the optimised C output was also compiled with Clang and run. A source patch in
upstream form has not been prepared yet.
