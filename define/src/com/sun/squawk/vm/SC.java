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
 * This class contains the offsets that define that define the layout of the array
 * (of type "[-local-") that implements a stack for a thread.
 */
public class SC {

    /**
     * The index of the pointer to the next stack chunk in the global list of stack chunks.
     */
    public final static int next = 0;

    /**
     * The index of the pointer to the Thread instance that owns this chunk.
     */
    public final static int owner = 1;

    /**
     * The pointer to the inner most activation frame in the stack.
     */
    public final static int lastFP = 2;

    /**
     * The pointer to the inner most activation frame's instruction pointer in the stack.
     * This value is an integer offset to the start of the inner most method which
     * is always the word pointed to by lastFP.
     */
    public final static int lastIP = 3;

    /**
     * The is a word that is always unused. If this word is ever non-zero then an
     * overflow of the stack has occured.
     */
    public final static int guard = 4;

    /**
     * The offset of the stack limit (i.e. the last slot that can be used without overwriting
     * one of the header slots defined above).
     *
     *
     *        :                  :
     *        |      parmN       |  9
     *        +------------------+        +
     *        |     returnIP     |  8     |
     *        +------------------+        |
     *        |     returnFP     |  7     } FIXED_FRAME_SIZE
     *        +------------------+        |
     *  sl -->|        MP        |  6     |
     *        +==================+        +
     *        |      guard       |  4
     *        +------------------+
     *        |      lastIP      |  3
     *        +------------------+
     *        |      lastFP      |  2
     *        +------------------+
     *        |      owner       |  1
     *        +------------------+
     *        |       next       |  0
     *        +------------------+
     *
     *
     *
     *
     */
    public final static int limit = guard + 1;

    /**
     * This describes which words in a stack chunk corresponding to the above indexes
     * are pointers that must be traversed by the garbage collector.
     */
    public final static int oopMap = (1 << next) |
                                     (1 << owner);
}
