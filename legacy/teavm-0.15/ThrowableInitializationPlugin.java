package io.instanto.compat.classlib;

import java.util.List;
import org.teavm.model.*;
import org.teavm.model.instructions.ConstructArrayInstruction;
import org.teavm.model.instructions.IntegerConstantInstruction;
import org.teavm.model.instructions.PutFieldInstruction;
import org.teavm.vm.spi.TeaVMHost;
import org.teavm.vm.spi.TeaVMPlugin;

/** Restores the field initializer omitted by TeaVM 0.15's replacement Throwable constructors. */
public final class ThrowableInitializationPlugin implements TeaVMPlugin {
  @Override
  public void install(TeaVMHost host) {
    host.add((ClassHolderTransformer) (cls, context) -> initializeSuppressedExceptions(cls));
  }

  private static void initializeSuppressedExceptions(ClassHolder cls) {
    if (!cls.getName().equals("java.lang.Throwable") || cls.getField("suppressed") == null) return;
    FieldReference field = new FieldReference(cls.getName(), "suppressed");
    ValueType itemType = ValueType.object(cls.getName());
    for (MethodHolder method : cls.getMethods()) {
      Program program = method.getProgram();
      if (!method.getName().equals("<init>") || program == null || program.basicBlockCount() == 0)
        continue;
      if (initializesField(program, field)) continue;
      IntegerConstantInstruction size = new IntegerConstantInstruction();
      size.setConstant(0);
      size.setReceiver(program.createVariable());
      ConstructArrayInstruction array = new ConstructArrayInstruction();
      array.setItemType(itemType);
      array.setSize(size.getReceiver());
      array.setReceiver(program.createVariable());
      PutFieldInstruction assignment = new PutFieldInstruction();
      assignment.setField(field);
      assignment.setFieldType(ValueType.arrayOf(itemType));
      assignment.setInstance(program.variableAt(0));
      assignment.setValue(array.getReceiver());
      program.basicBlockAt(0).addFirstAll(List.of(size, array, assignment));
    }
  }

  private static boolean initializesField(Program program, FieldReference field) {
    for (int index = 0; index < program.basicBlockCount(); index++) {
      for (Instruction instruction : program.basicBlockAt(index)) {
        if (instruction instanceof PutFieldInstruction put && put.getField().equals(field))
          return true;
      }
    }
    return false;
  }
}
