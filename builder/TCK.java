/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */

import java.util.*;
import java.io.*;

public abstract class TCK {

    private static final String DEFAULT_TCK_LOG_DIR               = "tck"+File.separator+"log";
    private static final String DEFAULT_ROMIZETCK_LOG_FILE_PREFIX = "romizetck";
    private static final String DEFAULT_RUNTCK_LOG_FILE_PREFIX    = "runtck";

    /**
     * This class encapsulates a single TCK test.
     */
    static final class Test implements Comparable {
        final String harnessClass;
        final String[] dependencies;
        final boolean isPositive;
        final String args;
        final String skipped;
        final int index;

        /**
         * Create a new test.
         * @param harnessClass
         * @param isPositive
         * @param dependencies
         * @param args
         */
        Test(String harnessClass, boolean isPositive, String[] dependencies, String args, String skipped, int index) {
            this.harnessClass = harnessClass;
            this.args         = args;
            this.dependencies = dependencies;
            this.isPositive   = isPositive;
            this.skipped      = skipped;
            this.index        = index;
        }

        public int compareTo(Object o) {
            int index = ((Test)o).index;
            if (this.index < index) {
                return -1;
            } else if (this.index > index) {
                return 1;
            } else {
                return 0;
            }
        }

        public boolean equals(Object o) {
            return index == ((Test)o).index;
        }

        /**
         * Return the name of the harness class as the string representation
         * of this test.
         * @return
         */
        public String toString() {
            return harnessClass;
        }

        /**
         * Serialize this test as XML to a specified output stream.
         * @param out
         * @param prefix
         */
        public void toXML(PrintStream out, String prefix) {
            out.println(prefix+"<test>");
            out.println(prefix+"  <name>"+harnessClass+"</name>");
            if (args != null && args.length() > 0) {
                out.println(prefix + "  <args>" + args + "</args>");
            }
            if (isPositive) {
                out.println(prefix+"  <positive/>");
            } else {
                out.println(prefix+"  <negative/>");
            }
            if (skipped != null) {
                out.println(prefix + "  <skipped>"+skipped+"</skipped>");
            }
            out.println(prefix+"  <dependencies>");
            for (int i = 0; i != dependencies.length; i++) {
                out.println(prefix+"    <dependency>"+dependencies[i]+"</dependency>");
            }
            out.println(prefix+"  </dependencies>");
            out.println(prefix+"</test>");
        }

        public void dump(PrintStream out) {
            out.println(harnessClass);
            for (int i = 0; i != dependencies.length; i++) {
                out.println(dependencies[i]);
            }
        }
    }

    public Test getTest(String harnessClassName) {
        Test[] tests = getTests();
        for (int i = 0; i != tests.length; i++) {
            Test test = tests[i];
            if (test.harnessClass.equals(harnessClassName)) {
                return test;
            }
        }
        return null;
    }

    /**
     * Get the array of objects representing the TCK tests.
     * @return the array of objects representing the TCK tests.
     */
    public Test[] getTests() {
        init();
        return tests;
    }

    public Test getTestForName(String name) {
        Test[] tests = getTests();
        for (int i = 0; i != tests.length; i++) {
            if (tests[i].harnessClass.equals(name)) {
                return tests[i];
            }
        }
        return null;
    }

    public void toXML(PrintStream out, boolean pos, boolean neg, boolean skipped) {
        Test[] tests = getTests();
        out.println("<tck>");
        out.println("  <name>"+getName()+"</name>");
        out.println("  <tests>");
        for (int i = 0; i != tests.length; ++i) {
            Test test = tests[i];
            if ((pos && test.isPositive) ||
                (neg && !test.isPositive) ||
                (skipped && test.skipped != null)) {
                tests[i].toXML(out, "    ");
            }
        }
        out.println("  </tests>");
        out.println("</tck>");
    }

    public void dump(PrintStream out, boolean pos, boolean neg, boolean skipped) {
        Test[] tests = getTests();
        for (int i = 0; i != tests.length; ++i) {
            Test test = tests[i];
            if ((pos && test.isPositive) ||
                (neg && !test.isPositive) ||
                (skipped && test.skipped != null)) {
                tests[i].dump(out);
            }
        }
    }

    public abstract String getName();

    /*---------------------------------------------------------------------------*\
     *                Load                                                       *
    \*---------------------------------------------------------------------------*/

    Test[] tests;

    /**
     * Load the tests from TCK.xml file.
     */
    private void init() {
        if (tests == null) {
            load();
        }
    }

    private void load() {
        Class thisClass = getClass();
        String xmlFileName = thisClass.getName()+".xml";
        InputStream in = thisClass.getResourceAsStream(xmlFileName);
        if (in == null) {
            throw new RuntimeException("Can't find "+xmlFileName);
        }

        try {
            Tag tag = Tag.create(in);

            tag.checkName("tck");
            Tag content = tag.getContent();

            getTagContents(content, "name");
            content = content.getNext();
            content = parseTests(content);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe.getMessage());
        }
    }

    private Tag parseTests(Tag tag) {
        tag.checkName("tests");
        tag = tag.getContent();
        int count = Tag.countTags(tag, "test");
        tests = new Test[count];
        for (int i = 0; i != count; i++) {
            tests[i] = parseTest(tag.getContent(), i);
            tag = tag.getNext();
        }
        return tag;
    }

    private Test parseTest(Tag tag, int index) {

        String name;
        String args;
        boolean isPositive;
        String skipped;
        String[] dependencies;

        /*
         * Parse the test's name (i.e. the name of its harness class)
         */
        name = getTagContents(tag, "name");
        tag = tag.getNext();

        /*
         * Parse the args of the test (if any)
         */
        if (tag != null && tag.isName("args")) {
            args = getTagContents(tag, null);
            tag = tag.getNext();
        } else {
            args = "";
        }

        /*
         * Parse the <positive/> or <negative/> tag
         */
        if (tag != null && (tag.isName("positive") || tag.isName("negative"))) {
            isPositive = tag.isName("positive");
            tag = tag.getNext();
        } else {
            isPositive = true;
        }

        /*
         * Parse the <skipped> tag
         */
        if (tag != null && tag.isName("skipped")) {
            skipped = getTagContents(tag, null);
            tag = tag.getNext();
        } else {
            skipped = null;
        }

        if (tag != null && tag.isName("dependencies")) {
            Tag list = tag.getContent();
            int count = Tag.countTags(list, "dependency");
            dependencies = new String[count];
            for (int i = 0; i != count; i++) {
                dependencies[i] = getTagContents(list, null);
                list = list.getNext();
            }
            tag = tag.getNext();
        } else {
            dependencies = new String[0];
        }

        return new Test(name, isPositive, dependencies, args, skipped, index);
    }

    private String getTagContents(Tag tag, String tagName) {
        if (tagName != null) {
            tag.checkName(tagName);
        }
        Tag cont = tag.getContent();
        if (cont == null) {
            return "";
        }
        return Tag.unescape(cont.getData());
    }

    /*---------------------------------------------------------------------------*\
     *                Translate and romize TCK                                   *
    \*---------------------------------------------------------------------------*/

    public void romizetck(String[] args, Build builder) {
        new RomizeTCK(args, builder);
    }

    class RomizeTCK {

        private static final int SUITES_PER_IMAGE = 10;

        final class Suite {
            Suite(String name, Vector tests) {
                this.name  = name;
                this.tests = tests;
            }
            String name;
            Vector tests;
            public String toString() {
                return name;
            }
        }

        /**
         * This class encapsulates the translation and romization of a single
         * VM image containing one or more TCK tests. It extends
         * <code>Thread</code> which enables images to be romized in parallel.
         */
        class Execution extends Thread {

            /**
             * The suites in the image.
             */
            final Vector suites;

            Execution(Vector suites) {
                this.suites = suites;
            }

            public void run() {
                try {
                    run0();
                } finally {
                    endExecution(this);
                }
            }

            public void run0() {

                String first = ((Suite)suites.firstElement()).name;
                String last = ((Suite)suites.lastElement()).name;

                String base = first.substring(0, first.lastIndexOf('_')) +
                    last.substring(last.lastIndexOf('_'));

                String suiteFileName = builder.fix("temp/" + base + ".bin");
                String imageFileName = builder.fix("temp/" + base + ".image");
                String mapFileName = builder.fix("temp/" + base + ".map");
                builder.delete(suiteFileName);
                boolean successfulTranslation = true;
                Properties  thisProps = new Properties();


                for (Enumeration e = suites.elements(); e.hasMoreElements(); ) {
                    Suite suite = (Suite)e.nextElement();
                    if (translateSuite(suite.name,
                                       suiteFileName,
                                       j2meSuiteFileName +
                                       (commonSuiteFileName.length() == 0 ? "" : ":"+commonSuiteFileName)))
                    {
                        /*
                         * Set the command lines to run the tests in the suite
                         */
                        for (Enumeration ee = suite.tests.elements();
                             ee.hasMoreElements(); ) {
                            Test test = (Test)ee.nextElement();
                            thisProps.setProperty(test.harnessClass, imageFileName);
                        }
                    } else {
                        successfulTranslation = false;
                    }

                }

                if (successfulTranslation) {
                    try {
                        builder.stdout().println("*** Romizing suites into image "+imageFileName+" ...");
                        builder.run(builder.cut("romize " +
                                                "-tck " +
                                                "-ram_sz 1024K " +
                                                "-format bin " +
                                                "-growrom " +
                                                "-m " + mapFileName + " " +
                                                "-image " + imageFileName + " " +
                                                j2meSuiteFileName + " " +
                                                commonSuiteFileName + " " +
                                                suiteFileName));

                        runProperties.putAll(thisProps);
                    } catch (Exception ex) {
                        logFailure("Error romizing suites in "+suiteFileName, ex, outputPS);
                        logFailure("Error romizing suites in "+suiteFileName, ex, failurePS);
                    }
                }
            }
        }

        /**
         * The collection of image romizations that have not yet completed.
         */
        final private Vector incompleteExecutions = new Vector();

        /**
         * The name of the file containing the core J2ME suite.
         */
        final String j2meSuiteFileName;

        /**
         * The name of the file containing the classes shared by at least
         * two TCK tests.
         */
        final String commonSuiteFileName;

        /**
         * The print stream for console output.
         */
        final PrintStream outputPS;

        /**
         * The print stream for failure output.
         */
        final PrintStream failurePS;

        /**
         * The properties object for running the romized tests.
         */
        final Properties runProperties = new Properties();

        /**
         * The number of processors available on the underlying hardware.
         */
        int processorCount = 1;

        /**
         * Log the start of a test execution. This method blocks until there is
         * (most likely) a processor free.
         *
         * @param execution
         */
        void startExecution(Execution execution) {
            while (incompleteExecutions.size() >= processorCount) {
                Thread.yield();
            }
            incompleteExecutions.addElement(execution);
        }

        /**
         * Log the completion of a test execution.
         *
         * @param execution
         */
        void endExecution(Execution execution) {
            incompleteExecutions.remove(execution);
        }


        /**
         * Get the argument to a command line option. If the argument is not provided,
         * then a usage message is printed and the system exits.
         *
         * @param args The command line arguments.
         * @param index The index at which the option's argument is located
         * @param opt The name of the option.
         * @return the options argument.
         */
        private String getOptArg(String[] args, int index, String opt) {
            if (index >= args.length) {
                usage("The " + opt + " option requires an argument.");
                System.exit(1);
            }
            return args[index];
        }

        /**
         * Displays the usage message for romizing the TCK tests.
         *
         * @param  errMsg  an optional error message to display
         */
        private void usage(String errMsg) {
            PrintStream out = System.err;
            if (errMsg != null) {
                out.println(errMsg);
            }
            String dir = DEFAULT_TCK_LOG_DIR;
            String romizePrefix = DEFAULT_ROMIZETCK_LOG_FILE_PREFIX;
            out.println("usage: romizetck [-options] [[ <first> [ <last> ]] | <test> | @<tests file>]");
            out.println("    Romize the range of TCK tests between <first> and <last> or ");
            out.println("    the single test <test> or the set of tests in <test file>.");
            out.println("where options include:");
            out.println("    -d <dir>       output directory for generated files (default: "+dir+")");
            out.println("    -p <prefix>    prefix to use for generated files (default: "+romizePrefix+")");
            out.println("    -o <file>      redirect output to <file> (default: <prefix>.output)");
            out.println("    -forcej2me     force recompilation and translation of core suite");
            out.println("    -exclPos       exclude positive tests");
            out.println("    -exclNeg       exclude negative tests");
            out.println("    -excl <spec>   use <spec> to exclude test(s) where spec is either");
            out.println("                   a single test name or a filename (with '@' prefix) of tests");
            out.println("    -np            the number of available processors");
            out.println("    -h             display this usage message and exit");
            out.println();
            out.println("Note: the files generated during romizing are:");
            out.println("    <prefix>.properties:  properties file for 'runtck'");
            out.println("    <prefix>.output:      output of romizing");
            out.println("    <prefix>.failed:      tests in a suite that failed to translate/romize");
            out.println("    <prefix>.skipped:     skipped tests");
        }

        /**
         * Logs a failure message to a print stream.
         *
         * @param   msg  the failure message
         * @param   ex   an exception whose stack trace will be logged if it's not null
         * @param   ps   the print stream to log to
         */
        private void logFailure(String msg, Exception ex, PrintStream ps) {
            ps.println(msg);
            if (ex != null) {
                ex.printStackTrace(ps);
            }
        }

        /**
         * Translate a set of class files into a suite.
         *
         * @param   suite          the name of the suite driver class
         * @param   suiteFileName  the file into which the suite should be written
         * @param   libs           the libraries to which the suite binds
         * @param   stdout         the console print stream
         * @param   failedPS       the print stream for failure messages
         * @return
         */
        private boolean translateSuite(String suite,
                                       String suiteFileName,
                                       String libs) {
            builder.stdout().println("*** Translating suite '"+suite+"' ...");
            try {
                String classpath;
                String linkageErrorsFlag;
                String outputFlag;
                boolean isSuite0 = (libs == null || libs.length() == 0);
                if (isSuite0) {
                    classpath = "j2me/classes";
                    linkageErrorsFlag = "";
                    outputFlag = "-o ";
                } else {
                    classpath = "tck/classes:tck/tck.jar";
                    linkageErrorsFlag = "-linkageerrors:pass ";
                    outputFlag = "-a ";
                }

                builder.run(builder.cut("translate " +
                    (libs == null ? "" : "-libs "+libs+" ") +
                    "-cp "+classpath+" " +
                    "-format bin " +
                    linkageErrorsFlag +
                    "-O " +
                    outputFlag + suiteFileName +
                    " @"+suite));
                return true;
            } catch (Exception ex) {
                String msg = "Error translating suite '"+suite+"'";
                logFailure(msg, ex, outputPS);
                logFailure(msg, ex, failurePS);
                return false;
            }
        }

        private Vector getLinesInFile(String fileName) throws IOException {
            Vector result = new Vector();
            BufferedReader br = new BufferedReader(new FileReader(fileName));
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (!line.startsWith("#")) {
                    result.addElement(line);
                }
            }
            return result;
        }

        /**
         * Parse the tail of a given array of command line args for a
         * specification of the tests to run.
         *
         * @param  args         an array of command line arguments
         * @param  argp         the index at which to start parsing the args
         * @param  skippedTests table to log skipped tests and the reason for skipping
         * @return the tests to romize
         */
        private SortedSet parseTestsSpec(String[] args, int argp,
                                         Hashtable skippedTests)
        {
            SortedSet tests;
            if (argp < args.length) {

                String arg = args[argp++];
                char ch = arg.charAt(0);

                if (!Character.isDigit(ch)) {
                    tests = new TreeSet();
                    if (ch == '@') {
                        try {
                            String fileName = arg.substring(1);
                            Vector lines = getLinesInFile(fileName);
                            for (Enumeration e = lines.elements(); e.hasMoreElements(); ) {
                                String name = ((String)e.nextElement()).trim();
                                int index = 0;
                                while (index != name.length() &&
                                       (name.charAt(index) == '.' ||
                                        Character.isJavaIdentifierPart(name.charAt(index)))) {
                                    index++;
                                }
                                if (index != 0) {
                                    name = name.substring(0, index);
                                    Test test = getTestForName(name);
                                    if (test != null) {
                                        if (test.skipped == null) {
                                            tests.add(test);
                                        } else {
                                            skippedTests.put(test.harnessClass, test.skipped);
                                        }
                                    } else {
                                        System.err.println("test not found: "+name);
                                    }
                                } else {
                                    System.err.println("ignoring badly formatted test line in "+fileName+": "+name);
                                }
                            }
                        } catch (IOException ex) {
                            System.err.println("Error reading tests file: ");
                            ex.printStackTrace();
                            throw new CommandFailedException("romizetck failed", 1);
                        }
                    } else {
                        Test test = getTestForName(arg);
                        if (test == null) {
                            System.err.println("Test not found: "+arg);
                            throw new CommandFailedException("romizetck failed", 1);
                        }
                        tests.add(test);
                    }
                } else {
                    int beginIndex;
                    int endIndex = getTests().length;
                    try {
                        beginIndex = Integer.parseInt(arg) - 1;
                        if (argp < args.length) {
                            int val = Integer.parseInt(args[argp++]);
                            if (val > beginIndex) {
                                endIndex = val;
                            }
                        }
                    } catch (NumberFormatException nfe) {
                        usage("Error in begin or end test index");
                        throw new CommandFailedException("romizetck failed", 1);
                    }
                    tests = getTestsWithinRange(beginIndex, endIndex, skippedTests);
                }
            } else {
                tests = getTestsWithinRange(0, getTests().length, skippedTests);
            }
            return tests;
        }

        /**
         * This is the interface by which the TCK is (partially) translated and
         * romized.
         *
         * @param  args
         * @param  builder
         */
        RomizeTCK(String[] args, Build builder) {
            this.builder = builder;
            File logDir = new File(DEFAULT_TCK_LOG_DIR);
            String logFilePrefix = DEFAULT_ROMIZETCK_LOG_FILE_PREFIX;
            boolean exclPos = false;
            boolean exclNeg = false;
            Set exclTests = new HashSet();
            String output = null;
            boolean forcej2me = false;

            int argp = 0;
            while (argp != args.length) {
                String arg = args[argp];
                if (arg.charAt(0) == '-') {
                    if (arg.equals("-d")) {
                        logDir = new File(getOptArg(args, ++argp, "-d"));
                    } else if (arg.equals("-p")) {
                        logFilePrefix = getOptArg(args, ++argp, "-p");
                    } else if (arg.equals("-o")) {
                        output = getOptArg(args, ++argp, "-o");
                    } else if (arg.equals("-forcej2me")) {
                        forcej2me = true;
                    } else if (arg.equals("-np")) {
                        processorCount = Integer.parseInt(getOptArg(args, ++argp, "-d"));
                        if (processorCount == 0) {
                            processorCount = 1;
                        }
                    } else if (arg.equals("-exclPos")) {
                        exclPos = true;
                    } else if (arg.equals("-exclNeg")) {
                        exclNeg = true;
                    } else if (arg.equals("-excl")) {
                        String spec = getOptArg(args, ++argp, "-excl");
                        if (spec.charAt(0) == '@') {
                            try {
                                Vector lines = getLinesInFile(spec.substring(1));
                                exclTests.addAll(lines);
                            } catch (IOException ex) {
                                System.err.println("Error reading exclude spec file: ");
                                ex.printStackTrace();
                                throw new CommandFailedException("romizetck failed", 1);
                            }
                        } else {
                            exclTests.add(spec);
                        }
                    } else if (arg.equals("-h")) {
                        usage(null);
                        throw new CommandFailedException("romizetck failed", 1);
                    } else {
                        System.err.println("Unknown option ignored: "+arg);
                    }
                } else {
                    break;
                }
                argp++;
            }

            Hashtable skippedTests = new Hashtable();
            SortedSet tests = parseTestsSpec(args, argp, skippedTests);

            Build.ensureDirExists(logDir.getPath());

            if (output == null) {
                output = logFilePrefix + ".output";
            }
            String failed   = logFilePrefix + ".failed";
            String skipped  = logFilePrefix + ".skipped";
            String propFile = logFilePrefix + ".properties";

            outputPS  = openPrintStreamToFile(logDir, output, System.out);
            failurePS  = openPrintStreamToFile(logDir, failed, System.out);

            PrintStream skippedPS = openPrintStreamToFile(logDir, skipped, System.out);
            PrintStream savedOut  = builder.setOut(outputPS);
            PrintStream savedErr  = builder.setErr(failurePS);

            try {

                j2meSuiteFileName = "temp/j2me_tck.bin";
                if (forcej2me || !(new File(j2meSuiteFileName)).exists()) {
                    /*
                     * Compile the core API classes if the required suite file is missing
                     */
                    builder.stdout().println("*** Compiling J2ME core suite ...");
                    try {
                        builder.run(builder.cut("j2me"));
                        builder.run(builder.cut("tck"));
                    } catch (Exception ioe) {
                        logFailure("Error compiling core API classes", ioe, outputPS);
                        logFailure("Error compiling core API classes", ioe, failurePS);
                        throw new CommandFailedException("romizetck failed", 1);
                    }

                    /*
                     * Translate the j2me core suite along with java.lang.TckRunner
                     */
                    builder.delete(j2meSuiteFileName);
                    if (!translateSuite("j2me", j2meSuiteFileName, null)) {
                        throw new CommandFailedException("romizetck failed", 1);
                    }
                }

                /*
                 * Translate and romize the suites in each group into an image
                 */
                Vector suites = generateSuites(tests, skippedTests, "tck", exclPos,
                                               exclNeg, exclTests, skippedPS, failurePS);

                /*
                 * Remove the common suite
                 */
                Suite commonSuite = (Suite)suites.firstElement();
                if (commonSuite.name.equals("tcktests_common")) {
                    suites.remove(commonSuite);
                    commonSuiteFileName = "temp/tcktests_common.bin";
                    builder.delete(commonSuiteFileName);
                    if (!translateSuite("tcktests_common", commonSuiteFileName, j2meSuiteFileName)) {
                        return;
                    }
                } else {
                    commonSuite = null;
                    commonSuiteFileName = "";
                }

                Vector[] suiteGroups = partition(suites, SUITES_PER_IMAGE);
                for (int i = 0; i != suiteGroups.length; i++) {
                    suites = suiteGroups[i];

                    /*
                     * Translate and romize the suite group into an image
                     */
                    Execution execution = new Execution(suites);
                    startExecution(execution);
                    execution.start();
                }

                /*
                 * Wait for all threads to finish
                 */
                while (!incompleteExecutions.isEmpty()) {
                    Thread.yield();
                }

                /*
                 * Write the run properties to a file
                 */
                OutputStream propOS;
                try {
                    propOS = new FileOutputStream(new File(logDir, propFile));
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                    propOS = System.out;
                }

                try {
                    runProperties.store(propOS, "TCK run command lines");
                } catch (IOException ex) {
                    builder.stdout().println("Error writing out "+propFile);
                    ex.printStackTrace(builder.stdout());
                }

            } finally {
                if (outputPS != savedOut && outputPS != savedErr) {
                    outputPS.close();
                }
                builder.setOut(savedOut);
                builder.setErr(savedErr);
            }
        }

        Build builder;


        private Vector[] partition(Vector v, int partitionSize) {
            int partitionCount = ((v.size()-1) / partitionSize)+1;
            Vector[] partitions = new Vector[partitionCount];

            int i = 0;
            Vector partition = partitions[i] = new Vector(partitionSize);
            for (Enumeration e = v.elements(); e.hasMoreElements(); ) {
                partition.addElement(e.nextElement());
                if (partition.size() == partitionSize && e.hasMoreElements()) {
                    ++i;
                    partition = partitions[i] = new Vector(partitionSize);
                }
            }
            return partitions;
        }

        /**
         * Get a subset of the TCK tests. The returned vector contains a TCK.Test
         * object for every test in the range that is not skipped and a null
         * for every test that is skipped. The names of skipped tests are inserted
         * into the given 'skipped' hash table along with the reason they were
         * skipped.
         * @param beginIndex The beginning index, inclusive.
         * @param endIndex The end index, exclusive. If this is less than or equal
         * to beginIndex, then it is adjusted to be one greater than beginIndex so
         * that only the test at beginIndex is returned.
         * @param skipped
         * @return
         */
        private SortedSet getTestsWithinRange(int beginIndex, int endIndex, Hashtable skipped) {

            TCK.Test[] tests = getTests();

            SortedSet result = new TreeSet();

            if (endIndex <= beginIndex) {
                endIndex = beginIndex + 1;
            }

            /*
             * Build the set of tests for the given century
             */
            for (int i = beginIndex; i < endIndex; ++i) {
                TCK.Test test = tests[i];
                if (test.index != i) {
                    throw new RuntimeException();
                }
                if (test.skipped != null) {
                    skipped.put(test, test.skipped);
                } else {
                    result.add(test);
                }
            }
            return result;
        }


        /**
         * Get the set of common classes from the dependencies of a specified
         * set of TCK tests.
         * @return
         */
        private Set getCommonDependencies(SortedSet tests) {
            Set dependencies = new HashSet();
            Set common = new HashSet();
            for (Iterator iter = tests.iterator(); iter.hasNext(); ) {
                Test test = (Test)iter.next();
                for (int i = 0; i != test.dependencies.length; ++i) {
                    String dependency = test.dependencies[i];
                    boolean exists = !dependencies.add(dependency);
                    if (exists) {
                        common.add(dependency);
                    }
                }
            }
            return common;
        }

        /**
         * Create a Java source file representing the driver for a suite.
         * @param path
         * @param name
         * @param classes
         * @param dependencies
         * @param flags
         * @return
         * @throws Exception
         */
        private File createSuiteDriverSourceFile(String path, String name, Iterator classes, String flags, PrintStream failedPS) {
            File file = new File(builder.fix(path+"/"+name+".java"));
            PrintStream out;
            try {
                out = new PrintStream(new FileOutputStream(file));
            } catch (IOException ioe) {
                logFailure("Error opening driver file: "+file, ioe, builder.stdout());
                logFailure("Error opening driver file: "+file, ioe, failedPS);
                return null;
            }
            out.println("public class "+name+" {");
            out.println("    private static final String __SUITE_CLASSES__ =");

            while (classes.hasNext()) {
                name = (String)classes.next();
                name = name.replace('.', '/');
                out.print("        \""+name+" \"");
                if (classes.hasNext()) {
                    out.println(" +");
                } else {
                    out.println(";");
                }
            }

            if (flags != null && flags.length() > 0) {
                out.println("    private static final String __SUITE_FLAGS__ = \""+flags+"\";");
            }

            out.println('}');

            out.close();
            return file;
        }

        /**
         * Partition a range of the TCK into suites and generate suite driver classes
         * for these suites.
         * @param beginIndex The beginning index of the TCK test range, inclusive
         * @param endIndex   The end index of the TCK test range, exclusive
         * @param files the (relative) path names of generated files are written
         * into this vector.
         * @return the generated suites or null if there was an error
         */
        public Vector generateSuites(SortedSet tests,
                                     Hashtable skipped,
                                     String dir,
                                     boolean exclPos,
                                     boolean exclNeg,
                                     Set exclTests,
                                     PrintStream skippedPS,
                                     PrintStream failedPS) {

            String genDir = builder.fix(dir + "/gen");

            Vector suites = new Vector();

            /*
             * Filter out excluded tests
             */
            if (exclPos || exclNeg || !exclTests.isEmpty()) {
                for (Iterator i = tests.iterator(); i.hasNext();) {
                    Test test = (Test)i.next();
                    if (((exclPos && test.isPositive)  ||
                         (exclNeg && (!test.isPositive || test.skipped != null)) ||
                         (exclTests.contains(test.harnessClass)) )) {
                        i.remove();
                        skipped.remove(test.harnessClass);
                    }
                }
            }

            /*
             * Print out the skipped tests
             */
            for (Iterator i = skipped.entrySet().iterator(); i.hasNext(); ) {
                Map.Entry entry = (Map.Entry)i.next();
                skippedPS.println(entry.getKey()+" : "+entry.getValue());
            }

            Set common = getCommonDependencies(tests);

            /*
             * Create suite driver files
             */
            builder.stdout().println("Generating suites...");

            /*
             * Delete any generated files from a previous run of romizetck
             */
            try {
                builder.clean(new File("tck/gen"), ".java");
            } catch (Exception ex) {
            }

            if (common.size() > 0) {
                createSuiteDriverSourceFile(genDir, "tcktests_common", common.iterator(), "reflective", failedPS);
                suites.addElement(new Suite("tcktests_common", new Vector()));
            }

            Suite suite = null;
            Vector classes    = new Vector();
            for (Iterator iter = tests.iterator(); iter.hasNext();) {
                Test test = (Test)iter.next();
                classes.addElement(test.harnessClass);

                if (suite == null) {
                    suite = new Suite("tcktests_"+test.index+"_", new Vector());
                }

                suite.tests.addElement(test);

                for (int i = 0; i != test.dependencies.length; ++i) {
                    String dependency = test.dependencies[i];
                    if (!common.contains(dependency)) {
                        classes.addElement(dependency);
                    }
                }

                /*
                 * Create a new suite if the number of classes is greater than 100 or this is the last test
                 */
                if (classes.size() > 100 || !iter.hasNext()) {
                    /*
                     * Complete the suite name
                     */
                    suite.name += test.index;

                    /*
                     * Create the Java source for the suite's driver.
                     */
                    createSuiteDriverSourceFile(genDir, suite.name, classes.iterator(), "reflective", failedPS);

                    /*
                     * Add the name of the suite to set of generated suites
                     */
                    suites.addElement(suite);

                    if (iter.hasNext()) {
                        /*
                         * Reset the classes set and start the next suite
                         */
                        classes.clear();
                        suite = null;
                    }
                }
            }

            /*\
             * Compile the driver classes
             */
            builder.stdout().println("Compiling "+suites.size()+" generated suite drivers...");
            try {
                builder.javac_j2me("j2me/classes", "tck", builder.find(genDir, ".java"));
            } catch (Exception ioe) {
                logFailure("Error compiling suite drivers", ioe, builder.stdout());
                logFailure("Error compiling suite drivers", ioe, failedPS);
            }

            /*
             * Delete the generated source files
             */
            if (!builder.verbose) {
                try {
                    builder.clean(new File("tck/gen"), ".java");
                } catch (Exception ex) {
                }
            }

            return suites;
        }
    }

    /*---------------------------------------------------------------------------*\
     *                                 Run TCK                                   *
    \*---------------------------------------------------------------------------*/

    public void runtck(String[] args) {
        RunTCK driver = new RunTCK(args);
        driver.run();
    }

    class RunTCK {

        /**
         * This class encapsulates one execution of a TCK test. It extends
         * <code>Thread</code> which enables many tests to be run in parallel.
         */
        class Execution extends Thread {

            /**
             * The TCK test to run.
             */
            final Test test;

            /**
             * The index of this execution within an ordered set of executions.
             */
            final int index;

            /**
             * The number of tests being executed.
             */
            final int executionCount;

            /**
             * The VM heap image containing the romized test.
             */
            final String image;

            /**
             * The exit value returned by the subprocess once it completes.
             */
            int exitValue;

            /**
             * The console output generated during execution.
             */
            String consoleOutput;

            /**
             * Creates a new <code>Execution</code> instance to run a single
             * TCK test.
             *
             * @param   test       TCK test to run
             * @param   cmdPrefix  subprocess command line prefix
             * @param   envp       environment to pass to the subprocess
             * @param   index      index of this execution
             * @param   executionCount the total number of test executions
             * @param   image      the romized image containing this test
             */
            Execution(Test test, int index, int executionCount, String image) {
                this.test = test;
                this.index = index;
                this.image = image;
                this.executionCount = executionCount;
            }

            /**
             * Runs the test.
             */
            public void run() {
                try {
                    run0();
                } finally {
                    endExecution(this);
                }
            }

            private void run0() {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                PrintStream out = new PrintStream(baos);
                exitValue = -1;

                /*
                 * Create the command line
                 */
                String cmd = processCommandPrefix +
                    " -Ximage:" + image +
                    " java.lang.TckRunner";
                if (!test.isPositive) {
                    cmd += " -";
                }
                cmd += " " + test.harnessClass + " " + test.args;

                Process process = null;
                try {
                    /*
                     * Test output prologue
                     */
                    out.println("--------------------------------------------------");
                    out.println("test index: "+test.index);
                    out.println("test: "+test.harnessClass);
                    out.println("exec: "+cmd);
                    out.println("status: "+index+"/"+executionCount);
                    out.println("-- test console output start --");

                    /*
                     * Start test execution as subprocess
                     */
                    process = Runtime.getRuntime().exec(cmd, jniEnv);
                    StreamGobbler error  = new StreamGobbler(process.getErrorStream(), out);
                    StreamGobbler output = new StreamGobbler(process.getInputStream(), out);
                    error.start();
                    output.start();

                    try {
                        exitValue = process.waitFor();
                        error.join();
                        output.join();
                    } catch (InterruptedException ex1) {
                        out.println("InterruptedException while executing: "+cmd);
                    }

                } catch (IOException ex) {
                    out.println("I/O Error while executing: "+cmd);
                    ex.printStackTrace(out);
                } finally {
                    out.println("-- test console output end --");

                    /*
                     * Ensure that the native process (if any is killed).
                     */
                    if (process != null) {
                        process.destroy();
                    }
                }

                consoleOutput = new String(baos.toByteArray());
            }
        }


        /**
         * The collection of test executions that have not yet completed.
         */
        final private Vector incompleteExecutions = new Vector();

        /**
         * The collection of test executions.
         */
        final private Vector executions = new Vector();

        /**
         * The environment parameters passed to the process to enable an
         * embedded JVM to be started if necessary.
         */
        final String[] jniEnv;

        /**
         * The prefix of the command line used to execute every test.
         */
        final String processCommandPrefix;

        /**
         * The logging directory.
         */
        File logDir;

        /**
         * The prefix to use for log files.
         */
        String logFilePrefix;

        /**
         * The name of the file to which console output should be logged.
         */
        String consoleOutputLogFile;

        /**
         * The number of processors available on the underlying hardware.
         */
        int processorCount = 1;

        /**
         * The properties object that specified the tests to be executed.
         */
        final Properties runProperties;

        /**
         * Displays the usage message for executing the TCK tests.
         *
         * @param  errMsg  an optional error message to display
         */
        private void usage(String errMsg) {
            PrintStream out = System.err;
            if (errMsg != null) {
                out.println(errMsg);
            }
            String dir = DEFAULT_TCK_LOG_DIR;
            String romizePrefix = DEFAULT_ROMIZETCK_LOG_FILE_PREFIX;
            String runPrefix = DEFAULT_RUNTCK_LOG_FILE_PREFIX;
            out.println("usage: runtck [-options] [<run properties file>]");
            out.println("    <run properties file> will default to "+dir+File.separator+romizePrefix+".properties");
            out.println("where options include:");
            out.println("    -d <dir>       output directory for generated files (default: "+dir+")");
            out.println("    -p <prefix>    prefix to use for generated files (default: "+runPrefix+")");
            out.println("    -o <file>      redirect output to <file> (default: <prefix>.output)");
            out.println("    -c             execute TCK tests with native VM otherwise use Java VM");
            out.println("    -egc           execute TCK tests with execessive garbage collection");
            out.println("    -J<flag>       pass <flag> to the VM command line");
            out.println("    -np            the number of available processors");
            out.println("    -h             display this usage message and exit");
            out.println();
            out.println("Note: '-' may be specified for '-o' option to send");
            out.println("      output to the standard output stream.");
            out.println();
            out.println("The files generated during running the TCK are:");
            out.println("    <prefix>.output:         output of execution");
            out.println("    <prefix>.passed:         log of passed tests");
            out.println("    <prefix>.failed:         log of failed tests");
            out.println("    <prefix>.untranslatable: log of untranslatable tests");
        }

        /**
         * Log the start of a test execution. This method blocks until there is
         * (most likely) a processor free.
         *
         * @param execution
         */
        void startExecution(Execution execution) {
            int index = execution.index;
            executions.setSize(index+1);
            executions.setElementAt(execution, index);

            while (incompleteExecutions.size() >= processorCount) {
                Thread.yield();
            }
            incompleteExecutions.addElement(execution);
        }

        /**
         * Log the completion of a test execution.
         *
         * @param execution
         */
        void endExecution(Execution execution) {
            incompleteExecutions.remove(execution);
        }

        /**
         * Gets the OS dependent environment settings required to load an
         * embedded JVM via JNI.
         *
         * @return
         */
        private String[] getJniEnvironment() {
            /*
             * Configure OS
             */
            String osName = System.getProperty("os.name").toLowerCase();
            Build.OS os = Build.createOS(osName);
            if (os == null) {
                usage("Non-supported OS: " + osName);
                throw new CommandFailedException("runtck failed", 1);
            }
            String env = os.getJVMLibraryPath();
            if (env == null) {
                System.out.println("Couldn't find JVM library.");
                os.showJNIEnvironmentMessage(System.out);
                throw new CommandFailedException("runtck failed", 1);
            }
            return new String[] { env };
        }

        /**
         * Get the argument to a command line option. If the argument is not provided,
         * then a usage message is printed and the system exits.
         *
         * @param   args   the command line arguments
         * @param   index  the index at which the option's argument is located
         * @param   opt    the name of the option
         * @return  the options argument.
         */
        public String getOptArg(String[] args, int index, String opt) {
            if (index >= args.length) {
                usage("The " + opt + " option requires an argument.");
            }
            return args[index];
        }

        /**
         * Create a new instance which will be used to run a (sub)set of the
         * tests in this TCK.
         *
         * @param   args  the execution parameters
         */
        RunTCK(String[] args) {
            int argp = 0;
            boolean nativeVM = false;
            boolean useExactGC = false;
            String vmArgs = null;
            logFilePrefix = DEFAULT_RUNTCK_LOG_FILE_PREFIX;
            logDir = new File(DEFAULT_TCK_LOG_DIR);

            while (argp != args.length) {
                String arg = args[argp];
                if (arg.charAt(0) == '-') {
                    if (arg.equals("-c")) {
                        nativeVM = true;
                    } else if (arg.equals("-egc")) {
                        useExactGC = true;
                    } else if (arg.equals("-o")) {
                        consoleOutputLogFile = getOptArg(args, ++argp, "-o");
                    } else if (arg.equals("-d")) {
                        logDir = new File(getOptArg(args, ++argp, "-d"));
                    } else if (arg.equals("-np")) {
                        processorCount = Integer.parseInt(getOptArg(args, ++argp, "-d"));
                        if (processorCount == 0) {
                            processorCount = 1;
                        }
                    } else if (arg.equals("-p")) {
                        logFilePrefix = getOptArg(args, ++argp, "-p");
                    } else if (arg.startsWith("-J")) {
                        String flag = arg.substring("-J".length());
                        if (vmArgs == null) {
                            vmArgs = flag;
                        } else {
                            vmArgs += " " + flag;
                        }
                    } else if (arg.startsWith("-h")) {
                        usage(null);
                        throw new CommandFailedException("runtck failed", 1);
                    } else {
                        System.err.println("Unknown option ignored: "+arg);
                    }
                } else {
                    break;
                }
                argp++;
            }

            Build.ensureDirExists(logDir.getPath());
            if (consoleOutputLogFile == null) {
                consoleOutputLogFile = logFilePrefix + ".output";
            }

            // Load the properties file that specifies the tests to be run.
            String propertiesFile;
            if (argp != args.length) {
                propertiesFile = Build.fix(args[argp]);
            } else {
                propertiesFile = DEFAULT_TCK_LOG_DIR +
                                 File.separator +
                                 DEFAULT_ROMIZETCK_LOG_FILE_PREFIX +
                                 ".properties";

            }
            runProperties = new Properties();
            try {
                runProperties.load(new FileInputStream(propertiesFile));
            } catch (IOException ioe) {
                ioe.printStackTrace();
                throw new CommandFailedException("runtck failed", 1);
            }

            // Create the command line prefix
            String cmdPrefix;
            if (nativeVM) {
                cmdPrefix = Build.fix("vm/bld/squawk");
                if (System.getProperty("os.name").indexOf("Windows") != -1) {
                    cmdPrefix += ".exe";
                }
            } else {
                cmdPrefix = Build.fix("java -Xbootclasspath#a:j2se/classes;vm/classes;j2me/classes com.sun.squawk.vm.Interpreter");
            }

            // Add the classpath for the VM and I/O classes when executing the native VM
            if (nativeVM) {
                cmdPrefix += " -Xcp:" + Build.fix("vm/classes;j2se/classes;j2me/classes");
            }

            // Add the excessive GC flag if necessary
            if (useExactGC) {
                cmdPrefix += " -XexcessiveGC";
            }

            // Add the flag specifying where resources can be found
            cmdPrefix += " -Xresourcepath:" + Build.fix("tck/tck.jar");

            if (vmArgs != null) {
                cmdPrefix += " "+vmArgs;
            }
            processCommandPrefix = cmdPrefix;

            jniEnv = nativeVM ? getJniEnvironment() : null;
        }

        /**
         * Execute the tests.
         */
        void run() {
            try {
                int index = 1;
                for (Enumeration e = runProperties.propertyNames(); e.hasMoreElements(); ) {
                    String name = (String)e.nextElement();
                    String image = runProperties.getProperty(name);

                    Test test = getTestForName(name);
                    if (test == null) {
                        System.out.println("Can't find test: " + name);
                        continue;
                    }

                    Execution execution = new Execution(test, index, runProperties.size(), image);
                    startExecution(execution);
                    execution.start();
                    index++;
                }
            } finally {
                while (!incompleteExecutions.isEmpty()) {
                    Thread.yield();
                }
            }

            // Write the output
            PrintStream out = openPrintStreamToFile(logDir, consoleOutputLogFile, System.out);
            PrintStream passedPS  = openPrintStreamToFile(logDir, logFilePrefix + ".passed", System.out);
            PrintStream failedPS  = openPrintStreamToFile(logDir, logFilePrefix + ".failed", System.out);
            PrintStream untransPS = openPrintStreamToFile(logDir, logFilePrefix + ".untranslatable", System.out);
            Properties failedProps = new Properties();
            for (Enumeration e = executions.elements(); e.hasMoreElements();) {
                Execution execution = (Execution)e.nextElement();
                if (execution == null) {
                    continue;
                }
                Test test = execution.test;
                if (execution.consoleOutput != null) {
                    out.print(execution.consoleOutput);
                }

                if (execution.exitValue == 95) {
                    passedPS.println(test);
                } else if (execution.exitValue == 105) {
                    untransPS.println(test);
                } else {
                    failedPS.println(test);
                    failedProps.put(test.harnessClass, execution.image);
                }
            }
            if (!failedProps.isEmpty()) {
                try {
                    PrintStream fos = openPrintStreamToFile(logDir, logFilePrefix + ".failed.properties", System.err);
                    failedProps.store(fos, "Command lines for failed tests");
                    fos.close();
                } catch (IOException ioe) {
                }
            }
        }
    }

    /**
     * Opens a print stream that directs its output to a file.
     *
     * @param dir
     * @param file
     * @param defaultPS
     * @return
     */
    private static PrintStream openPrintStreamToFile(File dir, String file, PrintStream defaultPS) {
        if (file.equals("-")) {
            return System.out;
        }
        try {
            return new PrintStream(new FileOutputStream(new File(dir, file)));
        } catch (IOException ex) {
            ex.printStackTrace();
            return System.out;
        }
    }
}
