/*
 * Copyright 1994-2001 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 */

package com.sun.squawk.io.j2me.channel;

import java.io.*;
import com.sun.squawk.vm.ChannelConstants;

/**
 * ChannelOutputStream
 */
public class ChannelOutputStream extends DataOutputStream {

    Protocol parent;
    int channelID;

    public ChannelOutputStream(Protocol parent) throws IOException {
        super(null);
        this.parent = parent;
        this.channelID = parent.channelID;
        VM.execIO(ChannelConstants.OPENOUTPUT, channelID, 0, 0, 0, 0, 0, 0, null, null);
    }

    public void flush() throws IOException {
        VM.execIO(ChannelConstants.FLUSH, channelID, 0, 0, 0, 0, 0, 0, null, null);
    }

    public void close() throws IOException {
        VM.execIO(ChannelConstants.CLOSEOUTPUT, channelID, 0, 0, 0, 0, 0, 0, null, null);
        channelID = -1;
        parent.decrementCount();
    }

    public void write(int v) throws IOException {
        VM.execIO(ChannelConstants.WRITEBYTE, channelID, v, 0, 0, 0, 0, 0, null, null);
    }

    public void writeShort(int v) throws IOException {
        VM.execIO(ChannelConstants.WRITESHORT, channelID, v, 0, 0, 0, 0, 0, null, null);
    }

    public void writeChar(int v) throws IOException {
        writeShort(v);
    }

    public void writeInt(int v) throws IOException {
        VM.execIO(ChannelConstants.WRITEINT, channelID, v, 0, 0, 0, 0, 0, null, null);
    }

    public void writeLong(long v) throws IOException {
        VM.execIO(ChannelConstants.WRITELONG, channelID, (int)(v >>> 32), (int)v, 0, 0, 0, 0, null, null);
    }

    public void write(byte b[], int off, int len) throws IOException {
        if (b == null) {
            throw new NullPointerException();
        }
        VM.execIO(ChannelConstants.WRITEBUF, channelID, off, len, 0, 0, 0, 0, b, null);
    }

}


