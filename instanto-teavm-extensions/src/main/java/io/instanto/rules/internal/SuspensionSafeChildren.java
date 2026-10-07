/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.rules.internal;

import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * The collection JUnit's {@code Description} holds its children in, under TeaVM.
 *
 * <p>JUnit declares that field as {@code Collection} but instantiates a {@code
 * ConcurrentLinkedQueue}, because descriptions can be built while another thread iterates them
 * during parallel execution. TeaVM has no parallelism, so the thread safety is irrelevant — but the
 * other half of that choice is not: a weakly consistent iterator that never throws {@code
 * ConcurrentModificationException}.
 *
 * <p>That still matters here, because TeaVM can suspend a method at an asynchronous call site and
 * resume it later, letting another continuation add children while an iteration is in flight. A
 * fail-fast collection such as {@code ArrayList} would throw in exactly the case JUnit chose a
 * collection that does not. This class keeps that guarantee and nothing else:
 *
 * <ul>
 *   <li>{@link #add} links one cell and calls no caller-supplied code, so it cannot be suspended
 *       part-way.
 *   <li>{@link #iterator} snapshots the live cells, so a traversal never fails after an intervening
 *       addition and never throws {@code ConcurrentModificationException}.
 *   <li>{@code remove()} on that iterator unlinks the exact cell it returned rather than the first
 *       equal one, so duplicates behave.
 * </ul>
 *
 * <p>It is deliberately not a {@code ConcurrentLinkedQueue} substitute. It is private to rule
 * support and reached only because the compiler rewrites {@code Description}'s constructor to use
 * it, so a framework user writing {@code new ConcurrentLinkedQueue<>()} still gets TeaVM's own
 * answer rather than a single-threaded stand-in posing as a concurrent collection.
 */
public final class SuspensionSafeChildren<E> extends AbstractCollection<E> implements Serializable {

  private static final class Cell<E> {
    private E value;
    private Cell<E> prev;
    private Cell<E> next;

    private Cell(E value) {
      this.value = value;
    }
  }

  private transient Cell<E> head;
  private transient Cell<E> tail;
  private transient int count;

  @Override
  public boolean add(E e) {
    Cell<E> cell = new Cell<>(e);
    if (tail == null) {
      head = cell;
    } else {
      tail.next = cell;
      cell.prev = tail;
    }
    tail = cell;
    count++;
    return true;
  }

  @Override
  public int size() {
    return count;
  }

  @Override
  public boolean isEmpty() {
    return count == 0;
  }

  @Override
  public Iterator<E> iterator() {
    ArrayList<Cell<E>> cells = new ArrayList<>(count);
    ArrayList<E> values = new ArrayList<>(count);
    for (Cell<E> cell = head; cell != null; cell = cell.next) {
      E value = cell.value;
      if (value != null) {
        cells.add(cell);
        values.add(value);
      }
    }
    return new SnapshotIterator(cells, values);
  }

  /** Unlinks a cell, re-linking its neighbours so nothing dead is retained. */
  private void unlink(Cell<E> cell) {
    if (cell.prev != null) {
      cell.prev.next = cell.next;
    } else {
      head = cell.next;
    }
    if (cell.next != null) {
      cell.next.prev = cell.prev;
    } else {
      tail = cell.prev;
    }
    cell.value = null;
    count--;
  }

  private final class SnapshotIterator implements Iterator<E> {
    private final ArrayList<Cell<E>> cells;
    private final ArrayList<E> values;
    private int cursor;
    private Cell<E> lastReturned;

    private SnapshotIterator(ArrayList<Cell<E>> cells, ArrayList<E> values) {
      this.cells = cells;
      this.values = values;
    }

    @Override
    public boolean hasNext() {
      return cursor < cells.size();
    }

    @Override
    public E next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      E value = values.get(cursor);
      lastReturned = cells.get(cursor);
      cursor++;
      return value;
    }

    @Override
    public void remove() {
      if (lastReturned == null) {
        throw new IllegalStateException();
      }
      Cell<E> cell = lastReturned;
      lastReturned = null;
      if (cell.value != null) {
        SuspensionSafeChildren.this.unlink(cell);
      }
    }
  }
}
