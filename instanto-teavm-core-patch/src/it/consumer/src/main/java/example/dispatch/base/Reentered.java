/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package example.dispatch.base;

import example.dispatch.BoundaryScenarios;

public final class Reentered {
  public static class Public extends BoundaryScenarios.PublicDerived {
    @Override
    public int value() {
      return 5;
    }
  }

  public static class Package extends BoundaryScenarios.PackageDerived {
    @Override
    int value() {
      return 6;
    }
  }
}
