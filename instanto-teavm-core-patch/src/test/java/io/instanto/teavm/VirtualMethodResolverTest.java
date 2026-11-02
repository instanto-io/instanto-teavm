/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.teavm;

import static org.junit.Assert.*;

import java.util.List;
import org.junit.Test;
import org.teavm.backend.wasm.vtable.WasmGCVirtualTableProvider;
import org.teavm.model.*;
import org.teavm.model.classes.VirtualTableBuilder;

/** Compiler-level cases supplement the same Java scenarios executed by the JVM/browser runner. */
public class VirtualMethodResolverTest {
  private static final MethodDescriptor VALUE = new MethodDescriptor("value", int.class);
  private final MutableClassHolderSource source = new MutableClassHolderSource();

  @Test
  public void allForeignAccessLevelsAndStaticMethodsLeavePackageBaseAlone() {
    var base = method("one.Base", null, AccessLevel.PACKAGE_PRIVATE);
    for (AccessLevel access : AccessLevel.values()) {
      var derived = method("two." + access, "one.Base", access);
      assertSame(base, resolve(derived, base));
      derived.getModifiers().add(ElementModifier.STATIC);
      assertSame(base, resolve(derived, base));
    }
  }

  @Test
  public void samePackageAndPublicProtectedOverridesRemainVirtual() {
    for (AccessLevel baseAccess :
        List.of(AccessLevel.PACKAGE_PRIVATE, AccessLevel.PROTECTED, AccessLevel.PUBLIC)) {
      var base = method("one.Base" + baseAccess, null, baseAccess);
      var local = method("one.Local" + baseAccess, base.getOwnerName(), AccessLevel.PUBLIC);
      assertSame(local, resolve(local, base));
      var foreign = method("two.Foreign" + baseAccess, base.getOwnerName(), AccessLevel.PUBLIC);
      assertSame(
          baseAccess == AccessLevel.PACKAGE_PRIVATE ? base : foreign, resolve(foreign, base));
    }
  }

  @Test
  public void privateDeclarationIsSelectedDirectlyAndPrivateCandidatesAreSkipped() {
    var base = method("one.Base", null, AccessLevel.PRIVATE);
    var child = method("one.Child", "one.Base", AccessLevel.PUBLIC);
    assertSame(base, resolve(child, base));
    base.setLevel(AccessLevel.PROTECTED);
    child.setLevel(AccessLevel.PRIVATE);
    assertSame(base, resolve(child, base));
  }

  @Test
  public void promotionIsTransitiveAndPackageReentryCanOverrideTwoFamilies() {
    var base = method("one.Base", null, AccessLevel.PACKAGE_PRIVATE);
    var foreign = method("two.Foreign", "one.Base", AccessLevel.PUBLIC);
    var reentry = method("one.Reentry", "two.Foreign", AccessLevel.PUBLIC);
    var third = method("three.Third", "one.Reentry", AccessLevel.PUBLIC);
    assertSame(reentry, resolve(reentry, base));
    assertSame(reentry, resolve(reentry, foreign));
    assertSame(third, resolve(third, base));
    assertTrue(VirtualMethodResolver.slots(source, third.getReference()).contains(slot(base)));
    assertTrue(VirtualMethodResolver.slots(source, third.getReference()).contains(slot(foreign)));
  }

  @Test
  public void inheritedSymbolicReferenceUsesItsActualDeclaration() {
    var base = method("one.Base", null, AccessLevel.PACKAGE_PRIVATE);
    cls("one.Middle", "one.Base");
    var child = method("two.Child", "one.Middle", AccessLevel.PROTECTED);
    var inherited = new MethodReference("one.Middle", VALUE);
    assertSame(base, VirtualMethodResolver.resolve(source, child.getOwnerName(), inherited));
    assertEquals(slot(base), VirtualMethodResolver.slot(source, inherited));
    assertNotEquals(slot(base), slot(child));
  }

  @Test
  public void abstractDeclarationSuppressesSuperclassImplementation() {
    var base = method("one.Base", null, AccessLevel.PUBLIC);
    var child = method("one.Child", "one.Base", AccessLevel.PUBLIC);
    child.getModifiers().add(ElementModifier.ABSTRACT);
    assertSame(child, resolve(child, base));
  }

  @Test
  public void mostSpecificInterfaceWinsAndAbstractRedeclarationSuppressesDefault() {
    var base = method("one.Interface", null, AccessLevel.PUBLIC);
    source.get(base.getOwnerName()).getModifiers().add(ElementModifier.INTERFACE);
    var sub = method("two.SubInterface", null, AccessLevel.PUBLIC);
    source.get(sub.getOwnerName()).getModifiers().add(ElementModifier.INTERFACE);
    source.get(sub.getOwnerName()).getInterfaces().add(base.getOwnerName());
    cls("three.Impl", null).getInterfaces().add(sub.getOwnerName());
    assertSame(sub, VirtualMethodResolver.resolve(source, "three.Impl", base.getReference()));
    sub.getModifiers().add(ElementModifier.ABSTRACT);
    assertNull(VirtualMethodResolver.resolve(source, "three.Impl", base.getReference()));
  }

  @Test
  public void cAndWasmTablesKeepIndependentSlotsAndUpdateEveryRealOverride() {
    cls("java.lang.Object", null);
    // Like a real runtime class source, include the standard marker interface.
    cls("java.lang.Cloneable", null).getModifiers().add(ElementModifier.INTERFACE);
    var base = method("one.Base", "java.lang.Object", AccessLevel.PACKAGE_PRIVATE);
    var foreign = method("two.Foreign", "one.Base", AccessLevel.PUBLIC);
    var reentry = method("one.Reentry", "two.Foreign", AccessLevel.PUBLIC);
    var calls = List.of(base.getReference(), foreign.getReference());
    var builder = new VirtualTableBuilder(source);
    builder.setMethodsUsedAtCallSites(calls);
    var tables = builder.build();
    assertNotEquals(
        tables.lookup(base.getReference()).getIndex(),
        tables.lookup(foreign.getReference()).getIndex());
    assertEquals(
        base.getReference(), tables.lookup("two.Foreign").getEntry(slot(base)).getImplementor());
    assertEquals(
        foreign.getReference(),
        tables.lookup("two.Foreign").getEntry(slot(foreign)).getImplementor());
    assertEquals(
        reentry.getReference(), tables.lookup("one.Reentry").getEntry(slot(base)).getImplementor());
    assertEquals(
        reentry.getReference(),
        tables.lookup("one.Reentry").getEntry(slot(foreign)).getImplementor());
    var wasm = new WasmGCVirtualTableProvider(source, calls, m -> true);
    assertNotEquals(
        wasm.lookup(base.getReference()).getIndex(),
        wasm.lookup(foreign.getReference()).getIndex());
    assertEquals(
        base.getReference(),
        wasm.lookup("two.Foreign").implementor(wasm.lookup(base.getReference())));
    assertEquals(
        foreign.getReference(),
        wasm.lookup("two.Foreign").implementor(wasm.lookup(foreign.getReference())));
    assertEquals(
        reentry.getReference(),
        wasm.lookup("one.Reentry").implementor(wasm.lookup(base.getReference())));
    assertEquals(
        reentry.getReference(),
        wasm.lookup("one.Reentry").implementor(wasm.lookup(foreign.getReference())));
  }

  @Test
  public void missingDeclarationsAndClassesDoNotInventTargets() {
    var base = method("one.Base", null, AccessLevel.PUBLIC);
    assertNull(VirtualMethodResolver.resolve(source, "missing.Class", base.getReference()));
    assertNull(
        VirtualMethodResolver.resolve(
            source, "one.Base", new MethodReference("missing.Class", VALUE)));
  }

  private MethodReader resolve(MethodHolder receiver, MethodHolder declaration) {
    return VirtualMethodResolver.resolve(
        source, receiver.getOwnerName(), declaration.getReference());
  }

  private MethodDescriptor slot(MethodHolder method) {
    return VirtualMethodResolver.slot(source, method.getReference());
  }

  private ClassHolder cls(String name, String parent) {
    ClassHolder cls = new ClassHolder(name);
    cls.setParent(parent);
    source.putClassHolder(cls);
    return cls;
  }

  private MethodHolder method(String name, String parent, AccessLevel access) {
    ClassHolder cls = cls(name, parent);
    MethodHolder method = new MethodHolder(VALUE);
    method.setLevel(access);
    method.getModifiers().add(ElementModifier.NATIVE);
    cls.addMethod(method);
    return method;
  }
}
