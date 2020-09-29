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

public class Test12 implements Types {

    public static void main(String[] args) {

        Compiler c = Compilation.newCompiler();

        Label hw1 = c.label();
        Label hw2 = c.label();
        Label hw3 = c.label();
        Label hwn = c.label();

        MethodMap mm0 = new MethodMap();
        MethodMap mm1 = new MethodMap();
        MethodMap mm2 = new MethodMap();
        MethodMap mm3 = new MethodMap();
        MethodMap mm4 = new MethodMap();
        MethodMap mm5 = new MethodMap();

        c.enter();
        Local p = c.parm(INT);
        c.result(VOID);
            c.literal(new Label[]{hw1, hw2, hw3});
            c.load(p);
            c.literal(4);
            c.mul();
            c.add();
            c.read(REF);
            c.call(1, VOID);
            c.ret();
        c.leave(mm0);

        c.enter(hw1);
        c.result(VOID);
            c.literal(1);
            c.literal(hwn);
            c.call(2, VOID);
            c.ret();
        c.leave(mm1);

        c.enter(hw2);
        c.result(VOID);
            c.literal(2);
            c.literal(hwn);
            c.call(2, VOID);
            c.ret();
        c.leave(mm2);

        c.enter(hw3);
        c.result(VOID);
            c.literal(3);
            c.literal(hwn);
            c.call(2, VOID);
            c.ret();
        c.leave(mm3);

        c.enter(hwn);
        Local n = c.parm(INT);
        c.result(VOID);
            c.load(n);
            c.literal("Hello World %d\n\u0000".getBytes());
            c.symbol("printf");
            c.call(3, VOID);
            c.ret();
        c.leave(mm4);

        /*
         * Dummy function that has some of oops
         */
        c.enter();
        Local x = c.parm(OOP);
        c.result(VOID);
            Local y = c.local(OOP);
            Local z = c.local(REF);
            c.load(x);
            c.store(y);
            c.load(y);
            c.literal(1);
            c.add(); // Adding 1 to an OOP should produce a REF
            c.store(z);
            c.ret();
        c.leave(mm5);

        c.compile();

        printMmap(mm0, "mm0");
        printMmap(mm1, "mm1");
        printMmap(mm2, "mm2");
        printMmap(mm3, "mm3");
        printMmap(mm4, "mm4");
        printMmap(mm5, "mm5");

        int entry = Compilation.newLinker(c).link();
        CSystem.vcall(new Parm(entry).parm(1));
    }

    public static void printMmap(MethodMap mmap, String name) {
        System.out.println("Method Map for " + name);
        System.out.print("  locals = ");
        printMap(mmap.getLocalSlotCount(), mmap.getLocalOopMap());
        System.out.println();
        System.out.print("  parms  = ");
        printMap(mmap.getParameterSlotCount(), mmap.getParameterOopMap());
        System.out.println();
    }

    public static void printMap(int count, byte[] oopmap) {
        int offset = 0;
        int mask = 1;
        for (int i = 0 ; i < count ; i++) {
            if ((oopmap[offset] & mask) == 0) {
                System.out.print("0");
            } else {
                System.out.print("1");
            }
            mask <<= 1;
            if (mask == 0x100) {
                offset++;
                mask = 1;
            }
        }
    }



}
