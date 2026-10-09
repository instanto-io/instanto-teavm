package example;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSBodyImport;

public class Main {
  // Two imports from two modules. Expected output: "first second".
  @JSBody(
      script = "return first.name + ' ' + second.name;",
      imports = {
        @JSBodyImport(alias = "first", fromModule = "./first.js"),
        @JSBodyImport(alias = "second", fromModule = "./second.js")
      })
  static native String names();

  public static void main(String[] args) {
    System.out.println(names());
  }
}
