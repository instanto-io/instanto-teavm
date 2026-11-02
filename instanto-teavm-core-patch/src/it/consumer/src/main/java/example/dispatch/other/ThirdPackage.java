/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package example.dispatch.other;

import example.dispatch.BoundaryScenarios;

public class ThirdPackage extends BoundaryScenarios.PublicDerived {
  @Override
  public int value() {
    return 7;
  }
}
