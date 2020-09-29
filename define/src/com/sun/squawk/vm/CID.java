/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */
package com.sun.squawk.vm;

public final class CID {

    public final static int
        NULL                 = 0,
        OBJECT               = 1,
        STRING               = 2,
        THROWABLE            = 3,
        CLASS                = 4,
        VOID                 = 5,
        BOOLEAN              = 6,
        BYTE                 = 7,
        CHAR                 = 8,
        SHORT                = 9,
        INT                  = 10,
        LONG                 = 11,
        LONG2                = 12,
        FLOAT                = 13,
        DOUBLE               = 14,
        DOUBLE2              = 15,
        OBJECT_ARRAY         = 16,
        STRING_ARRAY         = 17,
        BOOLEAN_ARRAY        = 18,
        BYTE_ARRAY           = 19,
        CHAR_ARRAY           = 20,
        SHORT_ARRAY          = 21,
        INT_ARRAY            = 22,
        LONG_ARRAY           = 23,
        FLOAT_ARRAY          = 24,
        DOUBLE_ARRAY         = 25,
        STRING_OF_BYTES      = 26,  /* Small strings.                       */
        LOCAL                = 27,  /* Slot in stack chunk structure.       */
        GLOBAL               = 28,  /* Slot in class state structure.       */
        LOCAL_ARRAY          = 29,  /* Stack chunk structure.               */
        GLOBAL_ARRAY         = 30,  /* Class state structure.               */
        GLOBAL_ARRAYARRAY    = 31,  /* Table of class state structures.     */
        BYTECODE             = 32,  /* A bytecode.                          */
        BYTECODE_ARRAY       = 33,  /* An array of bytes that is a method.  */
        ADDRESS              = 34,  /* Abstraction over machine addresses   */
        ADDRESS_ARRAY        = 35,  /* Abstraction over machine addresses   */
        UWORD                = 36,  /* Abstraction over machine words       */
        UWORD_ARRAY          = 37,  /* Abstraction over machine words       */
        OFFSET               = 38,  /* Abstraction over directed address offsets */
        UNSAFE               = 39,  /* Peek/poke methods                    */

        LAST_CLASS_ID        = UNSAFE;
}
