/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package example;

/** Shared by the browser JUnit suite and a real Maven application compilation. */
public final class InitializationScenarios {
  private InitializationScenarios() {}

  private static void operation() {
    Integer.parseInt("invalid input");
  }

  public static int umbrella() {
    Outcome result;
    try {
      operation();
      result = Outcome.SUCCESS;
    } catch (Exception failure) {
      result = Outcome.FAILURE;
    }
    return result.code;
  }

  static class Outcome {
    static final Outcome SUCCESS = new Outcome(200);
    static final Outcome FAILURE = new Outcome(500);
    final int code;

    Outcome(int code) {
      this.code = code;
    }
  }

  public static int enumConstant() {
    Status result;
    try {
      operation();
      result = Status.SUCCESS;
    } catch (Exception failure) {
      result = Status.FAILURE;
    }
    return result.ordinal();
  }

  enum Status {
    SUCCESS,
    FAILURE
  }

  public static int computedPrimitive() {
    try {
      operation();
      return Codes.SUCCESS;
    } catch (Exception failure) {
      return Codes.FAILURE;
    }
  }

  static class Codes {
    static final int SUCCESS = compute(200);
    static final int FAILURE = compute(500);

    static int compute(int value) {
      return value;
    }
  }

  public static int fallbackHelper() {
    Response result;
    try {
      operation();
      result = Response.SUCCESS;
    } catch (Exception failure) {
      result = fallback();
    }
    return result.code;
  }

  private static Response fallback() {
    return Response.FAILURE;
  }

  static class Response {
    static final Response SUCCESS = new Response(200);
    static final Response FAILURE = new Response(500);
    final int code;

    Response(int code) {
      this.code = code;
    }
  }

  public static int afterCatch() {
    try {
      operation();
      return Joined.SUCCESS;
    } catch (Exception failure) {
      // Continue to the common fallback below.
    }
    return Joined.FAILURE;
  }

  static class Joined {
    static final int SUCCESS = Integer.parseInt("200");
    static final int FAILURE = Integer.parseInt("500");
  }

  public static int nestedCatch() {
    try {
      try {
        operation();
        return Nested.SUCCESS;
      } catch (NumberFormatException inner) {
        operation();
        return Nested.FAILURE;
      }
    } catch (Exception outer) {
      return Nested.FAILURE;
    }
  }

  static class Nested {
    static final int SUCCESS = Integer.parseInt("200");
    static final int FAILURE = Integer.parseInt("500");
  }

  public static int finallyFallback() {
    int result;
    try {
      try {
        operation();
        return Finalized.SUCCESS;
      } catch (Exception failure) {
        // The finally block supplies the fallback.
      }
    } finally {
      result = Finalized.FAILURE;
    }
    return result;
  }

  static class Finalized {
    static final int SUCCESS = Integer.parseInt("200");
    static final int FAILURE = Integer.parseInt("500");
  }

  private static int stage;
  private static int initializations;

  public static int lazyInitialization() {
    stage = 0;
    try {
      operation();
      return Lazy.SUCCESS;
    } catch (Exception failure) {
      stage = 7;
      int first = Lazy.VALUE;
      int second = Lazy.VALUE;
      if (first != second || initializations != 1) throw new AssertionError();
      return first;
    }
  }

  static class Lazy {
    static final int SUCCESS = Integer.parseInt("200");
    static final int VALUE;

    static {
      initializations++;
      VALUE = stage;
    }
  }

  public static boolean boxedBoolean() {
    Boolean result;
    try {
      operation();
      result = Boolean.TRUE;
    } catch (Exception failure) {
      result = Boolean.FALSE;
    }
    return result.booleanValue();
  }
}
