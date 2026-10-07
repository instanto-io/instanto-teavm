/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.rules;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;

/**
 * Throwables support suppressed exceptions from construction.
 *
 * <p>In TeaVM 0.15 every throwable was left with a null {@code suppressed} array, because its field
 * initialiser ran only in constructors TeaVM discards, so {@code getSuppressed} and {@code
 * addSuppressed} threw. TeaVM 0.16 initialises it (konsoletyper/teavm#1252).
 */
@RunWith(TeaVMTestRunner.class)
@SkipJVM
public class SuppressedExceptionsTeaVmTest {

  @Test
  public void aNewThrowableHasNoSuppressedExceptions() {
    assertEquals(0, new RuntimeException("primary").getSuppressed().length);
  }

  @Test
  public void addSuppressedRecordsTheException() {
    RuntimeException primary = new RuntimeException("primary");
    RuntimeException secondary = new RuntimeException("secondary");
    primary.addSuppressed(secondary);
    assertArrayEquals(new Throwable[] {secondary}, primary.getSuppressed());
  }
}
