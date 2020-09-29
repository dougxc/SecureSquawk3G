package com.sun.squawk.compiler.tests;

import com.sun.squawk.compiler.*;
import com.sun.squawk.compiler.Compiler;
import com.sun.squawk.os.*;

public class TestL3 implements Types {

    public static void main(String[] args) {

         Compiler c = Compilation.newCompiler();
         Label mul = c.label();
         Label mul2 = c.label();

         c.enter();
         Local resultCount = c.local(INT);
         c.result(INT);
             c.begin();
                 c.literal(9).store(resultCount);  // number of tests that may fail
                 String longfmt = longFormat();

                 c.literal(5L).literal(2L).swapForABI();
                 c.literal(mul).call(3, LONG);
                 printResult(c, "5*2 = \u0000", longfmt + ".  Expecting: 10.\n\u0000", 10L);
                 c.load(resultCount).swap().sub().store(resultCount);  // decrement counter if test passed

                 c.literal(0L).literal(5L).swapForABI();
                 c.literal(mul).call(3, LONG);
                 printResult(c, "0*5 = \u0000", longfmt + ".  Expecting: 0.\n\u0000", 0L);
                 c.load(resultCount).swap().sub().store(resultCount);

                 c.literal(8000000L).literal(2L).swapForABI();
                 c.literal(mul).call(3, LONG);
                 printResult(c, "8000000*2 = \u0000", longfmt + ".  Expecting: 16000000.\n\u0000", 16000000L);
                 c.load(resultCount).swap().sub().store(resultCount);  // decrement counter if test passed

                 c.literal(5L).literal(987654321L).swapForABI();
                 c.literal(mul).call(3, LONG);
                 printResult(c, "5*987654321 = \u0000", longfmt + ".  Expecting: 4938271605.\n\u0000", 4938271605L);
                 c.load(resultCount).swap().sub().store(resultCount);

                 c.literal(1L).literal(2147483647L).swapForABI();
                 c.literal(mul).call(3, LONG);
                 printResult(c, "(2^31-1)*1 = \u0000", longfmt + ".  Expecting: 2147483647.\n\u0000", 2147483647L);
                 c.load(resultCount).swap().sub().store(resultCount);

                 c.literal(2147483647L).literal(2L).swapForABI();
                 c.literal(mul).call(3, LONG);
                 printResult(c, "2*(2^31-1) = \u0000", longfmt + ".  Expecting: 4294967294.\n\u0000", 4294967294L);
                 c.load(resultCount).swap().sub().store(resultCount);

                 c.literal(-2147483648L).literal(-2147483648L).swapForABI();
                 c.literal(mul).call(3, LONG);
                 printResult(c, "(-2^31)*(-2^31) = \u0000", longfmt + ".  Expecting: 4611686018427387904.\n\u0000", 4611686018427387904L);
                 c.load(resultCount).swap().sub().store(resultCount);

                 c.literal(5L).literal(2L).swapForABI();
                 c.literal(mul2).call(3, LONG);
                 printResult(c, "(5+5)*(2+2) = \u0000", longfmt + ".  Expecting: 40.\n\u0000", 40L);
                 c.load(resultCount).swap().sub().store(resultCount);

                 c.literal(2147483647L).literal(2L).swapForABI();
                 c.literal(mul2).call(3, LONG);
                 printResult(c, "((2^31-1)+5)*(2+2)) = \u0000", longfmt + ".  Expecting: 8589934608.\n\u0000", 8589934608L);
                 c.load(resultCount).swap().sub().store(resultCount);

                 c.load(resultCount);
                 c.ret();
            c.end();
        c.leave();

        c.enter(mul);
        Local x = c.parm(LONG);      // x
        Local y = c.parm(LONG);      // y
        c.result(LONG);
            c.begin();
                c.load(x);          // x
                c.load(y);          // y
                c.mul();            // x * y
                c.ret();
            c.end();
        c.leave();

        c.enter(mul2);
        x = c.parm(LONG);
        y = c.parm(LONG);
        c.result(LONG);
            c.begin();
                c.load(x).literal(5L).add();
                c.load(y).literal(2L).add();
                c.mul().ret();
            c.end();
        c.leave();

        c.compile();
        Linker linker = Compilation.newLinker(c);
        int entry = linker.link();
        int res = CSystem.icall(new Parm(entry));
        System.out.println("Long multiplication tests failed = " + res);
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
