package com.sun.squawk.compiler.tests;

import com.sun.squawk.compiler.*;
import com.sun.squawk.compiler.Compiler;
import com.sun.squawk.os.*;

public class TestL10 implements Types {

    public static void main(String[] args) {

         Compiler c = Compilation.newCompiler();
         Label or = c.label();

         c.enter();
         Local resultCount = c.local(INT);
         c.result(INT);
             c.begin();
                 c.literal(6).store(resultCount);  // number of tests that may fail
                 String longfmt = longFormat();

                 c.literal(7L).literal(2L).swapForABI();
                 c.literal(or).call(3, LONG);
                 printResult(c, "7 or 2 = \u0000", longfmt + ".  Expecting: 7.\n\u0000", 7L);
                 c.load(resultCount).swap().sub().store(resultCount);

                 c.literal(2L).literal(7L).swapForABI();
                 c.literal(or).call(3, LONG);
                 printResult(c, "2 or 7 = \u0000", longfmt + ".  Expecting: 7.\n\u0000", 7L);
                 c.load(resultCount).swap().sub().store(resultCount);

                 c.literal(8000000L).literal(2L).swapForABI();
                 c.literal(or).call(3, LONG);
                 printResult(c, "8000000 or 2 = \u0000", longfmt + ".  Expecting: 8000002.\n\u0000", 8000002L);
                 c.load(resultCount).swap().sub().store(resultCount);

                 c.literal(5L).literal(987654321L).swapForABI();
                 c.literal(or).call(3, LONG);
                 printResult(c, "5 or 987654321 = \u0000", longfmt + ".  Expecting: 987654325.\n\u0000", 987654325L);
                 c.load(resultCount).swap().sub().store(resultCount);

                 c.literal(0L).literal(2147483647L).swapForABI();
                 c.literal(or).call(3, LONG);
                 printResult(c, "0 or (2^31-1) = \u0000", longfmt + ".  Expecting: 2147483647.\n\u0000", 2147483647L);
                 c.load(resultCount).swap().sub().store(resultCount);

                 c.literal(-2147483648L).literal(-2147483648L).swapForABI();
                 c.literal(or).call(3, LONG);
                 printResult(c, "(-2^31)or(-2^31) = \u0000", longfmt + ".  Expecting: -2147483648.\n\u0000", -2147483648L);
                 c.load(resultCount).swap().sub().store(resultCount);

                 c.load(resultCount);
                 c.ret();
            c.end();
        c.leave();

        c.enter(or);
        Local x2 = c.parm(LONG);      // x
        Local y2 = c.parm(LONG);      // y
        c.result(LONG);
            c.begin();
                c.load(x2);          // x
                c.load(y2);          // y
                c.or();              // x or y
                c.ret();
            c.end();
        c.leave();

        c.compile();
        Linker linker = Compilation.newLinker(c);
        int entry = linker.link();
        int res = CSystem.icall(new Parm(entry));
        System.out.println("Long or tests failed = " + res);
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
