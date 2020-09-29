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

public class TestL21 implements Types {

    public static void main(String[] args) {

        Compiler c = Compilation.newCompiler();

        Label neg = c.label();
        Label com = c.label();

        c.enter();
        Local resultCount = c.local(INT);
         c.result(INT);
             c.begin();
                c.literal(8).store(resultCount);  // number of tests that may fail
                String longfmt = longFormat();

                c.literal(8L).literal(neg).call(2, LONG);
                printResult(c, "-(8) = \u0000", longfmt + ".  Expecting: -8.\n\u0000", -8L);
                c.load(resultCount).swap().sub().store(resultCount);

                c.literal(-8L).literal(neg).call(2, LONG);
                printResult(c, "-(-8) = \u0000", longfmt + ".  Expecting: 8.\n\u0000", 8L);
                c.load(resultCount).swap().sub().store(resultCount);

                c.literal(8000000L).literal(neg).call(2, LONG);
                printResult(c, "-(8) = \u0000", longfmt + ".  Expecting: -8000000.\n\u0000", -8000000L);
                c.load(resultCount).swap().sub().store(resultCount);

                c.literal(-987654321L).literal(neg).call(2, LONG);
                printResult(c, "-(-987654321) = \u0000", longfmt + ".  Expecting: 987654321.\n\u0000", 987654321L);
                c.load(resultCount).swap().sub().store(resultCount);

                c.literal(8L).literal(com).call(2, LONG);
                printResult(c, "~(8) = \u0000", longfmt + ".  Expecting: -9.\n\u0000", -9L);
                c.load(resultCount).swap().sub().store(resultCount);

                c.literal(-8L).literal(com).call(2, LONG);
                printResult(c, "~(-8) = \u0000", longfmt + ".  Expecting: 7.\n\u0000", 7L);
                c.load(resultCount).swap().sub().store(resultCount);

                c.literal(8000000L).literal(com).call(2, LONG);
                printResult(c, "~(8000000) = \u0000", longfmt + ".  Expecting: -8000001.\n\u0000", -8000001L);
                c.load(resultCount).swap().sub().store(resultCount);

                c.literal(-987654321L).literal(com).call(2, LONG);
                printResult(c, "~(-987654321) = \u0000", longfmt + ".  Expecting: 987654320.\n\u0000", 987654320L);
                c.load(resultCount).swap().sub().store(resultCount);

                c.load(resultCount);
                c.ret();
        c.end();
        c.leave();

        c.enter(neg);
        Local x = c.parm(LONG);      // x
        c.result(LONG);
            c.begin();
                c.load(x).neg().ret();
            c.end();
        c.leave();

        c.enter(com);
        x = c.parm(LONG);      // x
        c.result(LONG);
            c.begin();
                c.load(x).com().ret();
            c.end();
        c.leave();

        c.compile();
        int entry = Compilation.newLinker(c).link();
        int res = CSystem.icall(new Parm(entry));
        System.out.println("Long negate and not tests failed = " + res);
        System.exit(res);
    }

    private static void printResult(Compiler c, String msg, String expected, long expectedResult) {
        c.literal(msg.getBytes()).symbol("printf").call(2,VOID);
        c.dup();
        c.literal(expected.getBytes()).symbol("printf").call(3,VOID);
        c.literal(expectedResult).eq(); // cmp tos against expectedResult and place 1 or 0 on tos
    }

    /**
     * Determines the format for displaying a long long in C using printf().
     *
     * @return the format used to display long long based on the platform used.
     */
    private static String longFormat() {
        if (System.getProperty("os.name").toLowerCase().startsWith("windows"))
            return "%I64d";
        else
            return "%lld";
    }

}
