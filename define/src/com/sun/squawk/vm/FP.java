/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */
package com.sun.squawk.vm;

/**
 * This class contains the offsets and constants that define the layout of an
 * activation frame for the current method. All the offsets are relative to
 * the current frame pointer, FP.
 */
public class FP {

    /**
     * The offset of the slot containing the first parameter.
     */
    public final static int parm0 = 3;

    /**
     * The offset of the slot containing the IP of the caller of the current method.
     */
    public final static int returnIP = 2;

    /**
     * The offset of the slot containing the FP of the caller of the current method.
     */
    public final static int returnFP = 1;

    /**
     * The offset of the slot containing the first local variable of the current method.
     */
    public final static int local0 = 0;

    /**
     * The offset of the slot containing the pointer to the current method.
     */
    public final static int method = local0;

    /**
     * This is the number of slots that must be reserved for a call to a method
     * above and beyond the slots it requires for its local variables and operand
     * stack.
     */
    public final static int FIXED_FRAME_SIZE = 3;
}
