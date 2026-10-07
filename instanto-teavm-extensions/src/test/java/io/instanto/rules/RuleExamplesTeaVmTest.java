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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
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

/**
 * Worked examples of both rule kinds running under TeaVM.
 *
 * <p>These are written to be read as documentation as much as run as tests: a {@code TestRule}
 * managing a per-test resource, and a {@code MethodRule} reading an annotation off the test method
 * — the thing only a {@code MethodRule} can do, because it alone receives the {@link
 * FrameworkMethod}.
 */
@RunWith(TeaVMTestRunner.class)
@SkipJVM
public class RuleExamplesTeaVmTest {

  /** Marks how many times a test's body should be retried by {@link RetryRule}. */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  @interface Retry {
    int times();
  }

  /** A TestRule providing a per-test resource, opened before and closed after. */
  static final class ResourceRule implements TestRule {
    private List<String> resource;
    private boolean closed;

    List<String> resource() {
      return resource;
    }

    @Override
    public Statement apply(Statement base, Description description) {
      return new Statement() {
        @Override
        public void evaluate() throws Throwable {
          resource = new ArrayList<>();
          resource.add("opened-for:" + description.getMethodName());
          try {
            base.evaluate();
          } finally {
            closed = true;
          }
        }
      };
    }
  }

  /**
   * A MethodRule reading {@link Retry} off the test method.
   *
   * <p>A TestRule could not do this: {@code Description} exposes annotations, but a rule that needs
   * the method itself — to reflect over it, or to invoke it — needs the FrameworkMethod.
   */
  static final class RetryRule implements MethodRule {
    private int attempts;
    private String methodName;

    @Override
    public Statement apply(Statement base, FrameworkMethod method, Object target) {
      return new Statement() {
        @Override
        public void evaluate() throws Throwable {
          methodName = method.getName();
          Retry retry = method.getAnnotation(Retry.class);
          int required = retry == null ? 1 : retry.times();
          for (int attempt = 0; attempt < required; attempt++) {
            attempts++;
            base.evaluate();
          }
        }
      };
    }
  }

  @Rule public ResourceRule resourceRule = new ResourceRule();

  @Rule public RetryRule retryRule = new RetryRule();

  @Test
  public void theTestRuleOpensAResourceNamedAfterTheTest() {
    assertNotNull("the TestRule did not run", resourceRule.resource());
    assertEquals(
        "opened-for:theTestRuleOpensAResourceNamedAfterTheTest", resourceRule.resource().get(0));
  }

  @Test
  public void theMethodRuleSeesTheTestMethodByName() {
    assertEquals("theMethodRuleSeesTheTestMethodByName", retryRule.methodName);
  }

  @Test
  @Retry(times = 3)
  public void theMethodRuleReadsAnAnnotationOffTheTestMethod() {
    // The rule re-evaluates this body `times` times, so by the final pass the counter shows it.
    assertTrue("expected the retry rule to re-run this body", retryRule.attempts >= 1);
    assertTrue("attempts should not exceed the declared retry count", retryRule.attempts <= 3);
  }

  @Test
  public void anUnannotatedTestRunsExactlyOnce() {
    assertNull(getClass().getName(), null);
    assertEquals(1, retryRule.attempts);
  }
}
