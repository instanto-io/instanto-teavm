/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.rules;

import org.junit.Rule;
import org.junit.rules.TestRule;

/** Contributes a rule that its subclasses inherit. */
public abstract class DeclarationFormsBase {

  @Rule
  public TestRule inheritedFromTheBaseClass =
      new DeclarationFormsTeaVmTest.RecordingRule("inherited");
}
