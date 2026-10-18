/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.teavm;

import static org.junit.Assert.*;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.teavm.vm.spi.TeaVMHost;

public class PluginConfigurationTest {
  private String previousProperty;

  @Before
  public void isolateCompilerSystemProperty() {
    previousProperty = System.getProperty(ThreadLocalChecksPlugin.PROPERTY);
    System.clearProperty(ThreadLocalChecksPlugin.PROPERTY);
  }

  @After
  public void restoreCompilerSystemProperty() {
    if (previousProperty == null) {
      System.clearProperty(ThreadLocalChecksPlugin.PROPERTY);
    } else {
      System.setProperty(ThreadLocalChecksPlugin.PROPERTY, previousProperty);
    }
  }

  @Test
  public void systemPropertyEnablesChecksUnlessCompilerExplicitlyDisablesThem() {
    System.setProperty(ThreadLocalChecksPlugin.PROPERTY, "true");
    // This fake host has no supported backend: reaching backend validation proves activation.
    ThreadLocalChecksPlugin plugin = new ThreadLocalChecksPlugin();
    TeaVMHost invalidHost = host(null, new ArrayList<>());
    IllegalArgumentException error =
        assertThrows(IllegalArgumentException.class, () -> plugin.install(invalidHost));
    assertTrue(error.getMessage().contains("supports JavaScript and Wasm GC only"));
    List<String> calls = new ArrayList<>();
    new ThreadLocalChecksPlugin().install(host("false", calls));
    assertEquals(List.of("getProperties"), calls);

    System.setProperty(ThreadLocalChecksPlugin.PROPERTY, "invalid");
    TeaVMHost invalidHost2 = host(null, new ArrayList<>());
    assertThrows(IllegalArgumentException.class, () -> plugin.install(invalidHost2));
  }

  @Test
  public void defaultAndExplicitFalseInstallNothing() {
    for (String value : new String[] {null, "false"}) {
      List<String> calls = new ArrayList<>();
      new ThreadLocalChecksPlugin().install(host(value, calls));
      assertEquals(List.of("getProperties"), calls);
    }
  }

  @Test
  public void invalidOptionAndUnsupportedTargetsFailClearly() {
    ThreadLocalChecksPlugin plugin = new ThreadLocalChecksPlugin();
    TeaVMHost invalidOptionHost = host("tru", new ArrayList<>());
    TeaVMHost unsupportedHost = host("true", new ArrayList<>());
    assertThrows(IllegalArgumentException.class, () -> plugin.install(invalidOptionHost));
    assertThrows(IllegalArgumentException.class, () -> plugin.install(unsupportedHost));
  }

  private static TeaVMHost host(String value, List<String> calls) {
    Properties properties = new Properties();
    if (value != null) {
      properties.setProperty(ThreadLocalChecksPlugin.PROPERTY, value);
    }
    return (TeaVMHost)
        Proxy.newProxyInstance(
            TeaVMHost.class.getClassLoader(),
            new Class<?>[] {TeaVMHost.class},
            (proxy, method, args) -> {
              calls.add(method.getName());
              return method.getName().equals("getProperties") ? properties : null;
            });
  }
}
