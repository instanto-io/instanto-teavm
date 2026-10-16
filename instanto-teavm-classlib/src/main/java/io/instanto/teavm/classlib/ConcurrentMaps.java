/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.teavm.classlib;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * {@code ConcurrentHashMap.computeIfAbsent} and {@code computeIfPresent} as the JDK defines them: a
 * function that returns {@code null} leaves the key without a mapping. TeaVM's own map stores the
 * {@code null}, so the key stays present and {@code getOrDefault} returns {@code null}.
 *
 * <p>Each is written with the map's atomic operations, as {@code ConcurrentMap}'s defaults are.
 */
public final class ConcurrentMaps {
  private ConcurrentMaps() {}

  /** {@code ConcurrentHashMap.computeIfAbsent(key, mappingFunction)}. */
  @SuppressWarnings("unchecked")
  public static Object computeIfAbsent(
      ConcurrentHashMap<Object, Object> map, Object key, Function<Object, Object> mappingFunction) {
    if (mappingFunction == null) throw new NullPointerException();
    Object current = map.get(key);
    if (current != null) return current;
    Object computed = mappingFunction.apply(key);
    if (computed == null) return null;
    Object raced = map.putIfAbsent(key, computed);
    return raced != null ? raced : computed;
  }

  /** {@code ConcurrentHashMap.computeIfPresent(key, remappingFunction)}. */
  public static Object computeIfPresent(
      ConcurrentHashMap<Object, Object> map,
      Object key,
      BiFunction<Object, Object, Object> remappingFunction) {
    if (remappingFunction == null) throw new NullPointerException();
    while (true) {
      Object current = map.get(key);
      if (current == null) return null;
      Object computed = remappingFunction.apply(key, current);
      if (computed == null) {
        if (map.remove(key, current)) return null;
      } else if (map.replace(key, current, computed)) {
        return computed;
      }
    }
  }
}
