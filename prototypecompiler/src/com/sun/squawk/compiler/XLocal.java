/*
 * Copyright 1994-2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 */

package com.sun.squawk.compiler;

import java.io.PrintStream;
import com.sun.squawk.util.Assert;
import com.sun.squawk.compiler.cgen.*;

/**
 * Class that defines a local variable in the source language of the compiler.
 * The class ActivationSlot represents a local variable that is created as a part
 * of a method's activation record.
 *
 * @author   Nik Shaylor
 */
public class XLocal implements Local {

    /**
     * The sentinel value for a dead loacl
     */
    private static Scope DEAD = new Scope(null);

    /**
     * The type of the local valiable
     */
    private Type type;

    /**
     * The scope of the local valiable (null if parameter)
     */
    private Scope scope;

    /**
     * The data structure that represents a slot in an activation record.
     */
    private ActivationSlot aslot;

    /**
     * Constructor for local variables.
     *
     * @param type the type of the local
     * @param scope the scope of the local
     */
    public XLocal(Type type, Scope scope) {
        this.type  = type;
        this.scope = scope;
    }

    /**
     * Constructor for local variables that are parameters.
     *
     * @param type the type of the local
     * @param offset the offset to the parameter in the acticvation record
     */
    public XLocal(Type type, int offset) {
        Assert.that(offset >= 0);
        this.type = type;
        aslot = new ActivationSlot(type, offset, true);
    }

    /**
     * Get the type of the local.
     *
     * @return the type
     */
    public Type type() {
        return type;
    }

    /**
     * Get the scope of the local.
     *
     * @return the scope
     */
    public Scope getScope() {
        return scope;
    }

    /**
     * Kill the use of the local.
     */
    public void kill() {
        scope = DEAD;
    }

    /**
     * Check that the local is alive.
     */
    public void checkAlive() {
        if (scope == DEAD) {
            throw new RuntimeException("access to local is out of scope");
        }
    }

    /**
     * Find a free local of a specific type.
     *
     * @param cgen  the code generator
     */
    public void allocLocal(CodeGenerator cgen) {
        if (scope == null) {
            cgen.addParameterSlot(aslot);
        } else {
            Assert.that(aslot == null);
            aslot = cgen.allocSlot(type);
        }
    }

    /**
     * Return the slot used for the local.
     *
     * @return the slot
     */
    public ActivationSlot getSlot() {
        Assert.that(aslot != null);
        return aslot;
    }

    /**
     * Find a free local of a specific type.
     *
     * @param cgen  the code generator
     */
    public void freeLocal(CodeGenerator cgen) {
        Assert.that(aslot != null);
        cgen.freeSlot(aslot);
    }

    /**
     * Print the IR.
     *
     * @param out the output stream
     */
    public void print(PrintStream out) {
        int offset = aslot.getOffset();
        if (aslot.isParm()) {
            out.print("P"+offset);
        } else {
            out.print("L"+offset);
        }
    }
}
