/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package example;

public final class InitializationApplication {
  public static void main(String[] args) {
    check(500, InitializationScenarios.umbrella());
    check(1, InitializationScenarios.enumConstant());
    check(500, InitializationScenarios.computedPrimitive());
    check(500, InitializationScenarios.fallbackHelper());
    check(500, InitializationScenarios.afterCatch());
    check(500, InitializationScenarios.nestedCatch());
    check(500, InitializationScenarios.finallyFallback());
    check(7, InitializationScenarios.lazyInitialization());
    if (InitializationScenarios.boxedBoolean()) throw new AssertionError();
  }

  private static void check(int expected, int actual) {
    if (expected != actual) throw new AssertionError("Expected " + expected + ", got " + actual);
  }
}
