/*
 * Copyright 1993-2004 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package com.sun.squawk.compiler;

/**
 * Definition of all the constants used in the shadow stack.
 *
 * @author Cristina Cifuentes
 */
interface ShadowStackConstants {

    public static final int S_REG = 1,
                            S_LIT = 2,
                            S_LOCAL = 3,
                            S_OBJECT = 4,
                            S_FIXUP_SYM = 5,
                            S_LABEL = 6,
                            S_OTHER = 10;

}
