/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */
package com.sun.squawk.util;

import java.util.*;
import java.util.Vector;
import java.io.*;
import java.util.zip.*;

/**
 * This class provides a limited subset of the unix find utility.
 */
public class Find {

    /**
     * Find all the class files in a given path. Each entry in the path is a directory
     * (given in Unix format i.e. '/' as separator) and each dir is separated by ':' or ';'.
     * @param addTo The unique list of classes found are returned in this variable.
     */
    public static void findAllClassesInPath(String path, Vector addTo) {
        StringTokenizer st = new StringTokenizer(path,":;");
        HashSet uniqClasses = new HashSet();
        while (st.hasMoreTokens()) {
            Vector classes = new Vector();

            String entry = st.nextToken();
            if (entry.endsWith(".zip") || entry.endsWith(".jar")) {
                ZipFile zipFile = null;
                try {
                    zipFile = new ZipFile(entry);
                    Enumeration e = zipFile.entries();
                    while (e.hasMoreElements()) {
                        ZipEntry zipEntry = (ZipEntry)e.nextElement();
                        String name = zipEntry.getName();
                        if (name.endsWith(".class")) {
                            addTo.addElement(name.substring(0, name.length() - ".class".length()));
                        }
                    }
                } catch (IOException ioe) {
                    System.err.println("Exception while opening/reading " + entry);
                    continue;
                } finally {
                    if (zipFile != null) {
                        try {
                            zipFile.close();
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    }
                }

            }
            else {
                String dirName = entry;

                /*
                 * Add ending '/' if it's missing
                 */
                if (!dirName.endsWith("/")) {
                    dirName += "/";
                }

                /*
                 * Convert to file system specific path and search
                 */
                File dir = new File(dirName.replace('/',File.separatorChar));
                try {
                    find(dir, ".class", classes, false);
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                    continue;
                }
                for (int i = 0; i != classes.size(); i++) {
                    String file = (String)classes.elementAt(i);
                    file = file.replace(File.separatorChar,'/');
                    if (!(file.startsWith(dirName) && file.endsWith(".class"))) {
                        throw new RuntimeException("find went wrong: file="+file+", dirName="+dirName);
                    }
                    file = file.substring(dirName.length(),file.length()-6);
                    uniqClasses.add(file);
                }
            }
        }
        addTo.addAll(uniqClasses);
    }

    /**
     * Find all the files or directories under a given directory recursively.
     *
     * @param   dir      the directory to search from
     * @param   suffix   the suffix used to filter the results or null
     *                   for no filtering
     * @param   results  the Vector into which the found files should be put
     * @throws  IOException
     */
    public static void find(File dir, String suffix, Vector results, boolean dirs) throws IOException {
        File[] files = dir.listFiles();
        if (dirs && (suffix == null || dir.getName().endsWith(suffix))) {
            results.addElement(dir.getPath());
        }
        if (files == null) {
            return;
        }
        for(int i = 0 ; i < files.length ; i++) {
            File f = files[i];
            if (f.isDirectory()) {
                find(f, suffix, results, dirs);
            } else {
                if (!dirs && (suffix == null || f.getName().endsWith(suffix))) {
                    results.addElement(f.getPath());
                }
            }
        }
    }
}