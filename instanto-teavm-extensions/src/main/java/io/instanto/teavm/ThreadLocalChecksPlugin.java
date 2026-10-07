/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.teavm;

import org.teavm.backend.javascript.TeaVMJavaScriptHost;
import org.teavm.backend.wasm.TeaVMWasmGCHost;
import org.teavm.model.MethodReference;
import org.teavm.vm.spi.TeaVMHost;
import org.teavm.vm.spi.TeaVMPlugin;

/** Optional diagnostic checks; mere discovery never changes application behaviour. */
public final class ThreadLocalChecksPlugin implements TeaVMPlugin {
  public static final String PROPERTY = "instanto.teavm.threadLocalChecks";

  @Override
  public void install(TeaVMHost host) {
    String enabled = host.getProperties().getProperty(PROPERTY);
    if (enabled == null) {
      enabled = System.getProperty(PROPERTY, "false");
    }
    if (enabled.equals("false")) {
      return;
    }
    if (!enabled.equals("true")) {
      throw new IllegalArgumentException(PROPERTY + " must be true or false");
    }
    var js = host.getExtension(TeaVMJavaScriptHost.class);
    if (js == null && host.getExtension(TeaVMWasmGCHost.class) == null) {
      throw new IllegalArgumentException(PROPERTY + " supports JavaScript and Wasm GC only");
    }
    host.add(new ThreadLocalTransformer());
    if (js != null) {
      var context = new JavaScriptThreadContext();
      js.add(
          new MethodReference(ThreadLocalGuard.class, "hasJavaScriptThread", boolean.class),
          context);
      js.add(context);
    }
  }
}
