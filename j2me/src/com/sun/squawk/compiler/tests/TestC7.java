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

public class TestC7 implements Types {

    public static void main(String[] args) {

        Compiler c = Compilation.newCompiler();

        Label exp1 = c.label();

        c.enter();
        Local resultCount = c.local(INT);
        c.result(INT);
             c.begin();
                 c.literal(1).store(resultCount);  // number of tests that may fail

                 c.literal(2).literal(5).swapForABI();
                 c.literal(exp1).call(3, INT);
                 printResult(c, "((2+5)*20)/10 = \u0000", " %d.  Expecting: 14.\n\u0000", 14);
                 c.load(resultCount).swap().sub().store(resultCount);

                 c.load(resultCount);
                 c.ret();
            c.end();
        c.leave();

        c.enter(exp1);
        Local x = c.parm(INT);
        Local y = c.parm(INT);
        c.result(INT);
            c.begin();
                c.literal(20);
                c.load(x).load(y).add();
                c.mul().literal(10).div();
                c.ret();
            c.end();
        c.leave();

        c.compile();
        Linker linker = Compilation.newLinker(c);
        int entry = linker.link();
        int res = CSystem.icall(new Parm(entry));
        System.out.println("Expression tests failed = " + res);
        System.exit(res);
    }

    private static void printResult(Compiler c, String msg, String expected, int expectedResult) {
        c.literal(msg.getBytes()).symbol("printf").call(2, VOID);
        c.dup();
        c.literal(expected.getBytes()).symbol("printf").call(3, VOID);
        c.literal(expectedResult).eq(); // cmp tos against expectedResult and place 1 or 0 on tos
    }

}
