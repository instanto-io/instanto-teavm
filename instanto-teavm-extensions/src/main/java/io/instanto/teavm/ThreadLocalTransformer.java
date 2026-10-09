/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.teavm;

import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassHolderTransformerContext;
import org.teavm.model.FieldReference;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;
import org.teavm.model.instructions.GetFieldInstruction;
import org.teavm.model.instructions.InvocationType;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.model.instructions.NullConstantInstruction;
import org.teavm.runtime.Fiber;

final class ThreadLocalTransformer implements ClassHolderTransformer {
  private static final MethodDescriptor[] METHODS = {
    new MethodDescriptor("get", Object.class),
    new MethodDescriptor("set", Object.class, void.class),
    new MethodDescriptor("remove", void.class)
  };
  private static final MethodDescriptor[] CALLBACK_METHODS = {
    new MethodDescriptor("complete", Object.class, void.class),
    new MethodDescriptor("error", Throwable.class, void.class)
  };
  // Every JavaScript AsyncCallback is wrapped in this class; Wasm GC uses Fiber's implementation.
  private static final String JS_CALLBACK = "org.teavm.platform.plugin.AsyncCallbackWrapper";
  private static final String FIBER_CALLBACK = Fiber.class.getName() + "$AsyncCallbackImpl";
  private static final MethodReference CHECK_COMPLETION =
      new MethodReference(ThreadLocalGuard.class, "checkCompletion", Fiber.class, void.class);

  @Override
  public void transformClass(ClassHolder cls, ClassHolderTransformerContext context) {
    if (cls.getName().equals(JS_CALLBACK) || cls.getName().equals(FIBER_CALLBACK)) {
      for (var descriptor : CALLBACK_METHODS) {
        checkCompletion(cls, requireMethod(cls, descriptor));
      }
      return;
    }
    // TeaVM has already mapped its class-library name to the Java API name.
    if (!cls.getName().equals("java.lang.ThreadLocal")) {
      return;
    }
    for (var descriptor : METHODS) {
      var method = cls.getMethod(descriptor);
      if (method == null
          || method.getProgram() == null
          || method.getProgram().basicBlockCount() == 0) {
        throw new IllegalStateException(
            "Unsupported TeaVM ThreadLocal implementation: " + descriptor);
      }
      var check = new InvokeInstruction();
      check.setType(InvocationType.SPECIAL);
      check.setMethod(new MethodReference(ThreadLocalGuard.class, "check", void.class));
      method.getProgram().basicBlockAt(0).addFirst(check);
    }
  }

  private static MethodHolder requireMethod(ClassHolder cls, MethodDescriptor descriptor) {
    var method = cls.getMethod(descriptor);
    if (method == null
        || method.getProgram() == null
        || method.getProgram().basicBlockCount() == 0) {
      throw new IllegalStateException(
          "Unsupported TeaVM AsyncCallback implementation: " + cls.getName() + "." + descriptor);
    }
    return method;
  }

  private static void checkCompletion(ClassHolder cls, MethodHolder method) {
    var program = method.getProgram();
    var entry = program.basicBlockAt(0);
    var waiting = program.createVariable();
    if (cls.getName().equals(FIBER_CALLBACK)) {
      var read = new GetFieldInstruction();
      read.setInstance(program.variableAt(0));
      read.setField(new FieldReference(FIBER_CALLBACK, "fiber"));
      read.setFieldType(ValueType.object(Fiber.class.getName()));
      read.setReceiver(waiting);
      entry.addFirst(read);
    } else {
      var none = new NullConstantInstruction();
      none.setReceiver(waiting);
      entry.addFirst(none);
    }
    var check = new InvokeInstruction();
    check.setType(InvocationType.SPECIAL);
    check.setMethod(CHECK_COMPLETION);
    check.setArguments(waiting);
    entry.getFirstInstruction().insertNext(check);
  }
}
