/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.teavm;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.interop.Async;
import org.teavm.interop.AsyncCallback;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;
import org.teavm.jso.browser.Window;
import org.teavm.junit.OnlyPlatform;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMProperties;
import org.teavm.junit.TeaVMProperty;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.TestPlatform;

@RunWith(TeaVMTestRunner.class)
@SkipJVM
@OnlyPlatform({TestPlatform.JAVASCRIPT, TestPlatform.WEBASSEMBLY_GC})
@TeaVMProperties(@TeaVMProperty(key = ThreadLocalChecksPlugin.PROPERTY, value = "true"))
public class ThreadLocalChecksTest {
  @Test
  public void callbackCannotReadWriteOrRemoveSuspendedThreadState() {
    ThreadLocal<String> local = new ThreadLocal<>();
    local.set("owner");
    for (Throwable failure : accessInCallback(local)) {
      assertTrue(String.valueOf(failure), failure instanceof UnsupportedOperationException);
    }
    assertEquals("owner", local.get());
    local.remove();
    assertNull(local.get());
  }

  @Test
  public void rejectedReadDoesNotCallInitialValue() {
    int[] calls = {0};
    ThreadLocal<String> local = ThreadLocal.withInitial(() -> "initial-" + ++calls[0]);
    for (Throwable failure : accessInCallback(local)) {
      assertTrue(failure instanceof UnsupportedOperationException);
    }
    assertEquals(0, calls[0]);
    assertEquals("initial-1", local.get());
    local.remove();
    assertEquals("initial-2", local.get());
  }

  @Test
  public void twoJavaThreadsRetainIndependentValuesAcrossSuspension() throws Exception {
    ThreadLocal<String> local = new ThreadLocal<>();
    Throwable[] failures = new Throwable[2];
    boolean[] started = new boolean[2];
    Thread[] workers = new Thread[2];
    for (int i = 0; i < 2; ++i) {
      final int index = i;
      workers[i] =
          new Thread(
              () -> {
                try {
                  assertNull(local.get());
                  local.set("worker-" + index);
                  started[index] = true;
                  while (!started[1 - index]) {
                    Thread.sleep(1);
                  }
                  Thread.sleep(1);
                  assertEquals("worker-" + index, local.get());
                  local.remove();
                  assertNull(local.get());
                } catch (Throwable failure) {
                  failures[index] = failure;
                  started[index] = true;
                }
              });
      workers[i].start();
    }
    for (Thread worker : workers) {
      worker.join();
    }
    for (Throwable failure : failures) {
      assertNull(String.valueOf(failure), failure);
    }
    assertNull(local.get());
  }

  @Test
  public void callbackCanStartAnIndependentJavaThread() throws Exception {
    ThreadLocal<String> local = new ThreadLocal<>();
    local.set("owner");
    Throwable[] failure = new Throwable[1];
    Thread worker =
        startInCallback(
            () -> {
              try {
                assertNull(local.get());
                local.set("callback-worker");
                Thread.sleep(1);
                assertEquals("callback-worker", local.get());
              } catch (Throwable error) {
                failure[0] = error;
              }
            });
    worker.join();
    assertNull(String.valueOf(failure[0]), failure[0]);
    assertEquals("owner", local.get());
  }

  @Test
  public void completingAnotherThreadsCallbackIsRejected() throws Exception {
    ThreadLocal<String> local = new ThreadLocal<>();
    Thread[] completerSaw = new Thread[1];
    String[] completerValue = new String[1];
    Throwable[] completion = new Throwable[1];
    pending = null;
    Thread waiter =
        new Thread(
            () -> {
              local.set("waiter");
              waitForCompletion();
            },
            "waiter");
    waiter.start();
    for (int i = 0; i < 100 && pending == null; ++i) {
      Thread.sleep(1);
    }
    assertNotNull("the waiter suspended", pending);
    Thread completer =
        new Thread(
            () -> {
              local.set("completer");
              completion[0] = capture(() -> pending.complete(null));
              completerSaw[0] = Thread.currentThread();
              completerValue[0] = local.get();
            },
            "completer");
    completer.start();
    completer.join();

    assertTrue(
        String.valueOf(completion[0]), completion[0] instanceof UnsupportedOperationException);
    assertSame(completer, completerSaw[0]);
    assertEquals("completer", completerValue[0]);

    // Release the waiter the supported way, from the event loop.
    completeFromEventLoop(pending);
    waiter.join();
  }

  private static void completeFromEventLoop(AsyncCallback<Void> callback) {
    // Not Window.setTimeout: optimised TeaVM 0.16.0 can compile that call as null.setTimeout.
    later(() -> callback.complete(null));
  }

  @JSBody(params = "action", script = "setTimeout(action, 0);")
  private static native void later(Action action);

  @JSFunctor
  interface Action extends JSObject {
    void run();
  }

  @Test
  public void completingWithinTheAsyncMethodIsAllowed() {
    assertEquals("immediate", completeImmediately());
  }

  @Test
  public void monitorHandOverBetweenJavaThreadsIsAllowed() throws Exception {
    Object monitor = new Object();
    boolean[] ready = {false};
    Throwable[] failure = new Throwable[1];
    Thread waiter =
        new Thread(
            () -> {
              try {
                synchronized (monitor) {
                  while (!ready[0]) {
                    monitor.wait();
                  }
                }
              } catch (Throwable error) {
                failure[0] = error;
              }
            });
    waiter.start();
    Thread.sleep(1);
    synchronized (monitor) {
      ready[0] = true;
      monitor.notifyAll();
    }
    waiter.join();
    assertNull(String.valueOf(failure[0]), failure[0]);
  }

  private static AsyncCallback<Void> pending;

  @Async
  private static native Void waitForCompletion();

  private static void waitForCompletion(AsyncCallback<Void> callback) {
    pending = callback;
  }

  @Async
  private static native String completeImmediately();

  private static void completeImmediately(AsyncCallback<String> callback) {
    callback.complete("immediate");
  }

  @Async
  private static native Throwable[] accessInCallback(ThreadLocal<String> local);

  private static void accessInCallback(
      ThreadLocal<String> local, AsyncCallback<Throwable[]> callback) {
    Window.setTimeout(
        () ->
            callback.complete(
                new Throwable[] {
                  capture(local::get), capture(() -> local.set("illegal")), capture(local::remove)
                }),
        0);
  }

  private static Throwable capture(Runnable access) {
    try {
      access.run();
      return null;
    } catch (Throwable failure) {
      return failure;
    }
  }

  @Async
  private static native Thread startInCallback(Runnable body);

  private static void startInCallback(Runnable body, AsyncCallback<Thread> callback) {
    Window.setTimeout(
        () -> {
          Thread worker = new Thread(body);
          worker.start();
          callback.complete(worker);
        },
        0);
  }
}
