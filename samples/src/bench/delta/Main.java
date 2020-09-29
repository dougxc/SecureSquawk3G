
package bench.delta;

public class Main {

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
        DeltaBlue.main(null);
        long end = System.currentTimeMillis();
        return end-start;
    }


    public static void main(String[] args) {
        try {
            new Main().run();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }



}

