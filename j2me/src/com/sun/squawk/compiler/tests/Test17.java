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

public class Test17 implements Types {


    public static void main(String[] args) {

        Compiler c = Compilation.newCompiler();

        Label rtn1 = c.label();

        c.enter();
        c.result(INT);

            Label loop  = c.label();
            Local count = c.local(INT);
            Local acc   = c.local(INT);

            c.literal(0);
            c.store(count);
            c.literal(0);
            c.store(acc);

        c.bind(loop);

            /*
             * Call rtn1 with 42 as the parameter
             */
            c.literal(42);
            c.literal(rtn1);
            c.call(INT);

            /*
             * Add the result into acc
             */
            c.load(acc);
            c.add();
            c.store(acc);

            /*
             * Add 1 to count and branch to loop if it is less than 1000000.
             *
             * This will test that the parameter was correctly removed from
             * the stack after the call instruction.
             */
            c.load(count);
            c.literal(1);
            c.add();
            c.dup();
            c.store(count);
            c.literal(1000000);
            c.eq();
            c.bf(loop);

            /*
             * Return acc
             */
            c.load(acc);
            c.ret();
        c.leave();

        c.enter(rtn1);
        Local parm = c.parm(INT);
        c.result(INT);
            c.load(parm);
            c.ret();
        c.leave();


        c.compile();

        int entry = Compilation.newLinker(c).link();
        int res = CSystem.icall(new Parm(entry));
        System.out.println("res = "+res);

        if (res == 42000000)
            System.exit(0);
        else
            System.exit(1);
    }



}
