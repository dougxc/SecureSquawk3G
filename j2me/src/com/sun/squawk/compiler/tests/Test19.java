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

public class Test19 implements Types {


    public static void main(String[] args) {

        Compiler c = Compilation.newCompiler();

        Label rtn1 = c.label();
        Local mp, ip, lp, ss;

        c.enter(Compiler.E_REGISTER);
        c.result(INT);
            mp = c.local(MP);
            ip = c.local(IP);
            lp = c.local(LP);
            ss = c.local(SS);

            c.load(mp);
            c.literal(c.getJumpSize());
            c.add();
            c.store(ip);

            c.literal(0);    // Number of extra local bytes
            c.literal(2*4);  // Number of extra stack bytes
            c.stackCheck();

            /*
             * Call rtn1 with 40 and 2 as the parameters.
             */
            c.literal(2);
            c.push();
            c.literal(40);
            c.push();
            c.swapForABI();
            c.literal(rtn1);
            c.call(INT, Compiler.C_DYNAMIC);
            c.ret();

        c.leave();

        c.enter(rtn1);
        Local p1 = c.parm(INT);
        Local p2 = c.parm(INT);
        c.result(INT);
            c.load(p1);
            c.load(p2);
            c.add();
            c.ret();
        c.leave();


        c.compile();

        int entry = Compilation.newLinker(c).link();
        int res = CSystem.icall(new Parm(entry));
        System.out.println("res = "+res);

        if (res == 42)
            System.exit(0);
        else
            System.exit(1);
    }



}
