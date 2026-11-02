/*
 * Copyright 2026 Carl Stainton
 * SPDX-License-Identifier: Apache-2.0
 */
package example;

import example.dispatch.BoundaryScenarios;

/** Independently demonstrates dispatch failure without the class-initialization reproducer. */
public final class DispatchApplication {
  public static void main(String[] args) {
    check(12, BoundaryScenarios.protectedCollision());
    check(13, BoundaryScenarios.publicCollision());
    check(14, BoundaryScenarios.packageCollision());
  }

  private static void check(int expected, int actual) {
    if (expected != actual)
      throw new AssertionError("Dispatch: expected " + expected + ", got " + actual);
  }
}
