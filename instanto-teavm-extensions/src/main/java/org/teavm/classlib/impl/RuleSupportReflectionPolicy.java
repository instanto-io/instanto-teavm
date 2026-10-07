/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.teavm.classlib.impl;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.extension.Autoregistered;
import org.teavm.extension.spi.reflection.SimpleReflectionPolicy;

/**
 * Retains reflection metadata for JUnit test classes so {@code MethodRule} can be given a real
 * {@link java.lang.reflect.Method}.
 */
@Autoregistered
public class RuleSupportReflectionPolicy extends SimpleReflectionPolicy {
  @Override
  protected void setup() {
    // Only @Test methods need to be resolvable; that is all a FrameworkMethod ever names.
    selectClasses(withAnnotation(RunWith.class)).reflectableMethods(withAnnotation(Test.class));
  }
}
