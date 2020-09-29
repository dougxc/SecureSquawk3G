/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */
package java.lang;

import java.io.PrintStream;
import java.util.StringTokenizer;
import com.sun.squawk.util.Tracer;
import com.sun.squawk.util.Hashtable;

/**
 * The Java application manager is the master isolate used to coordinate application execution.
 *
 * @author Nik Shaylor
 */
public class JavaApplicationManager {

    /**
     * The class path to use when loading through the translator instance (if any).
     */
    private static String classPath = ".";

    /**
     * The suite to which the open suite will be bound (if any).
     */
    private static String parentSuiteURL = null;

    /**
     * Command line option to enable display of execution statistics before exiting.
     */
    private static boolean displayExecutionStatistics;

    /**
     * TEMP -- Flag to test hibernation.
     */
    private static boolean hibernatetest;

    /**
     * Main routine.
     *
     * @param args the command line argument array
     */
    public static void main(String[] args) throws Exception {

        /*
         * Process any switches.
         */
        if (args.length != 0) {
            args = processVMOptions(args);
        }

        /*
         * Check that there is a main class specified.
         */
        if (args.length == 0) {
            usage("");
        }

        /*
         * Split out the class name from the other arguments.
         */
        String mainClassName = args[0].replace('/', '.');
        String[] javaArgs = new String[args.length - 1];
        for (int i = 0 ; i < javaArgs.length ; i++) {
            javaArgs[i] = args[i+1];
        }

        /*
         * Get the start time.
         */
        long startTime = System.currentTimeMillis();

        /*
         * Create the application isolate and run it.
         */
        Isolate isolate = new Isolate(mainClassName, javaArgs, classPath, parentSuiteURL);

        /*
         * Start the isolate and wait for it to complete.
         */
        isolate.start();
        isolate.join();

        /*
         * If the isolate was hibernated then save it.
         */
        if (isolate.isHibernated()) {
            try {
                System.out.println("Saved isolate to " + isolate.save());
            } catch (java.io.IOException ioe) {
                System.err.println("I/O error while trying to save isolate: ");
                ioe.printStackTrace();
            }
        }

        /*
         * If the isolate was hibernated then test hibernation if requested.
         */
        if (isolate.isHibernated() && hibernatetest) {
            isolate.unhibernate();
            isolate.join();
        }

        /*
         * Get the exit status.
         */
        int exitCode = isolate.getExitCode();

        /*
         * Show execution statistics if requested
         */
        if (displayExecutionStatistics) {
            long endTime = System.currentTimeMillis();
            System.out.println();
            System.out.println("=============================");
            System.out.println("Squawk VM exiting with code "+exitCode);
            if (GC.getPartialCollectionCount() > 0) {
                System.out.println(""+GC.getPartialCollectionCount()+" partial collections");
            }
            if (GC.getFullCollectionCount() > 0) {
                System.out.println(""+GC.getFullCollectionCount()+" full collections");
            }
            System.out.println("Execution time was "+(endTime-startTime)+" ms");
            System.out.println("=============================");
            System.out.println();
        }

        /*
         * Stop the VM.
         */
        VM.stopVM(exitCode);
    }

    /**
     * Process any VM command line options.
     *
     * @param args the arguments as supplied by the VM.startup code
     * @return the arguments needed by the main() routine of the isolate
     */
    private static String[] processVMOptions(String[] args) {
        int offset = 0;
        while (offset != args.length) {
            String arg = args[offset];
            if (arg.charAt(0) == '-') {
                processVMOption(arg);
            } else {
                break;
            }
             offset++;
        }
        String[] javaArgs = new String[args.length - offset];
        for (int i = 0 ; i < javaArgs.length ; i++) {
            javaArgs[i] = args[offset++];
        }
        return javaArgs;
    }

    /**
     * Shows the version information.
     *
     * @param out  the print stream to use
     */
    private static void showVersion(PrintStream out) {
        out.println((Klass.SQUAWK_64 ? "64" : "32") + " bit squawk:");
        out.println("    assertions and debug code " + (Klass.DEBUG ? "enabled" : "disabled"));
        boolean floatSupported = "${build.properties:FLOATS}".equals("true");
        if (floatSupported) {
            out.println("    floating point supported");
        } else {
            out.println("    no floating point support");
        }
        out.println("    bootstrap suite: ");
        StringTokenizer st = new StringTokenizer(SuiteManager.getSuite(0).getConfiguration(), ",");
        while (st.hasMoreTokens()) {
            out.println("        " + st.nextToken().trim());
        }
        VM.printConfiguration();
    }

    /**
     * Shows the classes in the image.
     *
     * @param out  the print stream to use
     * @param packagesOnly if true, only a listing of the packages in the image is shown
     */
    private static void showImageContents(PrintStream out, boolean packagesOnly) {
        Suite bootstrapSuite = SuiteManager.getSuite(0);
        if (packagesOnly) {
            out.println("Packages in image:");
            Hashtable packages = new Hashtable();
            int count = bootstrapSuite.getClassCount();
            for (int i = 0; i != count; ++i) {
                Klass klass = bootstrapSuite.getKlass(i);
                if (klass != null && !klass.isSynthetic()) {
                    String className = klass.getInternalName();
                    int index = className.lastIndexOf('.');
                    if (index != -1) {
                        String packageName = className.substring(0, className.lastIndexOf('.'));
                        if (packages.get(packageName) == null) {
                            out.println("  " + packageName);
                            packages.put(packageName, packageName);
                        }
                    }
                }
            }
        } else {
            out.println("Classes in image:");
            int count = bootstrapSuite.getClassCount();
            for (int i = 0; i != count; ++i) {
                Klass klass = bootstrapSuite.getKlass(i);
                if (klass != null && !klass.isSynthetic()) {
                    out.println("  " + klass.getName());
                }
            }
        }
    }

    /**
     * Process a VM command line option.
     *
     * @param arg the argument
     */
    private static void processVMOption(String arg) {
        if (arg.startsWith("-cp:")) {
            classPath = arg.substring("-cp:".length());
            // Fix up the class path with respect to the system dependant separator characters
            char sep = VM.getPathSeparatorChar();
            if (sep == ';') {
                classPath = classPath.replace(':', ';').replace('/', '\\');
            } else {
                classPath = classPath.replace(';', ':').replace('\\', '/');
            }
        } else if (arg.startsWith("-suite:")) {
            parentSuiteURL = "file://" + arg.substring(7) + ".suite";
/*if[FLASH_MEMORY]*/
        } else if (arg.startsWith("-flashsuite:")) {
            parentSuiteURL = "flash://" + arg.substring(12);
/*end[FLASH_MEMORY]*/
        } else if (arg.equals("-egc")) {
            GC.setExcessiveGC(true);
        } else if (arg.equals("-nogc")) {
            VM.allowUserGC(false);
        } else if (arg.equals("-imageclasses")) {
            showImageContents(System.err, false);
            VM.stopVM(0);
        } else if (arg.equals("-imagepackages")) {
            showImageContents(System.err, true);
            VM.stopVM(0);
        } else if (arg.equals("-version")) {
            showVersion(System.err);
            VM.stopVM(0);
        } else if (arg.equals("-verbose")) {
            if (!VM.isVerbose()) {
                VM.setVerboseLevel(1);
            }
        } else if (arg.equals("-veryverbose")) {
            if (!VM.isVeryVerbose()) {
                VM.setVerboseLevel(2);
            }
        } else if (arg.equals("-hibernatetest")) {
            hibernatetest = true;
        } else if (arg.equals("-stats")) {
            displayExecutionStatistics = true;
        } else if (arg.equals("-h")) {
            usage("");
            VM.stopVM(0);
        } else if (!processTranslatorOption(arg) && !GC.getCollector().processCommandLineOption(arg)) {
            usage("Unrecognised option: "+arg);
            VM.stopVM(0);
        }
    }

    /**
     * Process an option to see if it is an option for the translator.
     *
     * @param arg   the option to process
     * @return      true if <code>arg</code> was a translator option
     */
    private static boolean processTranslatorOption(String arg) {
        if (VM.getCurrentIsolate().getTranslator() != null) {
            if (Klass.DEBUG && arg.startsWith("-trace")) {
                if (arg.startsWith("-tracefilter:")) {
                    Tracer.setFilter(arg.substring("-tracefilter:".length()));
                } else {
                    String feature = arg.substring("-trace".length());
                    Tracer.enableFeature(feature);
                    if (arg.equals("-traceconverting")) {
                        Tracer.enableFeature("loading"); // -traceconverting subsumes -traceloading
                    }
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Get a long form an argument.
     *
     * @param arg the argument
     * @param prefix part of the argument before the number
     */
    private static long parseLongArg(String arg, String prefix) {
        try {
            return Long.parseLong(arg.substring(prefix.length()));
        } catch (NumberFormatException ex) {
            usage("Bad numeric parameter: "+arg);
            return 0;
        }
    }

    /**
     * Print a usage message and exit.
     *
     * @param msg error message
     */
    private static void usage(String msg) {
        PrintStream out = System.out;
        out.println();
        out.println("Copyright (c) Sun Microsystems, Inc.   All  rights  reserved.");
        out.println("Unpublished - rights reserved under the Copyright Laws of the");
        out.println("United States. SUN PROPRIETARY/CONFIDENTIAL.  U.S. Government");
        out.println("Rights - Commercial software. Government users are subject to");
        out.println("the  Sun Microsystems,  Inc.  standard license agreement  and");
        out.println("applicable provisions of the FAR and its supplements.  Use is");
        out.println("subject to license terms. Sun, Sun Microsystems, the Sun logo");
        out.println("and  Java  are  trademarks  or registered  trademarks  of Sun");
        out.println("Microsystems, Inc. in the U.S. and other countries.");
        out.println();
        if (msg.length() > 0) {
            out.println("** " + msg + " **\n");
        }
        out.println("Usage: squawk [-options] class [args...]");
        out.println();
        out.println("where options include:");
        out.println("    -cp:<directories and jar/zip files separated by ':' (Unix) or ';' (Windows)>");
        out.println("                            paths where classes, suites and sources can be found");
        translatorUsage(out);
        out.println("    -suite:<name>           suite name (without \".suite\") to load");
        out.println("    -imageclasses           show the images in the boot image and exit");
        out.println("    -imagepackages          show the packages in the boot image and exit");
        out.println("    -version                print product version and exit");
        out.println("    -verbose                report when a class is loaded");
        out.println("    -veryverbose            report when a class is initialized or looked up and");
        out.println("                            various other output");
        GC.getCollector().usage(out);
        out.println("    -egc                    enable excessive garbage collection");
        out.println("    -nogc                   disable application calls to Runtime.gc()");
        out.println("    -stats                  display execution statistics before exiting");
        out.println("    -h                      display this help message");
        out.println("    -X                      display help on native VM options");
        VM.stopVM(0);
    }

    /**
     * Prints the usage message for the translator specific options if the translator is present.
     *
     * @param out  the stream on which to print the message
     */
    private static void translatorUsage(PrintStream out) {
        if (VM.getCurrentIsolate().getTranslator() != null) {
            if (Klass.DEBUG) {
            out.println("    -traceloading       trace class loading");
            out.println("    -traceconverting    trace method conversion (includes -traceloading)");
            out.println("    -tracejvmverifier   trace verification of JVM/CLDC bytecodes");
            out.println("    -traceemitter       trace Squawk bytecode emitter");
            out.println("    -tracesquawkverifier trace verification of Squawk bytecodes");
            out.println("    -traceclassinfo     trace loading of class meta-info (i.e. implemented");
            out.println("                        interfaces, field meta-info & method meta-info)");
            out.println("    -traceclassfile     trace low-level class file elements");
            out.println("    -traceir0           trace the IR built from the JVM bytecodes");
            out.println("    -traceir1           trace optimized IR with JVM bytecode offsets");
            out.println("    -traceir2           trace optimized IR with Squawk bytecode offsets");
            out.println("    -tracemethods       trace emitted Squawk bytecode methods");
            out.println("    -traceoms           trace object memory serialization");
            out.println("    -tracesvm           trace secure JVM operations");
            out.println("    -tracefilter:<string>  filter trace with simple string filter");
            }
        }
    }
}
