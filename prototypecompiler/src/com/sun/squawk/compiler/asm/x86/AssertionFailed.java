/*
 * @(#)AssertionFailed.java             1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package com.sun.squawk.compiler.asm.x86;

/**
 * Thrown to indicate that an assertion has failed. This exception usually shows
 * up a bug in the source code of the compiler.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class AssertionFailed extends RuntimeException {
    /**
     * Constructs a new exception with no detail message.
     */
    public AssertionFailed() {
        super();
    }

    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param  msg  the detail message
     */
    public AssertionFailed(String msg) {
        super(msg);
    }
}
