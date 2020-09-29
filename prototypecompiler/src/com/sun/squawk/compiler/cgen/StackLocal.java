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
 * The representation of a local variable allocated on the Java stack.
 *
 * @author   Nik Shaylor
 */
public class StackLocal {

    /**
     * The type of the stack local.
     */
    private Type type;

    /**
     * Pointer to the activation slot for this stack entry.
     */
    private ActivationSlot aslot;

    /**
     * Constructor.
     *
     * @param type the type of the activation slot
     */
    public StackLocal(Type type) {
        this.type = type;
    }

    /**
     * Get the type.
     *
     * @return the type
     */
    public Type type() {
        return type;
    }

    /**
     * Get the activation slot for the stack variable.
     *
     * @param cgen the code generator
     * @return the activation slot
     */
    public ActivationSlot getSlot(CodeGenerator cgen) {
        if (aslot == null) {
            aslot = cgen.allocSlot(type);
        }
        return aslot;
    }

    /**
     * Free the activation slot.
     *
     * @param cgen the code generator
     */
    public void freeSlot(CodeGenerator cgen) {
        if (aslot != null) {
            cgen.freeSlot(aslot);
        }
    }

    /**
     * Print the IR.
     *
     * @param out the output stream
     */
    public void print(PrintStream out) {
        out.print("L"+aslot.getOffset());
    }

}
