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

public class Test03 implements Types {

    public static void main(String[] args) {

        Compiler c = Compilation.newCompiler();

        c.enter();
        Local x = c.local(INT);
        Local y = c.local(INT);
        c.result(INT);
            c.begin();
                c.literal(5).store(x);   // x
                c.literal(2).store(y);   // y
                c.load(x).load(y).add(); // x + y
                c.ret();                 // return
            c.end();
        c.leave();

        c.compile();
        Linker linker = Compilation.newLinker(c);
        int entry = linker.link();
        int res = CSystem.icall(new Parm(entry));

        System.out.println("5 + 2 = " + res);

        if (res == 7)   // correct result
            System.exit(0);
        else
            System.exit(1);

    }

}
