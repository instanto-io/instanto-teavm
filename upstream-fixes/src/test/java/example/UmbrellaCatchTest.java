/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package example;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
public class UmbrellaCatchTest {
  @Test
  public void reportsOperationFailure() {
    Outcome result = handleRequest();
    assertNotNull(result);
    assertEquals(500, result.status);
  }

  private static Outcome handleRequest() {
    try {
      performOperation();
      return Outcome.SUCCESS;
    } catch (Exception failure) {
      return Outcome.FAILURE;
    }
  }

  private static void performOperation() {
    Integer.parseInt("invalid input");
  }

  static final class Outcome {
    static final Outcome SUCCESS = new Outcome(200);
    static final Outcome FAILURE = new Outcome(500);
    final int status;

    Outcome(int status) {
      this.status = status;
    }
  }
}
