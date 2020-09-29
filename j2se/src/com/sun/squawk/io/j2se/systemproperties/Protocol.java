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

package com.sun.squawk.io.j2se.systemproperties;

import java.io.*;
import java.util.*;
import javax.microedition.io.*;
import com.sun.squawk.io.*;

/**
 * Simple protocol to read system properties.
 *
 * @author  Doug Simon
 */
public class Protocol extends ConnectionBase implements InputConnection {

    /**
     * Open the connection
     * @param name       the target for the connection
     * @param timeouts   a flag to indicate that the called wants
     *                   timeout exceptions
     */
     public Connection open(String protocol, String name, int mode, boolean timeouts) throws IOException {
         if (!name.equals("")) {
             throw new IllegalArgumentException( "Bad protocol option:" + name);
         }
         return this;
     }


    /**
     * Return the system properties as a stream of UTF8 encoded <name,value> pairs.
     *
     * @return     an input stream for reading bytes from this socket.
     * @exception  IOException  if an I/O error occurs when creating the
     *                          input stream.
     */
    public InputStream openInputStream() throws IOException {

        Properties properties = System.getProperties();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        for (Iterator iterator = properties.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry)iterator.next();
            dos.writeUTF((String)entry.getKey());
            dos.writeUTF((String)entry.getValue());
        }
        baos.close();
        return new DataInputStream(new ByteArrayInputStream(baos.toByteArray()));
    }

    /**
     * Test driver.
     */
    public static void main(String[] args) throws IOException {
        DataInputStream propertiesStream = Connector.openDataInputStream("systemproperties:");
        Properties properties = new Properties();
        while (propertiesStream.available() != 0) {
            properties.setProperty(propertiesStream.readUTF(), propertiesStream.readUTF());
        }
        properties.list(System.out);
    }
}
