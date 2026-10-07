/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.rules;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;

/** Exercises {@code MethodRule} support, mirroring JUnit 4.13 application order. */
@RunWith(TeaVMTestRunner.class)
@SkipJVM
public class MethodRuleTeaVmTest {

  static final List<String> EVENTS = new ArrayList<>();

  /** A MethodRule that records the method name it was handed. */
  static final class RecordingMethodRule implements MethodRule {
    private final String label;

    RecordingMethodRule(String label) {
      this.label = label;
    }

    @Override
    public Statement apply(Statement base, FrameworkMethod method, Object target) {
      return new Statement() {
        @Override
        public void evaluate() throws Throwable {
          EVENTS.add("enter:" + label + ":" + method.getName());
          base.evaluate();
          EVENTS.add("exit:" + label);
        }
      };
    }
  }

  /** A TestRule, for checking it ends up outside the MethodRule at equal order. */
  static final class RecordingTestRule implements TestRule {
    private final String label;

    RecordingTestRule(String label) {
      this.label = label;
    }

    @Override
    public Statement apply(Statement base, Description description) {
      return new Statement() {
        @Override
        public void evaluate() throws Throwable {
          EVENTS.add("enter:" + label);
          base.evaluate();
          EVENTS.add("exit:" + label);
        }
      };
    }
  }

  @Rule public RecordingMethodRule methodRule = new RecordingMethodRule("method");

  @Rule public RecordingTestRule testRule = new RecordingTestRule("test");

  @Test
  public void methodRuleReceivesTheTestMethodAndRunsInsideTheTestRule() {
    // The rules wrap this body, so assert on what they recorded before it.
    assertEquals("enter:test", EVENTS.get(EVENTS.size() - 2));
    assertEquals(
        "enter:method:methodRuleReceivesTheTestMethodAndRunsInsideTheTestRule",
        EVENTS.get(EVENTS.size() - 1));
  }
}
