package com.sun.squawk.compiler.tests;

import com.sun.squawk.compiler.*;
import com.sun.squawk.compiler.Compiler;
import com.sun.squawk.os.*;

public class Test09 implements Types {

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
        new Test09().run();
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
        c.data(table, new Label[]{add, sub, con, load, store, ret});  // table is an Object here
        c.leave();

        c.compile();
        int entry = Compilation.newLinker(c).link();

        int res = CSystem.icall(new Parm(entry).cbytes(new byte[]{CON, 100, CON, 1, ADD, RET}));
        System.out.println("100 + 1 -> "+res);

        int result = 0;
        if (res != 101)
            result++;

        res = CSystem.icall(new Parm(entry).cbytes(new byte[]{CON, 87, CON, 50, SUB, RET}));
        System.out.println("87 - 50 -> "+res);

        if (res != 37)
            result++;

        res = CSystem.icall(new Parm(entry).cbytes(new byte[]{CON, 87, STORE, 1, LOAD, 1, RET}));
        System.out.println("store 87 at loc1, load loc1 -> "+res);

        if (res != 87)
            result++;
        System.exit(result);
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
        c.read(BYTE);  // added
        c.load(ip);    // added
//        c.dup();   // removed
        c.literal(1);
        c.add();
        c.store(ip);
//        c.read(BYTE);   // removed
        c.comment("-getbyte");
    }

    private void gotonext() {
        c.comment("+gotonext");
        getbyte();
        c.literal(4);
        c.mul();
        c.literal(table);   // table is a Label here
        c.add();
        c.read(REF);
        c.jump();
        c.comment("-gotonext");
    }

}
