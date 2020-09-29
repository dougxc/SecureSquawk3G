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
 * Class representing a convertion instruction.
 *
 * @author   Nik Shaylor
 */
public class CvtOp extends UnOp {

    /**
     * Convert flag. If this is true then some kind of arithmetic convertion is
     * required like a float of 3.14 becoming an int of 3. If it is set false
     * then the data represented on the input is forced to be the new type without
     * changing its bit-wize representation. This is like Float.floatToIntBits()
     * will convert a float into an int representation of the bits in the floating
     * point number.
     */
    boolean conv;

    /**
     * Constructor.
     *
     * @param p1   the instruction producing the input operand
     * @param to   the type the convertion is to
     * @param conv true if the value should be convert, false if it should be forced
     */
    public CvtOp(Instruction p1, Type to, boolean conv) {
        super((Codes.OP_CVT + (to.getTypeCode() << 4)), p1, to);
        this.conv = conv;
    }

    /**
     * Call the approperate function of an CodeEmitter.
     *
     * @param emitter the CodeEmitter
     */
    public void emit(CodeEmitter emitter) {
        emitter.emitCvtOp(this);
        emitter.freeLocal(in);
    }

    /**
     * Print the IR.
     *
     * @param out the output stream
     */
    public void print(PrintStream out) {
        super.printBase(out);
        out.print(" CvtOp " + (opcode &0xF));
        out.print(" ");
        in.print(out);
    }

}