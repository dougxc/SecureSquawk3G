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

public class TestL22 implements Types {

    public static void main(String[] args) {

        Compiler c = Compilation.newCompiler();

        Label bt = c.label();
        Label bf = c.label();
        Label br = c.label();
        Label l1 = c.label();
        Label l2 = c.label();
        Label l3 = c.label();
        Label l4 = c.label();
        Label jmplabel = c.label();

        c.enter();
        Local resultCount = c.local(INT);
         c.result(INT);
             c.begin();
                c.literal(4).store(resultCount);  // number of tests that may fail

                c.literal(8L).literal(3L).literal(bt).call(3, INT);
                printResult(c, "Using bt(label).  3 < 8 = \u0000", " %d.  Expecting: 1.\n\u0000", 1);
                c.load(resultCount).swap().sub().store(resultCount);

                c.literal(8L).literal(3L).literal(bf).call(3, INT);
                printResult(c, "Using bf(label).  3 < 8 = \u0000", " %d.  Expecting: 1.\n\u0000", 1);
                c.load(resultCount).swap().sub().store(resultCount);

                c.literal(br).call(1, INT);
                printResult(c, "Using br(label).  Result = \u0000", " %d.  Expecting: 123.\n\u0000", 123);
                c.load(resultCount).swap().sub().store(resultCount);

                // how do we test branches to int?

                // how about all cases of jump somewhere?
                c.literal(jmplabel).call(1, INT);
                printResult(c, "Using jump(label).  Result = \u0000", " %d.  Expecting: 234.\n\u0000", 234);
                c.load(resultCount).swap().sub().store(resultCount);

                c.load(resultCount);
                c.ret();
        c.end();
        c.leave();

        c.enter(bt);
        Local x = c.parm(LONG);      // x
        Local y = c.parm(LONG);      // y
        c.result(INT);
            c.begin();
                c.load(x).load(y).lt();
                c.bt(l1);
                c.literal(0).ret();       // false
            c.bind(l1);
                c.literal(1).ret();       // true
            c.end();
        c.leave();

        c.enter(bf);
        x = c.parm(LONG);      // x
        y = c.parm(LONG);      // y
        c.result(INT);
            c.begin();
                c.load(x).load(y).ge();
                c.bf(l2);
                c.literal(0).ret();       // false
            c.bind(l2);
                c.literal(1).ret();       // true
            c.end();
        c.leave();

        c.enter(br);
        c.result(INT);
            c.begin();
                c.br(l3);
                c.literal(012).ret();
            c.bind(l3);
                c.literal(123).ret();
            c.end();
        c.leave();

        c.enter(jmplabel);
        c.result(INT);
            c.begin();
                c.literal(l4);
                c.jump();
                c.literal(012).ret();
            c.bind(l4);
                c.literal(234).ret();
            c.end();
        c.leave();

        c.compile();
        int entry = Compilation.newLinker(c).link();
        int res = CSystem.icall(new Parm(entry));
        System.out.println("Long branch tests failed = " + res);
        System.exit(res);
    }

    private static void printResult(Compiler c, String msg, String expected, int expectedResult) {
        c.literal(msg.getBytes()).symbol("printf").call(2,VOID);
        c.dup();
        c.literal(expected.getBytes()).symbol("printf").call(3,VOID);
        c.literal(expectedResult).eq(); // cmp tos against expectedResult and place 1 or 0 on tos
    }

}
