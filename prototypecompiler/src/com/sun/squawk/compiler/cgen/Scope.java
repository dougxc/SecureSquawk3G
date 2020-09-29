/*
 * Copyright 1994-2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 */

package com.sun.squawk.compiler.cgen;

/**
 * Class representing begin/end scope in the compiler's source language.
 *
 * @author   Nik Shaylor
 */
public class Scope {

    /**
     * Pointer to the hierarchically previous scope.
     */
    private Scope previous;

    /**
     * Constructor.
     *
     * @param previous the hierarchically previous scope
     */
    public Scope(Scope previous) {
        this.previous = previous;
    }

    /**
     * Get the previous scope.
     *
     * @return the previous scope
     */
    public Scope getPrevious() {
        return previous;
    }

}
