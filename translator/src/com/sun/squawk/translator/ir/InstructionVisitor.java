/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM translator.
 */
package com.sun.squawk.translator.ir;

import com.sun.squawk.translator.ir.instr.*;

/**
 * This interface provides a visitor pattern mechanism for traversing
 * the instructions in an IR.
 *
 * @author  Doug Simon
 */
public interface InstructionVisitor {

    /**
     * Visit instruction.
     *
     * @param instruction the instruction
     */
    public void doArithmeticOp      (ArithmeticOp       instruction);

    /**
     * Visit instruction.
     *
     * @param instruction the instruction
     */
    public void doArrayLength       (ArrayLength        instruction);

    /**
     * Visit instruction.
     *
     * @param instruction the instruction
     */
    public void doArrayLoad         (ArrayLoad          instruction);

    /**
     * Visit instruction.
     *
     * @param instruction the instruction
     */
    public void doArrayStore        (ArrayStore         instruction);

    /**
     * Visit instruction.
     *
     * @param instruction the instruction
     */
    public void doBranch            (Branch             instruction);

    /**
     * Visit instruction.
     *
     * @param instruction the instruction
     */
    public void doCheckCast         (CheckCast          instruction);

    /**
     * Visit instruction.
     *
     * @param instruction the instruction
     */
    public void doConversionOp      (ConversionOp       instruction);

    /**
     * Visit instruction.
     *
     * @param instruction the instruction
     */
    public void doComparisonOp      (ComparisonOp       instruction);

    /**
     * Visit instruction.
     *
     * @param instruction the instruction
     */
    public void doTry               (Try                instruction);

    /**
     * Visit instruction.
     *
     * @param instruction the instruction
     */
    public void doTryEnd            (TryEnd             instruction);

    /**
     * Visit instruction.
     *
     * @param instruction the instruction
     */
    public void doIf                (If                 instruction);

    /**
     * Visit instruction.
     *
     * @param instruction the instruction
     */
    public void doIfCompare         (IfCompare          instruction);

    /**
     * Visit instruction.
     *
     * @param instruction the instruction
     */
    public void doIncDecLocal       (IncDecLocal        instruction);

    /**
     * Visit instruction.
     *
     * @param instruction the instruction
     */
    public void doInstanceOf        (InstanceOf         instruction);

    /**
     * Visit instruction.
     *
     * @param instruction the instruction
     */
    public void doFindSlot          (FindSlot           instruction);

    /**
     * Visit instruction.
     *
     * @param instruction the instruction
     */
    public void doInvokeSlot        (InvokeSlot         instruction);

    /**
     * Visit instruction.
     *
     * @param instruction the instruction
     */
    public void doInvokeStatic      (InvokeStatic       instruction);

    /**
     * Visit instruction.
     *
     * @param instruction the instruction
     */
    public void doInvokeSuper       (InvokeSuper        instruction);

    /**
     * Visit instruction.
     *
     * @param instruction the instruction
     */
    public void doInvokeVirtual     (InvokeVirtual      instruction);

    /**
     * Visit instruction.
     *
     * @param instruction the instruction
     */
    public void doConstant          (Constant           instruction);

    /**
     * Visit instruction.
     *
     * @param instruction the instruction
     */
    public void doCatch             (Catch              instruction);

    /**
     * Visit instruction.
     *
     * @param instruction the instruction
     */
    public void doGetField          (GetField           instruction);

    /**
     * Visit instruction.
     *
     * @param instruction the instruction
     */
    public void doGetStatic         (GetStatic          instruction);

    /**
     * Visit instruction.
     *
     * @param instruction the instruction
     */
    public void doLoadLocal         (LoadLocal          instruction);

    /**
     * Visit instruction.
     *
     * @param instruction the instruction
     */
    public void doLookupSwitch      (LookupSwitch       instruction);

    /**
     * Visit instruction.
     *
     * @param instruction the instruction
     */
    public void doMonitorEnter      (MonitorEnter       instruction);

    /**
     * Visit instruction.
     *
     * @param instruction the instruction
     */
    public void doMonitorExit       (MonitorExit        instruction);

    /**
     * Visit instruction.
     *
     * @param instruction the instruction
     */
    public void doNegationOp        (NegationOp         instruction);

    /**
     * Visit instruction.
     *
     * @param instruction the instruction
     */
    public void doNewArray          (NewArray           instruction);

    /**
     * Visit instruction.
     *
     * @param instruction the instruction
     */
    public void doNewDimension      (NewDimension       instruction);

    /**
     * Visit instruction.
     *
     * @param instruction the instruction
     */
    public void doNew               (New                instruction);

    /**
     * Visit instruction.
     *
     * @param instruction the instruction
     */
    public void doPhi               (Phi                instruction);

    /**
     * Visit instruction.
     *
     * @param instruction the instruction
     */
    public void doPop               (Pop                instruction);

    /**
     * Visit instruction.
     *
     * @param instruction the instruction
     */
    public void doPosition          (Position           instruction);

    /**
     * Visit instruction.
     *
     * @param instruction the instruction
     */
    public void doReturn            (Return             instruction);

    /**
     * Visit instruction.
     *
     * @param instruction the instruction
     */
    public void doPutField          (PutField           instruction);

    /**
     * Visit instruction.
     *
     * @param instruction the instruction
     */
    public void doPutStatic         (PutStatic          instruction);

    /**
     * Visit instruction.
     *
     * @param instruction the instruction
     */
    public void doStoreLocal        (StoreLocal         instruction);

    /**
     * Visit instruction.
     *
     * @param instruction the instruction
     */
    public void doStackMerge        (StackMerge         instruction);

    /**
     * Visit instruction.
     *
     * @param instruction the instruction
     */
    public void doStackOp           (StackOp            instruction);

    /**
     * Visit instruction.
     *
     * @param instruction the instruction
     */
    public void doTableSwitch       (TableSwitch        instruction);

    /**
     * Visit instruction.
     *
     * @param instruction the instruction
     */
    public void doThrow             (Throw              instruction);
}