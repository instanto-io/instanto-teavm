/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.teavm;

import org.teavm.interop.PlatformMarker;
import org.teavm.interop.Platforms;
import org.teavm.runtime.Fiber;

/** Runtime implementation used only by instrumented TeaVM code, not an application API. */
public final class ThreadLocalGuard {
  private ThreadLocalGuard() {}

  public static void check() {
    if (!(isJavaScript() ? hasJavaScriptThread() : Fiber.current() != null)) {
      throw new UnsupportedOperationException(
          "ThreadLocal access requires Java thread context. "
              + "Start a Java thread before accessing thread locals from a native callback.");
    }
  }

  /**
   * Called at the start of {@code AsyncCallback.complete} and {@code error}. A running Java thread
   * that completes another thread's callback would leave itself with the wrong thread identity, and
   * so with the other thread's thread locals. TeaVM itself only completes callbacks from the event
   * loop or from within the waiting thread's own suspension.
   *
   * @param waiting the waiting fiber on Wasm GC; {@code null} on JavaScript
   */
  public static void checkCompletion(Fiber waiting) {
    boolean fromOtherJavaThread;
    if (isJavaScript()) {
      fromOtherJavaThread = hasJavaScriptThread();
    } else {
      Fiber current = Fiber.current();
      fromOtherJavaThread = current != null && current != waiting;
    }
    if (fromOtherJavaThread) {
      throw new UnsupportedOperationException(
          "An AsyncCallback cannot be completed from another running Java thread. "
              + "Complete it from a native callback, for example via setTimeout.");
    }
  }

  @PlatformMarker(Platforms.JAVASCRIPT)
  private static boolean isJavaScript() {
    return false;
  }

  private static native boolean hasJavaScriptThread();
}
