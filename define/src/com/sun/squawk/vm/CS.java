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
 * This class contains the offsets that define the layout of the array
 * (of type "[-global-") that holds the class state (i.e. static variables/globals) for a class.
 */
public class CS {

    /**
     * The index of the pointer to the class to which the variables pertain.
     */
    public final static int klass = 0;

    /**
     * The index of the pointer to the next class state record.
     */
    public final static int next = 1;

    /**
     * The index of the first static variable.
     */
    public final static int firstVariable = 2;

}
