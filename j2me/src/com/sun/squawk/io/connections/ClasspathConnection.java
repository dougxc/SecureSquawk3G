/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 */

package com.sun.squawk.io.connections;

import java.io.*;
import javax.microedition.io.*;

public interface ClasspathConnection extends Connection {

    public InputStream openInputStream(String name) throws IOException;

}

