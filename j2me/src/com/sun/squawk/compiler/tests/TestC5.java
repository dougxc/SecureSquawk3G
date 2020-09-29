package com.sun.squawk.compiler.tests;

import com.sun.squawk.compiler.*;
import com.sun.squawk.compiler.Compiler;
import com.sun.squawk.os.*;

public class TestC5 implements Types {

    public static void main(String[] args) {

         Compiler c = Compilation.newCompiler();
         Label rem = c.label();

         c.enter();
         Local resultCount = c.local(INT);
         c.result(INT);
             c.begin();
                 c.literal(4).store(resultCount);  // number of tests that may fail

                 c.literal(5).literal(2).swapForABI();
                 c.literal(rem).call(3, INT);
                 printResult(c, "5 rem 2 = \u0000", " %d.  Expecting: 1.\n\u0000", 1);
                 c.load(resultCount).swap().sub().store(resultCount);

                 c.literal(2).literal(5).swapForABI();
                 c.literal(rem).call(3, INT);
                 printResult(c, "2 rem 5 = \u0000", " %d.  Expecting: 2.\n\u0000", 2);
                 c.load(resultCount).swap().sub().store(resultCount);

                 c.literal(0).literal(2147483647).swapForABI();
                 c.literal(rem).call(3, INT);
                 printResult(c, "0 rem (2^31-1) = \u0000", " %d.  Expecting: 0.\n\u0000", 0);
                 c.load(resultCount).swap().sub().store(resultCount);

                 c.literal(-2147483648).literal(-2147483648).swapForABI();
                 c.literal(rem).call(3, INT);
                 printResult(c, "(-2^31)rem(-2^31) = \u0000", " %d.  Expecting: 0.\n\u0000", 0);
                 c.load(resultCount).swap().sub().store(resultCount);

                 c.load(resultCount);
                 c.ret();
            c.end();
        c.leave();

        c.enter(rem);
        Local x2 = c.parm(INT);      // x
        Local y2 = c.parm(INT);      // y
        c.result(INT);
            c.begin();
                c.load(x2);          // x
                c.load(y2);          // y
                c.rem();             // x % y
                c.ret();
            c.end();
        c.leave();

        c.compile();
        Linker linker = Compilation.newLinker(c);
        int entry = linker.link();
        int res = CSystem.icall(new Parm(entry));
        System.out.println("Remainder tests failed = " + res);
        System.exit(res);
    }

    private static void printResult(Compiler c, String msg, String expected, int expectedResult) {
        c.literal(msg.getBytes()).symbol("printf").call(2,VOID);
        c.dup();
        c.literal(expected.getBytes()).symbol("printf").call(3,VOID);
        c.literal(expectedResult).eq(); // cmp tos against expectedResult and place 1 or 0 on tos
    }

}
