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

public class Test13 implements Types {

    public static void main(String[] args) {

        /*
         * Compile and relocate a function with an unresolved reference
         * to a function called foobar.
         */
        Compiler c1 = Compilation.newCompiler();
        c1.enter();
        c1.result(VOID);
            c1.symbol("foobar");
            c1.call(VOID, Compiler.C_JVM);
            c1.ret();
        c1.leave();
        c1.compile();
        Linker l1 = Compilation.newLinker(c1).relocate();

        /*
         * Compile and relocate the foobar function.
         */
        Compiler c2 = Compilation.newCompiler();
        c2.enter();
        c2.result(VOID);
            c2.literal("Hello World\n\u0000".getBytes());
            c2.symbol("printf");
            c2.call(VOID);
            c2.ret();
        c2.leave();
        c2.compile();
        Linker l2 = Compilation.newLinker(c2).relocate("foobar");

        /*
         * Link both functions and call the first one.
         */
        int entry = l1.link();
        l2.link();
        CSystem.vcall(new Parm(entry));
    }


}
