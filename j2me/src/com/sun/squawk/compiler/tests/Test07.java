package com.sun.squawk.compiler.tests;

import com.sun.squawk.compiler.*;
import com.sun.squawk.compiler.Compiler;
import com.sun.squawk.os.*;

public class Test07 implements Types {

    public static void main(String[] args) {

        Compiler c = Compilation.newCompiler();

        Label l1 = c.label();
        Label l2 = c.label();

        c.enter();
        Local x = c.parm(INT);
        Local y = c.parm(INT);
        c.result(INT);
            c.begin();
                Local t = c.local(INT);
                c.literal(1);
                c.load(x);          // x
                c.load(y);          // y
                c.eq();
                c.bt(l1);           // Literal 1 on stack here
                c.literal(2);
                c.store(t);
                c.br(l2);
            c.bind(l1);
                c.literal(3);
                c.store(t);
            c.bind(l2);             // Merge point
                c.load(t);
                c.add();
                c.ret();
            c.end();
        c.leave();

        c.compile();
        int entry = Compilation.newLinker(c).link();

        int res = CSystem.icall(new Parm(entry).parm(123).parm(456));
        System.out.println("123, 456 -> "+res);

        int result = 0;
        if (res != 3)
            result++;

        int res2 = CSystem.icall(new Parm(entry).parm(999).parm(999));
        System.out.println("999, 999 -> "+res2);

        if (res2 != 4)
            result++;
        System.exit(result);
    }

}
