package com.sun.squawk.regression;

import java.io.*;
import javax.microedition.io.*;
import com.sun.squawk.util.ArgsUtilities;

/**
 * This is the harness for running a number of applications on Squawk as
 * a set of regression tests.
 */

public class Main {

    /**
     * The stream used to log regression test status and results.
     */
    private final PrintStream out;

    /**
     * The amount of time to run each sample before hibernating it.
     */
    final int samplesExecuteTime;

    /**
     * Specifies whether or not the TCK tests should be run as part of the regression suite
     */
    final boolean runTCK;

    /**
     * The prefix to use for output files.
     */
    private final String prefix;

    private Main(String prefix, int samplesExecuteTime, boolean runTCK) throws IOException {
        this.prefix = prefix;
        this.out = new PrintStream(Connector.openOutputStream("file://" + prefix + "regression.log"));
        this.samplesExecuteTime = samplesExecuteTime;
        this.runTCK = runTCK;
    }

    /**
     * Runs a given isolate for some duration that is measured in the number of yields
     * this thread should perform. Once the duration is completed, the isolate is hibernated
     * or exited.
     *
     * @param isolate     the isolate to run
     * @param delay       the milliseconds this thread should sleep before hibernating/exiting the isolate
     * @param hibernate   if true, the isolate is hibernated, otherwise it is exited
     */
    private void runForSometime(Isolate isolate, int delay, boolean hibernate) throws IOException {
        try {
            while (delay > 0) {
                int sleep = Math.min(delay, 5000);
                if (VM.isVeryVerbose()) {
                    VM.println("[regression: running " + isolate + " for another " + (delay/1000) + " seconds]");
                }
                Thread.sleep(sleep);
                delay = delay - sleep;
            }
        } catch (InterruptedException ie) {
        }

        if (isolate.isAlive()) {
            if (hibernate) {
                if (VM.isVeryVerbose()) {
                    VM.println("[regression: hibernating " + isolate + "]");
                }
                isolate.hibernate();
            } else {
                if (VM.isVeryVerbose()) {
                    VM.println("[regression: stopping " + isolate + "]");
                }
                isolate.exit(0);
            }
        } else {
            if (hibernate) {
                throw new RuntimeException(isolate + " stopped before it could be hibernated");
            }
        }
    }

    /**
     * Runs a given application in its own isolate.
     *
     * @param mainClassName   the main class of the application
     * @param argsString      the command line arguments to pass to the application
     * @param classPath       the class path to use
     * @param delay           the milliseconds this thread should sleep before hibernating/exiting the isolate
     *                        or 0 if the isolate should be allowed to completely naturally
     * @param expectedExitCode the expected exit code of the application
     * @return true if the application exited with the expected exit code
     */
    boolean run0(String mainClassName, String argsString, String classPath, final int delay, int expectedExitCode) {
        String[] args = ArgsUtilities.cut(argsString);
        Isolate isolate = new Isolate(mainClassName, args, classPath, null);

        out.println(mainClassName + " " + argsString + ":");

        long start = System.currentTimeMillis();
        Throwable exception = null;

        try {
            isolate.start();

            boolean hibernate = (delay != 0);
            if (hibernate) {
                runForSometime(isolate, delay, true);
            }

            isolate.join();

            // If the isolate was hibernated, make sure it can be saved and resumed
            if (isolate.isHibernated()) {
                String url = isolate.save();
                if (VM.isVeryVerbose()) {
                    VM.println("[regression: loading and unhibernating " + url + "]");
                }
                isolate = Isolate.load(url);
                isolate.unhibernate();

                // Let it run again
                runForSometime(isolate, delay, false);
                isolate.join();
            }
        } catch (Throwable t) {
            exception = t;
            exception.printStackTrace(out);
        }

        int exitCode = isolate.getExitCode();
        out.println("    exit code = " + exitCode + "(expected exit code = " + expectedExitCode + ")");
        out.println("    time = "  + (System.currentTimeMillis() - start) + "ms");
        if (exception != null) {
            exitCode = -1;
        }

        return exitCode == expectedExitCode;
    }

    boolean run(String mainClassName, String argsString, String classPath, final int delay, int expectedExitCode) {
        System.gc();
        long freeBefore = Runtime.getRuntime().freeMemory();
        boolean result = run0(mainClassName, argsString, classPath, delay, expectedExitCode);
        System.gc();
        long freeAfter = Runtime.getRuntime().freeMemory();
        if (freeAfter > freeBefore + 1000) {
            VM.println("******* Memory appears to have leaked after running " + mainClassName + " [before="+freeBefore+", after="+freeAfter+"] *******");
        }
        return result;
    }

    boolean run() {
        String samplesClassPath = ArgsUtilities.toPlatformPath("graphics/j2meclasses:samples/j2meclasses:.", true);
        boolean failure = false;

        failure = failure || !run("java.lang.Test",         "", ".",              0,                  12345);
        failure = failure || !run("example.mpeg.Main",      "", samplesClassPath, samplesExecuteTime, 0);
        failure = failure || !run("example.chess.Main",     "", samplesClassPath, samplesExecuteTime, 0);
        failure = failure || !run("example.shell.Main",     "", samplesClassPath, samplesExecuteTime, 0);
        failure = failure || !run("example.manyballs.Main", "", samplesClassPath, samplesExecuteTime, 0);
        failure = failure || !run("example.cubes.Main",     "", samplesClassPath, samplesExecuteTime, 0);

        if (runTCK) {
            failure = failure || !run("com.sun.squawk.tck.Main", "-o:"+prefix+"pos_tck    " + ArgsUtilities.toPlatformPath("@tck/positiveclasses.txt", false), ".", 0, 0);
            failure = failure || !run("com.sun.squawk.tck.Main", "-o:"+prefix+"neg_tck -n " + ArgsUtilities.toPlatformPath("@tck/negativeclasses.txt", false), ".", 0, 0);
        }

        out.close();
        return !failure;
    }

    private static void usage(String errMsg) {
        PrintStream out = System.err;
        if (errMsg != null) {
            out.println("** " + errMsg + " **\n");
        }
        out.println("Usage: com.sun.squawk.regression.Main [-options] ");
        out.println();
        out.println("where options include:");
        out.println("    -p:<name> prefix to use for output files (default=\"\")");
        out.println("    -tck      include TCK tests in regression");
        out.println("    -set:<n>  execute samples 'n' secs before & after hibernation (default=100)");
        out.println("    -h        display help message and exit");
        out.println();
    }

    public static void main(String[] args) {
        boolean passed = true;
        try {
            String prefix = "";
            int samplesExecuteTime = 100000; // 100s
            boolean runTCK = false;

            int argc = 0;
            while (argc != args.length) {
                String arg = args[argc++];
                if (arg.startsWith("-set:")) {
                    samplesExecuteTime = Integer.parseInt(arg.substring("-set:".length())) * 1000;
                } else if (arg.startsWith("-p:")) {
                    prefix = arg.substring("-p:".length());
                } else if (arg.equals("-h")) {
                    usage(null);
                    return;
                } else if (arg.equals("-tck")) {
                    runTCK = true;
                } else {
                    usage("Unknown option: " + arg);
                    System.exit(1);
                }
            }

            Main instance = new Main(prefix, samplesExecuteTime, runTCK);
            passed = instance.run();
        } catch (IOException ex) {
            ex.printStackTrace();
            passed = false;
        }
        System.exit(passed ? 0 : -1);
    }
}
