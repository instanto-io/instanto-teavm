/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.teavm.classlib;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;

/**
 * Suppressed exceptions work whichever {@code Throwable} constructor ran.
 *
 * <p>TeaVM 0.15 left the list null after some constructors, and teavm-compat patched it during
 * compilation. TeaVM 0.16 needs no patch; this keeps a TeaVM upgrade from bringing the fault back.
 */
@RunWith(TeaVMTestRunner.class)
public class ThrowableSuppressedTest {

  @Test
  public void theFullConstructorStartsWithNoSuppressedExceptions() {
    assertEquals(0, new Detailed("failed").getSuppressed().length);
  }

  @Test
  public void theFullConstructorAcceptsSuppressedExceptions() {
    Detailed failure = new Detailed("failed");
    IllegalStateException cleanup = new IllegalStateException("cleanup");
    failure.addSuppressed(cleanup);
    assertArrayEquals(new Throwable[] {cleanup}, failure.getSuppressed());
  }

  @Test
  public void theOrdinaryConstructorsAcceptSuppressedExceptions() {
    RuntimeException failure = new RuntimeException("failed", new IllegalArgumentException());
    IllegalStateException cleanup = new IllegalStateException("cleanup");
    failure.addSuppressed(cleanup);
    assertArrayEquals(new Throwable[] {cleanup}, failure.getSuppressed());
  }

  /** Uses {@code Throwable(String, Throwable, boolean, boolean)}. */
  private static final class Detailed extends RuntimeException {
    Detailed(String message) {
      super(message, null, true, true);
    }
  }
}
