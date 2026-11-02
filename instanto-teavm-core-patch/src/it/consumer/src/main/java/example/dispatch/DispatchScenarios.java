/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package example.dispatch;

import example.dispatch.base.PackageBase;

public final class DispatchScenarios {
  private DispatchScenarios() {}

  public static int privateBaseCall() {
    return new PrivateDerived().readBaseValue();
  }

  public static int directPrivateCall() {
    return new PrivateDerived().readPrivateValue();
  }

  public static int inheritedPrivateBaseCall() {
    return new FurtherDerived().readBaseValue();
  }

  public static int staticBaseCall() {
    return new StaticDerived().readBaseValue();
  }

  public static int directStaticCall() {
    return StaticDerived.value();
  }

  public static int publicOverride() {
    PackageBase instance = new RealOverride();
    return instance.publicValue();
  }

  public static int protectedOverride() {
    return new RealOverride().readProtectedValue();
  }

  public static int mixedReceivers() {
    PackageBase[] instances = {
      new PrivateDerived(), new PackageBase.LocalOverride(), new StaticDerived()
    };
    int result = 0;
    for (PackageBase instance : instances) {
      result = result * 10 + instance.readBaseValue();
    }
    return result;
  }

  public static int defaultMethod() {
    return new DefaultDerived().defaultValue();
  }

  public static class PrivateDerived extends PackageBase {
    // Legal Java: this is not an override of the other package's method.
    private int value() {
      return 2;
    }

    public int readPrivateValue() {
      return value();
    }
  }

  public static class FurtherDerived extends PrivateDerived {}

  public static class StaticDerived extends PackageBase {
    private static int value() {
      return 3;
    }
  }

  public static class RealOverride extends PackageBase {
    @Override
    public int publicValue() {
      return 11;
    }

    @Override
    protected int protectedValue() {
      return 21;
    }
  }

  public interface DefaultValue {
    default int defaultValue() {
      return 5;
    }
  }

  public static class DefaultDerived extends PrivateDerived implements DefaultValue {}
}
