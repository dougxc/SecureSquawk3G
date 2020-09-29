/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM translator.
 */
package com.sun.squawk.translator.ci;

import com.sun.squawk.translator.*;
import com.sun.squawk.util.*;

import java.io.*;

/**
 * An instance of <code>ClassFileReader</code> is used to read an input
 * stream opened on a class file.
 *
 * @author  Doug Simon
 */
public final class ClassFileReader extends StructuredFileInputStream {

    /**
     * Creates a <code>ClassFileReader</code> that reads class components
     * from a given input stream.
     *
     * @param   in        the input stream
     * @param   filePath  the file from which <code>in</code> was created
     */
    public ClassFileReader(InputStream in, String filePath) {
        super(in, filePath, "classfile");
    }

    /**
     * Throw a ClassFormatError instance to indicate there was an IO error
     * or malformed class file error while reading the class.
     *
     * @param   msg  the cause of the error
     * @return  the LinkageError raised
     */
    public LinkageError formatError(String msg) {
        if (msg == null) {
            Translator.throwClassFormatError(getFileName());
        }
        Translator.throwClassFormatError(getFileName()+": "+msg);
        return null;
    }
}
