/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM translator.
 */
package com.sun.squawk.translator.ci;

import java.io.ByteArrayInputStream;

/**
 * This is a subclass of <code>ByteArrayInputStream</code> that enables the
 * current read position to be queried. That is, the index in the underlying
 * array corresponding to the current read position can be queried.
 */
public final class IndexedInputStream extends ByteArrayInputStream {

    /**
     * Creates an <code>IndexedInputStream</code> instance.
     *
     * @param   buf      the input buffer
     * @param   offset   the offset in the buffer of the first byte to read
     * @param   length   the maximum number of bytes to read from the buffer
     */
    IndexedInputStream(byte[] buf, int offset, int length) {
        super(buf, offset, length);
    }

    /**
     * Gets the index in the underlying byte array buffer corresponding to the
     * current read position of the stream. Note that the returned value is
     * the index relative to the offset of the first byte read by this stream.
     *
     * @return  the index in the underlying byte array buffer corresponding
     *          to the current read position of the stream. This is the index
     *          relative to value of the <code>offset</code> parameter in the
     *          constructor.
     */
    int getCurrentIndex() {
        return pos - mark;
    }

    /**
     * Gets the underlying byte array.
     *
     * @return the underlying byte array
     */
    byte[] getBuffer() {
        return buf;
    }
}
