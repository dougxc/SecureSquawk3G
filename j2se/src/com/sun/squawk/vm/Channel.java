/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */
package com.sun.squawk.vm;

import java.io.*;
import javax.microedition.io.*;

/**
 * The base class for the specialized channels for stream IO, graphic operations and event handling.
 */
public abstract class Channel implements java.io.Serializable {

    /**
     * The identifier of this channel.
     */
    private final int channelID;

    /**
     * The result of the last operation on this channel.
     */
    protected long result;

    /**
     * The owner of this channel.
     */
    protected final ChannelIO cio;

    /**
     * The event number used for blocking.
     */
    int eventNumber;

    /**
     * Constructor.
     *
     * @param cio        the owner of this channel
     * @param channelID  the identifier of this channel
     * @param logging    specifies if the channel is to log its operations
     */
    public Channel(ChannelIO cio, int channelID) {
        this.cio = cio;
        this.channelID = channelID;
        eventNumber = EventQueue.getNextEventNumber();
    }

    /**
     * Executes an operation on the given channel.
     *
     * @param  op  the operation to perform
     * @param  i1
     * @param  i2
     * @param  i3
     * @param  i4
     * @param  i5
     * @param  i6
     * @param  o1
     * @param  o2
     * @param  o3
     * @return the result
     */
    abstract int execute(
                          int    op,
                          int    i1,
                          int    i2,
                          int    i3,
                          int    i4,
                          int    i5,
                          int    i6,
                          Object o1,
                          Object o2
                        ) throws IOException;


    /**
     * Clear the result.
     */
    public final void clearResult() {
        result = 0;
    }

    /**
     * Gets the result of the last successful operation on this channel. The value
     * returned is undefined if the last operation on this channel was not successful
     * or did not generate a result value.
     *
     * @return long the result of the last successful operation on this channel
     */
    public final long getResult() {
        return result;
    }

    /**
     * Gets the identifier of this channel.
     *
     * @return  the identifier of this channel
     */
    public final int getChannelID() {
        return channelID;
    }

    /**
     * Gets the identifier of this channel.
     *
     * @return  the identifier of this channel
     */
    public final int getEventNumber() {
        return eventNumber;
    }

    /**
     * Closes this channel.
     */
    public abstract void close();
}

