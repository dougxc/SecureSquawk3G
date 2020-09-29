package com.sun.squawk.util;

import java.io.*;
import java.util.StringTokenizer;
import java.util.Enumeration;
import javax.microedition.io.*;
import com.sun.squawk.util.*;
import com.sun.squawk.io.connections.ClasspathConnection;

/**
 * A collection of utilities for command line argument parsing.
 */
public class ArgsUtilities {

    /**
     * Reads all the lines of a file into a given vector.
     *
     * @param   file   the file to read
     * @param   lines  the vector to augment
     */
    public static void readLines(String file, Vector lines) {
        try {
            InputStream is = Connector.openInputStream("file://" + file);
            InputStreamReader isr = new InputStreamReader(is);
            LineReader lr = new LineReader(isr);
            lr.readLines(lines);
        } catch (IOException ioe) {
            throw new RuntimeException("Error while processing file '"+file+"': " + ioe);
        }
    }

    /**
     * Processes a file containing command line arguments. The file is parsed as a
     * sequence of white space separated arguments.
     *
     * @param   name  the name of the args file
     * @param   args  the vector of arguments to be added to
     */
    public static void readArgFile(String name, Vector args) {
        Vector lines = new Vector();
        readLines(name, lines);
        for (Enumeration e = lines.elements(); e.hasMoreElements();) {
            String line = (String) e.nextElement();
            StringTokenizer st = new StringTokenizer(line);
            while (st.hasMoreTokens()) {
                args.addElement(st.nextToken());
            }
        }
    }

    /**
     * Expands any argfiles. Finds any components in <code>args</code> that
     * start with '@' (thus denoting a file containing more arguments) and
     * expands the arguments inline in <code>args</code>. The expansion is
     * not recursive.
     *
     * @param   args  the original command line arguments
     * @return  the given arguments with any inline argfiles expanded
     */
    public static String[] expandArgFiles(String[] args) {
        Vector expanded = new Vector(args.length);
        for (int i = 0; i != args.length; ++i) {
            String arg = args[i];
            if (arg.charAt(0) == '@') {
                readArgFile(arg.substring(1), expanded);
            } else {
                expanded.addElement(arg);
            }
        }
        if (expanded.size() != args.length) {
            args = new String[expanded.size()];
            expanded.copyInto(args);
        }
        return args;
    }

    /**
     * Converts a given file or class path to the correct format for the
     * underlying platform. For example, if the underlying platform uses
     * '/' to separate directories in a file path then any instances of
     * '\' in <code>path</code> will be converted to '/'.
     *
     * @param   path         to the path to convert
     * @param   isClassPath  specifies if <code>path</code> is a class path
     * @return  the value of <code>path</code> reformatted (if necessary) to
     *                be correct for the underlying platform
     */
    public static String toPlatformPath(String path, boolean isClassPath) {
        char fileSeparatorChar = VM.getFileSeparatorChar();
        if (fileSeparatorChar == '/') {
            path = path.replace('\\', '/');
        } else if (fileSeparatorChar == '\\') {
            path = path.replace('/', '\\');
        } else {
            throw new RuntimeException("OS with unknown separator: '" + fileSeparatorChar + "'");
        }
        if (isClassPath) {
            char pathSeparatorChar = VM.getPathSeparatorChar();
            if (pathSeparatorChar == ':') {
                path = path.replace(';', ':');
            } else if (pathSeparatorChar == ';') {
                path = path.replace(':', ';');
            } else {
                throw new RuntimeException("OS with unknown path separator: '"+ pathSeparatorChar+"'");
            }
        }
        return path;
    }

    /**
     * Processes a single command line argument that specifies a file containing a set of
     * class names, one per line.
     *
     * @param   arg      the command line argument to process
     * @param   classes  the list of class names to augment
     */
    public static void processClassListArg(String arg, Vector classes) {
        readLines(arg, classes);
    }

    /**
     * Processes a single command line argument that specifies a jar or zip
     * file of class files.
     *
     * @param   arg      the command line argument to process
     * @param   classes  the list of class names to augment
     */
    public static void processClassJarOrZipArg(String arg, Vector classes) {
        try {
            ClasspathConnection cp = (ClasspathConnection)Connector.open("classpath://" + arg);
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
                         * Replace '/' with '.'
                         */
                        className = className.replace('/', '.');
                        classes.addElement(className);
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
     * Processes a single command line argument that specifies one or more
     * class names.
     *
     * @param   arg      the command line argument to process
     * @param   classes  the list of class names to augment
     */
    public static void processClassArg(String arg, Vector classes) {
        if (arg.charAt(0) == '@') {
            arg = arg.substring(1);
            processClassListArg(arg, classes);
        } else if (arg.endsWith(".zip") || arg.endsWith(".jar")) {
            processClassJarOrZipArg(arg, classes);
        } else {
            DataInputStream dis = null;
            try {
                dis = Connector.openDataInputStream("file://" + arg + "//");
                try {
                    int baseDirPrefix = arg.length() + 1;
                    while (true) {
                        String name = dis.readUTF();
                        if (name.endsWith(".class")) {
                            /*
                             * Strip off the base directory name
                             */
                            name = name.substring(baseDirPrefix);

                            /*
                             * Strip off ".class" suffix
                             */
                            String className = name.substring(0, name.length() - ".class".length());

                            /*
                             * Replace '/' with '.'
                             */
                            className = className.replace('/', '.');
                            classes.addElement(className);
                        }

                    }
                } catch (EOFException ex) {
                    return;
                }
            } catch (ConnectionNotFoundException e) {
                // argument is not a directory - add it as a class name
                classes.addElement(arg);
            } catch (IOException ioe) {
                throw new RuntimeException("error parsing '" + arg + "': " + ioe);
            } finally {
                if (dis != null) {
                    try {
                        dis.close();
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                        Assert.shouldNotReachHere();
                    }
                }
            }
        }
    }

    /**
     * Cuts a string of white space separated tokens into an array of strings, one element for each token.
     *
     * @param str   the string to cut
     * @return 'str' as an array of strings
     */
    public static String[] cut(String str) {
        StringTokenizer st = new StringTokenizer(str, " ");
        String res[] = new String[st.countTokens()];
        int i = 0;
        while (st.hasMoreTokens()) {
            res[i++] = st.nextToken();
        }
        return res;
    }

    /**
     * Gets the argument to a command line option. If the argument is not
     * provided, then a usage message is printed and RuntimeException is
     * thrown.
     *
     * @param  args   the command line arguments
     * @param  index  the index at which the option's argument is located
     * @param  opt    the name of the option
     * @return the value of the option's argument or null if it is missing
     */
    public static String getOptArg(String[] args, int index, String opt) {
        if (index >= args.length) {
            return null;
        }
        return args[index];
    }
}
