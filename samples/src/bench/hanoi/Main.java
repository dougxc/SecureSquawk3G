package bench.hanoi;

public class Main {

    static int discs = 22;

    public static void main (String args[]) {
        try {
            if (args.length > 0) {
                discs = Integer.parseInt(args[0]);
            }
            new Main().run();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public void run() {
        String stats0 = System.getProperty("kvmjit.stats");
        long     res1 = run1();
        String stats1 = System.getProperty("kvmjit.stats");
        long     res2 = run1();
        String stats2 = System.getProperty("kvmjit.stats");
        long     res3 = run1();
        String stats3 = System.getProperty("kvmjit.stats");


        System.out.println("***********res0 "+stats0);
        System.out.println("***********res1 time = "+res1+ " "+stats1);
        System.out.println("***********res2 time = "+res2+ " "+stats2);
        System.out.println("***********res3 time = "+res3+ " "+stats3);
    }

    public long run1() {
        long start = System.currentTimeMillis();
        hanoi(discs, 3, 1, 2);
        long end = System.currentTimeMillis();
        return end-start;
    }

    int count;

    void hanoi(int n, int t, int f, int u) {
        if (n > 0) {
            hanoi(n-1, u, f, t);
            count++;
            hanoi(n-1, t, u, f);
        }
    }


}


/*

with count++

Method void hanoi(int, int, int, int)
   0 iload_1
   1 ifle 36
   4 aload_0
   5 iload_1
   6 iconst_1
   7 isub
   8 iload 4
  10 iload_3
  11 iload_2
  12 invokevirtual #10 <Method void hanoi(int, int, int, int)>

  15 aload_0
  16 dup
  17 getfield #17 <Field int count>
  20 iconst_1
  21 iadd
  22 putfield #17 <Field int count>

  25 aload_0
  26 iload_1
  27 iconst_1
  28 isub
  29 iload_2
  30 iload 4
  32 iload_3
  33 invokevirtual #10 <Method void hanoi(int, int, int, int)>
  36 return



6:  0   extend_6
7:  0   methodid        .int 2674
12: 0   load_1
13: 1   ifle            .byte 30 (45)
15: 0   load_0
16: 1   load_1
17: 2   const_1
18: 3   isub
19: 2   load_4
20: 3   load_3
21: 4   load_2
22: 5   invokevirtual   .ubyte 11

24: 0   load_0
25: 1   store_5
26: 0   load_5
27: 1   getfield        .ubyte 0
29: 1   const_1
30: 2   iadd
31: 1   store_6
32: 0   load_5
33: 1   load_6
34: 2   putfield        .ubyte 0

36: 0   load_0
37: 1   load_1
38: 2   const_1
39: 3   isub
40: 2   load_2
41: 3   load_4
42: 4   load_3
43: 5   invokevirtual   .ubyte 11
45: 0   return



with count = count + 1;


   0 iload_1
   1 ifle 36
   4 aload_0
   5 iload_1
   6 iconst_1
   7 isub
   8 iload 4
  10 iload_3
  11 iload_2
  12 invokevirtual #10 <Method void hanoi(int, int, int, int)>

  15 aload_0
  16 aload_0
  17 getfield #17 <Field int count>
  20 iconst_1
  21 iadd
  22 putfield #17 <Field int count>

  25 aload_0
  26 iload_1
  27 iconst_1
  28 isub
  29 iload_2
  30 iload 4
  32 iload_3
  33 invokevirtual #10 <Method void hanoi(int, int, int, int)>
  36 return

(25 inst in 37 bytes)


6:  0   extend_6
7:  0   load_1
8:  1   ifle .byte 24 (34)
10: 0   load_0
11: 1   load_1
12: 2   const_1
13: 3   isub
14: 2   load_4
15: 3   load_3
16: 4   load_2
17: 5   invokevirtual .ubyte 11
19: 0   this_getfield .ubyte 0
21: 1   const_1
22: 2   iadd
23: 1   this_putfield .ubyte 0
25: 0   load_0
26: 1   load_1
27: 2   const_1
28: 3   isub
29: 2   load_2
30: 3   load_4
31: 4   load_3
32: 5   invokevirtual .ubyte 11
34: 0   return

(24 inst in 29 bytes)
*/
