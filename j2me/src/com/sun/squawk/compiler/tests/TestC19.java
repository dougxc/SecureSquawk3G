package com.sun.squawk.compiler.tests;

import com.sun.squawk.compiler.*;
import com.sun.squawk.compiler.Compiler;
import com.sun.squawk.os.*;

public class TestC19 implements Types {

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
                c.literal(8).store(resultCount);  // number of tests that may fail

                 c.literal(5).literal(2).swapForABI();
                 c.literal(ushr).call(3, INT);
                 printResult(c, "5>>>2 = \u0000", " %d.  Expecting: 1.\n\u0000", 1);
                 c.load(resultCount).swap().sub().store(resultCount);

                 c.literal(2).literal(5).swapForABI();
                 c.literal(ushr).call(3, INT);
                 printResult(c, "2>>>5 = \u0000", " %d.  Expecting: 0.\n\u0000", 0);
                 c.load(resultCount).swap().sub().store(resultCount);

                 c.literal(0).literal(2147483647).swapForABI();
                 c.literal(ushr).call(3, INT);
                 printResult(c, "0>>>(2^31-1) = \u0000", " %d.  Expecting: 0.\n\u0000", 0);
                 c.load(resultCount).swap().sub().store(resultCount);

                 c.literal(-16).literal(3).swapForABI();
                 c.literal(ushr).call(3, INT);
                 printResult(c, "-16>>>3 = \u0000", " %d.  Expecting: 536870910.\n\u0000", 536870910);
                 c.load(resultCount).swap().sub().store(resultCount);

                 c.literal(16).literal(-3).swapForABI();
                 c.literal(ushr).call(3, INT);
                 printResult(c, "16>>>-3 = \u0000", " %d.  Expecting: 0.\n\u0000", 0);
                 c.load(resultCount).swap().sub().store(resultCount);

                 c.literal(10).literal(ushr2).call(2, INT);
                 printResult(c, "10>>>3 = \u0000", " %d.  Expecting: 1.\n\u0000", 1);
                 c.load(resultCount).swap().sub().store(resultCount);

                 c.literal(10).literal(ushr3).call(2, INT);
                 printResult(c, "3>>>10 = \u0000", " %d.  Expecting: 0.\n\u0000", 0);
                 c.load(resultCount).swap().sub().store(resultCount);

                 c.literal(10).literal(ushr4).call(2, INT);
                 printResult(c, "10>>>1 = \u0000", " %d.  Expecting: 5.\n\u0000", 5);
                 c.load(resultCount).swap().sub().store(resultCount);

                 c.load(resultCount);
                c.ret();
            c.end();
        c.leave();

        c.enter(ushr);
        Local x = c.parm(INT);      // x
        Local y = c.parm(INT);      // y
        c.result(INT);
            c.begin();
                c.load(x);          // x
                c.load(y);          // y
                c.ushr();           // x >>u y
                c.ret();
            c.end();
        c.leave();

        c.enter(ushr2);
        x = c.parm(INT);
        c.result(INT);
            c.begin();
                c.load(x).literal(3).ushr();
                c.ret();
            c.end();
        c.leave();

        c.enter(ushr3);
        x = c.parm(INT);
        c.result(INT);
            c.begin();
                c.literal(3).load(x).ushr();
                c.ret();
            c.end();
        c.leave();

        c.enter(ushr4);
        x = c.parm(INT);
        c.result(INT);
            c.begin();
                c.load(x).literal(1).ushr();
                c.ret();
            c.end();
        c.leave();

        c.compile();
        Linker linker = Compilation.newLinker(c);
        int entry = linker.link();
        int res = CSystem.icall(new Parm(entry));
        System.out.println("Unsigned shift right tests failed = " + res);
        System.exit(res);
    }

    private static void printResult(Compiler c, String msg, String expected, int expectedResult) {
        c.literal(msg.getBytes()).symbol("printf").call(2,VOID);
        c.dup();
        c.literal(expected.getBytes()).symbol("printf").call(3,VOID);
        c.literal(expectedResult).eq(); // cmp tos against expectedResult and place 1 or 0 on tos
    }

}
