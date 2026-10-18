/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.teavm.junit;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.net.MalformedURLException;
import java.net.URL;
import org.junit.Test;

/** The plugin is the only component that sees the classpath order rule support depends on. */
public class RuleSupportShadowingTest {

  private static final String RULE_SUPPORT = "jar:file:/repo/teavm-rule-support-0.1.0.jar!/";
  private static final String TEAVM_JUNIT = "jar:file:/repo/teavm-junit-0.15.0.jar!/";
  private static final String PLUGIN = "org/teavm/junit/RuleSupportPlugin.class";
  private static final String ENTRY_POINT = "org/teavm/junit/TestEntryPoint.class";

  @Test
  @SuppressWarnings("java:S2699")
  public void acceptsRuleSupportAheadOfTeaVmJunit() throws MalformedURLException {
    RuleSupportPlugin.requireSameOrigin(
        new URL(RULE_SUPPORT + PLUGIN), new URL(RULE_SUPPORT + ENTRY_POINT));
  }

  @Test
  @SuppressWarnings("java:S2699")
  public void acceptsClassesBuiltIntoADirectory() throws MalformedURLException {
    String target = "file:/build/teavm-rule-support/target/classes/";
    RuleSupportPlugin.requireSameOrigin(new URL(target + PLUGIN), new URL(target + ENTRY_POINT));
  }

  @Test
  public void reportsTeaVmJunitWinningTheOrdering() throws MalformedURLException {
    URL pluginUrl = new URL(RULE_SUPPORT + PLUGIN);
    URL entryPointUrl = new URL(TEAVM_JUNIT + ENTRY_POINT);
    IllegalStateException failure =
        assertThrows(
            IllegalStateException.class,
            () -> RuleSupportPlugin.requireSameOrigin(pluginUrl, entryPointUrl));

    assertTrue(
        "names the offending jar: " + failure.getMessage(),
        failure.getMessage().contains("teavm-junit-0.15.0.jar"));
    assertTrue(
        "says how to fix it: " + failure.getMessage(),
        failure.getMessage().contains("before org.teavm:teavm-junit"));
  }

  @Test
  @SuppressWarnings("java:S2699")
  public void staysSilentWhenTheClasspathCannotBeInspected() throws MalformedURLException {
    RuleSupportPlugin.requireSameOrigin(new URL(RULE_SUPPORT + PLUGIN), null);
    RuleSupportPlugin.requireSameOrigin(null, new URL(TEAVM_JUNIT + ENTRY_POINT));
  }
}
