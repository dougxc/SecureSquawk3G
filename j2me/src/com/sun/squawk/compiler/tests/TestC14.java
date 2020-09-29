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

public class TestC14 implements Types {

    public static void main(String[] args) {

        Compiler c = Compilation.newCompiler();

        Label eq = c.label();
        Label eq2 = c.label();
        Label eq3 = c.label();
        Label l1 = c.label();
        Label l2 = c.label();
        Label l3 = c.label();

        c.enter();
        Local resultCount = c.local(INT);
         c.result(INT);
             c.begin();
                c.literal(7).store(resultCount);  // number of tests that may fail

                c.literal(1).literal(2).swapForABI();
                c.literal(eq).call(3, INT);
                printResult(c, "compare (1 == 2) = \u0000", " %d.  Expecting: 0.\n\u0000", 0);
                c.load(resultCount).swap().sub().store(resultCount);

                c.literal(2).literal(1).swapForABI();
                c.literal(eq).call(3, INT);
                printResult(c, "compare (2 == 1) = \u0000", " %d.  Expecting: 0.\n\u0000", 0);
                c.load(resultCount).swap().sub().store(resultCount);

                c.literal(9999).literal(9999).swapForABI();
                c.literal(eq).call(3, INT);
                printResult(c, "compare (9999 == 9999) = \u0000", " %d.  Expecting: 1.\n\u0000", 1);
                c.load(resultCount).swap().sub().store(resultCount);

                c.literal(2).literal(eq2).call(2, INT);
                printResult(c, "compare (5 == 2) = \u0000", " %d.  Expecting: 0.\n\u0000", 0);
                c.load(resultCount).swap().sub().store(resultCount);

                c.literal(10).literal(eq2).call(2, INT);
                printResult(c, "compare (5 == 10) = \u0000", " %d.  Expecting: 0.\n\u0000", 0);
                c.load(resultCount).swap().sub().store(resultCount);

                c.literal(2).literal(eq3).call(2, INT);
                printResult(c, "compare (2 == 5) = \u0000", " %d.  Expecting: 0.\n\u0000", 0);
                c.load(resultCount).swap().sub().store(resultCount);

                c.literal(10).literal(eq3).call(2, INT);
                printResult(c, "compare (10 == 5) = \u0000", " %d.  Expecting: 0.\n\u0000", 0);
                c.load(resultCount).swap().sub().store(resultCount);

                c.load(resultCount);
                c.ret();
            c.end();
        c.leave();

        c.enter(eq);
        Local x = c.parm(INT);      // x
        Local y = c.parm(INT);      // y
        c.result(INT);
            c.begin();
                c.load(x);          // x
                c.load(y);          // y
                c.eq();             // x == y ?
                c.bt(l1);
                c.literal(0);       // false
                c.ret();
            c.bind(l1);
                c.literal(1);       // true
                c.ret();
            c.end();
        c.leave();

        c.enter(eq2);
        y = c.parm(INT);            // y
        c.result(INT);
            c.begin();
                c.literal(5);       // x
                c.load(y);          // y
                c.eq();             // x == y ?
                c.bt(l2);
                c.literal(0);       // false
                c.ret();
            c.bind(l2);
                c.literal(1);       // true
                c.ret();
            c.end();
        c.leave();

        c.enter(eq3);
        x = c.parm(INT);            // x
        c.result(INT);
            c.begin();
                c.load(x);          // x
                c.literal(5);       // y
                c.eq();             // x == y ?
                c.bt(l3);
                c.literal(0);       // false
                c.ret();
            c.bind(l3);
                c.literal(1);       // true
                c.ret();
            c.end();
        c.leave();

        c.compile();
        int entry = Compilation.newLinker(c).link();
        int res = CSystem.icall(new Parm(entry));
        System.out.println("Equal to tests failed = " + res);
        System.exit(res);
    }

    private static void printResult(Compiler c, String msg, String expected, int expectedResult) {
        c.literal(msg.getBytes()).symbol("printf").call(2,VOID);
        c.dup();
        c.literal(expected.getBytes()).symbol("printf").call(3,VOID);
        c.literal(expectedResult).eq(); // cmp tos against expectedResult and place 1 or 0 on tos
    }

}
