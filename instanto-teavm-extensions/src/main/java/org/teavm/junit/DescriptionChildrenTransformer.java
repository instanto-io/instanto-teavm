/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.teavm.junit;

import io.instanto.rules.internal.SuspensionSafeChildren;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassHolderTransformerContext;
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReference;
import org.teavm.model.Program;
import org.teavm.model.ValueType;
import org.teavm.model.instructions.ConstructInstruction;
import org.teavm.model.instructions.InvokeInstruction;

/**
 * Points JUnit's {@code Description} at a collection TeaVM can compile.
 *
 * <p>{@code Description} declares its children as {@code Collection} but instantiates a {@code
 * ConcurrentLinkedQueue}, which TeaVM's class library does not provide. Rather than substituting
 * {@code ConcurrentLinkedQueue} for the whole program — which would hand every consumer a
 * single-threaded stand-in for a concurrent collection — this rewrites the two instructions in
 * {@code Description}'s constructor that name it.
 *
 * <p>Only {@code add}, {@code isEmpty} and {@code iterator} are ever called on that field, so the
 * replacement needs nothing more. See {@link SuspensionSafeChildren} for why a fail-fast collection
 * would not do.
 */
public class DescriptionChildrenTransformer implements ClassHolderTransformer {
  private static final String DESCRIPTION = "org.junit.runner.Description";
  private static final String CONCURRENT_LINKED_QUEUE =
      "java.util.concurrent.ConcurrentLinkedQueue";
  private static final String REPLACEMENT = SuspensionSafeChildren.class.getName();

  @Override
  public void transformClass(ClassHolder cls, ClassHolderTransformerContext context) {
    if (!cls.getName().equals(DESCRIPTION)) {
      return;
    }
    int rewritten = 0;
    for (MethodHolder method : cls.getMethods()) {
      Program program = method.getProgram();
      if (program == null) {
        continue;
      }
      for (int i = 0; i < program.basicBlockCount(); i++) {
        for (var instruction : program.basicBlockAt(i)) {
          if (instruction instanceof ConstructInstruction construct
              && CONCURRENT_LINKED_QUEUE.equals(construct.getType())) {
            construct.setType(REPLACEMENT);
            rewritten++;
          } else if (instruction instanceof InvokeInstruction invoke
              && CONCURRENT_LINKED_QUEUE.equals(invoke.getMethod().getClassName())
              && "<init>".equals(invoke.getMethod().getName())) {
            invoke.setMethod(new MethodReference(REPLACEMENT, "<init>", ValueType.VOID));
            rewritten++;
          }
        }
      }
    }
    if (rewritten != 2) {
      // Fail loudly rather than silently leaving Description referencing a class TeaVM
      // cannot compile, which would surface much later as a confusing link error.
      throw new IllegalStateException(
          "Expected to rewrite the ConcurrentLinkedQueue"
              + " construction in "
              + DESCRIPTION
              + " but rewrote "
              + rewritten
              + " instructions. This JUnit version builds its children differently;"
              + " DescriptionChildrenTransformer needs updating.");
    }
  }
}
