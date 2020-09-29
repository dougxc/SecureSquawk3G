/*
 * Copyright 1994-2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 */

package com.sun.squawk.compiler.cgen;

import java.io.PrintStream;
import com.sun.squawk.util.Assert;
import com.sun.squawk.compiler.*;


/**
 * The base representation for all instructions in the IR.
 *
 * @author   Nik Shaylor
 */
public abstract class Instruction implements Types {

    /**
     * The next instruction id
     */
    private static int nextid = 0;

    /**
     * The instruction id for tracing
     */
    private int id = nextid++;

    /**
     * The data type the instruction produces.
     */
    private Type type;

    /**
     * The successing instruction in the IR.
     */
    private Instruction next;

    /**
     * The stack local variable that is the target for the output of this instruction.
     */
    private StackLocal target;

    /**
     * Construct an instruction of a certain type.
     *
     * @param type the instruction type
     */
    public Instruction(Type type) {
        this.type = type;
    }

    /**
     * Get the type the instruction produces.
     *
     * @return the type
     */
    public Type type() {
        return type;
    }

    /**
     * Get the type code.
     *
     * @return the type code ('I', 'L', etc...)
     */
    public int getTypeCode() {
        return type.getTypeCode();
    }

    /**
     * Set the next instruction.
     *
     * @see #getNext
     * @param inst the successor instruction
     */
    public void setNext(Instruction inst) {
        next = inst;
    }

    /**
     * Get the next instruction.
     *
     * @see #setNext
     * @return the successor instruction
     */
    public Instruction getNext() {
        return next;
    }

    /**
     * Set the stack variable that is the target for the output of this instruction.
     *
     * @see #getTarget
     * @param slocal the target stack variable
     */
    public void setTarget(StackLocal slocal) {
        Assert.always(target == null || target == slocal, "Instruction target already has a target");
        target = slocal;
    }

    /**
     * Get the instruction's target stack local variable.
     *
     * @see #setTarget
     * @return the type code ('I', 'L', etc...)
     */
    public StackLocal getTarget() {
        return target;
    }

    /**
     * The function that will produce the compiled code.
     *
     * @param cgen the code generator
     */
    abstract public void emit(CodeEmitter cgen);

    /**
     * The function that free any stack local varibles.
     */
    public void freeLocals() {
    }

    /**
     * Test to see if the instrction can fall through to the next insatruction in sequence.
     *
     * @return true if it can
     */
    public boolean canFallThrough() {
        return true;
    }

    /**
     * Print the IR.
     *
     * @param out the output stream
     */
    public void printBase(PrintStream out) {
        out.print("["+id+"]\t");
        if (target != null) {
            out.print(" ");
            target.print(out);
            out.print(" = ");
        }
    }

    /**
     * Print the IR.
     *
     * @param out the output stream
     */
    public void print(PrintStream out) {
        printBase(out);
    }
}
