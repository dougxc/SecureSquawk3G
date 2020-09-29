package bench.cubes;

public class Main implements Runnable {

    static int mul = 1;

    public void run() {
        String stats0 = System.getProperty("kvmjit.stats");
        long     res1 = run1();
        String stats1 = System.getProperty("kvmjit.stats");
        long     res2 = run1();
        String stats2 = System.getProperty("kvmjit.stats");
        long     res3 = run1();
        String stats3 = System.getProperty("kvmjit.stats");

        Thread t = Thread.currentThread();
        System.out.println(t + ":***********res0 "+stats0);
        System.out.println(t + ":***********res1 time = "+res1+ " "+stats1);
        System.out.println(t + ":***********res2 time = "+res2+ " "+stats2);
        System.out.println(t + ":***********res3 time = "+res3+ " "+stats3);
    }

    public long run1() {
        long start = System.currentTimeMillis();
        CubeCanvas canvas = new CubeCanvas(50*mul);
        canvas.run();
        long end = System.currentTimeMillis();
        return end-start;
    }


    public static void main(String[] args) {
        if (args.length > 0) {
            mul = Integer.parseInt(args[0]);
        }
        try {
            new Main().run();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
