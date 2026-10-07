package example;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.jso.JSBody;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
@SkipJVM
public class JoinAfterCatchTest {
  static Object sink;
  static int sinkInt;

  @Test
  public void objectReadAfterEmptyCatch() {
    try {
      failingCall();
      sink = ObjectHolder.VALUE;
    } catch (RuntimeException failure) {
      // The handler does not touch ObjectHolder.
    }
    assertNotNull(ObjectHolder.VALUE);
  }

  @Test
  public void computedPrimitiveReadAfterEmptyCatch() {
    try {
      failingCall();
      sinkInt = IntHolder.VALUE;
    } catch (RuntimeException failure) {
      // The handler does not touch IntHolder.
    }
    assertEquals(42, IntHolder.VALUE);
  }

  static class ObjectHolder {
    static final Object VALUE = create();

    static Object create() {
      return new StringBuilder("v");
    }
  }

  static class IntHolder {
    static final int VALUE = compute();

    static int compute() {
      return Integer.parseInt("42");
    }
  }

  private static void failingCall() {
    if (fails()) {
      throw new IllegalStateException("expected");
    }
  }

  @JSBody(script = "return true;")
  private static native boolean fails();
}
