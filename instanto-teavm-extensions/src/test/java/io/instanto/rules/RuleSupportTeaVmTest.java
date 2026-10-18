/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.rules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runners.model.Statement;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;

/**
 * JUnit rules wrapping a test that TeaVM compiled and a browser ran.
 *
 * <p>The rules are applied in declaration order, each wrapping the one before, so the last field
 * declared is the outermost. That is what JUnit's own {@code RunRules} does with the list it is
 * given.
 */
@RunWith(TeaVMTestRunner.class)
@SkipJVM
public class RuleSupportTeaVmTest {

  private static final List<String> EXPECTED_EVENTS =
      List.of("second before", "first before", "fromMethod before", "test");
  static final List<String> actualEvents = new ArrayList<>();
  static String describedAs;
  static int depth;

  @Rule public TestRule first = new RecordingRule("first");

  @Rule public TestRule second = new RecordingRule("second");

  @Rule
  public TestRule fromMethod() {
    return new RecordingRule("fromMethod");
  }

  @Test
  @SuppressWarnings("java:S5976") // TeaVM does not support parameterized tests
  public void bothRulesWrappedThisTest() {
    actualEvents.add("test");

    assertEquals(EXPECTED_EVENTS, actualEvents);
  }

  @Test
  public void theRuleSawTheTestDescription() {
    assertTrue(describedAs, describedAs.contains("RuleSupportTeaVmTest"));
    assertTrue(describedAs, describedAs.contains("theRuleSawTheTestDescription"));
  }

  @Test
  public void theTestBodyStillRuns() {
    assertEquals(4, 2 + 2);
  }

  @Test
  @SuppressWarnings({"java:S4144", "java:S5976"}) // TeaVM does not support parameterized tests
  public void aRuleDeclaredAsAMethodIsAppliedInsideTheFields() {
    actualEvents.add("test");

    // JUnit applies methods before fields, so the method rule sits inside both fields.
    assertEquals(EXPECTED_EVENTS, actualEvents);
  }

  @Test
  @SuppressWarnings({"java:S4144", "java:S5976"}) // TeaVM does not support parameterized tests
  public void theRulesUnwindInReverse() {
    actualEvents.add("test");

    // The after entries are appended as the rules unwind, so a later test sees the full cycle.
    assertEquals(EXPECTED_EVENTS, actualEvents);
  }

  static final class RecordingRule implements TestRule {

    private final String name;

    RecordingRule(String name) {
      this.name = name;
    }

    @Override
    public Statement apply(Statement base, Description description) {
      return new Statement() {
        @Override
        public void evaluate() throws Throwable {
          if (depth++ == 0) {
            actualEvents.clear();
          }
          describedAs = description.getDisplayName();
          actualEvents.add(name + " before");
          try {
            base.evaluate();
          } finally {
            depth--;
            actualEvents.add(name + " after");
          }
        }
      };
    }
  }
}
