/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package example;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.jso.browser.Window;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
@SkipJVM
public class TimerTest {
  @Test
  public void timerCanCaptureAJavaCollection() {
    List<String> values = new ArrayList<>();
    int handle = Window.setTimeout(() -> values.add("fired"), 100000);
    Window.clearTimeout(handle);
    assertTrue(values.isEmpty());
  }
}
