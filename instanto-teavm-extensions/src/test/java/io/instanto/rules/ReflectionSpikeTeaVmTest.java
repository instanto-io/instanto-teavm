/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.rules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.junit.runner.RunWith;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;

/**
 * Guards the reflection policy: proves the pieces MethodRule support needs are available under
 * TeaVM: real reflection metadata (via the registered ReflectionPolicy), a real {@link
 * FrameworkMethod}, and rule application.
 */
@RunWith(TeaVMTestRunner.class)
@SkipJVM
public class ReflectionSpikeTeaVmTest {

  /**
   * The reflection policy retains {@code @Test} methods only, so the probe target is itself a test
   * method. It asserts nothing; it exists to be resolved reflectively below.
   */
  @Test
  public void probeTarget() {}

  @Test
  public void canObtainAndInvokeADeclaredMethod() throws Exception {
    Method method = ReflectionSpikeTeaVmTest.class.getDeclaredMethod("probeTarget");
    assertNotNull("getDeclaredMethod returned null", method);
    assertEquals("probeTarget", method.getName());
    assertNull("a void method invoked reflectively returns null", method.invoke(this));
  }

  @Test
  public void canBuildAFrameworkMethodFromThatReflection() throws Throwable {
    Method method = ReflectionSpikeTeaVmTest.class.getDeclaredMethod("probeTarget");
    FrameworkMethod frameworkMethod = new FrameworkMethod(method);

    assertEquals("probeTarget", frameworkMethod.getName());
    frameworkMethod.invokeExplosively(this);
  }

  @Test
  public void aMethodRuleCanWrapAStatementUsingThatFrameworkMethod() throws Throwable {
    Method method = ReflectionSpikeTeaVmTest.class.getDeclaredMethod("probeTarget");
    FrameworkMethod frameworkMethod = new FrameworkMethod(method);

    boolean[] ran = {false};
    Statement base =
        new Statement() {
          @Override
          public void evaluate() {
            ran[0] = true;
          }
        };

    String[] observedName = {null};
    MethodRule rule =
        (statement, describedMethod, target) ->
            new Statement() {
              @Override
              public void evaluate() throws Throwable {
                observedName[0] = describedMethod.getName();
                statement.evaluate();
              }
            };

    rule.apply(base, frameworkMethod, this).evaluate();

    assertTrue("wrapped statement did not run", ran[0]);
    assertEquals("probeTarget", observedName[0]);
  }
}
