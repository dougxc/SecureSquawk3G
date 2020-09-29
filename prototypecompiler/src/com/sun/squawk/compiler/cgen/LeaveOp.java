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
 * Class representing an leave instruction.
 *
 * @author   Nik Shaylor
 */
public class LeaveOp extends Instruction {

    /**
     * The method map for the function
     */
    MethodMap mmap;

    /**
     * Constructor.
     *
     * @param  mmap the method map for the function
     */
    public LeaveOp(MethodMap mmap) {
        super(VOID);
        this.mmap = mmap;
    }

    /**
     * Call the approperate function of an CodeEmitter.
     *
     * @param emitter the CodeEmitter
     */
    public void emit(CodeEmitter emitter) {
        emitter.emitLeaveOp(this);
        CodeGenerator cgen = (CodeGenerator)emitter;
        cgen.emitLiterals();
        if (mmap != null) {
            mmap.setup(
                        cgen.getLocalSlotCount(),
                        cgen.getLocalOopMap(),
                        cgen.getParameterSlotCount(),
                        cgen.getParameterOopMap()
                      );
        }

    }

    /**
     * Print the IR.
     *
     * @param out the output stream
     */
    public void print(PrintStream out) {
        super.print(out);
        out.print(" LeaveOp ");
    }
}