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

public class Test05 implements Types {

    public static void main(String[] args) {

        Compiler c = Compilation.newCompiler();

        Label l1 = c.label();

        c.enter();
        Local x = c.parm(INT);      // x
        Local y = c.parm(INT);      // y
        c.result(INT);
            c.begin();
                c.load(x);          // x
                c.load(y);          // y
                c.lt();
                c.bt(l1);
                c.literal(0);       // false
                c.ret();
            c.bind(l1);
                c.literal(1);       // true
                c.ret();
            c.end();
        c.leave();

        c.compile();
        int entry = Compilation.newLinker(c).link();

        int res = CSystem.icall(new Parm(entry).parm(1).parm(2));
        System.out.println("1 < 2 -> "+res);

        int result = 0;
        if (res != 1)
            result++;

        int res2 = CSystem.icall(new Parm(entry).parm(9999).parm(9999));
        System.out.println("9999 < 9999 -> "+res2);

        if (res2 != 0)
            result++;
        System.exit(result);
    }

}
