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

public class Test10 implements Types {

    public static void main(String[] args) {

        Compiler c = Compilation.newCompiler();

        c.enter();
        c.result(VOID);

            /*
             * Print "Hello" using the absolute address of printf
             */
            c.literal("Hello \u0000".getBytes());
            c.literal(CSystem.lookup("printf"));
            c.call(2, VOID);

            /*
             * Print "World" using the symbolic address of printf
             */
            c.literal("World\n\u0000".getBytes());
            c.symbol("printf");
            c.call(2, VOID);
//            c.call(2, VOID, Compiler.C_JVM);
            c.ret();
        c.leave();

        c.compile();
        int entry = Compilation.newLinker(c).link();
        CSystem.vcall(new Parm(entry));

    }

}
