/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package example;
import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.jso.*;
import org.teavm.junit.*;
@RunWith(TeaVMTestRunner.class)
@SkipJVM
@OnlyPlatform(TestPlatform.JAVASCRIPT)
public class GlobalInvocationTest {
    @Test public void globalCallPreservesStrictThis() {
        install();
        assertTrue(Globals.strictThis());
    }
    @Test public void globalVarargsPreserveStrictThis() {
        install();
        assertEquals(6, Globals.sum(1, 2, 3));
    }
    @Test public void instanceCallKeepsReceiver() {
        assertEquals(42, instance().add(2));
    }
    @JSBody(script = "globalThis.teavmStrictThis = function() { 'use strict'; return this === undefined; }; "
            + "globalThis.teavmSum = function() { 'use strict'; if (this !== undefined) return -1; "
            + "return Array.prototype.reduce.call(arguments, function(a, b) { return a+b; }, 0); };")
    private static native void install();
    abstract static class Globals implements JSObject {
        @JSTopLevel @JSMethod("teavmStrictThis") static native boolean strictThis();
        @JSTopLevel @JSMethod("teavmSum") static native int sum(int... values);
    }
    @JSBody(script = "return { base: 40, add: function(n) { return this.base+n; } };")
    private static native Instance instance();
    interface Instance extends org.teavm.jso.JSObject { int add(int n); }
}
