/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.rules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.junit.runner.RunWith;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;

/**
 * A void method invoked reflectively returns {@code null}, as the JDK specifies, both directly and
 * through the {@code FrameworkMethod} a {@code MethodRule} is given.
 *
 * <p>TeaVM 0.15 returned JavaScript {@code undefined} instead, so rule support used to normalise
 * the result. TeaVM 0.16 returns {@code null} (konsoletyper/teavm#1261).
 */
@RunWith(TeaVMTestRunner.class)
@SkipJVM
public class VoidInvokeTeaVmTest {

  /**
   * Asserts what a MethodRule sees when it invokes a void test method reflectively.
   *
   * <p>Only {@link VoidInvokeTeaVmTest#probeVoidMethod()} is invoked, so the extra call re-enters
   * an empty body. Probing whichever test happened to be running would re-enter that test's own
   * assertions.
   */
  static final class InvokingRule implements MethodRule {
    static final String PROBE = "probeVoidMethod";

    @Override
    public Statement apply(Statement base, FrameworkMethod method, Object target) {
      return new Statement() {
        @Override
        public void evaluate() throws Throwable {
          if (PROBE.equals(method.getName())) {
            assertNull(
                "a void method invoked through a MethodRule returns null",
                method.invokeExplosively(target));
          }
          base.evaluate();
        }
      };
    }
  }

  @Rule public InvokingRule invokingRule = new InvokingRule();

  @Test
  public void rawReflectionReturnsNullForAVoidMethod() throws Exception {
    Method method = VoidInvokeTeaVmTest.class.getDeclaredMethod("probeVoidMethod");
    assertEquals(void.class, method.getReturnType());
    assertTrue("Void.TYPE is void.class", Void.TYPE == method.getReturnType());

    assertNull(method.invoke(this));
  }

  /** The rule invokes this method reflectively and checks the result; the body stays empty. */
  @Test
  public void probeVoidMethod() {}
}
