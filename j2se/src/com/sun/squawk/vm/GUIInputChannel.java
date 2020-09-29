/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */
package com.sun.squawk.vm;

import java.util.*;
import com.sun.squawk.vm.ChannelConstants;

/**
 * Special channel for input events.
 */
public class GUIInputChannel extends Channel {

    Vector inputQueue = new Vector();
    boolean blocked;

    /**
     * Creates the channel.
     *
     * @param cio        the owner of the channel
     * @param channelID  the identifier of the channel
     */
    public GUIInputChannel(ChannelIO cio, int channelID) {
        super(cio, channelID);
        if (channelID != ChannelConstants.CHANNEL_GUIIN) {
            throw new RuntimeException("The GUI imput channel must have identifier " + ChannelConstants.CHANNEL_GUIIN);
        }
    }

    /**
     * {@inheritDoc}
     */
    synchronized int execute(
                              int    op,
                              int    i1,
                              int    i2,
                              int    i3,
                              int    i4,
                              int    i5,
                              int    i6,
                              Object o1,
                              Object o2
                            ) {
        switch (op) {
            case ChannelConstants.READLONG: {
                if (inputQueue.size() == 0) {
                    blocked = true;
                    return getEventNumber(); // Block the channel
                }
                Long event = (Long)inputQueue.firstElement();
                inputQueue.removeElementAt(0);
                result = event.longValue();
                if (ChannelIO.TRACING_ENABLED) ChannelIO.trace("execute result = "+result);
                break;
            }
            default: throw new RuntimeException("Illegal channel operation "+op);
        }
        return 0;
    }

    /**
     * Unblocks the thread waiting on this channel.
     */
    private void unblock() {
        if (blocked) {
            blocked = false;
            result = 0; // blocked
            cio.unblock(getEventNumber()); // Unblock the channel
        }
    }

    /**
     * Adds an event to the queue of events.
     */
    synchronized void addToGUIInputQueue(int key1_high, int key1_low, int key2_high, int key2_low) {
        long key1 = (key1_high << 16) | (key1_low & 0x0000FFFF);
        long key2 = (key2_high << 16) | (key2_low & 0x0000FFFF);
        long event = key1 << 32 | (key2 & 0x00000000FFFFFFFFL);
        inputQueue.addElement(new Long(event));
        unblock();
    }

    /**
     * Closes this channel.
     */
    public void close() {
        unblock();
    }

    /**
     * Debugging.
     */
    private void debug(long result) {
        int key1 = (int)(result >> 32);
        int key2 = (int)(result);
        int key1_H = (key1 >> 16) & 0xFFFF;
        int key1_L =  key1 & 0xFFFF;
        int key2_H = (key2 >> 16) & 0xFFFF;
        int key2_L =  key2 & 0xFFFF;
        System.err.println("["+cio.getCIOIndex()+"] "+key1_H+":"+key1_L+":"+key2_H+":"+key2_L);
    }
}

