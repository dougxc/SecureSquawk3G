package com.sun.squawk.compiler.tests;

import com.sun.squawk.compiler.*;
import com.sun.squawk.compiler.Compiler;
import com.sun.squawk.os.*;

public class TestL2 implements Types {

    public static void main(String[] args) {

         Compiler c = Compilation.newCompiler();
         Label sub1 = c.label();
         Label sub2 = c.label();
         Label sub3 = c.label();

         c.enter();
         Local resultCount = c.local(INT);
         c.result(INT);
             c.begin();
                 c.literal(9).store(resultCount);  // number of tests that may fail
                 String longfmt = longFormat();

                 c.literal(5L).literal(2L).swapForABI();
                 c.literal(sub1).call(3, LONG);
                 printResult(c, "5-2 = \u0000", longfmt + ".  Expecting: 3.\n\u0000", 3L);
                 c.load(resultCount).swap().sub().store(resultCount);  // decrement counter if test passed

                 c.literal(2L).literal(5L).swapForABI();
                 c.literal(sub1).call(3, LONG);
                 printResult(c, "2-5 = \u0000", longfmt + ".  Expecting: -3.\n\u0000", -3L);
                 c.load(resultCount).swap().sub().store(resultCount);

                 c.literal(8000000L).literal(2L).swapForABI();
                 c.literal(sub1).call(3, LONG);
                 printResult(c, "8000000-2 = \u0000", longfmt + ".  Expecting: 7999998.\n\u0000", 7999998L);
                 c.load(resultCount).swap().sub().store(resultCount);  // decrement counter if test passed

                 c.literal(5L).literal(987654321L).swapForABI();
                 c.literal(sub1).call(3, LONG);
                 printResult(c, "5-987654321 = \u0000", longfmt + ".  Expecting: -987654316.\n\u0000", -987654316L);
                 c.load(resultCount).swap().sub().store(resultCount);

                 c.literal(2147483647L).literal(2147483647L).swapForABI();
                 c.literal(sub1).call(3, LONG);
                 printResult(c, "(2^31-1)-(2^31-1) = \u0000", longfmt + ".  Expecting: 0.\n\u0000", 0L);
                 c.load(resultCount).swap().sub().store(resultCount);

                 c.literal(0L).literal(2147483647L).swapForABI();
                 c.literal(sub1).call(3, LONG);
                 printResult(c, "0-(2^31-1) = \u0000", longfmt + ".  Expecting: -2147483647.\n\u0000", -2147483647L);
                 c.load(resultCount).swap().sub().store(resultCount);

                 c.literal(-2147483648L).literal(-2147483648L).swapForABI();
                 c.literal(sub1).call(3, LONG);
                 printResult(c, "(-2^31)-(-2^31) = \u0000", longfmt + ".  Expecting: 0.\n\u0000", 0L);
                 c.load(resultCount).swap().sub().store(resultCount);

                 c.literal(10L).literal(sub2).call(2, LONG);
                 printResult(c, "10-3 = \u0000", longfmt + ".  Expecting: 7.\n\u0000", 7L);
                 c.load(resultCount).swap().sub().store(resultCount);

                 c.literal(10L).literal(sub3).call(2, LONG);
                 printResult(c, "3-10 = \u0000", longfmt + ".  Expecting: -7.\n\u0000", -7L);
                 c.load(resultCount).swap().sub().store(resultCount);

                 c.load(resultCount);
                 c.ret();
            c.end();
        c.leave();

        c.enter(sub1);
        Local x = c.parm(LONG);      // x
        Local y = c.parm(LONG);      // y
        c.result(LONG);
            c.begin();
                c.load(x);          // x
                c.load(y);          // y
                c.sub();            // x - y
                c.ret();
            c.end();
        c.leave();

        c.enter(sub2);
        x = c.parm(LONG);
        c.result(LONG);
            c.begin();
                c.load(x).literal(3L).sub();
                c.ret();
            c.end();
        c.leave();

        c.enter(sub3);
        x = c.parm(LONG);
        c.result(LONG);
            c.begin();
                c.literal(3L).load(x).sub();
                c.ret();
            c.end();
        c.leave();

        c.compile();
        Linker linker = Compilation.newLinker(c);
        int entry = linker.link();
        int res = CSystem.icall(new Parm(entry));
        System.out.println("Long subtract tests failed = " + res);
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
