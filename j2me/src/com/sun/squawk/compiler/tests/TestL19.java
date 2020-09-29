package com.sun.squawk.compiler.tests;

import com.sun.squawk.compiler.*;
import com.sun.squawk.compiler.Compiler;
import com.sun.squawk.os.*;

public class TestL19 implements Types {

    public static void main(String[] args) {

         Compiler c = Compilation.newCompiler();
         Label ushr = c.label();
         Label ushr2 = c.label();
         Label ushr3 = c.label();
         Label ushr4 = c.label();

         c.enter();
         Local resultCount = c.local(INT);
         c.result(INT);
             c.begin();
                c.literal(10).store(resultCount);  // number of tests that may fail
                String longfmt = longFormat();

                 c.literal(5L).literal(2).swapForABI();
                 c.literal(ushr).call(3, LONG);
                 printResult(c, "5>>>2 = \u0000", longfmt + ".  Expecting: 1.\n\u0000", 1L);
                 c.load(resultCount).swap().sub().store(resultCount);

                 c.literal(2L).literal(5).swapForABI();
                 c.literal(ushr).call(3, LONG);
                 printResult(c, "2>>>5 = \u0000", longfmt + ".  Expecting: 0.\n\u0000", 0L);
                 c.load(resultCount).swap().sub().store(resultCount);

                 c.literal(8000000L).literal(2).swapForABI();
                 c.literal(ushr).call(3, LONG);
                 printResult(c, "8000000>>>2 = \u0000", longfmt + ".  Expecting: 2000000.\n\u0000", 2000000L);
                 c.load(resultCount).swap().sub().store(resultCount);

                 c.literal(5L).literal(987654321&31).swapForABI();
                 c.literal(ushr).call(3, LONG);
                 printResult(c, "5>>>(987654321&31) = \u0000", longfmt + ".  Expecting: 0.\n\u0000", 0L);
                 c.load(resultCount).swap().sub().store(resultCount);

                 c.literal(0L).literal(2147483647&31).swapForABI();
                 c.literal(ushr).call(3, LONG);
                 printResult(c, "0>>>((2^31-1)&31) = \u0000", longfmt + ".  Expecting: 0.\n\u0000", 0L);
                 c.load(resultCount).swap().sub().store(resultCount);

                 c.literal(-16L).literal(3).swapForABI();
                 c.literal(ushr).call(3, LONG);
                 printResult(c, "-16>>>3 = \u0000", longfmt + ".  Expecting: 2305843009213693950.\n\u0000", 2305843009213693950L);
                 c.load(resultCount).swap().sub().store(resultCount);

                 c.literal(16L).literal((-3&31)).swapForABI();
                 c.literal(ushr).call(3, LONG);
                 printResult(c, "16>>>(-3&31) = \u0000", longfmt + ".  Expecting: 0.\n\u0000", 0L);
                 c.load(resultCount).swap().sub().store(resultCount);

                 c.literal(10L).literal(ushr2).call(2, LONG);
                 printResult(c, "10>>>3 = \u0000", longfmt + ".  Expecting: 1.\n\u0000", 1L);
                 c.load(resultCount).swap().sub().store(resultCount);

                 c.literal(10).literal(ushr3).call(2, LONG);
                 printResult(c, "3>>>10 = \u0000", longfmt + ".  Expecting: 0.\n\u0000", 0L);
                 c.load(resultCount).swap().sub().store(resultCount);

                 c.literal(10L).literal(ushr4).call(2, LONG);
                 printResult(c, "10>>>1 = \u0000", longfmt + ".  Expecting: 5.\n\u0000", 5L);
                 c.load(resultCount).swap().sub().store(resultCount);

                 c.load(resultCount);
                c.ret();
            c.end();
        c.leave();

        c.enter(ushr);
        Local x = c.parm(LONG);      // x
        Local y = c.parm(INT);      // y
        c.result(LONG);
            c.begin();
                c.load(x);          // x
                c.load(y);          // y
                c.ushr();           // x >>u y
                c.ret();
            c.end();
        c.leave();

        c.enter(ushr2);
        x = c.parm(LONG);
        c.result(LONG);
            c.begin();
                c.load(x).literal(3).ushr();
                c.ret();
            c.end();
        c.leave();

        c.enter(ushr3);
        x = c.parm(INT);
        c.result(LONG);
            c.begin();
                c.literal(3L).load(x).ushr();
                c.ret();
            c.end();
        c.leave();

        c.enter(ushr4);
        x = c.parm(LONG);
        c.result(LONG);
            c.begin();
                c.load(x).literal(1).ushr();
                c.ret();
            c.end();
        c.leave();

        c.compile();
        Linker linker = Compilation.newLinker(c);
        int entry = linker.link();
        int res = CSystem.icall(new Parm(entry));
        System.out.println("Long unsigned shift right tests failed = " + res);
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
