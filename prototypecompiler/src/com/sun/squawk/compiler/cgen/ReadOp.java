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
public class ReadOp extends Instruction {

    /**
     * The stack loacl variable for the address operand.
     */
    StackLocal ref;

    /**
     * The type of read
     */
    Type readType;

    /**
     * Constructor.
     *
     * @param addr the instriction supplying the address
     * @param type the data type to read
     */
    public ReadOp(Instruction addr, Type type) {
        super(type.getPrimitiveType());
        ref = new StackLocal(addr.type());
        addr.setTarget(ref);
        readType = type;
    }

    /**
     * Call the approperate function of an CodeEmitter.
     *
     * @param emitter the CodeEmitter
     */
    public void emit(CodeEmitter emitter) {
        emitter.emitReadOp(this);
        emitter.freeLocal(ref);
    }

    /**
     * Print the IR.
     *
     * @param out the output stream
     */
    public void print(PrintStream out) {
        super.print(out);
        out.print(" ReadOp ");
        ref.print(out);
        out.print(" " + type().getPrimitiveType().getTypeCode());
    }
}