/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package example;

import example.dispatch.BoundaryScenarios;
import example.dispatch.DispatchScenarios;

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
    check(2, DispatchScenarios.directPrivateCall());
    check(1, DispatchScenarios.privateBaseCall());
    check(1, DispatchScenarios.inheritedPrivateBaseCall());
    check(3, DispatchScenarios.directStaticCall());
    check(1, DispatchScenarios.staticBaseCall());
    check(11, DispatchScenarios.publicOverride());
    check(21, DispatchScenarios.protectedOverride());
    check(141, DispatchScenarios.mixedReceivers());
    check(5, DispatchScenarios.defaultMethod());
    check(12, BoundaryScenarios.protectedCollision());
    check(13, BoundaryScenarios.publicCollision());
    check(14, BoundaryScenarios.packageCollision());
    check(78, BoundaryScenarios.promotions());
    check(55, BoundaryScenarios.packageReentry());
    check(64, BoundaryScenarios.independentPackageReentry());
    check(17, BoundaryScenarios.thirdPackageOverride());
    check(19, BoundaryScenarios.interfaceCollision());
    check(1168, BoundaryScenarios.interfaceDefaultHierarchy());
    check(1117856111, BoundaryScenarios.polymorphicBoundary());
    if (!"bridge".equals(BoundaryScenarios.covariantBridge())) throw new AssertionError();
  }

  private static void check(int expected, int actual) {
    if (expected != actual) throw new AssertionError("Expected " + expected + ", got " + actual);
  }
}
