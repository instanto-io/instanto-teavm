/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package example;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;

/** A separate ADVANCED/FULL failure: the class is initialized before entering the try/catch. */
@RunWith(TeaVMTestRunner.class)
public class RepeatedFieldReadTest {
  @Test
  public void readsFieldAfterException() {
    assertEquals(1, Holder.WARMUP);
    assertEquals(7, read());
  }

  private static void operation() {
    Integer.parseInt("invalid input");
  }

  private static int read() {
    try {
      operation();
      return Holder.VALUE;
    } catch (Exception failure) {
      return Holder.VALUE;
    }
  }

  static class Holder {
    static final int WARMUP = Integer.parseInt("1");
    static final int VALUE = Integer.parseInt("7");
  }
}
