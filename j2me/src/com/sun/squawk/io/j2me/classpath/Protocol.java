/*
 *  Copyright (c) 1999 Sun Microsystems, Inc., 901 San Antonio Road,
 *  Palo Alto, CA 94303, U.S.A.  All Rights Reserved.
 *
 *  Sun Microsystems, Inc. has intellectual property rights relating
 *  to the technology embodied in this software.  In particular, and
 *  without limitation, these intellectual property rights may include
 *  one or more U.S. patents, foreign patents, or pending
 *  applications.  Sun, Sun Microsystems, the Sun logo, Java, KJava,
 *  and all Sun-based and Java-based marks are trademarks or
 *  registered trademarks of Sun Microsystems, Inc.  in the United
 *  States and other countries.
 *
 *  This software is distributed under licenses restricting its use,
 *  copying, distribution, and decompilation.  No part of this
 *  software may be reproduced in any form by any means without prior
 *  written authorization of Sun and its licensors, if any.
 *
 *  FEDERAL ACQUISITIONS:  Commercial Software -- Government Users
 *  Subject to Standard License Terms and Conditions
 */

package com.sun.squawk.io.j2me.classpath;

import java.io.*;
import java.util.*;
import javax.microedition.io.*;
import com.sun.squawk.io.*;
import com.sun.squawk.io.connections.*;
import com.sun.squawk.util.Vector;    // Version without synchronization
import com.sun.squawk.util.Hashtable; // Version without synchronization


/**
 * Simple protocol to set connection variables
 *
 * @author  Nik Shaylor
 */
public class Protocol extends ConnectionBase implements ClasspathConnection {

    public static final String sepch;

    private boolean MULTIPLEINSTANCESCHECK = false;

    static {
        String sep = System.getProperty("path.separator");
        if (sep != null ) {
            sepch = sep;
        } else {
            sepch = "" + VM.getPathSeparatorChar();
        }
    }

    /**
     * The classpath array
     */
    private Vector classPathArray = new Vector();

    /**
     * Open the connection
     */
    public Connection open(String protocol, String name, int mode, boolean timeouts) throws IOException {

//System.out.println("classpath: name="+name);

        if(name.charAt(0) != '/' || name.charAt(1) != '/') {
            throw new IllegalArgumentException("Protocol must start with \"//\" "+name);
        }

        String path = name.substring(2);

        StringTokenizer st = new StringTokenizer(path, sepch);
        while (st.hasMoreTokens()) {
            String dirName = st.nextToken();
            if (dirName.endsWith("\\") || dirName.endsWith("/")) {
                dirName = dirName.substring(0, dirName.length() - 1);
            }
            classPathArray.addElement(dirName);
        }

        return this;
    }

    /**
     * If <code>fileName</code> ends with "/" or "//" then this is treated as a
     * directory listing request, where a "//" ending implies a recursive listing.
     *
     * @return     an input stream for reading bytes from this socket.
     * @exception  IOException  if an I/O error occurs when creating the
     *                          input stream.
     */
    public InputStream openInputStream(String fileName) throws IOException {

        if (fileName.endsWith("/")) {
            return lookupRequest(fileName);
        }

        InputStream is = null;
        for (int i = 0  ; i < classPathArray.size(); i++) {

           /*
            * Get the section of the classpath array
            */
            String classPathEntry = (String)classPathArray.elementAt(i);

//System.out.println("classPathEntry = "+classPathEntry);

            if (classPathEntry.endsWith(".zip") || classPathEntry.endsWith(".jar")) {

               /*
                * Open the file inside a zip file
                */
                try {
//System.out.println("trying "+"zip://"+classPathEntry+"~"+fileName);
                    InputStream _is = Connector.openDataInputStream("zip://"+classPathEntry+"@"+fileName);
                    if (is == null)  {
                        if (!MULTIPLEINSTANCESCHECK) {
                            return _is;
                        }
                        is = _is;
                    } else {
                        _is.close();
                        if (fileName.endsWith(".class")) {
                            System.err.println("Warning: multiple instances of " + fileName + " in class path may cause linkage errors");
                        }
                    }
                } catch (ConnectionNotFoundException e) {
                }

            } else {

               /*
                * Open the file
                */
                try {
//System.out.println("trying "+"file://"+classPathEntry+"/"+fileName);
                    InputStream _is = Connector.openDataInputStream("file://" + classPathEntry + "/" + fileName);
                    if (is == null)  {
                        if (!MULTIPLEINSTANCESCHECK) {
                            return _is;
                        }
                        is = _is;
                    } else {
                        _is.close();
                        if (fileName.endsWith(".class")) {
                            System.err.println("Warning: multiple instances of " + fileName + " on class path may cause linkage errors");
                        }
                    }
                } catch (ConnectionNotFoundException e ) {
                }
            }
        }

        if (is == null) {
            throw new ConnectionNotFoundException(fileName);
        }

        return is;
    }


    /**
     * Get a file listing for the pattern.
     */
    public InputStream lookupRequest(String fileName) throws IOException {

        Hashtable table = new Hashtable();

        for (int i = 0  ; i < classPathArray.size() ; i++) {

            DataInputStream is = null;

           /*
            * Get the section of the classpath array
            */
            String classPathEntry = (String)classPathArray.elementAt(i);
            String prefix;
            if (classPathEntry.endsWith(".zip") || classPathEntry.endsWith(".jar")) {

                prefix = null;
               /*
                * Open the file inside a zip file
                */
                try {
                    is = Connector.openDataInputStream("zip://"+classPathEntry+"@"+fileName);
                } catch (ConnectionNotFoundException ex) {
                }

            } else {

                prefix = classPathEntry.replace('\\', '/') + '/';
               /*
                * Open the file
                */
                try {
                    is = Connector.openDataInputStream("file://"+classPathEntry+"/"+fileName);
                } catch (ConnectionNotFoundException ex) {
                }
            }

            if (is != null) {
                try {
                    for (;;) {
                        String str = is.readUTF();
                        // Make entry relative to this classpath entry
                        if (prefix != null && str.startsWith(prefix)) {
                            str = str.substring(prefix.length());
                        }
                        if (table.get(str) == null) {
                            table.put(str, str);
                        }
                    }
                } catch (EOFException ex) {
                }
                is.close();
            }
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        for (Enumeration e = table.keys() ; e.hasMoreElements() ; ) {
            String str = (String)e.nextElement();
            dos.writeUTF(str);
        }
        dos.close();
        return new ByteArrayInputStream(baos.toByteArray());
    }



    /**
     * Test driver.
     */
    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            ClasspathConnection classpath = (ClasspathConnection)Connector.open("classpath://j2se/classes;j2me/classes");
            print(classpath, "com/sun/squawk/util/");
            classpath.close();
        }
        else {
            ClasspathConnection classpath = (ClasspathConnection)Connector.open("classpath://" + args[0]);
            for (int i = 1; i != args.length; i++) {
                print(classpath, args[i]);
            }
            classpath.close();
        }
    }

    private static void print(ClasspathConnection classpath, String entry) throws IOException {
        DataInputStream dis = new DataInputStream(classpath.openInputStream(entry));
        System.out.println("Directory: "+entry);
        try {
            for (;;) {
                System.out.println(dis.readUTF());
            }
        } catch (EOFException ex) {
        }
        dis.close();
    }
}
