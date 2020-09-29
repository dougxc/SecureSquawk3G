/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */

import java.io.*;
import java.util.*;

/**
 * This is a utility to provide some assistance in configuring the
 * environment so that the JVM embedded in the native Squawk VM
 * will start properly.
 */
class jvmenv {

    /**
     * Find a single file matching a specified suffix and an optional
     * extra filter.
     * @param dir The directory
     * @param suffix
     * @param match
     * @return
     * @throws Exception
     */
    static String findOne(String dir, String suffix, String match) throws Exception {
        Vector results = new Vector();
        com.sun.squawk.util.Find.find(new File(dir), suffix, results, false);
        for (Enumeration e = results.elements(); e.hasMoreElements();){
            String f = (String)e.nextElement();
            if (f.indexOf(match) != -1) {
                return f;
            }
        }
        return null;
    }

    static String findOne(String dir, String suffix, String[] matches) throws Exception {
        String result = null;
        for (int i = 0; i != matches.length && result == null; i++) {
            result = findOne(dir, suffix, matches[i]);
        }
        return result;
    }

/*
    public static void main(String[] args) throws Exception {
        System.out.println(Integer.TYPE + ".getSuperclass() == "+Integer.TYPE.getSuperclass());
        Runnable r = new Runnable() { public void run() {} };
        System.out.println("r.getClass().getSuperclass() == "+r.getClass().getSuperclass());
        System.out.println("Runnable.class.getSuperclass() == "+Runnable.class.getSuperclass());
    }
*/

    public static void main(String[] args) throws Exception {
        PrintStream out = System.out;
        String javaLib;
        String jhome = System.getProperty("java.home");
        if (System.getProperty("os.name").startsWith("Windows")) {
            String jvm = findOne(jhome, "jvm.dll", new String[] { "hotspot", "client", "" });
            if (jvm != null) {
                out.println();
                out.println("To configure the environment for Squawk, try the following command:");
                out.println();
                out.println("    set JVMDLL="+jvm);
                out.println();
            } else {
                out.println();
                out.println("The JVMDLL environment variable must be set to the full path of 'jvm.dll'.");
                out.println();
            }
        } else {
            String jvm      = findOne(jhome, "libjvm.so", new String[] { "hotspot", "client", "" });
            String verifier = findOne(jhome, "libverify.so", "");
            if (jvm != null && verifier != null) {
                jvm      = (new File(jvm)).getParent();
                verifier = (new File(verifier)).getParent();
                out.println();
                out.println("To configure the environment for Squawk, try the following command under bash:");
                out.println();
                out.println("    export LD_LIBRARY_PATH=\"$LD_LIBRARY_PATH:"+jvm+":"+verifier+"\"");
                out.println();
                out.println("or in csh/tcsh");
                out.println();
                out.println("    setenv LD_LIBRARY_PATH=\"$LD_LIBRARY_PATH:"+jvm+":"+verifier+"\"");
                out.println();
            } else {
                out.println();
                out.println("The LD_LIBRARY_PATH environment variable must be set to include the directories");
                out.println("containing 'libjvm.so' and 'libverify.so'.");
                out.println();
            }
        }
    }

}
