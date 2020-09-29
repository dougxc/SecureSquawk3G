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

public class Test23 implements Types {

    public static void main(String[] args) {

        Compiler c = Compilation.newCompiler();

        c.enter();
        c.result(VOID);

            Local printf = c.local(REF);
            c.symbol("printf");
            c.store(printf);

            /*
             * Print "Hel" using the printf symbol stored in a local.
             */
            c.literal("Hel\u0000".getBytes());
            c.load(printf);
            c.call(VOID);

            /*
             * Print "lo " using the symbolic address of printf.
             */
            c.literal("lo \u0000".getBytes());
            c.symbol("printf");
            c.call(VOID);

            /*
             * Print "World" using the symbolic address of printf, but using a call via EAX.
             */
            c.literal("World\n\u0000".getBytes());
            c.symbol("printf");
            c.call(VOID, Compiler.C_JVM);
            c.ret();
        c.leave();

        c.compile();
        int entry = Compilation.newLinker(c).link();
        CSystem.vcall(new Parm(entry));

    }

}
