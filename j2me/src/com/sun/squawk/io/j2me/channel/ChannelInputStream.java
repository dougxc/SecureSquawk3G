/*
 * Copyright 1994-2001 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 */

package com.sun.squawk.io.j2me.channel;
import com.sun.squawk.vm.ChannelConstants;

import java.io.*;

/**
 * ChannelInputStream
 */
public class ChannelInputStream extends DataInputStream {

    Protocol parent;
    int channelID;

    public ChannelInputStream(Protocol parent) throws IOException {
        super(null);
        this.parent = parent;
        this.channelID = parent.channelID;
        VM.execIO(ChannelConstants.OPENINPUT, channelID, 0, 0, 0, 0, 0, 0, null, null);
    }

    public void close() throws IOException {
        if (channelID != -1) {
            VM.execIO(ChannelConstants.CLOSEINPUT, channelID, 0, 0, 0, 0, 0, 0, null, null);
            channelID = -1;
            parent.decrementCount();
        }
    }

    public int read() throws IOException {
        return VM.execIO(ChannelConstants.READBYTE, channelID, 0, 0, 0, 0, 0, 0, null, null);
    }

    public int readUnsignedShort() throws IOException {
        return VM.execIO(ChannelConstants.READSHORT, channelID, 0, 0, 0, 0, 0, 0, null, null);
    }

    public int readInt() throws IOException {
        return VM.execIO(ChannelConstants.READINT, channelID, 0, 0, 0, 0, 0, 0, null, null);
    }

    public long readLong() throws IOException {
        return VM.execIOLong(ChannelConstants.READLONG, channelID, 0, 0, 0, 0, 0, 0, null, null);
    }

    public int read(byte b[], int off, int len) throws IOException {
        if (b == null) {
            throw new NullPointerException();
        }
        return VM.execIO(ChannelConstants.READBUF, channelID, off, len, 0, 0, 0, 0, null, b);
    }

    public int read(byte b[]) throws IOException {
        return read(b, 0, b.length);
    }

    public long skip(long n) throws IOException {
        return VM.execIO(ChannelConstants.SKIP, channelID, (int)(n >>> 32), (int)n, 0, 0, 0, 0, null, null);
    }

    public int available() throws IOException {
        return VM.execIO(ChannelConstants.AVAILABLE, channelID, 0, 0, 0, 0, 0, 0, null, null);
    }

    public void mark(int readlimit) {
        try {
            VM.execIO(ChannelConstants.MARK, channelID, readlimit, 0, 0, 0, 0, 0, null, null);
        } catch (IOException ex) {}

    }

    public void reset() throws IOException {
        VM.execIO(ChannelConstants.RESET, channelID, 0, 0, 0, 0, 0, 0, null, null);
    }

    public boolean markSupported() {
        try {
            int res = VM.execIO(ChannelConstants.MARK, channelID, 0, 0, 0, 0, 0, 0, null, null);
            return res != 0;
        } catch (IOException ex) {
            return false;
        }
    }

}


