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

public class Test06 implements Types {

    public static void main(String[] args) {

        Compiler c = Compilation.newCompiler();

        Label l1 = c.label();
        Label l2 = c.label();

        c.enter();
        Local x = c.parm(INT);
        Local y = c.parm(INT);
        c.result(INT);
            c.begin();
                c.literal(1);
                c.load(x);          // x
                c.load(y);          // y
                c.eq();
                c.bt(l1);           // Literal 1 on stack here
                c.literal(2);
                c.br(l2);           // Literal 1 and 2 on stack here
            c.bind(l1);             // Phi with literal 1 on stack
                c.literal(3);
            c.bind(l2);             // Phi with literal 1 and the other value on stack
                c.add();
                c.ret();
            c.end();
        c.leave();

        c.compile();
        int entry = Compilation.newLinker(c).link();

        int res = CSystem.icall(new Parm(entry).parm(123).parm(456));
        System.out.println("123, 456 -> "+res);

        int result = 0;
        if (res != 3)
            result++;

        int res2 = CSystem.icall(new Parm(entry).parm(999).parm(999));
        System.out.println("999, 999 -> "+res2);

        if (res2 != 4)
            result++;
        System.exit(result);
    }

}
