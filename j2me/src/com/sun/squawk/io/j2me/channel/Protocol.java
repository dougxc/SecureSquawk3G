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

package com.sun.squawk.io.j2me.channel;

import java.io.*;
import javax.microedition.io.*;
import com.sun.squawk.io.*;
import com.sun.squawk.vm.ChannelConstants;

/**
 * Channel Connection
 */

public class Protocol extends ConnectionBase implements StreamConnection, StreamConnectionNotifier {

    /**
     * Channel number.
     */
    int channelID = -1;

    /**
     * Channel use count.
     */
    int useCount = 0;

    /**
     * Public constructor
     */
    public Protocol() {
    }

    /**
     * Private constructor
     */
    private Protocol(int chan) {
        this.channelID = chan;
        useCount++;
    }

    /**
     * open
     */
    public Connection open(String protocol, String name, int mode, boolean timeouts) throws IOException {
        if (protocol == null || name == null) {
            throw new NullPointerException();
        }
        channelID = VM.getChannel(ChannelConstants.CHANNEL_GENERIC);
        useCount++;
        VM.execIO(ChannelConstants.OPENCONNECTION, channelID, mode, timeouts?1:0, 0, 0, 0, 0, protocol+":"+name, null);
        return this;
    }

    /**
     * openInputStream
     */
    public InputStream openInputStream() throws IOException {
        useCount++;
        InputStream is = new ChannelInputStream(this);
/*if[BUFFERCHANNELINPUT]*/
        is = new BufferedInputStream(is);
/*end[BUFFERCHANNELINPUT]*/
        return is;
    }

    /**
     * openOutputStream
     */
    public OutputStream openOutputStream() throws IOException {
        useCount++;
        return new ChannelOutputStream(this);
    }

    /**
     * acceptAndOpen
     */
    public StreamConnection acceptAndOpen() throws IOException {
        int newChan = VM.execIO(ChannelConstants.ACCEPTCONNECTION, channelID, 0, 0, 0, 0, 0, 0, null, null);
        return new Protocol(newChan);
    }

    /**
     * Close the connection.
     */
    synchronized public void close() throws IOException {
        VM.execIO(ChannelConstants.CLOSECONNECTION, channelID, 0, 0, 0, 0, 0, 0, null, null);
        decrementCount();
    }

    /**
     * Decrement channel use count.
     */
    void decrementCount() {
        if (useCount > 0) {
            useCount--;
            if (useCount == 0) {
                finalize();
                VM.eliminateFinalizer(this);
            }
        }
    }

    /**
     * finalize
     */
    protected void finalize() {
        if (channelID != -1) {
            try {
                VM.freeChannel(channelID);
            } catch (IOException ioe) {
                System.err.println(ioe);
            }
        }
        channelID = -1;
    }
}
