/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.teavm.junit;

import java.net.URL;
import org.teavm.vm.spi.TeaVMHost;
import org.teavm.vm.spi.TeaVMPlugin;

/**
 * Compiler-level installation point for JUnit rule support.
 *
 * <p>Discovered through {@code ServiceLoader}, so this runs whichever order the classpath is in.
 * That makes it the only place able to report the ordering itself: rule support works by shadowing
 * {@code org.teavm.junit.TestEntryPoint} and its transformers, and a shadowed class is chosen by
 * classpath order long before any test runs. If {@code teavm-junit} wins, TeaVM's own entry point
 * is compiled in, rules are silently ignored, and tests pass without their rules ever applying.
 */
public final class RuleSupportPlugin implements TeaVMPlugin {
  private static final String PLUGIN =
      RuleSupportPlugin.class.getName().replace('.', '/') + ".class";
  private static final String SHADOWED = "org/teavm/junit/TestEntryPoint.class";

  @Override
  public void install(TeaVMHost host) {
    verifyRuleSupportIsNotShadowed();

    // Description needs its children collection rewritten in every compilation.
    host.add(new DescriptionChildrenTransformer());
  }

  private void verifyRuleSupportIsNotShadowed() {
    ClassLoader loader = RuleSupportPlugin.class.getClassLoader();
    if (loader == null) {
      return;
    }
    requireSameOrigin(loader.getResource(PLUGIN), loader.getResource(SHADOWED));
  }

  /** Both classes ship together, so a different origin means something else supplied it. */
  static void requireSameOrigin(URL plugin, URL entryPoint) {
    String ours = containerOf(plugin, PLUGIN);
    String resolved = containerOf(entryPoint, SHADOWED);
    if (ours == null || resolved == null || ours.equals(resolved)) {
      return;
    }
    throw new IllegalStateException(
        "instanto-teavm-extensions is on the classpath but "
            + "org.teavm.junit.TestEntryPoint was resolved from "
            + resolved
            + " instead of "
            + ours
            + ". TeaVM's own entry point takes precedence, so JUnit "
            + "rules are ignored and tests pass without them. Declare "
            + "io.instanto:instanto-teavm-extensions before org.teavm:teavm-junit.");
  }

  private static String containerOf(URL resource, String name) {
    if (resource == null) {
      return null;
    }
    String location = resource.toString();
    return location.endsWith(name)
        ? location.substring(0, location.length() - name.length())
        : location;
  }
}
