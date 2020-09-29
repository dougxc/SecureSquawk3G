//if[RESOURCE.CONNECTION]
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

package com.sun.squawk.io.j2me.resource;

import java.io.*;
import javax.microedition.io.*;
import com.sun.squawk.io.*;
import com.sun.squawk.io.connections.*;


/**
 * This class implements the default "resource:" protocol for KVM.
 *
 * The default is to open a file based upon the resource name.
 *
 * @author  Nik Shaylor
 * @version 1.0 2/12/2000
 */
public class Protocol extends ConnectionBase implements InputConnection {

    private ClasspathConnection pathConnection;
    private String name;

    /**
     * Open the connection
     */
    public Connection open(String protocol, String name, int mode, boolean timeouts) throws IOException {
        String resourcePath = Thread.currentThread().getIsolate().getClassPath();
//System.out.println("name="+name+" resourcePath="+resourcePath);
        if (resourcePath == null) {
            resourcePath = ".";
        }
        if (pathConnection == null) {
            pathConnection = (ClasspathConnection)Connector.open("classpath://" + resourcePath);
        }
        this.name = name;
        return this;
    }

    public InputStream openInputStream() throws IOException {
//System.out.println("name="+name);
        return pathConnection.openInputStream(name);
    }

    public void close() throws IOException {
    }
}
