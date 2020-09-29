/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */

package java.lang;

import java.io.*;
import java.util.*;
import javax.microedition.io.*;
import com.sun.squawk.io.connections.ClasspathConnection;

public class SuiteCreator {

    /**
     * Name of the suite to create.
     */
    private String suiteName;

    /**
     * Type of suite to create. This controls how much of the symbolic information is retained
     * in the suite when it is closed.
     */
    private int suiteType = Suite.APPLICATION;

    /**
     * Determines if the line number tables (if any) are to be retained in the suite when it is closed.
     */
    private boolean retainLNTs;

    /**
     * Determines if the local variable tables (if any) are to be retained in the suite when it is closed.
     */
    private boolean retainLVTs;

    /**
     * Prints the usage message.
     *
     * @param  errMsg  an optional error message
     */
    final void usage(String errMsg) {
        PrintStream out = System.out;
        if (errMsg != null) {
            out.println(errMsg);
        }
        out.println("Usage: "+this.getClass().getName()+" [-options] suite_name [prefixes...]");
        out.println("where options include:");
        out.println();
        out.println("    -prune:<t>   prune symbolic information according to <t>:");
        out.println("                    'd' - debug: retain all symbolic info");
        out.println("                    'a' - application (default): discard all symbolic info");
        out.println("                    'l' - library: discard symbolic info");
        out.println("                          for private/package-private fields and methods");
        out.println("                    'e' - extendable library: discard symbolic info");
        out.println("                          for private fields and methods");
        out.println("    -lnt            retain line number tables");
        out.println("    -lvt            retain local variable tables");
        out.println("    -help           show this help message and exit");
        out.println();
        out.println("Note: If no prefixes are specified, then all the classes found on the");
        out.println("      class path are used.");
    }

    /**
     * Processes the class prefixes to build the set of classes on the class path that must be loaded.
     *
     * @param   args     the command line arguments specifiying class name prefixes
     * @param   index    the index in <code>args</code> where the prefixes begin
     * @param   classes  the vector to which the matching class names will be added
     */
    void processClassPrefixes(String[] args, int index, Vector classes) {
        boolean all = (args.length == index);
        try {
            ClasspathConnection cp = (ClasspathConnection)Connector.open("classpath://" + VM.getCurrentIsolate().getClassPath());
            DataInputStream dis = new DataInputStream(cp.openInputStream("//"));
            try {
                for (;;) {
                    String name = dis.readUTF();
                    if (name.endsWith(".class")) {
                        /*
                         * Strip off ".class" suffix
                         */
                        String className = name.substring(0, name.length() - ".class".length());

                        /*
                         * Replace path separator with '.'
                         */
                        className = className.replace('/', '.');

                        boolean match = all;
                        if (!match) {
                            for (int i = index; i < args.length; ++i) {
                                if (className.startsWith(args[i])) {
                                    match = true;
                                    break;
                                }
                            }
                        }
                        if (match) {
                            classes.addElement(className);
                        }
                    }
                }
            } catch (EOFException ex) {
            }
            dis.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    /**
     * Commmand line interface.
     *
     * @param args
     */
    public static void main(String args[]) throws Exception {
        SuiteCreator sc = new SuiteCreator();
        sc.run(args);
    }

    private void run(String args[]) throws Exception {
        Vector classNames = new Vector();
        if (!processArgs(args, classNames)) {
            return;
        }

        Suite openSuite = VM.getCurrentIsolate().getOpenSuite();
        openSuite.updateName(suiteName);

        if (classNames.isEmpty()) {
            usage("no classes match the package specification");
            return;
        }
        for (Enumeration e = classNames.elements(); e.hasMoreElements();) {
            String className = (String)e.nextElement();
            Klass.forName(className, true, false);
        }

        openSuite.close(suiteType, retainLNTs, retainLVTs);
        String fileName = openSuite.save();
        System.out.println("Created suite and wrote it into " + fileName);
        System.exit(0);
    }

    /**
     * Parses and processes a given set of command line arguments to translate
     * a single suite.
     *
     * @param   args        the command line arguments
     * @param   classNames  a vector to collect the names of the classes to
     *                      be translated
     * @throws RuntimeException if the arguments are malformed
     */
    boolean processArgs(String[] args, Vector classNames) {
        int argc = 0;
        while (argc != args.length) {
            String arg = args[argc];

            if (arg.charAt(0) != '-') {
                break;
            } else if (arg.startsWith("-prune:")) {
                char type = arg.substring("-prune:".length()).charAt(0);
                if (type == 'a') {
                    suiteType = Suite.APPLICATION;
                } else if (type == 'd') {
                    suiteType = Suite.DEBUG;
                    retainLNTs = true;
                    retainLVTs = true;
                } else if (type == 'l') {
                    suiteType = Suite.LIBRARY;
                } else if (type == 'e') {
                    suiteType = Suite.EXTENDABLE_LIBRARY;
                } else {
                    usage("invalid suite type: " + type);
                    throw new RuntimeException();
                }
            } else if (arg.equals("-lnt")) {
                retainLNTs = true;
            } else if (arg.equals("-lvt")) {
                retainLVTs = true;
            } else if (arg.startsWith("-h")) {
                usage(null);
                return false;
            } else {
                usage("Unknown option "+arg);
                return false;
            }
            argc++;
        }


        if (argc >= args.length) {
            usage("missing suite name");
            return false;
        }
        suiteName = args[argc++];

        // Parse class specifiers
        processClassPrefixes(args, argc, classNames);
        return true;
    }
}
