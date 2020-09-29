package com.sun.squawk.tck;

import java.io.*;
import java.util.*;
import javax.microedition.io.*;

import com.sun.squawk.util.ArgsUtilities;
import com.sun.squawk.util.Vector;

/**
 * TCK test harness.
 */
class Main {

    /**
     * These are the TCK tests expected to fail and have yet to be fixed/investigated.
     */
    private static final String[] EXPECTED_TCK_FAILURES = {

        // Positive tests
        "javasoft.sqe.tests.vm.instr.invokespecial.invokespecial008.invokespecial00801m1.invokespecial00801_wrapper",
        "javasoft.sqe.tests.vm.instr.lreturn.lreturn004.lreturn00401m1.lreturn00401m1_wrapper", // Has intermittent dup2 problem in rtrn2()
        "javasoft.sqe.tests.lang.clss149.clss14902.clss14902_wrapper",
        "javasoft.sqe.tests.lang.lex010.lex01019.lex01019_wrapper",
        "javasoft.sqe.tests.vm.classfmt.atr.atrcvl004.atrcvl00401m1.atrcvl00401m1_wrapper",
        "javasoft.sqe.tests.vm.classfmt.cpl.cplint001.cplint00101m1.cplint00101m1_wrapper",
        "javasoft.sqe.tests.vm.classfmt.cpl.cpllng001.cpllng00101m1.cpllng00101m1_wrapper",
        "javasoft.sqe.tests.vm.concepts.interfaces018.interfaces01801.interfaces01801_wrapper",
        "javasoft.sqe.tests.vm.overview.SpecInitMethods.SpecInitMethods004.SpecInitMethods00405m1.SpecInitMethods004_wrapper",

        // Negative tests.
        "javasoft.sqe.tests.lang.binc001.binc00101.binc00101_wrapper",
        "javasoft.sqe.tests.lang.binc047.binc04701.binc04701_wrapper",
        "javasoft.sqe.tests.lang.exec005.exec00503.exec00503_wrapper",
        "javasoft.sqe.tests.vm.classfmt.atr.atrexc004.atrexc00401m1.atrexc00401m1_wrapper",
        "javasoft.sqe.tests.vm.classfmt.atr.atrexc005.atrexc00501m1.atrexc00501m1_wrapper",
        "javasoft.sqe.tests.vm.classfmt.atr.atrinc201.atrinc20101m1_1.atrinc20101m1_1_wrapper",
        "javasoft.sqe.tests.vm.classfmt.atr.atrlnt201.atrlnt20101m1.atrlnt20101m1_wrapper",
        "javasoft.sqe.tests.vm.classfmt.cpl.cplmbr201.cplmbr20101m1.cplmbr20101m1_wrapper",
        "javasoft.sqe.tests.vm.classfmt.cpl.cplmbr201.cplmbr20102m1.cplmbr20102m1_wrapper",
        "javasoft.sqe.tests.vm.classfmt.cpl.cplmbr201.cplmbr20103m1.cplmbr20103m1_wrapper",
        "javasoft.sqe.tests.vm.classfmt.ins.instr_016.instr_01603m1.instr_01603m1_wrapper",
        "javasoft.sqe.tests.vm.classfmt.ins.instr_207.instr_20703m1.instr_20703m1_wrapper",
        "javasoft.sqe.tests.vm.classfmt.lmt.argnum001.argnum00101m1.argnum00101m1_wrapper",
        "javasoft.sqe.tests.vm.classfmt.vrf.c_pool201.c_pool20101m1.c_pool20101m1_wrapper",
        "javasoft.sqe.tests.vm.concepts.execution081.execution08106.execution08106_wrapper", // Intermittent?
        "javasoft.sqe.tests.vm.constantpool.resolveClass.resolveClass00704.resolveClass00704_wrapper",
        "javasoft.sqe.tests.vm.instr.anewarray.anewarray008.anewarray00802m1.anewarray00802m1_wrapper",
        "javasoft.sqe.tests.vm.instr.areturn.areturn002.areturn00204m1_1.areturn00204m1_1_wrapper",
        "javasoft.sqe.tests.vm.instr.invokeinterface.invokeinterface020.invokeinterface02001m1.invokeinterface020_wrapper",
        "javasoft.sqe.tests.vm.instr.invokeinterface.invokeinterface020.invokeinterface02002m1.invokeinterface020_wrapper",
        "javasoft.sqe.tests.vm.instr.invokeinterface.invokeinterface020.invokeinterface02003m1_1.invokeinterface020_wrapper",
        "javasoft.sqe.tests.vm.instr.invokeinterface.invokeinterface020.invokeinterface02004m1.invokeinterface020_wrapper",
        "javasoft.sqe.tests.vm.instr.multianewarray.multianewarray010.multianewarray01001m1.multianewarray0100_wrapper",
        "javasoft.sqe.tests.vm.instr.newX.new002.new00201m1.new00201m1_wrapper",
    };

    /**
     * These are classes not run because of errors in the VM cause intermittent results.
     */
    private static String[] testsNotToRun = {
        "javasoft.sqe.tests.vm.instr.lreturn.lreturn004.lreturn00401m1.lreturn00401m1_wrapper", // dup2 problem in rtrn2()
    };

    /**
     * The path to the TCK tests.
     */
    private String classPath = "tck" + VM.getFileSeparatorChar() + "tck.jar";

    /**
     * The prefix used for the log files.
     */
    private String prefix = "tck";

    /**
     * The URL of the print stream to which the stdout and stderr for each test is redirected.
     */
    String logURL;

    /**
     * The print stream to which the stdout and stderr for each test is redirected.
     */
    PrintStream log;

    /**
     * The print stream to which the passed tests are logged.
     */
    PrintStream passed;

    /**
     * The print stream to which the failed tests are logged.
     */
    PrintStream failed;

    /**
     * The collection of TCK tests. Each element in this vector is a string with one or
     * more tokens separated by whitespaces. The first token is the wrapper class for a
     * TCK tests (i.e. the class with the 'main' method that runs the test). Any remaining
     * tokens are command line parameters that will be passed to the 'main' method in the
     * wrapper class.
     */
    private Vector tests = new Vector();

    /**
     * Flags whether or not the set of tests being run are positive or negative TCK tests.
     * Positive tests are expected to exit the VM with an exit code of 95 and negative tests
     * are expected to exit the VM with an exit code of anything but 95.
     */
    private boolean isPositive = true;


    /**
     * Prints the usage message.
     *
     * @param  errMsg  an optional error message
     */
    private void usage(String errMsg) {
        PrintStream out = System.out;
        if (errMsg != null) {
            out.println(errMsg);
        }
        out.println("Usage: TCK [-options] tests...");
        out.println("where options include:");
        out.println();
        out.println("    -cp:<directories and jar/zip files separated by '" + VM.getPathSeparatorChar() + "'>");
        out.println("               paths where TCK classes and resources can be found (default='" + classPath + "')");
        out.println("    -o:<name>  prefix to use for log files (default='" + prefix + "')");
        out.println("    -p         specifies that the tests are positive (default)");
        out.println("    -n         specifies that the tests are negative");
        out.println();
    }

    /**
     * Parses the command line arguments.
     *
     * @param args  the command line arguments
     * @return true if the arguments were well formed and this TCK harness is now ready for execution.
     */
    boolean parseArgs(String[] args) throws IOException {
        int argc = 0;
        while (argc != args.length) {
            String arg = args[argc];
            if (arg.charAt(0) != '-') {
                break;
            } else if (arg.startsWith("-cp:")) {
                classPath = arg.substring("-cp:".length());
            } else if (arg.startsWith("-o:")) {
                prefix = arg.substring("-o:".length());
            } else if (arg.equals("-p")) {
                isPositive = true;
            } else if (arg.equals("-n")) {
                isPositive = false;
            } else {
                usage("Invalid option: " + arg);
                return false;
            }

            ++argc;
        }

        if (argc == args.length) {
            usage("no TCK tests specified");
            return false;
        }

        while (argc != args.length) {
            String arg = args[argc++];
            ArgsUtilities.processClassArg(arg, tests);
        }

        // Initialize the logging streams
        logURL = "file://" + prefix + ".output.log;append=true";
        log = new PrintStream(Connector.openOutputStream(logURL));
        passed = new PrintStream(Connector.openOutputStream("file://" + prefix + ".passed.log"));
        failed = new PrintStream(Connector.openOutputStream("file://" + prefix + ".failed.log"));

        return true;
    }

    /**
     * Creates the suite containing all the classes in "tck/agent.jar". These classes
     * provide the framework for running a TCK test. Upon returning, the suite will
     * be accessible via the URL "File://agent.suite".
     */
    private void createAgentSuite() {
        String agentClassPath = "tck"+VM.getFileSeparatorChar()+"agent.jar";
        Isolate isolate = new Isolate("java.lang.SuiteCreator", new String[] { "-prune:l", prefix + ".agent" }, agentClassPath, null);
        isolate.start();
        isolate.join();
        if (isolate.getExitCode() != 0) {
            throw new RuntimeException("SuiteCreator exited with exit code " + isolate.getExitCode());
        }
    }

    /**
     * Test to see if a TCK class is expected to fail.
     *
     * @param mainClassName the main class of the test
     * @return true if it should fail
     */
    private static boolean shouldFail(String mainClassName, String[] testsNotToRun) {
        for (int i = 0 ; i < testsNotToRun.length ; i++) {
            if (testsNotToRun[i].equals(mainClassName)) {
                return true;
            }
        }
        return false;
    }

    private static final String[] NO_ARGS = {};

    /**
     * Runs a single TCK test.
     *
     * @param test   the main class and arguments of the test to run
     * @param testNo the number of the test
     * @return true if the test passed, false otherwise
     */
    boolean runOneTest(String test, int testNo) {
    String[] nameAndArgs = ArgsUtilities.cut(test);
        String[] args = NO_ARGS;
        String mainClassName = nameAndArgs[0];

        if (nameAndArgs.length != 1) {
            args = new String[nameAndArgs.length - 1];
            System.arraycopy(nameAndArgs, 1, args, 0, args.length);
        }

        log.println(testNo + "/" + tests.size() + ": " + test);

        /*
         * Run the TCK test in its own isolate.
         */
        Isolate isolate = new Isolate(mainClassName, args, classPath, "file://" +prefix + ".agent.suite");
        isolate.setProperty("java.lang.System.out", logURL + ";append=true");
        isolate.setProperty("java.lang.System.err", logURL + ";append=true");

        isolate.start();
        isolate.join();

        int exitCode = isolate.getExitCode();
        boolean pass = isPositive ? exitCode == 95 : exitCode != 95;

        if (pass) {
            passed.println(mainClassName);
        } else {
            failed.println(mainClassName);
        }

        log.flush();
        passed.flush();
        failed.flush();

        boolean shouldFail = shouldFail(mainClassName, EXPECTED_TCK_FAILURES);

        if (!pass && shouldFail) {
            return true;
        }

        if (pass && shouldFail) {
            log.println(mainClassName + " Was expected to fail");
        }

        return pass;
    }

    /**
     * Runs the set of TCK tests, logging the results to the appropriate log streams.
     *
     * @return the number of tests that failed
     * @throws IOException if there was an IO error
     */
    int run() throws IOException {

        int failedCount = 0;
        int testNo = 1;
        long start = System.currentTimeMillis();
        for (Enumeration e = tests.elements(); e.hasMoreElements(); ) {
            String test = (String)e.nextElement();
            if (!runOneTest(test, testNo++)) {
                ++failedCount;
            }
        }
        log.println("Total time: " + (System.currentTimeMillis() - start) + "ms");

        log.close();
        passed.close();
        failed.close();

        return failedCount;
    }

    /**
     * Entry point for running the TCK tests. This method always exits via {@link System.exit(int)}.
     * If no tests fail, then the exit code is 0. If one or more tests fail, the exit code
     * is the number of tests that fail. Any other error produces a negative exit code.
     *
     * @param args   comman line arguments as detailed by {@link usage(String)}.
     * @throws IOException if an IO error occurs
     */
    public static void main(String[] args) {
        Main tck = new Main();
        try {
            if (tck.parseArgs(args)) {
                tck.createAgentSuite();
                System.exit(tck.run());
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
        System.exit(-1);
    }
}

