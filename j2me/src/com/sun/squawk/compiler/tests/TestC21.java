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

public class TestC21 implements Types {

    public static void main(String[] args) {

        Compiler c = Compilation.newCompiler();

        Label neg = c.label();
        Label com = c.label();

        c.enter();
        Local resultCount = c.local(INT);
         c.result(INT);
             c.begin();
                c.literal(4).store(resultCount);  // number of tests that may fail

                c.literal(8).literal(neg).call(2, INT);
                printResult(c, "-(8) = \u0000", " %d.  Expecting: -8.\n\u0000", -8);
                c.load(resultCount).swap().sub().store(resultCount);

                c.literal(-8).literal(neg).call(2, INT);
                printResult(c, "-(-8) = \u0000", " %d.  Expecting: 8.\n\u0000", 8);
                c.load(resultCount).swap().sub().store(resultCount);

                c.literal(8).literal(com).call(2, INT);
                printResult(c, "~(8) = \u0000", " %d.  Expecting: -9.\n\u0000", -9);
                c.load(resultCount).swap().sub().store(resultCount);

                c.literal(-8).literal(com).call(2, INT);
                printResult(c, "~(-8) = \u0000", " %d.  Expecting: 7.\n\u0000", 7);
                c.load(resultCount).swap().sub().store(resultCount);

                c.load(resultCount);
                c.ret();
        c.end();
        c.leave();

        c.enter(neg);
        Local x = c.parm(INT);      // x
        c.result(INT);
            c.begin();
                c.load(x).neg().ret();
            c.end();
        c.leave();

        c.enter(com);
        x = c.parm(INT);      // x
        c.result(INT);
            c.begin();
                c.load(x).com().ret();
            c.end();
        c.leave();

        c.compile();
        int entry = Compilation.newLinker(c).link();
        int res = CSystem.icall(new Parm(entry));
        System.out.println("Negate and not tests failed = " + res);
        System.exit(res);
    }

    private static void printResult(Compiler c, String msg, String expected, int expectedResult) {
        c.literal(msg.getBytes()).symbol("printf").call(2,VOID);
        c.dup();
        c.literal(expected.getBytes()).symbol("printf").call(3,VOID);
        c.literal(expectedResult).eq(); // cmp tos against expectedResult and place 1 or 0 on tos
    }

}
