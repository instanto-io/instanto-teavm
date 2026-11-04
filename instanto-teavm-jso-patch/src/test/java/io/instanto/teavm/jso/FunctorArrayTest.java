/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.teavm.jso;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;

/**
 * A Java callback crosses into JavaScript as a function when it is passed on its own, but not when
 * it is an element of an array or varargs parameter: {@code JSValueMarshaller.wrapArgument} applies
 * the functor conversion only to a parameter whose type is the functor interface itself. The same
 * holds the other way for functions returned in an array.
 */
@RunWith(TeaVMTestRunner.class)
@SkipJVM
public class FunctorArrayTest {

  @Test
  public void singleCallbackIsAFunction() {
    List<String> calls = new ArrayList<>();
    caller().call(() -> calls.add("one"));
    assertEquals(List.of("one"), calls);
  }

  @Test
  public void varargsCallbacksAreFunctions() {
    List<String> calls = new ArrayList<>();
    caller().callEach(() -> calls.add("a"), () -> calls.add("b"));
    assertEquals(List.of("a", "b"), calls);
  }

  @Test
  public void arrayOfCallbacksAreFunctions() {
    List<String> calls = new ArrayList<>();
    caller().callAll(new Callback[] {() -> calls.add("a"), () -> calls.add("b")});
    assertEquals(List.of("a", "b"), calls);
  }

  @Test
  public void bodyParameterArrayOfCallbacksAreFunctions() {
    List<String> calls = new ArrayList<>();
    callAll(new Callback[] {() -> calls.add("a"), () -> calls.add("b")});
    assertEquals(List.of("a", "b"), calls);
  }

  @Test
  public void returnedFunctionIsACallback() {
    install();
    caller().single().run();
    assertEquals(1, count());
  }

  @Test
  public void returnedArrayOfFunctionsAreCallbacks() {
    install();
    for (Callback callback : caller().several()) {
      callback.run();
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

    Callback single();

    Callback[] several();
  }

  @JSBody(
      script =
          "return {"
              + " call: function(f) { f(); },"
              + " callEach: function() { for (var i = 0; i < arguments.length; i++) arguments[i](); },"
              + " callAll: function(fs) { for (var i = 0; i < fs.length; i++) fs[i](); },"
              + " single: function() { return function() { globalThis.teavmCount++; }; },"
              + " several: function() { return [function() { globalThis.teavmCount++; },"
              + " function() { globalThis.teavmCount++; }]; }"
              + "};")
  private static native Caller caller();

  @JSBody(
      params = "callbacks",
      script = "for (var i = 0; i < callbacks.length; i++) callbacks[i]();")
  private static native void callAll(Callback[] callbacks);

  @JSBody(script = "globalThis.teavmCount = 0;")
  private static native void install();

  @JSBody(script = "return globalThis.teavmCount;")
  private static native int count();
}
