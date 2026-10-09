package example;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;
import org.teavm.junit.OnlyPlatform;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.TestPlatform;

@RunWith(TeaVMTestRunner.class)
@SkipJVM
@OnlyPlatform(TestPlatform.JAVASCRIPT)
public class TopLevelCollisionTest {
  @Test public void topLevelFunctionDeclaration() { assertEquals(49, declared(() -> 42)); }
  @Test public void topLevelLet() { assertEquals(49, lexical(() -> 42)); }
  @Test public void nestedStillWorks() { assertEquals(42, nested(() -> 42)); }

  @JSBody(params = "value", script = "function b() { return 7; } return value() + b();")
  private static native int declared(Value value);
  @JSBody(params = "value", script = "let b = 7; return value() + b;")
  private static native int lexical(Value value);
  @JSBody(params = "value", script = "function read(b) { return value(); } return read();")
  private static native int nested(Value value);

  @JSFunctor
  interface Value extends JSObject {
    int get();
  }
}
