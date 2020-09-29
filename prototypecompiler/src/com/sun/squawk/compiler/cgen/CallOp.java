/*
 * Copyright 1994-2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 */

package com.sun.squawk.compiler.cgen;

import java.io.PrintStream;
import com.sun.squawk.util.Stack;     // Version without synchronization
import com.sun.squawk.compiler.*;


/**
 * Class representing a call instruction.
 *
 * @author   Nik Shaylor
 */
public class CallOp extends Instruction {

    /**
     * The calling convention to use.
     */
    int convention;

    /**
     * The stack of StackLocals for the input operands.
     */
    Stack inputs = new Stack();

    /**
     * Constructor.
     *
     * @param jvmCall true if the JVM calling convention is to be used
     * @param nparms the number of parameters
     * @param stack the stack from which to pop the input parameters
     * @param convention the calling convention
     * @param type the return type
     */
    public CallOp(int nparms, Stack stack, Type type, int convention) {
        super(type);
        this.convention = convention;
        int first = stack.size() - nparms;
        for (int i = 0 ; i < nparms ; i++) {
            Instruction p = (Instruction)stack.elementAt(first+i);
            StackLocal in = new StackLocal(p.type());
            p.setTarget(in);
            inputs.push(in);
        }
        while (nparms-- > 0) {
            stack.pop();
        }
    }

    /**
     * Call the approperate function of an CodeEmitter.
     *
     * @param emitter the CodeEmitter
     */
    public void emit(CodeEmitter emitter) {
        emitter.emitCallOp(this);
        for (int i = 0 ; i < inputs.size() ; i++) {
            StackLocal in = (StackLocal)inputs.elementAt(i);
            emitter.freeLocal(in);
        }
    }

    /**
     * Print the IR.
     *
     * @param out the output stream
     */
    public void print(PrintStream out) {
        super.print(out);
        out.print(" CallOp ");
        StackLocal fn = (StackLocal)inputs.peek();
        fn.print(out);
    }

}