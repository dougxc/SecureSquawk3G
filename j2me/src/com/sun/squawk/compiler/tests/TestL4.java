package com.sun.squawk.compiler.tests;

import com.sun.squawk.compiler.*;
import com.sun.squawk.compiler.Compiler;
import com.sun.squawk.os.*;

public class TestL4 implements Types {

    public static void main(String[] args) {

         Compiler c = Compilation.newCompiler();
         Label div = c.label();

         c.enter();
         Local resultCount = c.local(INT);
         c.result(LONG);
             c.begin();
                 c.literal(6).store(resultCount);  // number of tests that may fail
                 String longfmt = longFormat();

                 c.literal(5L).literal(2L).swapForABI();
                 c.literal(div).call(3, LONG);
                 printResult(c, "5/2 = \u0000", longfmt + ".  Expecting: 2.\n\u0000", 2L);
                 c.load(resultCount).swap().sub().store(resultCount);  // decrement counter if test passed

                 c.literal(2L).literal(5L).swapForABI();
                 c.literal(div).call(3, LONG);
                 printResult(c, "2/5 = \u0000", longfmt + ".  Expecting: 0.\n\u0000", 0L);
                 c.load(resultCount).swap().sub().store(resultCount);

                 c.literal(8000000L).literal(2L).swapForABI();
                 c.literal(div).call(3, LONG);
                 printResult(c, "8000000/2 = \u0000", longfmt + ".  Expecting: 4000000.\n\u0000", 4000000L);
                 c.load(resultCount).swap().sub().store(resultCount);  // decrement counter if test passed

                 c.literal(5L).literal(987654321L).swapForABI();
                 c.literal(div).call(3, LONG);
                 printResult(c, "5/987654321 = \u0000", longfmt + ".  Expecting: 0.\n\u0000", 0L);
                 c.load(resultCount).swap().sub().store(resultCount);

                 c.literal(0L).literal(2147483647L).swapForABI();
                 c.literal(div).call(3, LONG);
                 printResult(c, "0/(2^31-1) = \u0000", longfmt + ".  Expecting: 0.\n\u0000", 0L);
                 c.load(resultCount).swap().sub().store(resultCount);

                 c.literal(-2147483648L).literal(-2147483648L).swapForABI();
                 c.literal(div).call(3, LONG);
                 printResult(c, "(-2^31)/(-2^31) = \u0000", longfmt + ".  Expecting: 1.\n\u0000", 1L);
                 c.load(resultCount).swap().sub().store(resultCount);

                 c.load(resultCount);
                 c.ret();
            c.end();
        c.leave();

        c.enter(div);
        Local x = c.parm(LONG);      // x
        Local y = c.parm(LONG);      // y
        c.result(LONG);
            c.begin();
                c.load(x);          // x
                c.load(y);          // y
                c.div();            // x / y
                c.ret();
            c.end();
        c.leave();

        c.compile();
        Linker linker = Compilation.newLinker(c);
        int entry = linker.link();
        System.out.println("Long division not supported");
        //int res = CSystem.icall(new Parm(entry));
        //System.out.println("Long division tests failed = " + res);
        //System.exit(res);
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
