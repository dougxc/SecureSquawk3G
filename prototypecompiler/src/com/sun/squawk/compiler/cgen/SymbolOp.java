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
 * Class representing a literal integer value that is linked from a symbol.
 *
 * @author   Nik Shaylor
 */
public class SymbolOp extends Instruction {

    /**
     * The name of the symbol need to be bound to (or zero if does not require linking).
     */
    String name;

    /**
     * Constructor.
     *
     * @param name the name of the symbol needs to be linked.
     */
    public SymbolOp(String name) {
        super(REF);
        this.name  = name;
    }

    /**
     * Call the approperate function of an CodeEmitter.
     *
     * @param emitter the CodeEmitter
     */
    public void emit(CodeEmitter emitter) {
        emitter.emitSymbolOp(this);
    }

    /**
     * Print the IR.
     *
     * @param out the output stream
     */
    public void print(PrintStream out) {
        super.print(out);
        out.print(" SymbolOp " + name);
    }
}