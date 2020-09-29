/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */

package com.sun.squawk.listclass;

import java.io.*;
import java.util.*;
import org.apache.bcel.classfile.*;
import org.apache.bcel.*;
import org.apache.bcel.util.ClassPath;

/**
 * A classfile disassembler.
 */
public class Main {

    public static void usage(String errMsg) {
        PrintStream out = System.out;
        if (errMsg != null) {
            out.println(errMsg);
        }
        out.println("Usage: listclass [-options] [ class | classfile ]* ");
        out.println("where options include:");
        out.println("    -code            show code disassembly");
        out.println("    -constants       show constant pool disassembly");
        out.println("    -cp <path>       use path to search for classes");
        out.println("    -jasmin          show code disassembly in Jasmin format");
        out.println("    -h               show this help message and exit");
    }

    public static void main(String[] args) {
        boolean code = false;
        boolean constants = false;
        String name = null;
        ClassPath path = ClassPath.SYSTEM_CLASS_PATH;
        boolean jasmin = false;

        int argc = 0;
        while (argc != args.length) {
            String arg = args[argc];
            if (arg.charAt(0) == '-') {
                if (arg.equals("-constants")) {
                    constants = true;
                } else if (arg.equals("-code")) {
                    code = true;
                } else if (arg.equals("-cp")) {
                    path = new ClassPath(args[++argc].replace('/', File.separatorChar).replace(':',
                        File.pathSeparatorChar));
                } else if (arg.startsWith("-h")) {
                    usage(null);
                    return;
                } else if (arg.equals("-jasmin")) {
                    jasmin = true;
                } else {
                    usage("Unknown switch: " + arg);
                    return;
                }
            } else {
                break;
            }
            argc++;
        }

        if (argc == args.length) {
            usage("listclass: No input classes specified");
            return;
        }

        Vector classes = new Vector(args.length - argc);
        while (argc != args.length) {
            classes.addElement(args[argc++]);
        }

        for (Enumeration e = classes.elements(); e.hasMoreElements(); ) {
            try {
                name = (String)e.nextElement();
                JavaClass java_class;
                if (name.charAt(0) == '@') {
                    try {
                        String fn = name.substring(1);
                        BufferedReader br = new BufferedReader(new FileReader(fn));
                        String line;
                        while ((line = br.readLine()) != null) {
                            line = line.trim();
                            if (!line.startsWith("#")) {
                                classes.addElement(line);
                            }
                        }
                        br.close();
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                    continue;
                } else if (name.endsWith(".class")) {
                    if ((java_class = Repository.lookupClass(name)) == null) {
                        java_class = (new ClassParser(name)).parse();
                    }
                } else {
                    org.apache.bcel.util.ClassPath.ClassFile classFile = path.getClassFile(name);
                    java_class = (new ClassParser(classFile.getInputStream(), classFile.getPath())).parse();
                }

                if (jasmin) {
                    new JasminVisitor(java_class, System.out).disassemble();
                } else {
                    ConstantPool cp = java_class.getConstantPool();
                    System.out.println(java_class);
                    if (constants) {
                        System.out.println(cp);
                    }
                    if (code) {
                        printCode(java_class.getMethods());
                    }
                }
            } catch (IOException ex) {
                ex.printStackTrace();
                System.err.println("Couldn't find class " + name);
            } catch (Exception ex) {
                ex.printStackTrace();
            } catch (Error ex) {
                ex.printStackTrace();
            }
        }
    }

    public static void printCode(org.apache.bcel.classfile.Method methods[]) {
        try {
            for (int i = 0; i < methods.length; i++) {
                org.apache.bcel.classfile.Method method = methods[i];
                System.out.println(method);
                Code code = method.getCode();
                if (code != null) {
                    System.out.println(code.toString());
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
