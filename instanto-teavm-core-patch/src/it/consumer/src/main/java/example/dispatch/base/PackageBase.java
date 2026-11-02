/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package example.dispatch.base;

public class PackageBase {
  int value() {
    return 1;
  }

  public int readBaseValue() {
    return value();
  }

  public int publicValue() {
    return 10;
  }

  protected int protectedValue() {
    return 20;
  }

  public int readProtectedValue() {
    return protectedValue();
  }

  public static class LocalOverride extends PackageBase {
    @Override
    int value() {
      return 4;
    }
  }

  public static class PublicPromotion extends PackageBase {
    @Override
    public int value() {
      return 5;
    }
  }

  public static class ProtectedPromotion extends PackageBase {
    @Override
    protected int value() {
      return 6;
    }
  }
}
