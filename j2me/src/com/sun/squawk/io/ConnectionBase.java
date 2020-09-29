/*
 *  Copyright (c) 1999-2001 Sun Microsystems, Inc., 901 San Antonio Road,
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

package com.sun.squawk.io;

import java.io.*;
import javax.microedition.io.*;

/**
 * Base class for Connection protocols.
 */
abstract public class ConnectionBase implements Connection {

    /**
     * Open a connection to a target.
     *
     * @param protocol         The URL protocol
     * @param name             The URL for the connection
     * @param mode             The access mode
     * @param timeouts         A flag to indicate that the caller
     *                         wants timeout exceptions
     * @return                 A new Connection object
     *
     * @exception IllegalArgumentException If a parameter is invalid.
     * @exception ConnectionNotFoundException If the connection cannot
     *                                        be found.
     * @exception IOException  If some other kind of I/O error occurs.
     */
    abstract public Connection open(String protocol, String name, int mode, boolean timeouts) throws IOException;

    /**
     * Open and return a data input stream for a connection.
     *
     * @return                 An input stream
     * @exception IOException  If an I/O error occurs
     */
    public InputStream openInputStream() throws IOException {
        return null;
    }

    /**
     * Open and return a data output stream for a connection.
     *
     * @return                 An input stream
     * @exception IOException  If an I/O error occurs
     */
    public OutputStream openOutputStream() throws IOException {
        return null;
    }

    /**
     * Close
     *
     * @exception IOException  If an I/O error occurs
     */
    public void close() throws IOException {
    }


    /**
     * Create and open a connection input stream.
     *
     * @return                 A DataInputStream.
     *
     * @exception IllegalArgumentException If a parameter is invalid.
     * @exception ConnectionNotFoundException If the connection cannot
     *                                        be found.
     * @exception IOException  If some other kind of I/O error occurs.
     */
    public DataInputStream openDataInputStream() throws IOException {
        InputStream is = openInputStream();
        if (!(is instanceof DataInputStream)) {
           is = new DataInputStream(is);
        }
        return (DataInputStream)is;
    }

    /**
     * Create and open a connection output stream.
     *
     * @return                 A DataOutputStream.
     *
     * @exception IllegalArgumentException If a parameter is invalid.
     * @exception ConnectionNotFoundException If the connection cannot
     *                                        be found.
     * @exception IOException  If some other kind of I/O error occurs.
     */
    public DataOutputStream openDataOutputStream() throws IOException {
        OutputStream os = openOutputStream();
        if (!(os instanceof DataOutputStream)) {
           os = new DataOutputStream(os);
        }
        return (DataOutputStream)os;
    }
}


