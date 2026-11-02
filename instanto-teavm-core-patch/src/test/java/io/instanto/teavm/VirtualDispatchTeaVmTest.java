/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.teavm;

import static org.junit.Assert.assertEquals;

import example.dispatch.BoundaryScenarios;
import example.dispatch.DispatchScenarios;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
public class VirtualDispatchTeaVmTest {
  @Test
  public void privateMethodDoesNotOverrideBaseMethod() {
    assertEquals(2, DispatchScenarios.directPrivateCall());
    assertEquals(1, DispatchScenarios.privateBaseCall());
  }

  @Test
  public void privateMethodInAnIntermediateClassDoesNotOverrideBaseMethod() {
    assertEquals(1, DispatchScenarios.inheritedPrivateBaseCall());
  }

  @Test
  public void staticMethodDoesNotOverrideBaseMethod() {
    assertEquals(3, DispatchScenarios.directStaticCall());
    assertEquals(1, DispatchScenarios.staticBaseCall());
  }

  @Test
  public void realOverridesArePreserved() {
    assertEquals(11, DispatchScenarios.publicOverride());
    assertEquals(21, DispatchScenarios.protectedOverride());
  }

  @Test
  public void polymorphicReceiversKeepTheCorrectTargets() {
    assertEquals(141, DispatchScenarios.mixedReceivers());
  }

  @Test
  public void interfaceDefaultsArePreserved() {
    assertEquals(5, DispatchScenarios.defaultMethod());
  }

  @Test
  public void protectedCollision() {
    assertEquals(12, BoundaryScenarios.protectedCollision());
  }

  @Test
  public void publicCollision() {
    assertEquals(13, BoundaryScenarios.publicCollision());
  }

  @Test
  public void packageCollision() {
    assertEquals(14, BoundaryScenarios.packageCollision());
  }

  @Test
  public void promotions() {
    assertEquals(78, BoundaryScenarios.promotions());
  }

  @Test
  public void packageReentry() {
    assertEquals(55, BoundaryScenarios.packageReentry());
  }

  @Test
  public void independentPackageReentry() {
    assertEquals(64, BoundaryScenarios.independentPackageReentry());
  }

  @Test
  public void thirdPackageOverride() {
    assertEquals(17, BoundaryScenarios.thirdPackageOverride());
  }

  @Test
  public void interfaceCollision() {
    assertEquals(19, BoundaryScenarios.interfaceCollision());
  }

  @Test
  public void interfaceDefaultHierarchy() {
    assertEquals(1168, BoundaryScenarios.interfaceDefaultHierarchy());
  }

  @Test
  public void polymorphicBoundary() {
    assertEquals(1117856111, BoundaryScenarios.polymorphicBoundary());
  }

  @Test
  public void covariantBridge() {
    assertEquals("bridge", BoundaryScenarios.covariantBridge());
  }
}
