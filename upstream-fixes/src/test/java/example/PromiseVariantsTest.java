/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package example;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.jso.core.JSPromise;
import org.teavm.jso.core.JSString;
import org.teavm.jso.JSBody;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;
@RunWith(TeaVMTestRunner.class)
@SkipJVM
public class PromiseVariantsTest {
  @Test public void javaScriptString() {
    JSString value = JSPromise.resolve(JSString.valueOf("native")).await();
    assertEquals("native", value.stringValue());
  }
  @Test public void nativePromise() {
    JSString value = fromJavaScript().await();
    assertEquals("native", value.stringValue());
  }
  @Test public void nullValue() {
    assertNull(JSPromise.resolve((Object) null).await());
  }
  @JSBody(script = "return Promise.resolve('native');")
  static native JSPromise<JSString> fromJavaScript();
  @Test public void repeatedSuspensionAndInterfaceCast() {
    Object value = JSPromise.resolve((Object) new Payload()).await();
    assertEquals(42, ((Tag) value).value());
    assertEquals("second", JSPromise.resolve("second").await());
    assertEquals(42, ((Tag) value).value());
  }
  @Test public void invalidInterfaceCastStillThrows() {
    Object value = JSPromise.resolve((Object) "value").await();
    try {
      ((Tag) value).value();
      org.junit.Assert.fail("cast must fail");
    } catch (ClassCastException expected) {
      assertEquals("after", JSPromise.resolve("after").await());
    }
  }
  interface Tag { int value(); }
  static class Payload implements Tag { public int value() { return 42; } }
}
