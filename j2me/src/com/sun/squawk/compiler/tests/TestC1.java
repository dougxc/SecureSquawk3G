package com.sun.squawk.compiler.tests;

import com.sun.squawk.compiler.*;
import com.sun.squawk.compiler.Compiler;
import com.sun.squawk.os.*;

public class TestC1 implements Types {

    public static void main(String[] args) {

        Compiler c = Compilation.newCompiler();

        Label add1 = c.label();

         c.enter();
         Local resultCount = c.local(INT);
         c.result(INT);
             c.begin();
                 c.literal(6).store(resultCount);  // number of tests that may fail

                 c.literal(5).literal(2).swapForABI();
                 c.literal(add1).call(3, INT);
                 printResult(c, "5+2 = \u0000", " %d.  Expecting: 7.\n\u0000", 7);
                 c.load(resultCount).swap().sub().store(resultCount);  // decrement counter if test passed

                 c.literal(2).literal(5).swapForABI();
                 c.literal(add1).call(3, INT);
                 printResult(c, "2+5 = \u0000", " %d.  Expecting: 7.\n\u0000", 7);
                 c.load(resultCount).swap().sub().store(resultCount);

                 c.literal(0).literal(2147483647).swapForABI();
                 c.literal(add1).call(3, INT);
                 printResult(c, "(2^31-1)+0 = \u0000", " %d.  Expecting: 2147483647.\n\u0000", 2147483647);
                 c.load(resultCount).swap().sub().store(resultCount);

                 c.literal(1).literal(2147483647).swapForABI();
                 c.literal(add1).call(3, INT);
                 printResult(c, "(2^31-1)+1 = \u0000", " %d.  Expecting: -2147483648.\n\u0000", -2147483648);
                 c.load(resultCount).swap().sub().store(resultCount);

                 c.literal(2147483647).literal(2147483647).swapForABI();
                 c.literal(add1).call(3, INT);
                 printResult(c, "(2^31-1)+(2^31-1) = \u0000", " %d.  Expecting: -2.\n\u0000", -2);
                 c.load(resultCount).swap().sub().store(resultCount);

                 c.literal(2147483647).literal(2147483647).swapForABI();
                 c.literal(add1).call(3, INT);
                 c.literal(2).literal(add1).call(3, INT);
                 printResult(c, "(2^31-1)+(2^31-1)+2 = \u0000", " %d.  Expecting: 0.\n\u0000", 0);
                 c.load(resultCount).swap().sub().store(resultCount);

                 c.load(resultCount);
                 c.ret();
            c.end();
        c.leave();

        c.enter(add1);
        Local x = c.parm(INT);      // x
        Local y = c.parm(INT);      // y
        c.result(INT);
            c.begin();
                c.load(x);          // x
                c.load(y);          // y
                c.add();            // x + y
                c.ret();
            c.end();
        c.leave();

        c.compile();
        Linker linker = Compilation.newLinker(c);
        int entry = linker.link();
        int res = CSystem.icall(new Parm(entry));
        System.out.println("Addition tests failed = " + res);
        System.exit(res);
    }

    private static void printResult(Compiler c, String msg, String expected, int expectedResult) {
        c.literal(msg.getBytes()).symbol("printf").call(2,VOID);
        c.dup();
        c.literal(expected.getBytes()).symbol("printf").call(3,VOID);
        c.literal(expectedResult).eq(); // cmp tos against expectedResult and place 1 or 0 on tos
    }

}
