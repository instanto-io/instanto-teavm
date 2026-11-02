/*
 *  Copyright 2026 Alexey Andreev.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.teavm.backend.wasm.vtable;

import java.util.Collection;
import java.util.Map;
import java.util.function.Predicate;
import org.teavm.model.ListableClassReaderSource;
import org.teavm.model.MethodReference;
import org.teavm.model.VirtualMethodResolver;

public class WasmGCVirtualTableProvider {
  private ListableClassReaderSource classes;
  private Map<String, WasmGCVirtualTable> virtualTables;

  public WasmGCVirtualTableProvider(
      ListableClassReaderSource classes,
      Collection<MethodReference> methodsAtCallSites,
      Predicate<MethodReference> isVirtual) {
    this.classes = classes;
    var builder = new WasmGCVirtualTableBuilder();
    builder.classes = classes;
    builder.methodsAtCallSites = methodsAtCallSites;
    builder.isVirtual = isVirtual;
    builder.build();
    virtualTables = builder.result;
  }

  public WasmGCVirtualTableEntry lookup(MethodReference method) {
    var table = lookup(method.getClassName());
    return table != null ? table.entry(VirtualMethodResolver.slot(classes, method)) : null;
  }

  public WasmGCVirtualTable lookup(String name) {
    return virtualTables.get(name);
  }
}
