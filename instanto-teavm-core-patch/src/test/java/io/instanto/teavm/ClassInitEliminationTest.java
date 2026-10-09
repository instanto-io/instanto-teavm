/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.teavm;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.teavm.model.BasicBlock;
import org.teavm.model.MethodReference;
import org.teavm.model.Program;
import org.teavm.model.TryCatchBlock;
import org.teavm.model.ValueType;
import org.teavm.model.instructions.BranchingCondition;
import org.teavm.model.instructions.BranchingInstruction;
import org.teavm.model.instructions.ExitInstruction;
import org.teavm.model.instructions.InitClassInstruction;
import org.teavm.model.instructions.InvocationType;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.model.instructions.JumpInstruction;
import org.teavm.model.optimization.ClassInitElimination;

/** Tests the pass directly, including paths that a source-level test might optimize away. */
public class ClassInitEliminationTest {
  private static final String HOLDER = "example.Holder";

  @Test
  public void handlerMustNotInheritInitializationLaterInProtectedBlock() {
    Program program = blocks(2);
    call(program.basicBlockAt(0), "example.Operation");
    init(program.basicBlockAt(0));
    exit(program.basicBlockAt(0));
    protect(program.basicBlockAt(0), program.basicBlockAt(1));
    init(program.basicBlockAt(1));
    exit(program.basicBlockAt(1));
    optimize(program);
    assertInitializers(program, 1, 1);
  }

  @Test
  public void catchRejoiningNormalFlowMustNotInheritProtectedBlockExitState() {
    Program program = blocks(3);
    call(program.basicBlockAt(0), "example.Operation");
    init(program.basicBlockAt(0));
    jump(program.basicBlockAt(0), program.basicBlockAt(2));
    protect(program.basicBlockAt(0), program.basicBlockAt(1));
    jump(program.basicBlockAt(1), program.basicBlockAt(2));
    init(program.basicBlockAt(2));
    exit(program.basicBlockAt(2));
    optimize(program);
    assertInitializers(program, 1, 0, 1);
  }

  @Test
  public void invocationFactMustNotLeakIntoHandler() {
    Program program = blocks(2);
    call(program.basicBlockAt(0), "example.Operation");
    call(program.basicBlockAt(0), HOLDER);
    exit(program.basicBlockAt(0));
    protect(program.basicBlockAt(0), program.basicBlockAt(1));
    init(program.basicBlockAt(1));
    exit(program.basicBlockAt(1));
    optimize(program);
    assertInitializers(program, 0, 1);
  }

  @Test
  public void nestedHandlersRetainTheirOwnChecks() {
    Program program = blocks(3);
    for (int i = 0; i < 3; i++) {
      BasicBlock block = program.basicBlockAt(i);
      call(block, "example.Operation");
      init(block);
      exit(block);
      if (i < 2) protect(block, program.basicBlockAt(i + 1));
    }
    optimize(program);
    assertInitializers(program, 1, 1, 1);
  }

  @Test
  public void handlerLoopDoesNotProveInitializationOnFirstIteration() {
    Program program = blocks(4);
    jump(program.basicBlockAt(0), program.basicBlockAt(1));
    call(program.basicBlockAt(1), "example.Operation");
    init(program.basicBlockAt(1));
    jump(program.basicBlockAt(1), program.basicBlockAt(3));
    protect(program.basicBlockAt(1), program.basicBlockAt(2));
    init(program.basicBlockAt(2));
    jump(program.basicBlockAt(2), program.basicBlockAt(1));
    init(program.basicBlockAt(3));
    exit(program.basicBlockAt(3));
    optimize(program);
    assertInitializers(program, 0, 1, 1, 0);
  }

  @Test
  public void sharedHandlerCannotAssumeEitherProtectedBranchCompleted() {
    Program program = blocks(4);
    BranchingInstruction branch = new BranchingInstruction(BranchingCondition.EQUAL);
    branch.setOperand(program.createVariable());
    branch.setConsequent(program.basicBlockAt(1));
    branch.setAlternative(program.basicBlockAt(2));
    program.basicBlockAt(0).add(branch);
    for (int i = 1; i <= 2; i++) {
      call(program.basicBlockAt(i), "example.Operation");
      init(program.basicBlockAt(i));
      exit(program.basicBlockAt(i));
      protect(program.basicBlockAt(i), program.basicBlockAt(3));
    }
    init(program.basicBlockAt(3));
    exit(program.basicBlockAt(3));
    optimize(program);
    assertInitializers(program, 0, 1, 1, 1);
  }

  @Test
  public void completedInitializationBeforeTryStillEliminatesHandlerCheck() {
    Program program = blocks(3);
    init(program.basicBlockAt(0));
    jump(program.basicBlockAt(0), program.basicBlockAt(1));
    call(program.basicBlockAt(1), "example.Operation");
    exit(program.basicBlockAt(1));
    protect(program.basicBlockAt(1), program.basicBlockAt(2));
    init(program.basicBlockAt(2));
    exit(program.basicBlockAt(2));
    optimize(program);
    assertInitializers(program, 1, 0, 0);
  }

  @Test
  public void normalSuccessorStillUsesCompletedInitialization() {
    Program program = blocks(3);
    init(program.basicBlockAt(0));
    jump(program.basicBlockAt(0), program.basicBlockAt(1));
    protect(program.basicBlockAt(0), program.basicBlockAt(2));
    init(program.basicBlockAt(1));
    exit(program.basicBlockAt(1));
    exit(program.basicBlockAt(2));
    optimize(program);
    assertInitializers(program, 1, 0, 0);
  }

  @Test
  public void duplicateChecksWithinBlockAreStillRemoved() {
    Program program = blocks(1);
    init(program.basicBlockAt(0));
    init(program.basicBlockAt(0));
    exit(program.basicBlockAt(0));
    optimize(program);
    assertInitializers(program, 1);
  }

  private static Program blocks(int count) {
    Program program = new Program();
    for (int i = 0; i < count; i++) program.createBasicBlock();
    return program;
  }

  private static void init(BasicBlock block) {
    InitClassInstruction instruction = new InitClassInstruction();
    instruction.setClassName(HOLDER);
    block.add(instruction);
  }

  private static void call(BasicBlock block, String className) {
    InvokeInstruction instruction = new InvokeInstruction();
    instruction.setType(InvocationType.SPECIAL);
    instruction.setMethod(new MethodReference(className, "operation", ValueType.VOID));
    block.add(instruction);
  }

  private static void jump(BasicBlock from, BasicBlock to) {
    JumpInstruction instruction = new JumpInstruction();
    instruction.setTarget(to);
    from.add(instruction);
  }

  private static void exit(BasicBlock block) {
    block.add(new ExitInstruction());
  }

  private static void protect(BasicBlock block, BasicBlock handler) {
    TryCatchBlock tryCatch = new TryCatchBlock();
    tryCatch.setExceptionType("java.lang.Exception");
    tryCatch.setHandler(handler);
    block.getTryCatchBlocks().add(tryCatch);
  }

  private static void optimize(Program program) {
    new ClassInitElimination().optimize(null, program);
  }

  private static void assertInitializers(Program program, int... expected) {
    for (int i = 0; i < expected.length; i++) {
      int count = 0;
      for (var instruction : program.basicBlockAt(i)) {
        if (instruction instanceof InitClassInstruction) count++;
      }
      assertEquals("block " + i, expected[i], count);
    }
  }
}
