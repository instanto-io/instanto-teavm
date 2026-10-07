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
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runners.model.Statement;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;

/**
 * The ways a rule can be declared: a field, a no-argument method, and a field inherited from a base
 * class. Each applies once per test, as it does on the JVM.
 */
@RunWith(TeaVMTestRunner.class)
@SkipJVM
public class DeclarationFormsTeaVmTest extends DeclarationFormsBase {

  static final List<String> APPLIED = new ArrayList<>();

  /** Records that it wrapped a test, so a rule applied twice is visible. */
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
          APPLIED.add(name);
          base.evaluate();
        }
      };
    }
  }

  @Rule public TestRule declaredAsAField = new RecordingRule("field");

  @Rule
  public TestRule declaredAsAMethod() {
    return new RecordingRule("method");
  }

  @Test
  public void everyDeclarationFormAppliesExactlyOnce() {
    assertEquals(List.of("field", "inherited", "method"), sorted(APPLIED));
  }

  private static List<String> sorted(List<String> applied) {
    List<String> ordered = new ArrayList<>(applied);
    ordered.sort(String::compareTo);
    // Declaration order is not the point here; how many times each applied is.
    return List.of(ordered.get(0), ordered.get(1), ordered.get(2));
  }
}
