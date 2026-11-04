# Java callbacks in arrays and varargs are not converted to JavaScript functions

**TeaVM version:** 0.16.0, and master as of 2026-11-03 (`6df39ce`). JavaScript and
Wasm GC.

## Summary

A Java lambda passed as a single `@JSFunctor` argument reaches JavaScript as a
function. The same lambda passed as an element of an array or varargs parameter
of that functor type reaches JavaScript as a Java object, so calling it fails
with `TypeError: ... is not a function`. The reverse is also true: a single
function returned from JavaScript becomes a callable functor, but an array of
functions does not.

The code compiles without an error or warning.

## A typical case

Wrapping a JavaScript library whose API takes several handlers, here the
[page.js](https://github.com/visionmedia/page.js) router, where
`page('/users/:id', requireLogin, loadUser, showUser)` runs the handlers in order
and each decides whether to call `next()`:

```java
@JSFunctor
public interface RouteHandler extends JSObject {
    void handle(RouteContext context, Next next);
}

@JSFunctor
public interface Next extends JSObject {
    void call();
}

public abstract class Page implements JSObject {
    @JSTopLevel
    @JSMethod("page")
    public static native void route(String path, RouteHandler... handlers);
}

RouteHandler requireLogin = (context, next) -> {
    if (session.isSignedIn()) {
        next.call();
    } else {
        showLogin();                 // stop the chain: next is not called
    }
};

Page.route("/users/:id", requireLogin, this::loadUser, this::showUser);
```

When the route matches, page.js tries to call the first handler and fails with a
`TypeError`, because it was given Java objects rather than functions.

## Reproducer

```java
@RunWith(TeaVMTestRunner.class)
@SkipJVM
public class FunctorArrayTest {
    @Test
    public void singleCallbackIsAFunction() {
        List<String> calls = new ArrayList<>();
        caller().call(() -> calls.add("one"));
        assertEquals(List.of("one"), calls);                 // passes
    }

    @Test
    public void varargsCallbacksAreFunctions() {
        List<String> calls = new ArrayList<>();
        caller().callEach(() -> calls.add("a"), () -> calls.add("b"));
        assertEquals(List.of("a", "b"), calls);              // arguments[i] is not a function
    }

    @Test
    public void arrayOfCallbacksAreFunctions() {
        List<String> calls = new ArrayList<>();
        caller().callAll(new Callback[] {() -> calls.add("a"), () -> calls.add("b")});
        assertEquals(List.of("a", "b"), calls);              // fs[i] is not a function
    }

    @Test
    public void returnedArrayOfFunctionsAreCallbacks() {
        install();
        for (Callback callback : caller().several()) {
            callback.run();                                  // callback.run is not a function
        }
        assertEquals(2, count());
    }

    @JSFunctor
    interface Callback extends JSObject {
        void run();
    }

    interface Caller extends JSObject {
        void call(Callback callback);
        void callEach(Callback... callbacks);
        void callAll(Callback[] callbacks);
        Callback[] several();
    }

    @JSBody(script = "return {"
            + " call: function(f) { f(); },"
            + " callEach: function() { for (var i = 0; i < arguments.length; i++) arguments[i](); },"
            + " callAll: function(fs) { for (var i = 0; i < fs.length; i++) fs[i](); },"
            + " several: function() { return [function() { globalThis.teavmCount++; },"
            + " function() { globalThis.teavmCount++; }]; }"
            + "};")
    private static native Caller caller();

    @JSBody(script = "globalThis.teavmCount = 0;")
    private static native void install();

    @JSBody(script = "return globalThis.teavmCount;")
    private static native int count();
}
```

The callbacks go to an ordinary JavaScript object, not a `@JSTopLevel` function,
so this is independent of the global-call issue. A `@JSBody` method with a
`Callback[]` parameter fails the same way. On stock 0.16.0 the four array cases
fail on JavaScript and Wasm GC, at SIMPLE and FULL; the single-callback cases
pass in both directions. The full test, with a `@JSBody` parameter case, is at
https://github.com/instanto-io/instanto-teavm/blob/main/upstream-fixes/src/test/java/example/FunctorArrayTest.java

## Where the fault lies

`JSValueMarshaller.wrapArgument` checks for `@JSFunctor` only when the parameter
type is a `ValueType.Object`. For `Callback[]`, which is also the type of
`Callback...`, it falls through to `wrap`, whose array conversion (`JS.wrap(T[])`)
copies the elements unchanged. For a varargs parameter, `JSClassProcessor` may
mark the array as by-reference, and `wrap` then passes the Java array's backing
data (`JS.arrayData`) without any conversion. `unwrapReturnValue` has the same
single-value check, so `unwrapArray` copies returned functions into the Java array
without making them functors.

In #504 the reason given for a similar failure was that conversion needs the
functor interface in the method signature, which generics erase. Here the
functor type is in the signature, as the element type of the array.

## Proposed fix

When an array's element type is a proper `@JSFunctor` interface, convert each
element as a single functor is converted:

- wrapping, before the by-reference shortcut: a new `JS.wrapFunctors(Object[],
  JSObject method)` that builds a `JSArray` of `JS.function(JSWrapper.unwrap(item),
  method)`;
- unwrapping: a new `JS.unwrapFunctors(Class, JSObject array, JSObject method)`
  that builds the Java array from `JS.functionAsObject(item, method)`.

Null elements stay null, and other arrays are unchanged. Patch against 0.16.0:
https://github.com/instanto-io/instanto-teavm/blob/main/upstream-fixes/patches/functor-array.patch

With it all six reproducer cases pass on JavaScript and Wasm GC at SIMPLE and
FULL.

Nested arrays of functors (`Callback[][]`) are not covered by this change.
