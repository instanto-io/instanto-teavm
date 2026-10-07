/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.teavm.classlib;

import org.teavm.vm.spi.TeaVMHost;
import org.teavm.vm.spi.TeaVMPlugin;

/**
 * Fills gaps in TeaVM's class library while the application is compiled, without replacing any of
 * its classes:
 *
 * <ul>
 *   <li>{@code String.lines()}, as {@link StringAdditions#lines(String)};
 *   <li>{@code Character.getDirectionality(char)} and {@code (int)}, as {@link Directionality};
 *   <li>{@code Character.codePointCount(char[], int, int)} and both {@code offsetByCodePoints}
 *       methods, whose TeaVM bodies give wrong answers for slices and lone surrogates, as {@link
 *       CodePoints}.
 * </ul>
 *
 * <p>Each addition steps aside when TeaVM already provides the method, so an upgrade that adds it
 * upstream wins. The corrections always apply; their tests show when an upgrade makes one
 * redundant. Adding {@code instanto-teavm-classlib} to the compiler's classpath is enough.
 */
public final class ClasslibAdditionsPlugin implements TeaVMPlugin {
  @Override
  public void install(TeaVMHost host) {
    host.add(new ClasslibAdditions());
  }
}
