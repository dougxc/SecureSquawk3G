package bench.loop;

public class Main {

    static int loopCount = 1000000;

    public static void main (String args[]) {
        try {
            if (args.length > 0) {
                loopCount *= Integer.parseInt(args[0]);
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
        int count = loopCount;

        while (count-- > 0) {
           // This is the test...
        }

        long end = System.currentTimeMillis();
        return end-start;
    }


    int[][] block;

    public void test() {
        for (int i=0;i<6;i++)
           for (int j=0;j<64;j++)
               block[i][j] = 0;
    }

}
