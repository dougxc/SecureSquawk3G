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

package com.sun.squawk.io.j2se.file;

import java.io.*;
import java.util.*;
import javax.microedition.io.*;
import com.sun.squawk.io.j2se.*;
import com.sun.squawk.io.*;

/**
 * GenericStreamConnection to the J2SE file API.
 *
 * @author  Nik Shaylor
 * @version 1.0 10/08/99
 */

public class Protocol extends ConnectionBase implements StreamConnection {

    /** FileInputStream object */
    InputStream fis;

    /** FileInputStream object */
    OutputStream fos;

    /**
     * Open the connection
     */
    public void open(String name, int mode, boolean timeouts) throws IOException {
        throw new RuntimeException("Should not be called");
    }

    /**
     * Open the connection to file.
     * @param name the target for the connection (including any parameters).
     * @param timeouts A flag to indicate that the called wants timeout exceptions
     * <p>
     * The name string for this protocol should be:
     *             "<absolute or relavtive file name>[;<name>=<value>]*"
     *
     *             Any additional parameters must be separated by a ";" and
     *             spaces are not allowed.
     *
     *             The optional parameters are:
     *
     *             append:    Specifies if the file should be opened in append mode. Default is false.
     */
    public Connection open(String protocol, String name, int mode, boolean timeouts) throws IOException {

        if(name.charAt(0) != '/' || name.charAt(1) != '/') {
            throw new IllegalArgumentException("Protocol must start with \"//\" "+name);
        }

        boolean append = false;

        int parmIndex = name.indexOf(';', 2);
        if (parmIndex < 0) {
            name = name.substring(2);
        }
        else {
            String parms = name.substring(parmIndex);
            name = name.substring(2, parmIndex);
            StringTokenizer st = new StringTokenizer(parms, "=;", true);
            while (st.hasMoreTokens()) {
                try {
                    if (!st.nextToken().equals(";")) {
                        throw new NoSuchElementException();
                    }
                    String key = st.nextToken();
                    if (!st.nextToken().equals("=")) {
                        throw new NoSuchElementException();
                    }
                    String value = st.nextToken();
                    if (key.equals("append")) {
                        append = value.equals("true");
                    }
                    // ignore other parameters...
                } catch (NoSuchElementException nsee) {
                    throw new IllegalArgumentException("Bad param string: " + parms);
                }
            }
        }

//System.err.println("opening " + name);

        try {
            if (mode == Connector.READ) {
                if (name.endsWith("/")) {
                    fis = getListingFor(name);
                } else {
                    fis = new FileInputStream(name);
                }
            } else if (mode == Connector.WRITE) {
                fos = new FileOutputStream(name, append);
            } else {
                throw new IllegalArgumentException("Bad mode");
            }
        } catch (IOException ex) {
            throw new ConnectionNotFoundException(name);
        }
        return this;
    }

    /**
     * Returns a directory listing
     */
    private InputStream getListingFor(String dirName) throws IOException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        getListingForPrim(dirName, dos, dirName.endsWith("//"));
        dos.close();
        return new ByteArrayInputStream(baos.toByteArray());
    }

    /**
     * Build a directory listing
     */
    private void getListingForPrim(String dirName, DataOutputStream dos, boolean recursive) throws IOException {

        File dir = new File(dirName.replace('/',File.separatorChar));

        File[] files = dir.listFiles();
        if (files == null) {
            throw new ConnectionNotFoundException(dirName);
        }

        for(int i = 0 ; i < files.length ; i++) {
            File f = files[i];
            if (!f.isDirectory()) {
                dos.writeUTF(f.getPath().replace('\\','/'));
            }
            else {
                if (recursive) {
                    getListingForPrim(f.getPath(), dos, recursive);
                }
            }
        }
    }

    /**
     * Returns an input stream for this socket.
     *
     * @return     an input stream for reading bytes from this socket.
     * @exception  IOException  if an I/O error occurs when creating the
     *                          input stream.
     */
    public InputStream openInputStream() throws IOException {
        if (fis == null) {
            throw new IllegalArgumentException("Bad mode");
        }
        InputStream res = new UniversalFilterInputStream(this, fis);
        fis = null;
        return new BufferedInputStream(res);
    }

    /**
     * Returns an output stream for this socket.
     *
     * @return     an output stream for writing bytes to this socket.
     * @exception  IOException  if an I/O error occurs when creating the
     *                          output stream.
     */
    public OutputStream openOutputStream() throws IOException {
        if (fos == null) {
            throw new IllegalArgumentException("Bad mode");
        }
        OutputStream res = new UniversalFilterOutputStream(this, fos);
        fos = null;
        return res;
    }

    /**
     * Close the connection.
     *
     * @exception  IOException  if an I/O error occurs when closing the
     *                          connection.
     */
    public void close() throws IOException {
    }

}
