package com.sun.squawk.compiler.tests;

import com.sun.squawk.compiler.*;
import com.sun.squawk.compiler.Compiler;
import com.sun.squawk.os.*;

public class Test21 implements Types {

    Compiler c = Compilation.newCompiler();

    Label hello = c.label(); private byte HELLO = (byte)0;
    Label add   = c.label(); private byte ADD   = (byte)1;
    Label sub   = c.label(); private byte SUB   = (byte)2;
    Label con   = c.label(); private byte CON   = (byte)3;
    Label load  = c.label(); private byte LOAD  = (byte)4;
    Label store = c.label(); private byte STORE = (byte)5;
    Label addp  = c.label(); private byte ADDP  = (byte)6;
    Label ret   = c.label(); private byte RET   = (byte)7;
    Label table = c.label();


    Local mp, ip, lp, ss;

    public static void main(String[] args) {
        new Test21().run();
    }

    public void run() {
        c.enter(Compiler.E_REGISTER);
        c.result(INT);
            c.begin();
                mp = c.local(MP);
                ip = c.local(IP);
                lp = c.local(LP);
                ss = c.local(SS);

                c.load(mp);
                c.literal(c.getJumpSize());
                c.add();
                c.store(ip);

                c.literal(WORD.getStructureSize() * 5);  // Number of extra local bytes
                c.literal(WORD.getStructureSize() * 5);  // Number of extra stack bytes
                c.stackCheck();

                c.literal(WORD.getStructureSize() * 5); // Allocate memory for 5 locals.
                c.alloca();
                c.store(lp);                            // Save address in lp
                gotonext();

            c.bind(hello);
                c.comment("+++++++hello");
                c.literal("Hello\n\u0000".getBytes());
                c.push();
                c.symbol("printf");
                c.call(VOID, Compiler.C_DYNAMIC);   // Should reset esp to location after alloca() was called.
                gotonext();
                c.comment("-------hello");

            c.bind(add);
                c.comment("+++++++add");
                c.pop(INT);
                c.pop(INT);
                c.swap().add();
                c.push();
                gotonext();
                c.comment("-------add");

            c.bind(sub);
                c.comment("+++++++sub");
                c.pop(INT);
                c.pop(INT);
                c.swap().sub();
                c.push();
                gotonext();
                c.comment("-------sub");

            c.bind(con);
                c.comment("+++++++con");
                getbyte();
                c.push();
                gotonext();
                c.comment("-------con");

            c.bind(load);
                c.comment("+++++++load");
                getbyte();
                c.literal(4);
                c.mul();
                c.load(lp);
                c.add();
                c.read(INT);
                c.push();
                gotonext();
                c.comment("-------load");

            c.bind(store);
                c.comment("+++++++store");
                c.pop(INT);
                getbyte();
                c.literal(4);
                c.mul();
                c.load(lp);
                c.add();
                c.write(INT);
                gotonext();
                c.comment("-------store");

            c.bind(addp);
                c.comment("+++++++addp");
                getbyte();
                c.loadParm();
                c.pop(INT);
                c.add();
                c.push();
                c.comment("-------addp");

            c.bind(ret);
                c.comment("+++++++ret");
                c.pop(INT);
                c.ret();
                c.comment("-------ret");

            c.end();

        c.comment("table");
        c.data(table, new Label[]{hello, add, sub, con, load, store, addp, ret});
        c.leave();

        c.compile();
        int interp = Compilation.newLinker(c).link();

        /*
         * Define the program.
         */
        byte[] program = new byte[] {HELLO, CON, 1, STORE, 1, CON, 100, STORE, 2, LOAD, 2, LOAD, 1, SUB, ADDP, 0, RET};

        /*
         * Allocate the bytecode array and put a jump instruction
         * to the interpreter at the start of it followed by the
         * bytecodes for the program.
         */
        int bytecodes = CSystem.malloc(c.getJumpSize()+program.length);
        int bc = bytecodes;

        /*
         * Use special function in the compiler to get the jump instruction
         * to the interpreter. This is returned a single byte at a time.
         */
        for (int i = 0 ; i < c.getJumpSize() ; i++) {
            CSystem.setByte(bc++, c.getJumpByte(bytecodes, interp, i));
        }

        /*
         * Add the program to the array.
         */
        for (int i = 0 ; i < program.length ; i++) {
            CSystem.setByte(bc++, program[i]);
        }

        /*
         * Call the bytecode array.
         */
        int res = CSystem.icall(new Parm(bytecodes).parm(42));
        System.out.println("100 - 1 + 42 -> "+res);

        if (res == 141)
            System.exit(0);
        else
            System.exit(1);
    }

    private void getbyte() {
        c.comment("+getbyte");
        c.load(ip);
        c.read(BYTE);  // added
//        c.dup();   // removed
        c.load(ip);  // added
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
        c.literal(table);
        c.add();
        c.read(REF);
        c.jump();
        c.comment("-gotonext");
    }

}
