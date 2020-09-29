/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM translator.
 */
package com.sun.squawk.translator.ir;

import com.sun.squawk.util.Assert;
import com.sun.squawk.translator.ir.instr.*;

/**
 * An instance of <code>ExceptionHandler</code> represents an exception
 * handler expressed in terms of IR instructions.
 */
public class IRExceptionHandler {

    /**
     * The instruction denoting the position in the IR at which this handler
     * becomes active.
     */
    private Try entry;

    /**
     * The instruction denoting the position in the IR at which this handler
     * becomes deactive.
     */
    private TryEnd exit;

    /**
     * The target (and stack map) representing the address of the entry to
     * the exception handler.
     */
    private final Target target;

    /**
     * Constructor should only be called by an instance of
     * <code>IRBuilder</code>.
     *
     * @param target  the object encapsulating the stack map for the address of
     *                the entry to this exception handler
     */
    IRExceptionHandler(Target target) {
        this.target = target;
    }


    /*---------------------------------------------------------------------------*\
     *                                   Setters                                 *
    \*---------------------------------------------------------------------------*/

    /**
     * Sets the instruction denoting the position in the IR at which
     * this handler becomes active.<p>
     *
     * <b>This method should only be called by an instance of
     * <code>IRBuilder</code>.</b>
     *
     * @param entry  the instruction denoting the position in the IR
     *               at which this handler becomes active
     */
    void setEntry(Try entry) {
        Assert.that(this.entry == null, "cannot reset entry");
        this.entry = entry;
    }

    /**
     * Sets the instruction denoting the position in the IR at which
     * this handler becomes deactive.<p>
     *
     * <b>This method should only be called by an instance of
     * <code>IRBuilder</code>.</b>
     *
     * @param exit   the instruction denoting the position in the IR
     *               at which this handler becomes deactive
     */
    void setExit(TryEnd exit) {
        Assert.that(this.exit == null, "cannot reset exit");
        this.exit = exit;
    }


    /*---------------------------------------------------------------------------*\
     *                                   Getters                                 *
    \*---------------------------------------------------------------------------*/

    /**
     * Gets the instruction denoting the position in the IR at which
     * this handler becomes active.<p>
     *
     * @return  the instruction denoting the position in the IR
     *          at which this handler becomes active
     */
    public Try getEntry() {
        return entry;
    }

    /**
     * Gets the instruction denoting the position in the IR at which
     * this handler becomes deactive.<p>
     *
     * @return  the instruction denoting the position in the IR
     *          at which this handler becomes deactive
     */
    public TryEnd getExit() {
        return exit;
    }

    /**
     * Gets the instruction denoting the position in the IR at which the
     * code for this handler starts.
     *
     * @return  the entry position of this handler's code
     */
    public Catch getCatch() {
        return target.getCatch();
    }

    /**
     * Gets the object encapsulating the stack map for the address of
     * the entry to this exception handler
     *
     * @return the object encapsulating the stack map for the address of
     *         the entry to this exception handler
     */
    public Target getTarget() {
        return target;
    }
}
