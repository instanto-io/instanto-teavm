package org.teavm.junit;

import static org.junit.Assert.*;

import java.lang.reflect.InvocationTargetException;
import org.junit.Test;
import org.teavm.model.*;
import org.teavm.model.emit.ProgramEmitter;
import org.teavm.model.emit.ValueEmitter;

public class MissingDataProviderTest {
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
              MethodHolder method, ClassHolderTransformerContext context) {}
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
