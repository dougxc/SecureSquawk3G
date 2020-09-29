/*
 * Copyright 1994-2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 */

package com.sun.squawk.compiler;

/**
 * Class defining the oopmaps needed for a compiled method
 *
 * @author   Nik Shaylor
 */
public class MethodMap {

    /**
     * The number of local slots.
     */
    private int localSlotCount;

    /**
     * The local oopmap.
     */
    private byte[] localOopMap;

    /**
     * The number of parameter slots.
     */
    private int parameterSlotCount;

    /**
     * The parameter oopmap.
     */
    private byte[] parameterOopMap;

    /**
     * The constructor.
     */
    public MethodMap() {
    }

    /**
     * Set the contants of the method map from the code generator.
     */
    public void setup(int localSlotCount, byte[] localOopMap, int parameterSlotCount, byte[] parameterOopMap) {
        this.localSlotCount     = localSlotCount;
        this.localOopMap        = localOopMap;
        this.parameterSlotCount = parameterSlotCount;
        this.parameterOopMap    = parameterOopMap;
    }

    /**
     * Get the count of locals allocaed.
     *
     * @return the number allocated
     */
    public int getLocalSlotCount() {
        return localSlotCount;
    }

    /**
     * Get an oopmap for the local variables.
     *
     * @return the oopmap
     */
    public byte[] getLocalOopMap() {
        return localOopMap;
    }

    /**
     * Get the count of parameters allocaed.
     *
     * @return the number allocated
     */
    public int getParameterSlotCount() {
        return parameterSlotCount;
    }

    /**
     * Get an oopmap for the parameter variables.
     *
     * @return the oopmap
     */
    public byte[] getParameterOopMap() {
        return parameterOopMap;
    }

}
