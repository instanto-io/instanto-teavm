/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.teavm.junit;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import org.junit.Test;

/** The plugin is the only component that sees the classpath order rule support depends on. */
public class RuleSupportShadowingTest {

  private static final String RULE_SUPPORT = "jar:file:/repo/instanto-teavm-extensions-0.1.0.jar!/";
  private static final String TEAVM_JUNIT = "jar:file:/repo/teavm-junit-0.15.0.jar!/";
  private static final String PLUGIN = "org/teavm/junit/RuleSupportPlugin.class";
  private static final String ENTRY_POINT = "org/teavm/junit/TestEntryPoint.class";

  @Test
  public void acceptsRuleSupportAheadOfTeaVmJunit() throws Exception {
    RuleSupportPlugin.requireSameOrigin(
        new URL(RULE_SUPPORT + PLUGIN), new URL(RULE_SUPPORT + ENTRY_POINT));
  }

  @Test
  public void acceptsClassesBuiltIntoADirectory() throws Exception {
    String target = "file:/build/instanto-teavm-extensions/target/classes/";
    RuleSupportPlugin.requireSameOrigin(new URL(target + PLUGIN), new URL(target + ENTRY_POINT));
  }

  @Test
  public void reportsTeaVmJunitWinningTheOrdering() throws Exception {
    IllegalStateException failure =
        assertThrows(
            IllegalStateException.class,
            () ->
                RuleSupportPlugin.requireSameOrigin(
                    new URL(RULE_SUPPORT + PLUGIN), new URL(TEAVM_JUNIT + ENTRY_POINT)));

    assertTrue(
        "names the offending jar: " + failure.getMessage(),
        failure.getMessage().contains("teavm-junit-0.15.0.jar"));
    assertTrue(
        "says how to fix it: " + failure.getMessage(),
        failure.getMessage().contains("before org.teavm:teavm-junit"));
  }

  @Test
  public void staysSilentWhenTheClasspathCannotBeInspected() throws Exception {
    RuleSupportPlugin.requireSameOrigin(new URL(RULE_SUPPORT + PLUGIN), null);
    RuleSupportPlugin.requireSameOrigin(null, new URL(TEAVM_JUNIT + ENTRY_POINT));
  }
}
