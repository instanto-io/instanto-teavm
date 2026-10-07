/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package example;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.jso.*;
import org.teavm.junit.*;

@RunWith(TeaVMTestRunner.class)
@SkipJVM
@OnlyPlatform(TestPlatform.JAVASCRIPT)
public class MinifierCaptureTest {
  @Test
  public void callbackSurvivesNestedParameterNames() {
    assertEquals(42, nested(() -> 42));
  }

  @Test public void blockBinding() { assertEquals(42, block(() -> 42)); }
  @Test public void catchBinding() { assertEquals(42, caught(() -> 42)); }
  @Test public void localBinding() { assertEquals(42, local(() -> 42)); }
  @Test public void sameOriginalNameRemainsShadowed() { assertEquals(7, shadow(() -> 42)); }
  @Test public void aliasPrefixCollision() { assertEquals(49, alias(() -> 42)); }
  @Test public void namedFunction() { assertEquals(42, named(() -> 42)); }
  @Test public void nestedClosures() { assertEquals(42, closures(() -> 42)); }
  @JSBody(params = "value", script = "{ let b = 7; return value(); }")
  private static native int block(Value value);
  @JSBody(params = "value", script = "try { throw 7; } catch (b) { return value(); }")
  private static native int caught(Value value);
  @JSBody(params = "value", script = "var b = 7; return value();")
  private static native int local(Value value);
  @JSBody(params = "b", script = "function read(b) { return b; } return read(7);")
  private static native int shadow(Value b);
  @JSBody(params = "value", script = "var $jsbody$0 = 7; function read(b) { return value() + $jsbody$0; } return read();")
  private static native int alias(Value value);
  @JSBody(params = "value", script = "return (function b() { return value(); })();")
  private static native int named(Value value);
  @JSBody(params = "value", script = "function read(b) { return function(c) { return value(); }; } return read()(0);")
  private static native int closures(Value value);

  @JSFunctor
  interface Value extends JSObject {
    int get();
  }

  @JSBody(params = "value", script = "function read(b) { return value(); } return read();")
  private static native int nested(Value value);
}
