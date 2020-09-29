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
 * Class representing the association of some data with a label.
 *
 * @author   Nik Shaylor
 */
public class DataOp extends Instruction {

    /**
     * The literal data.
     */
    protected LiteralData data;

    /**
     * Constructor.
     *
     * @param label the label to bind to the data
     * @param obj the data to output
     */
    public DataOp(XLabel label, Object obj) {
        super(VOID);
        data = new LiteralData(label, obj);
    }

    /**
     * Call the approperate function of an CodeEmitter.
     *
     * @param emitter the CodeEmitter
     */
    public void emit(CodeEmitter emitter) {
        emitter.emitDataOp(this);
    }

    /**
     * Print the IR.
     *
     * @param out the output stream
     */
    public void print(PrintStream out) {
        super.print(out);
        out.print(" DataOp " + data.getValue());
        out.print(" ");
        data.getLabel().print(out);
    }

}