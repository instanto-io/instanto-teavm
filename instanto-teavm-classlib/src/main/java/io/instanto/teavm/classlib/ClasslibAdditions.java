/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.teavm.classlib;

import org.teavm.model.AccessLevel;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassHolderTransformerContext;
import org.teavm.model.ElementModifier;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodHolder;
import org.teavm.model.ValueType;
import org.teavm.model.emit.ProgramEmitter;
import org.teavm.model.emit.ValueEmitter;

/**
 * Adds the missing class-library methods, and replaces the bodies of faulty ones, by delegating to
 * plain Java helpers.
 */
final class ClasslibAdditions implements ClassHolderTransformer {
  private static final ValueType STREAM = ValueType.object("java.util.stream.Stream");
  private static final ValueType CHARS = ValueType.arrayOf(ValueType.CHARACTER);
  private static final ValueType INT = ValueType.INTEGER;
  private static final ValueType CHAR_SEQUENCE = ValueType.object("java.lang.CharSequence");

  @Override
  public void transformClass(ClassHolder cls, ClassHolderTransformerContext context) {
    switch (cls.getName()) {
      case "java.lang.String" -> addStringLines(cls, context);
      case "java.lang.Character" -> {
        addDirectionality(cls, context, ValueType.CHARACTER);
        addDirectionality(cls, context, ValueType.INTEGER);
        delegate(cls, context, "codePointCount", "count", CHARS, INT, INT, INT);
        delegate(cls, context, "offsetByCodePoints", "offset", CHAR_SEQUENCE, INT, INT, INT);
        delegate(cls, context, "offsetByCodePoints", "offset", CHARS, INT, INT, INT, INT, INT);
      }
      default -> {}
    }
  }

  /** {@code public Stream<String> lines()}, calling {@code StringAdditions.lines(this)}. */
  private static void addStringLines(ClassHolder cls, ClassHolderTransformerContext context) {
    MethodDescriptor lines = new MethodDescriptor("lines", STREAM);
    if (cls.getMethod(lines) != null) return;
    MethodHolder method = new MethodHolder(lines);
    method.setLevel(AccessLevel.PUBLIC);
    ProgramEmitter emit = ProgramEmitter.create(method, context.getHierarchy());
    emit.invoke(
            StringAdditions.class.getName(),
            "lines",
            STREAM,
            emit.var(0, ValueType.object("java.lang.String")))
        .returnValue();
    cls.addMethod(method);
  }

  /** {@code public static byte getDirectionality(char|int)}, calling {@code Directionality.of}. */
  private static void addDirectionality(
      ClassHolder cls, ClassHolderTransformerContext context, ValueType parameter) {
    MethodDescriptor descriptor =
        new MethodDescriptor("getDirectionality", parameter, ValueType.BYTE);
    if (cls.getMethod(descriptor) != null) return;
    MethodHolder method = new MethodHolder(descriptor);
    method.setLevel(AccessLevel.PUBLIC);
    method.getModifiers().add(ElementModifier.STATIC);
    ProgramEmitter emit = ProgramEmitter.create(method, context.getHierarchy());
    emit.invoke(Directionality.class.getName(), "of", ValueType.BYTE, emit.var(1, parameter))
        .returnValue();
    cls.addMethod(method);
  }

  /**
   * Replaces the body of the static {@code name(parameters...)}, whose last type is the result, with
   * a call to {@code CodePoints.helper} taking the same arguments. Absent methods are left alone.
   */
  private static void delegate(
      ClassHolder cls,
      ClassHolderTransformerContext context,
      String name,
      String helper,
      ValueType... signature) {
    MethodHolder method = cls.getMethod(new MethodDescriptor(name, signature));
    if (method == null || !method.hasModifier(ElementModifier.STATIC)) return;
    ValueType result = signature[signature.length - 1];
    ProgramEmitter emit = ProgramEmitter.create(method, context.getHierarchy());
    ValueEmitter[] arguments = new ValueEmitter[signature.length - 1];
    for (int index = 0; index < arguments.length; index++) {
      arguments[index] = emit.var(index + 1, signature[index]);
    }
    emit.invoke(CodePoints.class.getName(), helper, result, arguments).returnValue();
  }
}
