/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package example;

import static org.junit.Assert.assertFalse;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.jso.JSBody;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
@SkipJVM
public class ClassInitializationTest {
  @Test
  public void catchInitializesBooleanEvenWhenTryDidNot() {
    Boolean result;
    try {
      failingCall();
      result = Boolean.TRUE;
    } catch (RuntimeException failure) {
      result = Boolean.FALSE;
    }
    assertFalse(result);
  }

  private static void failingCall() {
    if (fails()) throw new IllegalStateException("expected");
  }

  // Keep the branch opaque to the Java optimizer so both bytecode paths remain reachable.
  @JSBody(script = "return true;")
  private static native boolean fails();
}
