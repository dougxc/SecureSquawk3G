package com.sun.squawk.compiler.tests;

import com.sun.squawk.compiler.*;
import com.sun.squawk.compiler.Compiler;
import com.sun.squawk.os.*;

public class TestC6 implements Types {

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

                 c.literal(2).literal(5).swapForABI();
                 c.literal(exp1).call(3, INT);
                 printResult(c, "(5/3)+2 = \u0000", " %d.  Expecting: 3.\n\u0000", 3);
                 c.load(resultCount).swap().sub().store(resultCount);

                 c.literal(4).literal(1).swapForABI();
                 c.literal(exp2).call(3, INT);
                 printResult(c, "1+(3*4)-8 = \u0000", " %d.  Expecting: 5.\n\u0000", 5);
                 c.load(resultCount).swap().sub().store(resultCount);

                 c.literal(2).literal(5).swapForABI();
                 c.literal(exp3).call(3, INT);
                 printResult(c, "(5/3)+2 = \u0000", " %d.  Expecting: 3.\n\u0000", 3);
                 c.load(resultCount).swap().sub().store(resultCount);

                 c.literal(3).literal(6).literal(1).swapForABI();
                 c.literal(exp4).call(4, INT);
                 printResult(c, "((20-6)*3+1)/2 = \u0000", " %d.  Expecting: 21.\n\u0000", 21);
                 c.load(resultCount).swap().sub().store(resultCount);

                 c.load(resultCount);
                 c.ret();
            c.end();
        c.leave();

        c.enter(exp1);
        Local x = c.parm(INT);      // x
        Local y = c.parm(INT);      // y
        c.result(INT);
            c.begin();
                c.load(x);          // x
                c.load(y);          // y
                c.literal(3);
                c.div();            // y / 3
                c.add();            // (y/3) + x
                c.ret();
            c.end();
        c.leave();

        c.enter(exp2);
        x = c.parm(INT);
        y = c.parm(INT);
        c.result(INT);
            c.begin();
                c.load(x).literal(3).mul();
                c.load(y).add();
                c.literal(8).sub();
                c.ret();
            c.end();
        c.leave();

        c.enter(exp3);
        x = c.parm(INT);
        y = c.parm(INT);
        c.result(INT);
            c.begin();
                c.load(y).literal(3).div();
                c.load(x).add();
                c.ret();
            c.end();
        c.leave();

        c.enter(exp4);
        x = c.parm(INT);
        y = c.parm(INT);
        Local z = c.parm(INT);
        c.result(INT);
            c.begin();
                c.literal(20).load(y).sub();
                c.load(x).mul();
                c.load(z).add();
                c.literal(2).div();
                c.ret();
            c.end();
        c.leave();

        c.compile();
        Linker linker = Compilation.newLinker(c);
        int entry = linker.link();
        int res = CSystem.icall(new Parm(entry));
        System.out.println("Expression tests failed = " + res);
        System.exit(res);
    }

    private static void printResult(Compiler c, String msg, String expected, int expectedResult) {
        c.literal(msg.getBytes()).symbol("printf").call(2,VOID);
        c.dup();
        c.literal(expected.getBytes()).symbol("printf").call(3,VOID);
        c.literal(expectedResult).eq(); // cmp tos against expectedResult and place 1 or 0 on tos
    }

}
