/*
 * Copyright 2026 Carl Stainton
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.teavm;

import static org.junit.Assert.*;

import java.net.JarURLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarFile;
import org.junit.Test;
import org.teavm.asm.ClassReader;
import org.teavm.asm.ClassVisitor;
import org.teavm.asm.FieldVisitor;
import org.teavm.asm.MethodVisitor;
import org.teavm.asm.Opcodes;

/** Overlay methods must remain link-compatible with the untouched classes in the upstream jar. */
public class CompilerOverlayCompatibilityTest {
  @Test
  public void replacedClassesRetainUpstreamBinarySignatures() throws Exception {
    var loader = getClass().getClassLoader();
    var resources = loader.getResources("org/teavm/model/Program.class");
    JarFile upstream = null;
    while (resources.hasMoreElements()) {
      var resource = resources.nextElement();
      if (resource.getProtocol().equals("jar")) {
        var jar = ((JarURLConnection) resource.openConnection()).getJarFile();
        if (jar.getName().endsWith("teavm-core-0.16.0.jar")) upstream = jar;
      }
    }
    assertNotNull("Exact upstream core must be present on the test classpath", upstream);
    var sources = Path.of(System.getProperty("basedir"), "src/main/java");
    int checked = 0;
    try (var files = Files.walk(sources)) {
      for (var source : files.filter(p -> p.toString().endsWith(".java")).toList()) {
        var name =
            sources.relativize(source).toString().replace('\\', '/').replace(".java", ".class");
        var original = upstream.getJarEntry(name);
        if (original == null) continue; // Newly introduced helper, not an overlay.
        try (var originalBytes = upstream.getInputStream(original);
            var replacementBytes = loader.getResourceAsStream(name)) {
          var required = signatures(new ClassReader(originalBytes));
          var actual = signatures(new ClassReader(replacementBytes));
          required.removeAll(actual);
          assertTrue(name + " removed or changed binary members: " + required, required.isEmpty());
          checked++;
        }
      }
    }
    assertTrue("Must inspect the overlay sources", checked > 0);
  }

  private static Set<String> signatures(ClassReader reader) {
    Set<String> signatures = new HashSet<>();
    reader.accept(
        new ClassVisitor(Opcodes.ASM9) {
          @Override
          public MethodVisitor visitMethod(
              int access, String name, String descriptor, String signature, String[] exceptions) {
            if ((access & Opcodes.ACC_PRIVATE) == 0 && !name.equals("<clinit>")) {
              signatures.add("method " + linkageFlags(access) + " " + name + descriptor);
            }
            return null;
          }

          @Override
          public FieldVisitor visitField(
              int access, String name, String descriptor, String signature, Object value) {
            if ((access & Opcodes.ACC_PRIVATE) == 0) {
              signatures.add("field " + linkageFlags(access) + " " + name + ":" + descriptor);
            }
            return null;
          }
        },
        ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
    return signatures;
  }

  private static int linkageFlags(int access) {
    return access
        & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL);
  }
}
