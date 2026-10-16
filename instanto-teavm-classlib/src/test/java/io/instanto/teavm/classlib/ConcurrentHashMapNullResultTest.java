/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.teavm.classlib;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.ConcurrentHashMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;

/** A function returning {@code null} leaves a key without a mapping, as on the JDK. */
@RunWith(TeaVMTestRunner.class)
public class ConcurrentHashMapNullResultTest {

  @Test
  public void computeIfPresentRemovesTheKeyWhenTheFunctionReturnsNull() {
    var map = new ConcurrentHashMap<String, String>();
    map.put("key", "value");

    assertNull(map.computeIfPresent("key", (key, value) -> null));

    assertFalse(map.containsKey("key"));
    assertEquals("default", map.getOrDefault("key", "default"));
    assertEquals(0, map.size());
  }

  @Test
  public void computeIfPresentReplacesTheValue() {
    var map = new ConcurrentHashMap<String, String>();
    map.put("key", "value");

    assertEquals("value!", map.computeIfPresent("key", (key, value) -> value + "!"));
    assertEquals("value!", map.get("key"));
  }

  @Test
  public void computeIfPresentLeavesAnAbsentKeyAbsent() {
    var map = new ConcurrentHashMap<String, String>();

    assertNull(map.computeIfPresent("key", (key, value) -> "never"));
    assertFalse(map.containsKey("key"));
  }

  @Test
  public void computeIfAbsentRecordsNothingWhenTheFunctionReturnsNull() {
    var map = new ConcurrentHashMap<String, String>();

    assertNull(map.computeIfAbsent("key", key -> null));

    assertFalse(map.containsKey("key"));
    assertEquals("default", map.getOrDefault("key", "default"));
  }

  @Test
  public void computeIfAbsentKeepsAnExistingValue() {
    var map = new ConcurrentHashMap<String, String>();
    map.put("key", "value");

    assertEquals("value", map.computeIfAbsent("key", key -> "other"));
    assertTrue(map.containsKey("key"));
    assertEquals("added", map.computeIfAbsent("missing", key -> "added"));
  }
}
