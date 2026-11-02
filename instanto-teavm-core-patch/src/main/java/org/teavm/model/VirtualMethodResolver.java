/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.teavm.model;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/** Java virtual selection and dispatch identities (JVMS 5.4.5 and 5.4.6). */
public final class VirtualMethodResolver {
  private VirtualMethodResolver() {}

  public static MethodReader resolve(
      ClassReaderSource source, String className, MethodReference reference) {
    MethodReader declaration = source.resolve(reference);
    if (declaration == null) {
      return null;
    }
    if (declaration.getLevel() == AccessLevel.PRIVATE) {
      return declaration;
    }
    Set<String> interfaces = new LinkedHashSet<>();
    for (ClassReader cls = source.get(className);
        cls != null;
        cls = cls.getParent() != null ? source.get(cls.getParent()) : null) {
      if (!cls.hasModifier(ElementModifier.INTERFACE)) {
        MethodReader candidate = cls.getMethod(reference.getDescriptor());
        if (candidate != null && overrides(source, candidate, declaration)) {
          // An abstract class declaration suppresses an inherited implementation/default too.
          return candidate;
        }
      } else {
        interfaces.add(cls.getName());
      }
      interfaces.addAll(cls.getInterfaces());
    }
    Set<MethodReader> defaults = new LinkedHashSet<>();
    Set<String> visited = new HashSet<>();
    for (String iface : interfaces) {
      collectInterfaceMethods(source, iface, reference.getDescriptor(), visited, defaults);
    }
    defaults.removeIf(
        a ->
            defaults.stream()
                .anyMatch(
                    b ->
                        a != b
                            && source
                                .isSuperType(a.getOwnerName(), b.getOwnerName())
                                .orElse(false)));
    MethodReader result = null;
    for (MethodReader method : defaults) {
      if (!method.hasModifier(ElementModifier.ABSTRACT)) {
        if (result != null) {
          return null; // Conflicting defaults: no legal unique target.
        }
        result = method;
      }
    }
    return result;
  }

  private static void collectInterfaceMethods(
      ClassReaderSource source,
      String name,
      MethodDescriptor descriptor,
      Set<String> visited,
      Set<MethodReader> result) {
    if (!visited.add(name)) {
      return;
    }
    ClassReader cls = source.get(name);
    if (cls == null) {
      return;
    }
    MethodReader method = cls.getMethod(descriptor);
    if (method != null && isVirtual(method)) {
      result.add(method);
    }
    for (String parent : cls.getInterfaces()) {
      collectInterfaceMethods(source, parent, descriptor, visited, result);
    }
  }

  /** Whether candidate can override declaration; receiver ancestry is checked by the caller. */
  public static boolean overrides(
      ClassReaderSource source, MethodReader candidate, MethodReader declaration) {
    if (!candidate.getDescriptor().equals(declaration.getDescriptor())
        || !isVirtual(candidate)
        || !isVirtual(declaration)) {
      return false;
    }
    if (declaration.getLevel() == AccessLevel.PUBLIC
        || declaration.getLevel() == AccessLevel.PROTECTED
        || packageName(candidate.getOwnerName()).equals(packageName(declaration.getOwnerName()))) {
      return true;
    }
    // Package access can become accessible through an intermediate public/protected override.
    ClassReader cls = source.get(candidate.getOwnerName());
    while (cls != null && cls.getParent() != null) {
      cls = source.get(cls.getParent());
      if (cls == null || cls.getName().equals(declaration.getOwnerName())) {
        break;
      }
      MethodReader bridge = cls.getMethod(candidate.getDescriptor());
      if (bridge != null
          && isVirtual(bridge)
          && (bridge.getLevel() == AccessLevel.PUBLIC || bridge.getLevel() == AccessLevel.PROTECTED)
          && overrides(source, bridge, declaration)) {
        return true;
      }
    }
    return false;
  }

  /** Slot used by a call resolved to this declaration. Does not rename Java methods. */
  public static MethodDescriptor slot(ClassReaderSource source, MethodReference reference) {
    MethodReader method = source.resolve(reference);
    if (method == null || !isVirtual(method) || method.getLevel() != AccessLevel.PACKAGE_PRIVATE) {
      return reference.getDescriptor();
    }
    // '<'/'>' cannot occur in ordinary JVM method names, preventing user-name collisions.
    return new MethodDescriptor(
        "<package>" + packageName(method.getOwnerName()) + ":" + method.getName(),
        method.getDescriptor().getSignature());
  }

  /** A public/protected override may implement several otherwise independent package slots. */
  public static Set<MethodDescriptor> slots(ClassReaderSource source, MethodReference reference) {
    Set<MethodDescriptor> result = new LinkedHashSet<>();
    result.add(slot(source, reference));
    MethodReader method = source.resolve(reference);
    if (method == null || !isVirtual(method)) {
      return result;
    }
    source
        .getAncestors(method.getOwnerName())
        .forEach(
            cls -> {
              MethodReader ancestor = cls.getMethod(method.getDescriptor());
              if (ancestor != null && overrides(source, method, ancestor)) {
                result.add(slot(source, ancestor.getReference()));
              }
            });
    return result;
  }

  private static boolean isVirtual(MethodReader method) {
    return method.getLevel() != AccessLevel.PRIVATE
        && !method.hasModifier(ElementModifier.STATIC)
        && !method.getName().startsWith("<");
  }

  private static String packageName(String className) {
    return className.substring(0, className.lastIndexOf('.') + 1);
  }
}
