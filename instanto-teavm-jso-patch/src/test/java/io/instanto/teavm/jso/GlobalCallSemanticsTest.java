/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.teavm.jso;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSMethod;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSTopLevel;
import org.teavm.junit.OnlyPlatform;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.TestPlatform;

/**
 * A global call made as a plain function call keeps JavaScript's meaning: a strict-mode global
 * sees an undefined {@code this}, varargs are spread, and instance calls keep their receiver.
 */
@RunWith(TeaVMTestRunner.class)
@SkipJVM
@OnlyPlatform(TestPlatform.JAVASCRIPT)
public class GlobalCallSemanticsTest {

  @Test
  public void globalCallPreservesStrictThis() {
    install();
    assertTrue(Globals.strictThis());
  }

  @Test
  public void globalVarargsPreserveStrictThis() {
    install();
    assertEquals(6, Globals.sum(1, 2, 3));
  }

  @Test
  public void instanceCallKeepsReceiver() {
    assertEquals(42, instance().add(2));
  }

  abstract static class Globals implements JSObject {
    @JSTopLevel
    @JSMethod("teavmStrictThis")
    static native boolean strictThis();

    @JSTopLevel
    @JSMethod("teavmSum")
    static native int sum(int... values);
  }

  interface Instance extends JSObject {
    int add(int n);
  }

  @JSBody(
      script =
          "globalThis.teavmStrictThis = function() { 'use strict'; return this === undefined; };"
              + "globalThis.teavmSum = function() { 'use strict'; if (this !== undefined) return -1;"
              + " return Array.prototype.reduce.call(arguments, function(a, b) { return a + b; }, 0);"
              + " };")
  private static native void install();

  @JSBody(script = "return { base: 40, add: function(n) { return this.base + n; } };")
  private static native Instance instance();
}
