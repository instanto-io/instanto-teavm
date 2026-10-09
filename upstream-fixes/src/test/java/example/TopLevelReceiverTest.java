/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package example;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSMethod;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.JSTopLevel;
import org.teavm.jso.browser.Window;
import org.teavm.junit.OnlyPlatform;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.TestPlatform;

/**
 * Every kind of {@code @JSTopLevel} member, used the same way: a Java collection is captured by a
 * callback and is dead after the call. On a stock optimising compiler the global's null receiver
 * shares a variable slot with that collection, the slot is cleared, and the call is emitted as a
 * member access on it: {@code $values = null; $values["name"](...)}.
 */
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
  public void windowSetInterval() {
    List<String> values = new ArrayList<>();
    int handle = Window.setInterval(() -> values.add("fired"), 100000);
    Window.clearInterval(handle);
    assertTrue(values.isEmpty());
  }

  @Test
  public void declaredGlobalFunction() {
    install();
    List<String> values = new ArrayList<>();
    Globals.call(() -> values.add("called"));
    assertEquals(List.of("called"), values);
  }

  @Test
  public void declaredGlobalFunctionReturningAValue() {
    install();
    List<String> values = new ArrayList<>();
    String result = Globals.callAndName(() -> values.add("called"));
    assertEquals("named", result);
    assertEquals(List.of("called"), values);
  }

  @Test
  public void declaredGlobalVarargsFunction() {
    install();
    List<String> values = List.of("a", "b");
    assertEquals("a,b", Globals.join(values.get(0), values.get(1)));
  }

  @Test
  public void globalPropertyRead() {
    install();
    List<String> values = new ArrayList<>();
    remember(() -> values.add("remembered"));
    assertEquals("global value", Globals.value());
  }

  @Test
  public void globalPropertyWrite() {
    install();
    List<String> values = new ArrayList<>();
    remember(() -> values.add("remembered"));
    Globals.setValue("written");
    assertEquals("written", currentValue());
  }

  @Test
  public void globalPropertyWriteOfACallback() {
    install();
    List<String> values = new ArrayList<>();
    Globals.setHandler(() -> values.add("handled"));
    runHandler();
    assertEquals(List.of("handled"), values);
  }

  @Test
  public void globalPropertyReadOfACallback() {
    install();
    List<String> values = new ArrayList<>();
    remember(() -> values.add("remembered"));
    Callback handler = Globals.handler();
    handler.run();
    assertEquals(List.of("installed"), installedCalls());
  }

  @JSFunctor
  interface Callback extends JSObject {
    void run();
  }

  abstract static class Globals implements JSObject {
    @JSTopLevel
    @JSMethod("teavmCall")
    static native void call(Callback callback);

    @JSTopLevel
    @JSMethod("teavmCallAndName")
    static native String callAndName(Callback callback);

    @JSTopLevel
    @JSMethod("teavmJoin")
    static native String join(String... values);

    @JSTopLevel
    @JSProperty("teavmValue")
    static native String value();

    @JSTopLevel
    @JSProperty("teavmValue")
    static native void setValue(String value);

    @JSTopLevel
    @JSProperty("teavmHandler")
    static native Callback handler();

    @JSTopLevel
    @JSProperty("teavmHandler")
    static native void setHandler(Callback handler);
  }

  @JSBody(
      script =
          "globalThis.teavmValue = 'global value';"
              + "globalThis.teavmCall = function(f) { f(); };"
              + "globalThis.teavmCallAndName = function(f) { f(); return 'named'; };"
              + "globalThis.teavmJoin = function() {"
              + " return Array.prototype.join.call(arguments, ','); };"
              + "globalThis.teavmInstalledCalls = [];"
              + "globalThis.teavmHandler = function() { teavmInstalledCalls.push('installed'); };")
  private static native void install();

  @JSBody(params = "callback", script = "globalThis.teavmRemembered = callback;")
  private static native void remember(Callback callback);

  @JSBody(script = "return globalThis.teavmValue;")
  private static native String currentValue();

  @JSBody(script = "globalThis.teavmHandler();")
  private static native void runHandler();

  private static List<String> installedCalls() {
    List<String> calls = new ArrayList<>();
    for (int i = 0; i < installedCallCount(); i++) {
      calls.add(installedCall(i));
    }
    return calls;
  }

  @JSBody(script = "return globalThis.teavmInstalledCalls.length;")
  private static native int installedCallCount();

  @JSBody(params = "index", script = "return globalThis.teavmInstalledCalls[index];")
  private static native String installedCall(int index);
}
