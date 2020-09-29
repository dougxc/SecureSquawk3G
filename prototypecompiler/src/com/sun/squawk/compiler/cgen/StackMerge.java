/*
 * Copyright 1994-2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 */

package com.sun.squawk.compiler.cgen;

import com.sun.squawk.compiler.*;
import com.sun.squawk.util.Stack; // Version without synchronization


/**
 * Class representing a merge point where a number of code paths come togerther.
 * The main point of the implementation is to record all the data producing instructions
 * that feed data into one stack variable.
 *
 * @author   Nik Shaylor
 */
public class StackMerge extends Instruction {

    /**
     * List of instructions that produce the values on the stack at the branch target for this merge.
     */
    Stack producers = new Stack();

    /**
     * Construct a stack merge.
     *
     * @param type the type of the variable on the stack
     */
    public StackMerge(Type type) {
        super(type);
    }

    /**
     * Add a producing instruction.
     *
     * @param in the producing instruction
     */
    public void addProducer(Instruction in) {
        producers.push(in);
    }

    /**
     * Set the instruction's output stack variable.
     *
     * @param slocal the stack local variable that the instruction should write into
     */
    public void setTarget(StackLocal slocal) {
        for (int i = 0 ; i < producers.size() ; i++) {
            Instruction in = (Instruction)producers.elementAt(i);
            in.setTarget(slocal);
        }
    }

    /**
     * Call the approperate function of an CodeEmitter.
     *
     * @param emitter the CodeEmitter
     */
    public void emit(CodeEmitter emitter) {
        /* This is not a real instruction so nothing is emitted. */
    }

}
