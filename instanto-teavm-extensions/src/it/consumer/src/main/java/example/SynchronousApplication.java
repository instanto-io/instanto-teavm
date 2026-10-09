/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package example;

import org.teavm.jso.JSExport;

/** A real main/export boundary without any coroutine or test-runner machinery. */
public final class SynchronousApplication {
  private static final ThreadLocal<Integer> LOCAL = ThreadLocal.withInitial(() -> 13);
  private static boolean completed;

  public static void main(String[] args) {
    if (LOCAL.get() != 13) {
      throw new AssertionError("initial value");
    }
    LOCAL.set(42);
    if (LOCAL.get() != 42) {
      throw new AssertionError("set value");
    }
    LOCAL.remove();
    if (LOCAL.get() != 13) {
      throw new AssertionError("removed value");
    }
    if (args.length != 0) {
      throw new IllegalStateException("expected entry failure");
    }
    completed = true;
  }

  @JSExport
  public static boolean completed() {
    return completed;
  }

  @JSExport
  public static int callback() {
    int rejected = 0;
    try {
      LOCAL.get();
    } catch (UnsupportedOperationException expected) {
      rejected |= 1;
    }
    try {
      LOCAL.set(13);
    } catch (UnsupportedOperationException expected) {
      rejected |= 2;
    }
    try {
      LOCAL.remove();
    } catch (UnsupportedOperationException expected) {
      rejected |= 4;
    }
    return rejected;
  }
}
