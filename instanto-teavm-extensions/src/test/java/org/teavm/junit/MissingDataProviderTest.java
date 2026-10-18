package org.teavm.junit;

import static org.junit.Assert.*;

import java.lang.reflect.InvocationTargetException;
import org.junit.Test;
import org.teavm.model.*;
import org.teavm.model.emit.ProgramEmitter;
import org.teavm.model.emit.ValueEmitter;

public class MissingDataProviderTest {
  public static class Sample {
    public void requiresMissingProvider() {
      // Stub method for test runner inspection
    }
  }

  @Test
  public void jvmRunnerReportsMissingProviderRatherThanDereferencingNull() throws Exception {
    TeaVMTestRunner runner = new TeaVMTestRunner(Sample.class);
    var sourceField = TeaVMTestRunner.class.getDeclaredField("classSource");
    sourceField.setAccessible(true);
    ClassHolderSource source = (ClassHolderSource) sourceField.get(null);
    var method =
        source
            .get(Sample.class.getName())
            .getMethod(new MethodDescriptor("requiresMissingProvider", ValueType.VOID));
    AnnotationHolder annotation = new AnnotationHolder("org.testng.annotations.Test");
    annotation.getValues().put("dataProvider", new AnnotationValue("missing"));
    method.getAnnotations().add(annotation);
    var wrap =
        TeaVMTestRunner.class.getDeclaredMethod(
            "wrapWithDataProvider",
            TeaVMTestRunner.Runner.class,
            Object.class,
            java.lang.reflect.Method.class);
    wrap.setAccessible(true);
    try {
      InvocationTargetException error =
          assertThrows(
              InvocationTargetException.class,
              () ->
                  wrap.invoke(
                      runner,
                      (TeaVMTestRunner.Runner) args -> {},
                      new Sample(),
                      Sample.class.getMethod("requiresMissingProvider")));
      assertTrue(error.getCause().toString(), error.getCause() instanceof IllegalStateException);
      assertTrue(error.getCause().getMessage().contains("missing"));
      assertTrue(error.getCause().getMessage().contains("requiresMissingProvider"));
    } finally {
      method.getAnnotations().remove("org.testng.annotations.Test");
    }
  }

  @Test
  public void missingProviderReportsTestAndProviderNames() throws Exception {
    ClassHolder owner = new ClassHolder("example.SampleTest");
    MethodHolder test = new MethodHolder("sample", ValueType.VOID);
    owner.addMethod(test);
    ClassHierarchy hierarchy =
        new ClassHierarchy(name -> owner.getName().equals(name) ? owner : null);
    TestEntryPointTransformer transformer =
        new TestEntryPointTransformer(owner.getName()) {
          @Override
          protected void generateLaunchProgram(
              MethodHolder method, ClassHolderTransformerContext context) {
            // No-op for testing missing provider resolution
          }
        };
    var method =
        TestEntryPointTransformer.class.getDeclaredMethod(
            "generateAddLaunchersWithProvider",
            MethodReader.class,
            ClassHierarchy.class,
            ProgramEmitter.class,
            ValueEmitter.class,
            String.class,
            String.class);
    method.setAccessible(true);
    try {
      method.invoke(transformer, test, hierarchy, null, null, "missing", "Launcher");
      fail("Expected an actionable missing-provider error");
    } catch (InvocationTargetException e) {
      assertTrue(e.getCause() instanceof IllegalStateException);
      assertTrue(e.getCause().getMessage().contains("missing"));
      assertTrue(e.getCause().getMessage().contains("example.SampleTest"));
      assertTrue(e.getCause().getMessage().contains("sample"));
    }
  }
}
