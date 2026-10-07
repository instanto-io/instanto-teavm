/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.rules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import io.instanto.rules.internal.SuspensionSafeChildren;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;

/** The collection JUnit's Description is rewritten to use, and Description itself. */
@RunWith(TeaVMTestRunner.class)
@SkipJVM
public class SuspensionSafeChildrenTeaVmTest {

  @FunctionalInterface
  private interface ContinuationSlice<T> {
    T run();
  }

  private static <T> T runSlice(ContinuationSlice<T> slice) {
    return slice.run();
  }

  private static List<String> drain(Iterable<String> source) {
    List<String> seen = new ArrayList<>();
    for (String value : source) {
      seen.add(value);
    }
    return seen;
  }

  @Test
  public void addsInOrderAndReportsSize() {
    SuspensionSafeChildren<String> children = new SuspensionSafeChildren<>();
    assertTrue(children.isEmpty());

    children.add("a");
    children.add("b");

    assertFalse(children.isEmpty());
    assertEquals(2, children.size());
    assertEquals(Arrays.asList("a", "b"), drain(children));
  }

  /** The property JUnit chose ConcurrentLinkedQueue for: adding during iteration must not fail. */
  @Test
  public void iterationSurvivesAdditionByAnotherContinuation() {
    SuspensionSafeChildren<String> children = new SuspensionSafeChildren<>();
    children.add("a");
    children.add("b");

    Iterator<String> iterator = children.iterator();
    assertEquals("a", iterator.next());

    runSlice(
        () -> {
          children.add("c");
          return null;
        });

    assertEquals("b", iterator.next());
    assertFalse("the snapshot must not observe later additions", iterator.hasNext());
    assertEquals(3, children.size());
  }

  /** A removal elsewhere must not turn an in-flight traversal into nulls. */
  @Test
  public void iterationSurvivesRemovalByAnotherContinuation() {
    SuspensionSafeChildren<String> children = new SuspensionSafeChildren<>();
    children.add("p");
    children.add("q");
    children.add("r");

    Iterator<String> iterator = children.iterator();
    assertEquals("p", iterator.next());

    runSlice(
        () -> {
          children.remove("r");
          return null;
        });

    assertEquals("q", iterator.next());
    assertEquals("r", iterator.next());
    assertEquals(2, children.size());
  }

  /** Iterator removal must drop the element visited, not the first equal one. */
  @Test
  public void iteratorRemoveDropsTheVisitedDuplicate() {
    SuspensionSafeChildren<String> children = new SuspensionSafeChildren<>();
    children.add("a");
    children.add("b");
    children.add("a");

    Iterator<String> iterator = children.iterator();
    iterator.next();
    iterator.next();
    assertEquals("a", iterator.next());
    iterator.remove();

    assertEquals(2, children.size());
    assertEquals(Arrays.asList("a", "b"), drain(children));
  }

  /** Repeated removals must not retain dead cells. */
  @Test
  public void repeatedRemovalsDoNotGrowTheChain() {
    SuspensionSafeChildren<Integer> children = new SuspensionSafeChildren<>();
    for (int round = 0; round < 200; round++) {
      children.add(round);
      children.add(round + 1000);
      children.remove(round);
    }
    assertEquals(200, children.size());
  }

  /** The point of the whole exercise: Description compiles and works under TeaVM. */
  @Test
  public void junitDescriptionBuildsAChildTree() {
    Description parent = Description.createSuiteDescription("example.Suite");
    assertTrue(parent.getChildren().isEmpty());

    parent.addChild(Description.createTestDescription("example.Suite", "first"));
    parent.addChild(Description.createTestDescription("example.Suite", "second"));

    assertEquals(2, parent.getChildren().size());
    assertEquals("first", parent.getChildren().get(0).getMethodName());
    assertEquals("second", parent.getChildren().get(1).getMethodName());
    assertFalse(parent.getChildren().isEmpty());
  }
}
