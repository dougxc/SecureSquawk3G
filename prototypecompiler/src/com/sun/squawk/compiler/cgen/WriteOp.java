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
 * Class representing a load instruction.
 *
 * @author   Nik Shaylor
 */
public class WriteOp extends Instruction {

    /**
     * The stack local variable for the address operand.
     */
    StackLocal ref;

    /**
     * The stack local variable for the input operand.
     */
    StackLocal in;

    /**
     * The type of write
     */
    Type writeType;

    /**
     * Constructor.
     *
     * @param addr the instruction producing the address
     * @param p1 the instruction producing the input operand
     * @param type the type of write
     */
    public WriteOp(Instruction addr, Instruction p1, Type type) {
        super(VOID);
        ref = new StackLocal(addr.type());
        addr.setTarget(ref);
        in = new StackLocal(p1.type());
        p1.setTarget(in);
        this.writeType = type;
    }

    /**
     * Call the approperate function of an CodeEmitter.
     *
     * @param emitter the CodeEmitter
     */
    public void emit(CodeEmitter emitter) {
        emitter.emitWriteOp(this);
        emitter.freeLocal(ref);
        emitter.freeLocal(in);
    }

    /**
     * Print the IR.
     *
     * @param out the output stream
     */
    public void print(PrintStream out) {
        super.print(out);
        out.print(" WriteOp ");
        ref.print(out);
        out.print(" ");
        in.print(out);
        out.print(" " + type().getPrimitiveType().getTypeCode());
    }

}