/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.teavm;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;
import org.teavm.junit.OnlyPlatform;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMProperties;
import org.teavm.junit.TeaVMProperty;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.TestPlatform;

/** Kept separate so the JavaScript build has no suspension points. Also runs on the JVM. */
@RunWith(TeaVMTestRunner.class)
@OnlyPlatform({TestPlatform.JAVASCRIPT, TestPlatform.WEBASSEMBLY_GC})
@TeaVMProperties(@TeaVMProperty(key = ThreadLocalChecksPlugin.PROPERTY, value = "true"))
public class SynchronousThreadLocalTest {
  @Test
  public void ordinarySynchronousAccessIsLegal() {
    ThreadLocal<String> local = ThreadLocal.withInitial(() -> "initial");
    assertEquals("initial", local.get());
    local.set("changed");
    assertEquals("changed", local.get());
    local.remove();
    assertEquals("initial", local.get());
  }

  @Test
  @SkipJVM
  public void synchronousJavaScriptReentryRetainsTheJavaCallersContext() {
    ThreadLocal<String> local = new ThreadLocal<>();
    local.set("owner");
    reenter(() -> assertEquals("owner", local.get()));
    assertEquals("owner", local.get());
  }

  @JSFunctor
  private interface Callback extends JSObject {
    void run();
  }

  @JSBody(params = "callback", script = "callback();")
  private static native void reenter(Callback callback);
}
