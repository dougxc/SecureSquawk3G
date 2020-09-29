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

public class Test15 implements Types {


    public static void main(String[] args) {

        Compiler c = Compilation.newCompiler();

        Label rtn1 = c.label();
        Label rtn2 = c.label();
        Label rtn3 = c.label();
        Label rtn4 = c.label();

        c.enter();
        c.result(VOID);
            c.literal(rtn1).call(VOID);
            c.literal(rtn2).call(VOID, Compiler.C_JVM);
            c.literal(rtn3).call(VOID, Compiler.C_JVM);
            c.literal(rtn4).call(VOID, Compiler.C_JVM);
            c.ret();
        c.leave();

        c.enter(rtn1, Compiler.E_NONE);
        c.result(VOID);
            c.literal("I am rtn1\n\u0000".getBytes());
            c.symbol("printf");
            c.call(VOID);
            c.ret();
        c.leave();

        c.enter(rtn2, Compiler.E_NULL);
        c.result(VOID); {
            Local mp = c.local(MP);
            c.literal("I am rtn2 mp = %x\n\u0000".getBytes());
            c.load(mp);
            c.swapForABI();
            c.symbol("printf");
            c.call(VOID);
            c.ret();
            }
        c.leave();

        c.enter(rtn3, Compiler.E_ADDRESS);
        c.result(VOID); {
            Local mp = c.local(MP);
            c.literal("I am rtn3 mp = %x\n\u0000".getBytes());
            c.load(mp);
            c.swapForABI();
            c.symbol("printf");
            c.call(VOID);
            c.ret();
            }
        c.leave();

        c.enter(rtn4, Compiler.E_REGISTER);
        c.result(VOID); {
            Local mp = c.local(MP);
            Local ip = c.local(IP);

            c.load(mp);
            c.literal(c.getJumpSize());
            c.add();
            c.store(ip);

            c.literal("I am rtn4 mp = %x ip = %x\n\u0000".getBytes());
            c.load(mp);
            c.load(ip);
            c.swapForABI();
            c.symbol("printf");
            c.call(VOID);
            c.ret();
            }
        c.leave();

        c.compile();

        int entry = Compilation.newLinker(c).link();
        CSystem.vcall(new Parm(entry));
    }



}
