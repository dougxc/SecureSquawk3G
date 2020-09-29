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

public class Test14 implements Types {

    public static void main(String[] args) {

        /*
         * Compile and link a function with label in the middle to code
         * that can be branched to from another method.
         */
        Compiler c1 = Compilation.newCompiler();
        c1.enter();
        c1.result(VOID);
            c1.literal("Should not reach here 1\n\u0000".getBytes());
            c1.symbol("printf");
            c1.call(2, VOID);

            Label l = c1.label();
        c1.bind(l);

            c1.literal("Hello World\n\u0000".getBytes());
            c1.symbol("printf");
            c1.call(2, VOID);
            c1.ret();
        c1.leave();
        c1.compile();
        Linker l1 = Compilation.newLinker(c1).relocate();
        int l1entry = l1.link();

        /*
         * Calculate the absolute adddress of the label
         */
        int address = l.getOffset() + l1entry;

        /*
         * Compile a function that will branch to an absolute location.
         */
        Compiler c2 = Compilation.newCompiler();
        c2.enter();
        c2.result(VOID);
            c2.literal(true);
            c2.bt(address);
            c2.literal("Should not reach here 2\n\u0000".getBytes());
            c2.symbol("printf");
            c2.call(2, VOID);
            c2.ret();
        c2.leave();
        c2.compile();

        /*
         * Call the second function.
         */
        int entry = Compilation.newLinker(c2).link();
        CSystem.vcall(new Parm(entry));

    }


}
