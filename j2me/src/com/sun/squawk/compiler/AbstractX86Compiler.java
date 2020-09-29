/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */
package com.sun.squawk.compiler;

import com.sun.squawk.util.Assert;
import com.sun.squawk.compiler.Compiler;

abstract class AbstractX86Compiler implements Compiler {

    /**
     * @see Compiler
     */
    public int getFramePointerByteOffset(int fp_value) {
        switch (fp_value) {
            case com.sun.squawk.vm.FP.parm0:        return  8;
            case com.sun.squawk.vm.FP.returnIP:     return  4;
            case com.sun.squawk.vm.FP.returnFP:     return  0;
            case com.sun.squawk.vm.FP.local0:       return -4;
        }
        Assert.shouldNotReachHere();
        return 0;
    }

    /**
     * @see Compiler
     */
    public boolean loadsMustBeAligned() {
        return false;
    }

    /**
     * @see Compiler
     */
    public boolean isBigEndian() {
        return false;
    }

    /**
     * @see Compiler
     */
    public boolean tableSwitchPadding() {
        return false;
    }

    /**
     * @see Compiler
     */
    public boolean tableSwitchEndPadding() {
        return false;
    }

}
