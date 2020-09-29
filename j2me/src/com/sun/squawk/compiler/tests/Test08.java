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

public class Test08 implements Types {

    Compiler c  = Compilation.newCompiler();

    Label add   = c.label(); private byte ADD   = (byte)0;
    Label sub   = c.label(); private byte SUB   = (byte)1;
    Label con   = c.label(); private byte CON   = (byte)2;
    Label load  = c.label(); private byte LOAD  = (byte)3;
    Label store = c.label(); private byte STORE = (byte)4;
    Label ret   = c.label(); private byte RET   = (byte)5;
    Label table = c.label();

    Local ip, lp, sp;

    public static void main(String[] args) {
        new Test08().run();
    }

    public void run() {
        c.enter();
        ip = c.parm(REF);           // ip
        c.result(INT);
            c.begin();
                lp = c.local(REF);  // lp
                sp = c.local(REF);  // sp
                c.literal(40);      // Allocate memory for the 5 stack words and 5 locals
                c.alloca();         // ...
                c.literal(20);      // Get pointer to just before the stack
                c.add();            // ...
                c.store(sp);        // save
                c.load(sp);         // lp is same
                c.store(lp);        // save
                gotonext();

            c.bind(add);
                c.comment("+++++++add");
                pop();
                pop();
                c.swap().add();
                push();
                gotonext();
                c.comment("-------add");

            c.bind(sub);
                c.comment("+++++++sub");
                pop();
                pop();
                c.swap().sub();
                push();
                gotonext();
                c.comment("-------sub");

            c.bind(con);
                c.comment("+++++++con");
                getbyte();
                push();
                gotonext();
                c.comment("-------con");

            c.bind(load);
                c.comment("+++++++load");
                getbyte();
                c.literal(4);
                c.mul();
                c.load(lp);         // Get lp address
                c.add();
                c.read(INT);
                push();
                gotonext();
                c.comment("-------load");

            c.bind(store);
                c.comment("+++++++store");
                pop();
                getbyte();
                c.literal(4);
                c.mul();
                c.load(lp);         // Get lp address
                c.add();
                c.write(INT);
                gotonext();
                c.comment("-------store");

            c.bind(ret);
                c.comment("+++++++ret");
                pop();
                c.ret();
                c.comment("-------ret");

            c.end();

        c.comment("table");
        c.data(table, new Label[]{add, sub, con, load, store, ret});
        c.leave();

        c.compile();
        int entry = Compilation.newLinker(c).link();
        int res = CSystem.icall(new Parm(entry).cbytes(new byte[]{CON, 100, CON, 1, SUB, RET}));

        System.out.println("100 - 1 -> "+res);

        if (res == 99)
            System.exit(0);
        else
            System.exit(1);
    }

    private void push() {
        c.comment("+push");
        c.load(sp);
        c.literal(4);
        c.sub();
        c.store(sp);
        c.load(sp);
        c.write(INT);
        c.comment("-push");
    }

    private void pop() {
        c.comment("+pop");
        c.load(sp);
        c.read(INT);
        c.load(sp);
        c.literal(4);
        c.add();
        c.store(sp);
        c.comment("-pop");
    }

    private void getbyte() {
        c.comment("+getbyte");
        c.load(ip);
        c.dup();
        c.literal(1);
        c.add();
        c.store(ip);
        c.read(BYTE);
        c.comment("-getbyte");
    }

    private void gotonext() {
        c.comment("+gotonext");
        getbyte();
        c.literal(4);
        c.mul();
        c.literal(table);
        c.add();
        c.read(REF);
        c.jump();
        c.comment("-gotonext");
    }

}
