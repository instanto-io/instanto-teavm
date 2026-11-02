/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package example.dispatch;

import example.dispatch.base.PackageBase;
import example.dispatch.base.Reentered;
import example.dispatch.other.ThirdPackage;

public final class BoundaryScenarios {
  private BoundaryScenarios() {}

  public static class ProtectedDerived extends PackageBase {
    protected int value() {
      return 2;
    }

    public int readDerivedValue() {
      return value();
    }
  }

  public static class PublicDerived extends PackageBase {
    public int value() {
      return 3;
    }

    public int readDerivedValue() {
      return value();
    }
  }

  public static class PackageDerived extends PackageBase {
    int value() {
      return 4;
    }

    public int readDerivedValue() {
      return value();
    }
  }

  public static class PromotedPublic extends PackageBase.PublicPromotion {
    @Override
    public int value() {
      return 7;
    }
  }

  public static class PromotedProtected extends PackageBase.ProtectedPromotion {
    @Override
    protected int value() {
      return 8;
    }
  }

  public interface Value {
    int value();
  }

  public static class InterfaceDerived extends PackageBase implements Value {
    @Override
    public int value() {
      return 9;
    }
  }

  public interface DefaultValue {
    default int defaultValue() {
      return 6;
    }
  }

  public static class DefaultDerived extends PackageBase implements DefaultValue {}

  public interface ChildDefault extends DefaultValue {
    default int defaultValue() {
      return 8;
    }
  }

  public static class ChildDefaultDerived extends PackageBase implements ChildDefault {}

  public static int protectedCollision() {
    ProtectedDerived x = new ProtectedDerived();
    return x.readBaseValue() * 10 + x.readDerivedValue();
  }

  public static int publicCollision() {
    PublicDerived x = new PublicDerived();
    return x.readBaseValue() * 10 + x.readDerivedValue();
  }

  public static int packageCollision() {
    PackageDerived x = new PackageDerived();
    return x.readBaseValue() * 10 + x.readDerivedValue();
  }

  public static int promotions() {
    return new PromotedPublic().readBaseValue() * 10 + new PromotedProtected().readBaseValue();
  }

  public static int packageReentry() {
    Reentered.Public x = new Reentered.Public();
    return x.readBaseValue() * 10 + x.readDerivedValue();
  }

  public static int independentPackageReentry() {
    Reentered.Package x = new Reentered.Package();
    return x.readBaseValue() * 10 + x.readDerivedValue();
  }

  public static int thirdPackageOverride() {
    ThirdPackage x = new ThirdPackage();
    return x.readBaseValue() * 10 + x.readDerivedValue();
  }

  public static int interfaceCollision() {
    InterfaceDerived x = new InterfaceDerived();
    Value v = x;
    return x.readBaseValue() * 10 + v.value();
  }

  public static int interfaceDefaultHierarchy() {
    DefaultDerived a = new DefaultDerived();
    ChildDefaultDerived b = new ChildDefaultDerived();
    DefaultValue[] values = {a, b};
    return a.readBaseValue() * 1000
        + b.readBaseValue() * 100
        + values[0].defaultValue() * 10
        + values[1].defaultValue();
  }

  // The same call site must retain several targets after dependency analysis/optimization.
  public static int polymorphicBoundary() {
    PackageBase[] values = {
      new ProtectedDerived(),
      new PublicDerived(),
      new PackageDerived(),
      new PromotedPublic(),
      new PromotedProtected(),
      new Reentered.Public(),
      new Reentered.Package(),
      new ThirdPackage(),
      new InterfaceDerived(),
      new DefaultDerived()
    };
    int result = 0;
    for (PackageBase value : values) {
      result = result * 10 + value.readBaseValue();
    }
    return result;
  }

  public static class GenericBase<T> {
    public T get() {
      return null;
    }
  }

  public static class GenericDerived extends GenericBase<String> {
    @Override
    public String get() {
      return "bridge";
    }
  }

  public static String covariantBridge() {
    GenericBase<?> x = new GenericDerived();
    return (String) x.get();
  }
}
