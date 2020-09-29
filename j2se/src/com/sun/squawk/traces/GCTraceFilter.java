/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */
package com.sun.squawk.traces;

import java.io.*;
import java.util.regex.*;

/**
 * Filters a Squawk GC to show method names when tracing activation frames.
 */
public class GCTraceFilter {

    /**
     * The stream used to write the filtered output.
     */
    private PrintStream out = System.out;

    /**
     * The stream used to read the input to be filtered.
     */
    private InputStream in = System.in;

    /**
     * The map used to do the filtering.
     */
    private Symbols symbols = new Symbols();

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
        out.println("Usage: GCTraceFilter [options]");
        out.println("where options include:");
        out.println();
        out.println("    -s file       load additional symbols from file");
        out.println("    -i file       read input from file (default is standard input)");
        out.println("    -o file       output to file (default is standard output)");
        out.println();
        out.println("Note: symbols are loaded from all *.sym files in the current directory");
    }

    /**
     * Gets the argument to a command line option. If the argument is not
     * provided, then a usage message is printed and RuntimeException is
     * thrown.
     *
     * @param  args   the command line arguments
     * @param  index  the index at which the option's argument is located
     * @param  opt    the name of the option
     * @return the value of the option's argument
     * @throws RuntimeException if the required argument is missing
     */
    private String getOptArg(String[] args, int index, String opt) {
        if (index >= args.length) {
            usage("The " + opt + " option requires an argument.");
            throw new RuntimeException();
        }
        return args[index];
    }

    /**
     * Constructs a filter from some command line arguments.
     *
     * @param args   the command line arguments
     * @throws IOException
     */
    private GCTraceFilter(String[] args) throws IOException {

        // Load all *.sym files first
        symbols.loadIfFileExists("squawk.sym");
        symbols.loadIfFileExists("squawk_dynamic.sym");

        // Load additional symbol files
        int argc = 0;
        while (argc != args.length) {
            String arg = args[argc];
            if (arg.equals("-s")) {
                String file = getOptArg(args, ++argc, "-s");
                symbols.loadIfFileExists(file);
            } else if (arg.equals("-o")) {
                String file = getOptArg(args, ++argc, "-o");
                out = new PrintStream(new FileOutputStream(file));
            } else if (arg.equals("-i")) {
                String file = getOptArg(args, ++argc, "-i");
                in = new FileInputStream(file);
            } else {
                usage("Unknown option: " + arg);
                System.exit(1);
            }
            argc++;
        }
    }

    /**
     * Runs the filter over the input.
     *
     * @throws IOException
     */
    private void run() throws IOException {
        if (in.available() == 0) {
            System.err.println("Nothing available on standard input stream");
            return;
        }

        Pattern romMethod = Pattern.compile("mp = [0-9]+ \\(image @ ([0-9]*)\\)");
        Pattern ramMethod = Pattern.compile("mp = ([0-9]+)");

        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        String line = br.readLine();
        while (line != null) {

            int index = line.indexOf("mp = ");
            if (index != -1) {

                Matcher matcher = romMethod.matcher(line);
                String address = null;
                int start = 0;
                int end = 0;

                if (matcher.find()) {
                    address = matcher.group(1);
                    start = matcher.start(1);
                    end = matcher.end(1);
                } else {
                    matcher = ramMethod.matcher(line);
                    if (matcher.find()) {
                        address = matcher.group(1);
                        start = matcher.start(1);
                        end = matcher.end(1);
                    }
                }

                if (address != null) {
                    Symbols.Method method = symbols.lookupMethod(Long.parseLong(address));
                    if (method != null) {
                        line = line.substring(0, start) + address + " [" + method.getName(true) + "]" + line.substring(end);
                    }
                }
            }
            out.println(line);
            line = br.readLine();
        }

        out.close();
        in.close();
    }

    /**
     * Command line interface.
     *
     * @param args   command line arguments
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        new GCTraceFilter(args).run();
    }
}
