/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.teavm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import example.InitializationScenarios;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
public class ClassInitializationTeaVmTest {
  @Test
  public void umbrella() {
    assertEquals(500, InitializationScenarios.umbrella());
  }

  @Test
  public void enumConstant() {
    assertEquals(1, InitializationScenarios.enumConstant());
  }

  @Test
  public void computedPrimitive() {
    assertEquals(500, InitializationScenarios.computedPrimitive());
  }

  @Test
  public void fallbackHelper() {
    assertEquals(500, InitializationScenarios.fallbackHelper());
  }

  @Test
  public void afterCatch() {
    assertEquals(500, InitializationScenarios.afterCatch());
  }

  @Test
  public void nestedCatch() {
    assertEquals(500, InitializationScenarios.nestedCatch());
  }

  @Test
  public void finallyFallback() {
    assertEquals(500, InitializationScenarios.finallyFallback());
  }

  @Test
  public void lazyInitialization() {
    assertEquals(7, InitializationScenarios.lazyInitialization());
  }

  @Test
  public void boxedBoolean() {
    assertFalse(InitializationScenarios.boxedBoolean());
  }
}
