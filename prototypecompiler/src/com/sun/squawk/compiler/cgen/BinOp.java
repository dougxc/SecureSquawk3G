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
 * Class representing a binary instruction.
 *
 * @author   Nik Shaylor
 */
public class BinOp extends Instruction {

    /**
     * The opcode.
     */
    int opcode;

    /**
     * The stack local variable for the first operand.
     */
    StackLocal in1;

    /**
     * The stack local variable for the second operand.
     */
    StackLocal in2;

    /**
     * Binary instruction constructor.
     *
     * @param op   the opcode
     * @param type the return type
     * @param p1   the instruction producing the first operand
     * @param p2   the instruction producing the second operand
     */
    public BinOp(int op, Type type, Instruction p1, Instruction p2) {
        super(type);
        opcode = op + p1.getTypeCode();
        in1 = new StackLocal(p1.type());
        in2 = new StackLocal(p2.type());
        p1.setTarget(in1);
        p2.setTarget(in2);
    }

    /**
     * Binary instruction constructor.
     *
     * @param op the opcode
     * @param p1 the instruction producing the first operand
     * @param p2 the instruction producing the second operand
     */
    public BinOp(int op, Instruction p1, Instruction p2) {
        this(op, p1.type() == OOP ? REF : p1.type(), p1, p2);
    }

    /**
     * Call the approperate function of an CodeEmitter.
     *
     * @param emitter the CodeEmitter
     */
    public void emit(CodeEmitter emitter) {
        emitter.emitBinOp(this);
        emitter.freeLocal(in1);
        emitter.freeLocal(in2);
    }

    /**
     * Print the IR.
     *
     * @param out the output stream
     */
    public void print(PrintStream out) {
        super.print(out);
        out.print(" BinOp " + (opcode>>8) + " ");
        in1.print(out);
        out.print(" ");
        in2.print(out);
    }

}