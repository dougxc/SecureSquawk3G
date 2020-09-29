/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */

package com.sun.squawk.compiler.tests;

import com.sun.squawk.compiler.*;
import com.sun.squawk.compiler.Compiler;
import com.sun.squawk.os.*;

public class Test16 implements Types {

    public static void main(String[] args) {

        Compiler c = Compilation.newCompiler();

        c.enter();
        c.result(VOID);
            for (int i = 0 ; i < 4 ; i++) {
                c.literal(("p"+i+" = %d\n\u0000").getBytes());
                c.literal(i);
                c.loadParm();
                c.swapForABI();
                c.symbol("printf");
                c.call(VOID);
            }
            for (int i = 0 ; i < 4 ; i++) {
                c.literal(i+100);
                c.literal(i);
                c.storeParm();
            }
            for (int i = 0 ; i < 4 ; i++) {
                c.literal(("p"+i+" = %d\n\u0000").getBytes());
                c.literal(i);
                c.loadParm();
                c.swapForABI();
                c.symbol("printf");
                c.call(VOID);
            }
            c.ret();
        c.leave();

        c.compile();

        int entry = Compilation.newLinker(c).link();
        CSystem.vcall(new Parm(entry).parm(0).parm(1).parm((long)2));
    }

}
