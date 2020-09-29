/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */

package com.sun.squawk.compiler.tests;

import com.sun.squawk.compiler.*;
import com.sun.squawk.compiler.Compiler;
import com.sun.squawk.os.*;

public class Test11 implements Types {

    public static void main(String[] args) {

        Compiler c = Compilation.newCompiler();

        Label hw1 = c.label();
        Label hw2 = c.label();
        Label hw3 = c.label();

        c.enter();
        Local x = c.parm(INT);
        c.result(VOID);
            c.literal(new Label[]{hw1, hw2, hw3});
            c.load(x);
            c.literal(4);
            c.mul();
            c.add();
            c.read(REF);
            c.call(1, VOID);
            c.ret();
        c.leave();

        c.enter(hw1);
        c.result(VOID);
            c.literal("Hello World 1\n\u0000".getBytes());
            c.symbol("printf");
            c.call(2, VOID);
            c.ret();
        c.leave();

        c.enter(hw2);
        c.result(VOID);
            c.literal("Hello World 2\n\u0000".getBytes());
            c.symbol("printf");
            c.call(2, VOID);
            c.ret();
        c.leave();

        c.enter(hw3);
        c.result(VOID);
            c.literal("Hello World 3\n\u0000".getBytes());
            c.symbol("printf");
            c.call(2, VOID);
            c.ret();
        c.leave();

        c.compile();
        int entry = Compilation.newLinker(c).link();
        CSystem.vcall(new Parm(entry).parm(1));

    }

}
