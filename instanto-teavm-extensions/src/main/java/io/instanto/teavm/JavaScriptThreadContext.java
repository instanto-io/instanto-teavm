/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.teavm;

import org.teavm.backend.javascript.rendering.Renderer;
import org.teavm.backend.javascript.rendering.RenderingManager;
import org.teavm.backend.javascript.spi.Injector;
import org.teavm.backend.javascript.spi.InjectorContext;
import org.teavm.model.MethodReference;
import org.teavm.vm.BuildTarget;
import org.teavm.vm.spi.AbstractRendererListener;

/** Uses the coroutine marker, or tracks synchronous entry when TeaVM omits that runtime. */
final class JavaScriptThreadContext extends AbstractRendererListener implements Injector {
  private static final String CHECK = "instanto_threadLocalHasContext";
  private static final String ACTIVE = "instanto_threadLocalContext";
  private static final String START = "instanto_originalStartThread";
  private Renderer renderer;
  private boolean used;

  @Override
  public void begin(RenderingManager manager, BuildTarget target) {
    renderer = (Renderer) manager;
  }

  @Override
  public void generate(InjectorContext context, MethodReference method) {
    used = true;
    context.getWriter().appendFunction(CHECK).append("()");
  }

  @Override
  public void complete() {
    if (!used) {
      return;
    }
    var writer = renderer.getWriter();
    if (renderer.isThreadLibraryUsed()) {
      writer
          .startVariableDeclaration()
          .appendFunction(CHECK)
          .append("() => ")
          .appendFunction("$rt_nativeThread")
          .append("() !== null")
          .endDeclaration();
      return;
    }
    // simpleThread.js has no current-thread marker. Wrap only the Java runner;
    // the native completion callback must run after the marker has been restored.
    writer.startVariableDeclaration().appendFunction(ACTIVE).append("false").endDeclaration();
    writer
        .startVariableDeclaration()
        .appendFunction(CHECK)
        .append("() => ")
        .appendFunction(ACTIVE)
        .endDeclaration();
    writer
        .startVariableDeclaration()
        .appendFunction(START)
        .appendFunction("$rt_startThread")
        .endDeclaration();
    writer
        .appendFunction("$rt_startThread")
        .append(" = (runner, callback) => ")
        .appendFunction(START)
        .append("(() => {")
        .softNewLine()
        .indent()
        .append("let previous = ")
        .appendFunction(ACTIVE)
        .append(";")
        .softNewLine()
        .appendFunction(ACTIVE)
        .append(" = true;")
        .softNewLine()
        .append("try { return runner(); } finally { ")
        .appendFunction(ACTIVE)
        .append(" = previous; }")
        .softNewLine()
        .outdent()
        .append("}, callback);")
        .softNewLine();
  }
}
