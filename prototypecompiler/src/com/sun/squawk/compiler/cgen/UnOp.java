/*
 * Copyright 1994-2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 */

package com.sun.squawk.compiler.cgen;

import java.io.PrintStream;
import com.sun.squawk.compiler.*;

/**
 * Class representing a unary instruction.
 *
 * @author   Nik Shaylor
 */
public class UnOp extends Instruction {

    /**
     * The opcode.
     */
    int opcode;

    /**
     * The stack local variable for the input operand.
     */
    StackLocal in;

    /**
     * Constructor.
     *
     * @param op the opcode
     * @param p1 the instruction producing the input operand
     */
    public UnOp(int op, Instruction p1) {
        this(op, p1, p1.type());
    }

    /**
     * Constructor.
     *
     * @param op the opcode
     * @param p1 the instruction producing the input operand
     * @param type the type that the instruction produces
     */
    public UnOp(int op, Instruction p1, Type type) {
        super(type.getPrimitiveType());
        opcode = op + p1.getTypeCode();
        in = new StackLocal(p1.type());
        p1.setTarget(in);
    }

    /**
     * Constructor for DupOp.
     *
     * @param p1 the instruction producing the input operand
     * @param first the first DupOp in sequence or null if this is the first one,
     */
    public UnOp(Instruction p1, DupOp first) {
        super(p1.type());
        opcode = Codes.OP_CVT + (p1.getTypeCode() << 4) + p1.getTypeCode();
        if (first == null) {
            in = new StackLocal(p1.type());
        } else {
            in = first.in;
        }
        p1.setTarget(in);
    }


    /**
     * Call the approperate function of an CodeEmitter.
     *
     * @param emitter the CodeEmitter
     */
    public void emit(CodeEmitter emitter) {
        emitter.emitUnOp(this);
        emitter.freeLocal(in);
    }

    /**
     * Print the IR.
     *
     * @param out the output stream
     */
    public void print(PrintStream out) {
        super.print(out);
        out.print(" UnOp " + (opcode>>8) + " ");
        in.print(out);
    }

}