# @JSTopLevel calls compiled as member calls on null in optimised JavaScript

**TeaVM version:** 0.16.0, and master as of 2026-11-03 (`6df39ce`). JavaScript
backend; Wasm GC is not affected.

## Summary

With optimisation on, a call to a `@JSTopLevel` function, including TeaVM's
own `Window.setTimeout`, `Window.setInterval` and `Window.clearTimeout`, can be
compiled into a member call on `null`:

```
TypeError: Cannot read properties of null (reading 'setTimeout')
```

The same source compiles correctly without optimisation.

## Reproducer

```java
@RunWith(TeaVMTestRunner.class)
@SkipJVM
@OnlyPlatform(TestPlatform.JAVASCRIPT)
public class TopLevelReceiverTest {
    @Test
    public void windowSetTimeout() {
        List<String> values = new ArrayList<>();
        int handle = Window.setTimeout(() -> values.add("fired"), 100000);
        Window.clearTimeout(handle);
        assertTrue(values.isEmpty());
    }

    @Test
    public void declaredGlobalFunction() {
        install();
        List<String> values = new ArrayList<>();
        Globals.call(() -> values.add("called"));
        assertEquals(List.of("called"), values);
    }

    @JSFunctor
    interface Callback extends JSObject {
        void run();
    }

    abstract static class Globals implements JSObject {
        @JSTopLevel
        @JSMethod("teavmCall")
        static native void call(Callback callback);
    }

    @JSBody(script = "globalThis.teavmCall = function(f) { f(); };")
    private static native void install();
}
```

Run with the FULL optimisation level. Both tests fail; so do the same pattern
with `Window.setInterval` and with a declared global function that returns a
value. The full set, with guards for strict `this`, varargs and top-level
properties, is at
https://github.com/instanto-io/instanto-teavm/blob/main/upstream-fixes/src/test/java/example/TopLevelReceiverTest.java

## Generated code

```js
var$3 = null;
var$4 = "teavmCall";
var$5 = !(var$2 instanceof otji_JSWrapper) ? var$2 : (...).$js;
var$3[var$4](otji_JS_function(var$5, "run"));
```

## Where the fault lies

`JSClassProcessor` rewrites a `@JSTopLevel` call into
`JS.invoke(target, "name", args...)`. For a global without `@JSModule`,
`getCallTarget` gets its target from `JSValueMarshaller.moduleRef`, which
returns a variable holding a null constant: null stands for "no receiver".

`JSNativeInjector` then writes a bare `name(args)` only if that first argument
is literally a null `ConstantExpr` (`isNull`). Whether it is depends on the
decompiler folding the single-use variable into the call expression. When an
argument needs a conversion written as its own statement, as a Java lambda
passed as a `@JSFunctor` does, the null and the name stay in variables, `isNull`
sees a variable, and the injector writes `var[name](...)`.

In short, "this call is global" is carried through optimisation as an ordinary
null value, and code generation relies on an inlining decision it does not
control. `get` and `set` use the same `isNull` test; we have not seen them fail,
because their single argument is folded into the assignment.

## Proposed fix

For a global target, have `JSClassProcessor` pass a reference to the global
function itself (`marshaller.globalRef(name, ...)`) and call new intrinsics
`JS.invokeGlobal` (fixed arity) or `JS.applyGlobal` (varargs), which
`JSNativeInjector` writes as `(0, name)(args)` and `(0, name)(...args)`. The
`(0, …)` form keeps strict-mode `this` undefined, as a direct global call does.
Module, instance and Wasm GC calls keep their existing paths.

Patch against 0.16.0:
https://github.com/instanto-io/instanto-teavm/blob/main/upstream-fixes/patches/timer.patch

With it, all 13 reproducer and guard tests pass at SIMPLE, ADVANCED and FULL;
stock 0.16.0 fails 5 of them at FULL.
