/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.teavm;

import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.interop.Async;
import org.teavm.interop.AsyncCallback;
import org.teavm.jso.browser.Window;
import org.teavm.junit.OnlyPlatform;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMProperties;
import org.teavm.junit.TeaVMProperty;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.TestPlatform;

/** Presence of the extension must not start rejecting existing callback code. */
@RunWith(TeaVMTestRunner.class)
@SkipJVM
@OnlyPlatform({TestPlatform.JAVASCRIPT, TestPlatform.WEBASSEMBLY_GC})
@TeaVMProperties(@TeaVMProperty(key = ThreadLocalChecksPlugin.PROPERTY, value = "false"))
public class DefaultBehaviourTest {
  @Test
  public void explicitlyDisabledChecksPreserveCallbackBehaviour() {
    assertNull(accessInCallback());
  }

  @Async
  private static native Throwable accessInCallback();

  private static void accessInCallback(AsyncCallback<Throwable> callback) {
    Window.setTimeout(
        () -> {
          Throwable failure = null;
          try {
            ThreadLocal<String> local = new ThreadLocal<>();
            local.get();
            local.set("unchanged");
            local.remove();
          } catch (Throwable error) {
            failure = error;
          }
          callback.complete(failure);
        },
        0);
  }
}
