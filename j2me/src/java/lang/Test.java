/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */
package java.lang;

public class Test {

/*---------------------------------------------------------------------------*\
 *                                   "X" Tests                               *
\*---------------------------------------------------------------------------*/

    private static int two;

    public static void main(String[] args) {
        System.out.print("Running: java.lang.Test");
        for (int i = 0 ; i < args.length ; i++) {
            System.out.print(" " + args[i]);
        }
        System.out.println();

        try {
            runXTests();
        } finally {
            System.out.println("runXTests() returned!");
        }
    }

    public static void runXTests() {
        two = 2;
//x2_1();
//VM.stopVM(-555);

        x2_1();
        x2_2();
        x2_3();
        x2_4();
/*if[FLOATS]*/
        x2_5();
//VM.stopVM(-555);
        x2_6();
        x2_7();
        x2_8();
/*end[FLOATS]*/
        x3();
        x4();
        x5(2);
        x6(2);
        x7(2);
        x8(2);
        x9(2);
        x10();
        x11();
        x12();
        x13();
        x14();
        x15();
        x16();
        x20();
        x30();
        x31();
        x32();
        x33();
        x34();
        x35();
        x36();
        x37();
        x38();
        x39();
        x40();
        x41(null, 123);
        x42();
        x43();
        x44();
        x45();
//        x46();
        randomTimeTest();
        VM.print("Finished tests\n");
        System.exit(12345);
    }

    static void passed(String name) {
        if (VM.isVerbose()) {
            VM.print("Test ");
            VM.print(name);
            VM.print(" passed\n");
        }
    }

    static void failed(String name) {
        VM.print("Test ");
        VM.print(name);
        VM.print(" failed\n");
        System.exit(54321);
    }

    static void result(String name, boolean b) {
        if (b) {
            passed(name);
        } else {
            failed(name);
        }
    }

    static void x2_1() { result("x2_1", Integer.toString(2).equals("2"));     }
    static void x2_2() { result("x2_2", Long.toString(2L).equals("2"));       }
    static void x2_3() { result("x2_3", String.valueOf(true).equals("true")); }
    static void x2_4() { result("x2_4", String.valueOf('2').equals("2"));     }

/*if[FLOATS]*/
    static void x2_5() { result("x2_5", Double.toString(2.0d).equals("2.0")); }
    static void x2_6() { result("x2_6", Float.toString(2.0f).equals("2.0"));  }
    static void x2_7() { result("x2_7", Double.toString(12345.0d).equals("12345.0")); }
    static void x2_8() { result("x2_8", Float.toString(12345.0f).equals("12345.0")); }
/*end[FLOATS]*/

    static void x3() {
        int four = 4;
        result("x3", -four == -4);
    }

    static void x4() {
        passed("x4");
    }

    static void x5(int n) {
        boolean res = false;
        if (n == 2) {
            res = true;
        }
        result("x5", res);
    }

    static void x6(int n) {
        result("x5", n == 2);
    }

    static void x7(int n) {
        result("x7", 5+n == 7);
    }

    static void x8(int n) {
        result("x8", 5*n == 10);
    }

    static void x9(int n) {
        result("x9", -5*n == -10);
    }

    static void x10() {
        result("x10", -5*two == -10);
    }

    static void x11() {
        for (int i = 0 ; i < 10 ; i++) {
            VM.collectGarbage();
        }
        passed("x11");
    }

    static void x12() {
        result("x12", fib(20) == 10946);
    }

    public static int fib (int n) {
        if (n == 0) {
            VM.collectGarbage();
        }
        if (n<2) {
            return 1;
        }
        int x = fib(n/2-1);
        int y = fib(n/2);
        if (n%2==0) {
            return x*x+y*y;
        } else {
            return (x+x+y)*y;
        }
    }

    static void x13() {
        result("x13",(!(null instanceof Test)));
    }

    static void x14() {
        result("x14",("a string" instanceof String));
    }

    static void x15() {
        boolean res = true;
        try {
            Klass c = (Klass)null;
        } catch (Throwable t) {
            res = false;
        }
        result("x15",res);
    }

    static void x16() {
        boolean res = true;
        try {
            (new String[3])[1] = null;
        } catch (Throwable t) {
            res = false;
        }
        result("x16",res);
    }

    static void x20() {
        Test t = new Test();
        result("x20", t != null);
    }


    static void x30() {
        Object[] o = new Object[1];
        result("x30", o != null);
    }


    static void x31() {
        Object[] o = new Object[1];
        o[0] = o;
        result("x31", o[0] == o);
    }

    static void x32() {
        Object[] o1 = new Object[1];
        Object[] o2 = new Object[1];
        o1[0] = o1;
        System.arraycopy(o1, 0, o2, 0, 1);
        result("x32", o2[0] == o1);
    }

    static void x33() {
        Object[] o1 = new Object[2];
        Object[] o2 = new Object[2];
        o1[0] = o1;
        o1[1] = o2;
        System.arraycopy(o1, 0, o2, 0, 2);
        result("x33", o2[0] == o1 && o2[1] == o2);
    }

    static void x34() {
        Object[] o1 = new Object[2];
        String[] o2 = new String[2];
        o1[0] = "Hello";
        o1[1] = "World";
        System.arraycopy(o1, 0, o2, 0, 2);
        result("x34", o2[0].equals("Hello") && o2[1].equals("World"));
    }

    static void x35() {
        Object o = new Throwable();
        result("x35", o != null);
    }

    static void x36() {
        long l = 0xFF;
        int  i = 0xFF;
        result("x36",(l << 32) == 0xFF00000000L && ((long)i << 32) == 0xFF00000000L);
    }

    static void x37() {
        byte[] o1 = new byte[2];
        o1[0] = (byte)-3;
        result("x37", o1[0] == -3 && o1[1] == 0);
    }

    static void x38() {
        Object x = null;
        Object o = new Object();
        java.util.Vector v1 = new java.util.Vector();
        v1.addElement(v1);
        java.util.Vector v2 = new java.util.Vector();
        v2.addElement(v2);
        for (int i = 0 ; i < 1000 ; i++) {
            synchronized(o) {
                synchronized(v2) {
                    x = v1.elementAt(0);
                }
                synchronized(v1) {
                    x = v2.elementAt(0);
                }
            }
        }
        result("x38", true);
    }



    static int x39count;

    static void x39() {
        boolean res = false;
        try {
            x39prim();
        } catch(OutOfMemoryError ex) {
            res = true;
            System.out.println("x39 count = " + x39count);
        }
        result("x39", res);
    }

    static void x39prim() {
        int amount = (int)GC.totalMemory(true) / 40;
        Object[] last = new Object[amount];
        while(true) {
            Object[] next = new Object[amount];
            next[0] = last;
            last = next;
            x39count++;
        }
    }

    static int x40count = 0;

    static void x40() {
        boolean res = false;
        try {
            recursiveCall();
        } catch(OutOfMemoryError ex) {
            res = true;
            System.out.println("x40 recursion level = " + x40count);
        }
        result("x40", res);
    }

    static void recursiveCall() {
        x40count++;
        recursiveCall();
    }

    static void x41(Object obj, int val) {
        switch(val) {
            default:
                val++;
                break;
        }
        if (val == 123) {
            x41(obj, 3);
        }
        result("x41", true);
    }

    static void x42() {
        int res = 0;
        FOO foo = new FOO();
        Throwable thro = null;
        try {
            foo.foo(123L, 456L);
        } catch(ClassCastException ex) {
            thro = ex;
            res = 1;
        } catch(Exception ex) {
            thro = ex;
            res = 2;
        } catch(Error ex) {
            thro = ex;
            res = 3;
        }
        if (thro != null) {
            System.out.println("x42 printStackTrace - this should be a java.lang.NullPointerException");
            thro.printStackTrace();
        }
        result("x42", res == 2);
    }

    static class FOO {
        FOO xxx = null;
        int foo(long a, long b) {
            xxx.xxx();
            return (int)(a+b);
        }
        void xxx() {
        }
        protected void finalize() throws Throwable {
           System.out.println("FOO::finalize()");
        }
    }

    static void x43() {
        try {
           throw new Throwable("foo");
        } catch (Throwable ex) {
        }
        result("x43", true);
    }

    static void x44() {
        String[] arr = null;
        if (arr != null) {
            arr[0].concat("should not reach here");
        }
        result("x44", true);
    }



    static int x45count;
    static void x45() {
        for (int i = 0; i != 200; ++i) {
            Thread t = new Thread(null, i) {
                public void run() {
                    f();
                }
                private void f() {
                    x45count++;
                }
            };
            t.start();
            try {
                t.join();
            } catch (InterruptedException ie) {

            }
        }
        result("x45", x45count == 200);
    }

    static void x46Prim(int threadSize) {
        Thread t = new Thread(null, threadSize) {
            public void run() {

            }
        };
        t.start();
        try {
            t.join();
        } catch (InterruptedException ie) {

        }
    }

    /**
     * This tests that the VM can handle the case where a thread's stack takes up the remaining free memory.
     */
    static void x46() {
        int delta = -50;
        while (true) {
            int threadSize = 0;
            try {
                VM.collectGarbage();
                threadSize = ((int)GC.freeMemory(true) / com.sun.squawk.vm.HDR.BYTES_PER_WORD) + delta;
                x46Prim(threadSize);
                if (VM.isVerbose()) {
                    VM.print("threadSize = ");
                    VM.println(threadSize);
                }
                delta++;
            } catch (OutOfMemoryError oome) {
                break;
            }
        }
        result("x46", true);
    }

    static void randomTimeTest() {
        long iterations = System.currentTimeMillis() & 255;
        iterations = iterations*iterations*iterations;
        VM.println("random time test = "+iterations);
        while (iterations-- > 0) {
            iterations = iterations - 1;
        }
    }

}
