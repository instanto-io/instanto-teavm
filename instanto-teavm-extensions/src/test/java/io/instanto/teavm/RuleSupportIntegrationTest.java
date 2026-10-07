/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.teavm;

import static org.junit.Assert.*;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.junit.runners.model.Statement;
import org.teavm.junit.OnlyPlatform;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMProperties;
import org.teavm.junit.TeaVMProperty;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.TestPlatform;

/** Verifies the rule-support and guard compiler plugins work together. */
@RunWith(TeaVMTestRunner.class)
@SkipJVM
@OnlyPlatform({TestPlatform.JAVASCRIPT, TestPlatform.WEBASSEMBLY_GC})
@TeaVMProperties(@TeaVMProperty(key = ThreadLocalChecksPlugin.PROPERTY, value = "true"))
public class RuleSupportIntegrationTest {
  private final ThreadLocal<String> context = new ThreadLocal<>();

  @Rule
  public final TestRule contextRule =
      (base, description) ->
          new Statement() {
            @Override
            public void evaluate() throws Throwable {
              assertNull(context.get());
              context.set(description.getMethodName());
              try {
                base.evaluate();
                assertEquals(description.getMethodName(), context.get());
              } finally {
                context.remove();
                assertNull(context.get());
              }
            }
          };

  @Test
  public void ruleContextSurvivesSuspensionWithChecksEnabled() throws Exception {
    assertEquals("ruleContextSurvivesSuspensionWithChecksEnabled", context.get());
    Thread.sleep(1);
    assertEquals("ruleContextSurvivesSuspensionWithChecksEnabled", context.get());
  }
}
