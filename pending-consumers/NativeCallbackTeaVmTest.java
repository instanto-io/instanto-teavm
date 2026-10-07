/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.sarto.invoke.teavm;

import static io.instanto.mockatcha.Mockatcha.inOrder;
import static io.instanto.mockatcha.Mockatcha.mock;
import static io.instanto.mockatcha.Mockatcha.verifyNoMoreInteractions;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import io.instanto.sarto.async.PortableThreadExecutor;
import io.instanto.sarto.async.Promise;
import io.instanto.sarto.async.runtime.ManagedExecution;
import io.instanto.sarto.core.concurrency.ServiceScheduler;
import io.instanto.sarto.core.concurrency.ServiceThreadContext;
import io.instanto.sarto.invoke.Caller;
import io.instanto.sarto.invoke.spi.InvocationCapture;
import io.instanto.sarto.invoke.spi.InvocationChain;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.interop.Async;
import org.teavm.interop.AsyncCallback;
import org.teavm.jso.browser.Window;
import org.teavm.junit.OnlyPlatform;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMProperties;
import org.teavm.junit.TeaVMProperty;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.TestPlatform;

/** Real browser entry and service scheduling; only the application work is mocked. */
@RunWith(TeaVMTestRunner.class)
@SkipJVM
@TeaVMProperties(@TeaVMProperty(key = "instanto.teavm.threadLocalChecks", value = "true"))
@OnlyPlatform({TestPlatform.JAVASCRIPT, TestPlatform.WEBASSEMBLY_GC})
public class NativeCallbackTeaVmTest {
  private final Work work = mock(Work.class);
  private final ServiceScheduler service = new ServiceScheduler();
  private final ThreadLocal<String> local = new ThreadLocal<>();
  private final Promise<Void> release = Promise.pending();
  private final Promise<Void> finished = Promise.pending();
  private final Caller<Work> caller =
      new Caller<>(
          () -> {
            throw new AssertionError("Native entry must be rejected before resolving a service");
          },
          Runnable::run);

  @Test
  public void browserEntryCannotBorrowASuspendedServicesContext() throws Exception {
    service.bindServiceId("service");
    service.bindExecutor(new PortableThreadExecutor("service"));
    service.submit(this::serviceOperation);
    try {
      long deadline = System.currentTimeMillis() + 5000;
      while (!finished.isDone() && System.currentTimeMillis() < deadline) {
        Thread.sleep(1);
      }
      assertTrue("Browser callback and queued work must finish", finished.isDone());
      finished.await();

      var order = inOrder(work);
      order.verify(work).record("service.start");
      order.verify(work).record("callback.enter");
      order.verify(work).record("callback.queued");
      order.verify(work).record("service.end");
      order.verify(work).record("service.queued");
      verifyNoMoreInteractions(work);
    } finally {
      release.complete(null);
      service.close();
    }
  }

  @Test
  public void rawCallbackCannotReadMockatchasThreadLocal() {
    Throwable failure = nativeCallbackFailure(() -> work.record("illegal"));
    assertTrue(
        "The compiler guard must reject MockRuntime.scope()",
        failure instanceof UnsupportedOperationException);
    assertTrue(failure.getMessage().contains("ThreadLocal access requires Java thread context"));
    verifyNoMoreInteractions(work);
  }

  @Async
  private static native Throwable nativeCallbackFailure(Runnable action);

  private static void nativeCallbackFailure(Runnable action, AsyncCallback<Throwable> callback) {
    Window.setTimeout(
        () -> {
          Throwable failure = null;
          try {
            action.run();
          } catch (Throwable caught) {
            failure = caught;
          }
          callback.complete(failure);
        },
        0);
  }

  private void serviceOperation() {
    try {
      ServiceThreadContext.runWith(
          service,
          () -> {
            local.set("service-local");
            String chain = InvocationChain.currentChainId();
            work.record("service.start");
            Promise<String> result = Promise.pending();
            InvocationCapture.call(
                    () -> {
                      Window.setTimeout(this::browserCallback, 0);
                      // A gate, not a timed sleep: the callback must run while this context is
                      // active.
                      release.await();
                      return "service-result";
                    })
                .observe(result::complete, result::fail);
            assertEquals("service-result", result.await());
            assertEquals("service-local", local.get());
            assertEquals(chain, InvocationChain.currentChainId());
            assertSame(service, ServiceThreadContext.current());
            assertTrue(service.isServiceThread());
            local.remove();
            work.record("service.end");
          });
    } catch (Throwable failure) {
      finished.fail(failure);
    }
  }

  private void browserCallback() {
    // Never read Thread.currentThread, ThreadLocal, or Mockatcha state in this raw handler.
    Throwable rejectedCheck = null;
    try {
      assertFalse(ManagedExecution.hasCurrentThread());
      assertThrows(IllegalStateException.class, service::isServiceThread);
      assertThrows(IllegalStateException.class, InvocationChain::capture);
      assertThrows(IllegalStateException.class, ServiceThreadContext::current);
      assertThrows(IllegalStateException.class, ServiceThreadContext::clear);
      assertThrows(IllegalStateException.class, () -> caller.call(service -> null));
      assertThrows(
          IllegalStateException.class,
          () -> InvocationCapture.captureResult(Promise.completed("poison")));
      assertThrows(
          IllegalStateException.class, () -> service.submit(() -> work.record("illegal.submit")));
      assertThrows(
          IllegalStateException.class,
          () -> service.scheduleNow(() -> work.record("illegal.inline")));
    } catch (Throwable failure) {
      rejectedCheck = failure;
    }
    Throwable checkFailure = rejectedCheck;
    ManagedExecution.execute(() -> admittedCallback(checkFailure));
  }

  private void admittedCallback(Throwable checkFailure) {
    try {
      if (checkFailure != null) throw new AssertionError(checkFailure);
      assertTrue(ManagedExecution.hasCurrentThread());
      assertFalse(service.isServiceThread());
      assertNull(InvocationChain.capture());
      assertNull(ServiceThreadContext.current());
      assertNull(local.get());
      local.set("callback-local");
      work.record("callback.enter");
      service.scheduleNow(
          () -> {
            try {
              assertTrue(service.isServiceThread());
              work.record("service.queued");
              finished.complete(null);
            } catch (Throwable failure) {
              finished.fail(failure);
            }
          });
      work.record("callback.queued");
    } catch (Throwable failure) {
      finished.fail(failure);
    } finally {
      release.complete(null);
    }
  }

  public interface Work {
    void record(String event);
  }
}
