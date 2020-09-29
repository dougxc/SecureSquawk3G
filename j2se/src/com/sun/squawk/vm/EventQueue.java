/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */
package com.sun.squawk.vm;

import java.util.Vector;

/**
 * Sentinal object used when waiting for events.
 */
public class EventQueue {

    private static Vector events = new Vector();

    private static int nextEventNumber = 1;

    /*
     * waitFor
     */
    static void waitFor(long time) {
        if (ChannelIO.TRACING_ENABLED) ChannelIO.trace("++waitFor "+time);
        synchronized(events) {
            if (events.size() == 0) {
                try { events.wait(time); } catch(InterruptedException ex) {}
            }
        }
        if (ChannelIO.TRACING_ENABLED) ChannelIO.trace("--waitFor");
    }

    /*
     * getNextEventNumber
     */
    static int getNextEventNumber() {
        if (nextEventNumber == Integer.MAX_VALUE) {
            System.err.println("Reached event number limit"); // TEMP -- Need a way to recycle event numbers
            System.exit(0);
        }
        return nextEventNumber++;
    }

    /*
     * unblock
     */
    static void unblock(int event) {
        synchronized(events) {
            events.addElement(new Integer(event));
            events.notifyAll();
        }
        if (ChannelIO.TRACING_ENABLED) ChannelIO.trace("++unblock ");
    }

    /*
     * getEvent
     */
    static int getEvent() {
        int res = 0;
        synchronized(events) {
            if (events.size() > 0) {
                Integer event = (Integer)events.firstElement();
                events.removeElementAt(0);
                res = event.intValue();
            }
        }
        if (ChannelIO.TRACING_ENABLED) ChannelIO.trace("++getEvent = "+res);
        return res;
    }

}


