package hibernation;

import javax.microedition.io.*;
import java.io.*;

public class Test1 {

    public static void main(String[] args) throws java.io.IOException {
        Hibernator.main(args);
    }
}

class Hibernator {
    public static void main(String[] args) throws java.io.IOException {
        String cp = Thread.currentThread().getIsolate().getClassPath();
        Isolate isolate = new Isolate("hibernation.Hibernatee", args, cp, null);

        isolate.start();
        isolate.join();

        while (isolate.isHibernated()) {
            String url = isolate.save();
            if (VM.isVeryVerbose()) {
                VM.println("[loading and unhibernating " + url + "]");
            }
            isolate = Isolate.load(url);
            isolate.unhibernate();
            isolate.join();
        }
    }
}

class Hibernatee {


    public static void main(String[] args) {
        String runCountArg = args.length == 0 ? "10" : args[0];
        int runCount = Integer.parseInt(runCountArg);

        String threadCountArg = args.length < 2 ? "10" : args[1];
        int threadCount = Integer.parseInt(threadCountArg);
        Thread[] threads = new Thread[threadCount];

        final int value[] = { 0 };
        final int limit = runCount * threads.length;
        for (int i = 0; i != threads.length; ++i) {
            threads[i] = new Thread() {
                public void run() {
                    while (value[0] < limit) {
                        System.out.println(this +": " + value[0]);
                        value[0]++;
                        Thread.yield();
                    }
                }
            };
            threads[i].start();
        }

        while (runCount > 0) {
            Thread.yield();
            System.out.println("remaining runs: " + runCount + " (value=" + value[0] + ")");
            try {
                Isolate thisIsolate = Thread.currentThread().getIsolate();
                System.out.println("Hibernating " + thisIsolate);
                thisIsolate.hibernate();
                System.out.println("Reawoke " + thisIsolate);
            }
            catch (java.io.IOException ex) {
                System.err.println("Error hibernating isolate: " + ex);
//                ex.printStackTrace();
            }
            runCount--;
        }
    }
}
