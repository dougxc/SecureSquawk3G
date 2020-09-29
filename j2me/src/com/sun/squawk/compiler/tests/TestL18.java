package com.sun.squawk.compiler.tests;

import com.sun.squawk.compiler.*;
import com.sun.squawk.compiler.Compiler;
import com.sun.squawk.os.*;

public class TestL18 implements Types {

    public static void main(String[] args) {

         Compiler c = Compilation.newCompiler();
         Label shl = c.label();
         Label shl2 = c.label();
         Label shl3 = c.label();
         Label shl4 = c.label();

         c.enter();
         Local resultCount = c.local(INT);
         c.result(INT);
             c.begin();
                c.literal(10).store(resultCount);  // number of tests that may fail
                String longfmt = longFormat();

                 c.literal(5L).literal(2L).swapForABI();
                 c.literal(shl).call(3, LONG);
                 printResult(c, "5<<2 = \u0000", longfmt + ".  Expecting: 20.\n\u0000", 20L);
                 c.load(resultCount).swap().sub().store(resultCount);

                 c.literal(2L).literal(5).swapForABI();
                 c.literal(shl).call(3, LONG);
                 printResult(c, "2<<5 = \u0000", longfmt + ".  Expecting: 64.\n\u0000", 64L);
                 c.load(resultCount).swap().sub().store(resultCount);

                 c.literal(8000000L).literal(2).swapForABI();
                 c.literal(shl).call(3, LONG);
                 printResult(c, "8000000<<2 = \u0000", longfmt + ".  Expecting: 32000000.\n\u0000", 32000000L);
                 c.load(resultCount).swap().sub().store(resultCount);

                 c.literal(5L).literal(987654321L&31).swapForABI();
                 c.literal(shl).call(3, LONG);
                 printResult(c, "5<<(987654321&31) = \u0000", longfmt + ".  Expecting: 655360.\n\u0000", 655360L);
                 c.load(resultCount).swap().sub().store(resultCount);

                 c.literal(0L).literal(2147483647&31).swapForABI();
                 c.literal(shl).call(3, LONG);
                 printResult(c, "0<<((2^31-1)&31) = \u0000", longfmt + ".  Expecting: 0.\n\u0000", 0L);
                 c.load(resultCount).swap().sub().store(resultCount);

                 c.literal(-16L).literal(3).swapForABI();
                 c.literal(shl).call(3, LONG);
                 printResult(c, "-16<<3 = \u0000", longfmt + ".  Expecting: -128.\n\u0000", -128L);
                 c.load(resultCount).swap().sub().store(resultCount);

                 c.literal(16L).literal((-3)&31).swapForABI();
                 c.literal(shl).call(3, LONG);
                 printResult(c, "16<<(-3&31) = \u0000", longfmt + ".  Expecting: 8589934592.\n\u0000", 8589934592L);
                 c.load(resultCount).swap().sub().store(resultCount);

                 c.literal(10L).literal(shl2).call(2, LONG);
                 printResult(c, "10<<3 = \u0000", longfmt + ".  Expecting: 80.\n\u0000", 80L);
                 c.load(resultCount).swap().sub().store(resultCount);

                 c.literal(10).literal(shl3).call(2, LONG);
                 printResult(c, "3<<10 = \u0000", longfmt + ".  Expecting: 3072.\n\u0000", 3072L);
                 c.load(resultCount).swap().sub().store(resultCount);

                 c.literal(10L).literal(shl4).call(2, LONG);
                 printResult(c, "10<<1 = \u0000", longfmt + ".  Expecting: 20.\n\u0000", 20L);
                 c.load(resultCount).swap().sub().store(resultCount);

                 c.load(resultCount);
                c.ret();
            c.end();
        c.leave();

        c.enter(shl);
        Local x = c.parm(LONG);      // x
        Local y = c.parm(INT);       // y
        c.result(LONG);
            c.begin();
                c.load(x);          // x
                c.load(y);          // y
                c.shl();            // x << y
                c.ret();
            c.end();
        c.leave();

        c.enter(shl2);
        x = c.parm(LONG);
        c.result(LONG);
            c.begin();
                c.load(x).literal(3).shl();
                c.ret();
            c.end();
        c.leave();

        c.enter(shl3);
        x = c.parm(INT);
        c.result(LONG);
            c.begin();
                c.literal(3L).load(x).shl();
                c.ret();
            c.end();
        c.leave();

        c.enter(shl4);
        x = c.parm(LONG);
        c.result(LONG);
            c.begin();
                c.load(x).literal(1).shl();
                c.ret();
            c.end();
        c.leave();

        c.compile();
        Linker linker = Compilation.newLinker(c);
        int entry = linker.link();
        int res = CSystem.icall(new Parm(entry));
        System.out.println("Long shift left tests failed = " + res);
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
