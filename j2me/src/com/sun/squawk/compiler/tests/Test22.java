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

public class Test22 implements Types {

    public static void main(String[] args) {

        Compiler c = Compilation.newCompiler();

        Local mp, ip, lp, ss;

        c.enter(Compiler.E_REGISTER);
        c.result(VOID);
            mp = c.local(MP);
            ip = c.local(IP);
            lp = c.local(LP);
            ss = c.local(SS);

            c.load(mp);
            c.literal(c.getJumpSize());
            c.add();
            c.store(ip);

            c.literal(0);    // Number of extra local bytes
            c.literal(3*4);  // Number of extra stack bytes
            c.stackCheck();

            c.literal(0);    // Allocate memory for 5 locals.
            c.alloca();
            c.store(lp);     // Save address in lp

            c.literal(1).push();
            c.literal(2).push();
            c.literal(3).push();

            c.literal("peekReceiver = %d\n\u0000".getBytes());
            c.peekReceiver();
            c.popAll();
            c.swapForABI();
            c.symbol("printf");
            c.call(VOID);
            c.ret();
        c.leave();

        c.compile();
        int entry = Compilation.newLinker(c).link();
        CSystem.vcall(new Parm(entry));
    }


}
