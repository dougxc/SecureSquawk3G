package com.sun.squawk.compiler.tests;

import com.sun.squawk.compiler.*;
import com.sun.squawk.compiler.Compiler;
import com.sun.squawk.os.*;

public class TestL6 implements Types {

    public static void main(String[] args) {

        Compiler c = Compilation.newCompiler();

        Label exp1 = c.label();
        Label exp2 = c.label();
        Label exp3 = c.label();
        Label exp4 = c.label();

        c.enter();
        Local resultCount = c.local(INT);
         c.result(INT);
             c.begin();
                 c.literal(4).store(resultCount);  // number of tests that may fail
                 String longfmt = longFormat();

                 c.literal(2L).literal(5L).swapForABI();
                 c.literal(exp1).call(3, LONG);
                 printResult(c, "(5/3)+2 = \u0000", longfmt + ".  Expecting: 3.\n\u0000", 3L);
                 c.load(resultCount).swap().sub().store(resultCount);

                 c.literal(4L).literal(1L).swapForABI();
                 c.literal(exp2).call(3, LONG);
                 printResult(c, "1+(3*4)-8 = \u0000", longfmt + ".  Expecting: 5.\n\u0000", 5L);
                 c.load(resultCount).swap().sub().store(resultCount);

                 c.literal(2L).literal(5L).swapForABI();
                 c.literal(exp3).call(3, LONG);
                 printResult(c, "(5/3)+2 = \u0000", longfmt + ".  Expecting: 3.\n\u0000", 3L);
                 c.load(resultCount).swap().sub().store(resultCount);

                 c.literal(3L).literal(6L).literal(1L).swapForABI();
                 c.literal(exp4).call(4, LONG);
                 printResult(c, "((20-6)*3+1)/2 = \u0000", longfmt + ".  Expecting: 21.\n\u0000", 21L);
                 c.load(resultCount).swap().sub().store(resultCount);

                 c.load(resultCount);
                 c.ret();
            c.end();
        c.leave();

        c.enter(exp1);
        Local x = c.parm(LONG);      // x
        Local y = c.parm(LONG);      // y
        c.result(LONG);
            c.begin();
                c.load(x);          // x
                c.load(y);          // y
                c.literal(3L);
                c.div();            // y / 3
                c.add();            // (y/3) + x
                c.ret();
            c.end();
        c.leave();

        c.enter(exp2);
        x = c.parm(LONG);
        y = c.parm(LONG);
        c.result(LONG);
            c.begin();
                c.load(x).literal(3L).mul();
                c.load(y).add();
                c.literal(8L).sub();
                c.ret();
            c.end();
        c.leave();

        c.enter(exp3);
        x = c.parm(LONG);
        y = c.parm(LONG);
        c.result(LONG);
            c.begin();
                c.load(y).literal(3L).div();
                c.load(x).add();
                c.ret();
            c.end();
        c.leave();

        c.enter(exp4);
        x = c.parm(LONG);
        y = c.parm(LONG);
        Local z = c.parm(LONG);
        c.result(LONG);
            c.begin();
                c.literal(20L).load(y).sub();
                c.load(x).mul();
                c.load(z).add();
                c.literal(2L).div();
                c.ret();
            c.end();
        c.leave();

        c.compile();
        Linker linker = Compilation.newLinker(c);
        int entry = linker.link();
        int res = CSystem.icall(new Parm(entry));
        System.out.println("Long expression tests failed = " + res);
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
