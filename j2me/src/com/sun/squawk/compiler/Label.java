/*
 * Copyright 1994-2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 */

package com.sun.squawk.compiler;

/**
 * Class representing a label in the compiler interface.
 *
 * @author   Nik Shaylor
 */
public interface Label {

    /**
     * Test to see if the label has been bound.
     *
     * @return true if it is
     */
    public boolean isBound();

    /**
     * Get the offset to the label in the code buffer.
     *
     * @return the offset in bytes
     */
    public int getOffset();

}
