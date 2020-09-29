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
import java.util.*;
import javax.microedition.io.*;
import com.sun.squawk.util.*;
import com.sun.squawk.vm.ChannelConstants;
import java.net.*;

/**
 * This class contains the host side of the Squawk channel architecture that is used
 * to implement IO, graphics and events. A separate ChannelIO instance provides the
 * IO system for each isolate.
 */
public class ChannelIO implements java.io.Serializable {

    /**
     * The table of channel I/O instances.
     */
    private static IntHashtable cios = new IntHashtable();

    /**
     * The next availible cio index.
     */
    private static int nextcio = 1;

    /**
     * The channel context (used only for debugging)
     */
    private int context;

    /**
     * Flags if this IO system is in rundown mode.
     */
    private boolean rundown;

    /**
     * The table of channels open by this IO system.
     */
    private IntHashtable channels = new IntHashtable();

    /**
     * Used to allocate a new channel ID. Channels 0, 1, and 2 are reserved.
     */
    private int nextAvailableChannelID = 3;

    /**
     * The special events channel.
     */
    private GUIInputChannel guiInputChannel;

    /**
     * The special graphics channel.
     */
    private GUIOutputChannel guiOutputChannel;

    /**
     * The name of the class of the exception (if any) that occurred in the last call to
     * {@link execute()} with a channel ID of 0 or to {@link hibernate()}.
     */
    private String exceptionClassName;

    /**
     * The result of the last channel operation.
     */
    private long theResult;

    /**
     * Determines if tracing is enabled.
     */
    static final boolean TRACING_ENABLED = System.getProperty("cio.tracing", "false").equals("true");

    /**
     * Determines if loggin is enabled.
     */
    static final boolean LOGGING_ENABLED = System.getProperty("cio.logging", "false").equals("true");

    /**
     * Prints a trace message on a line.
     *
     * @param msg   the trace message to print
     */
    static void trace(String msg) {
        System.out.println(msg);
    }

    /**
     * Executes an operation on a given channel.
     *
     * @param  context the identifier of the channel context.
     * @param  op      the operation to perform
     * @param  channel the identifier of the channel to execute the operation on
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
    public static int execute(
                               int    context,
                               int    op,
                               int    channel,
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
            case ChannelConstants.GLOBAL_GETEVENT: {
                return EventQueue.getEvent();
            }
            case ChannelConstants.GLOBAL_WAITFOREVENT: {
                long time = (((long)i1) << 32) | (((long)i2) & 0x00000000FFFFFFFFL);
                EventQueue.waitFor(time);
                return ChannelConstants.RESULT_OK;
            }
            case ChannelConstants.GLOBAL_CREATECONTEXT: {
                ChannelIO cio = createCIO(i1);
                int index = nextcio++;
                cios.put(index, cio);
                cio.context = index;
                return index;
            }
            default: {
                ChannelIO cio = (ChannelIO)cios.get(context);
                if (cio == null) {
                    return ChannelConstants.RESULT_BADCONTEXT;
                }
                int res = cio.execute(op, channel, i1, i2, i3, i4, i5, i6, o1, o2);
                if (op == ChannelConstants.GLOBAL_DELETECONTEXT) {
                    cios.remove(context);
                }
                return res;
            }
        }
    }

    /**
     * Gets the result cio index.
     *
     * @return  the value
     */
    int getCIOIndex() {
        return context;
    }

    /**
     * Static constructor to create a ChannelIO system for an isolate.
     *
     * @param serialize_handle  the handle of a serialized ChannelIO object or 0 if this is
     *                          for a non-hibernated isolate
     * @return ChannelIO the created IO system
     */
    private static ChannelIO createCIO(int serialize_handle) {
        if (serialize_handle != 0) {
            try {
                String file = serialize_handle + ".cio";
//System.out.println("Deserializing channel from "+file);
                FileInputStream in = new FileInputStream(file);
                ObjectInputStream ois = new ObjectInputStream(in);
                ChannelIO cio = (ChannelIO)ois.readObject();
                ois.close();

                // Delete the files for the serialized ChannelIO as well as its isolate
                new File(file).delete();
                new File(serialize_handle+".isolate").delete();

                cio.rundown = false;
                cio.guiInputChannel.addToGUIInputQueue(ChannelConstants.GUIIN_REPAINT, 0, 0, 0); // Add a repaint command
                return cio;
            } catch (IOException ex) {
                System.err.println("Error deserializing channel "+ex);
            } catch (ClassNotFoundException ex) {
                System.err.println("Error deserializing channel "+ex);
            }
        }
        if (ChannelIO.TRACING_ENABLED) trace("new channel for isolate");
        return new ChannelIO();
    }

    /**
     * Constructor.
     *
     * @param mainClassName     the name of the isolate's main class
     */
    private ChannelIO() {
        guiInputChannel  = new GUIInputChannel(this, ChannelConstants.CHANNEL_GUIIN);
        guiOutputChannel = new GUIOutputChannel(this, ChannelConstants.CHANNEL_GUIOUT, guiInputChannel);
        channels.put(ChannelConstants.CHANNEL_GUIIN, guiInputChannel);
        channels.put(ChannelConstants.CHANNEL_GUIOUT, guiOutputChannel);
    }

    /**
     * Creates a new stream channel.
     */
    GenericConnectionChannel createGenericConnectionChannel() {
        while (channels.get(nextAvailableChannelID) != null) {
            nextAvailableChannelID++;
        }
        int chan = nextAvailableChannelID++;
        GenericConnectionChannel channel = new GenericConnectionChannel(this, chan);
        channels.put(chan, channel);
        if (ChannelIO.TRACING_ENABLED) trace("++createGenericConnectionChannel = "+channel);
        return channel;
    }

    /**
     * Executes an operation on a given channel.
     *
     * @param  op   the operation to perform
     * @param  chan the identifier of the channel to execute the operation on
     * @param  i1
     * @param  i2
     * @param  i3
     * @param  i4
     * @param  i5
     * @param  i6
     * @param  o1
     * @param  o2
     * @return the result
     */
    private int execute(
                         int    op,
                         int    chan,
                         int    i1,
                         int    i2,
                         int    i3,
                         int    i4,
                         int    i5,
                         int    i6,
                         Object o1,
                         Object o2
                       ) {
        if (ChannelIO.TRACING_ENABLED) trace("execute channel "+chan+" op " +  ChannelConstants.getMnemonic(op));

        /*
         * Reject the I/O request if the ChannelIO has been rundown.
         */
        if (rundown) {
            if (ChannelIO.TRACING_ENABLED) trace("execute status = javax.microedition.io.ConnectionNotFoundException");
            return raiseException("IsolateRundownError");
        }

        /*
         * Deal with some special case resuests.
         */
        switch (op) {
            case ChannelConstants.CONTEXT_GETRESULT: {
                return (int)theResult;
            }
            case ChannelConstants.CONTEXT_GETRESULT_2: {
                return (int)(theResult >>> 32);
            }
            case ChannelConstants.CONTEXT_GETERROR: {
                return getError();
            }
            case ChannelConstants.CONTEXT_GETCHANNEL: {
                switch (i1) { // Channel type
                    case ChannelConstants.CHANNEL_GUIIN:
                    case ChannelConstants.CHANNEL_GUIOUT: {
                        return i1; // Both channels are already open and the channel number is the channel type
                    }
                    case ChannelConstants.CHANNEL_GENERIC: {
                        return createGenericConnectionChannel().getChannelID();
                    }
                    default: {
                        return ChannelConstants.RESULT_BADPARAMETER;
                    }
                }
            }
            case ChannelConstants.CONTEXT_FREECHANNEL: {
                channels.remove(chan);
                return ChannelConstants.RESULT_OK;
            }
            case ChannelConstants.GLOBAL_HIBERNATECONTEXT: {
                return hibernate();
            }
            case ChannelConstants.GLOBAL_DELETECONTEXT: {
                close();
                return ChannelConstants.RESULT_OK;
            }
        }

        /*
         * Lookup the channel and reject the request if it does not exist.
         */
        Channel channel = (Channel)channels.get(chan);
        if (channel == null) {
            if (ChannelIO.TRACING_ENABLED) trace("execute status = javax.microedition.io.ConnectionNotFoundException");
            return raiseException("javax.microedition.io.ConnectionNotFoundException");
        }

        /*
         * Execute the channel request and save the result if it worked.
         */
        channel.clearResult(); // Set the result to zero by default
        int status;
        try {
            status = channel.execute(op, i1, i2, i3, i4, i5, i6, o1, o2);
        } catch (Throwable ex) {
            status = raiseException(ex.getClass().getName());
        }
        theResult = channel.getResult();

        /*
         * Return the status of the operation.
         */
        if (ChannelIO.TRACING_ENABLED) trace("execute status = "+status);
        return status;
    }

    /**
     * Get the next chanaracter of the error.
     *
     * @return the next character or 0 if none remain
     */
    private int getError() {
        if (exceptionClassName != null) {
            int ch = exceptionClassName.charAt(0);
            int lth = exceptionClassName.length();
            if (lth == 1) {
                exceptionClassName = null;
            } else {
                exceptionClassName = exceptionClassName.substring(1);
            }
            return ch;
        }
        return 0;
    }

    /**
     * Closes the IO system for an isolate.
     */
    private void close() {
        if (rundown == false) {
            rundown = true;
            Enumeration e = channels.elements();
            while (e.hasMoreElements()) {
                Channel channel = (Channel)e.nextElement();
                if (channel != null) {
                    channel.close();
                }
            }
            if (ChannelIO.TRACING_ENABLED) trace("++close ");
        }
    }

    /**
     * Hibernates the IO system for an isolate.
     *
     * @return   the positive identifier of the serialized file or a negative 'x' where x is the length of the
     *           name of the class of the exception that occurred during serialization
     */
    private int hibernate() {
        exceptionClassName = null;
        FileOutputStream file = null;
        try {
            int serializedID = 1;
            while (new File(serializedID+".cio").exists()) {
                serializedID++;
            }

//System.out.println("Serializing channel to "+serializedID+".cio");
            file = new FileOutputStream(serializedID+".cio");
            ObjectOutputStream oos = new ObjectOutputStream(file);
            oos.writeObject(this);
            return serializedID;
        } catch (Throwable t) {
            //System.err.println("Error serializing channel: " + t);
            //t.printStackTrace();
            return raiseException(t.getClass().getName());
        } finally {
            if (file != null) {
                try {
                    file.flush();
                    file.close();
                } catch (IOException ex1) {
                    ex1.printStackTrace();
                }
            }
        }
    }

    /**
     * Registers an exception that occurred on a non-channel specific call to this IO system.
     *
     * @param exceptionClassName   the name of the class of the exception that was raised
     * @return the negative value returned to the Squawk code indicating both that an error occurred
     *         as well as the length of the exception class name
     */
    private int raiseException(String exceptionClassName) {
        //System.out.println("raiseException >>>> "+exceptionClassName);
        if (exceptionClassName.length() == 0) {
            exceptionClassName = "?raiseException?";
        }
        this.exceptionClassName = exceptionClassName;
        return ChannelConstants.RESULT_EXCEPTION;
    }

    /*
     * unblock
     */
    void unblock(int event) {
        EventQueue.unblock(event);
    }


    /*=======================================================================*\
     *                           I/O Server code                             *
    \*=======================================================================*/

    /*
     * The following code can be run from the command line to make a standalone
     * I/O server. Squawk then has to be run with the -Xioport:8888 switch to
     * commumicate with the server.
     */

    /**
     * Debugging flag.
     */
    private static boolean DEBUG = false;

    /**
     * Timing interval. A zero value disables timing.
     */
    private static int timing = 0;

    /**
     * Prints the usage message.
     *
     * @param  errMsg  an optional error message
     */
    private static void usage(String errMsg) {
        PrintStream out = System.out;
        if (errMsg != null) {
            out.println(errMsg);
        }
        out.println("Usage: ChannelIO [-options]");
        out.println("where options include:");
        out.println();
        out.println("    -port:<port>  the port to listen on (default=9090)");
        out.println("    -d            run in debug mode");
        out.println("    -t<n>         show timing info every 'n' IO operations");
        out.println();
    }

    static class IOTimeInfo {
        long execute;
        long receive;
        long send;
        int count;

        void dump(PrintStream out) {
            long total = (receive + execute + send);
            out.println("average time per IO operation: ");
            out.println("    receive: " + (receive / count) + "ms");
            out.println("    send:    " + (send / count) + "ms");
            out.println("    execute: " + (execute / count) + "ms");
            out.println("    total:   " + (total / count) + "ms");
        }
    }

    /**
     * Entry point for the I/O server
     */
    public static void main(String[] args) {
        int port = 9090;

        // Parse the command line arguments
        for (int argc = 0; argc != args.length; ++argc) {
            String arg = args[argc];
            if (arg.startsWith("-port:")) {
                port = Integer.parseInt(arg.substring("-port:".length()));
            } else if (arg.equals("-d")) {
                DEBUG = true;
            } else if (arg.startsWith("-t")) {
                timing = Integer.parseInt(arg.substring(2));
            } else {
                usage("Unknown option: " + arg);
                System.exit(1);
            }
        }

        try {
            System.out.println("Starting server on port "+port);
            StreamConnectionNotifier ssocket = (StreamConnectionNotifier)Connector.open("serversocket://:"+port);

            IOTimeInfo timingInfo = null;
            if (timing != 0) {
                timingInfo = new IOTimeInfo();
            }

            for (;;) {
                try {
                    if (DEBUG) System.out.println("listening on port " + port);
                    StreamConnection con = ssocket.acceptAndOpen();
                    if (DEBUG) System.out.println("Got connection");
                    DataInputStream  in  = con.openDataInputStream();
                    DataOutputStream out = new DataOutputStream(new BufferedOutputStream(con.openOutputStream()));
                    for (;;) {
                        long start = 0L;
                        long end = 0L;
                        if (timing != 0) {
                            start = System.currentTimeMillis();
                        }
                        if (DEBUG) System.err.print("IO server receiving: ");
                        int cio    = readInt(in, "cio");            // 0
                        int op     = readInt(in, "op");             // 1
                        int cid    = readInt(in, "cid");            // 2
                        int i1     = readInt(in, "i1");             // 3
                        int i2     = readInt(in, "i2");             // 4
                        int i3     = readInt(in, "i3");             // 5
                        int i4     = readInt(in, "i4");             // 6
                        int i5     = readInt(in, "i5");             // 7
                        int i6     = readInt(in, "i6");             // 8
                        int retLth = readInt(in, "lth");            // 9
                        Object s1  = readObject(in);

                        if (timing != 0) {
                            end = System.currentTimeMillis();
                            timingInfo.receive += (end - start);
                            start = end;
                        }

                        if (DEBUG) System.err.println();
                        byte[] buf = new byte[retLth];
                        int status = -1;
                        try {
                            status = execute(cio, op, cid, i1, i2, i3, i4, i5, i6, s1, buf);
                        } catch(Throwable ex) {
                            System.err.println("Exception cause in I/O server "+ex);
                            buf = new byte[0];
                        }
                        int low  = execute(cio, ChannelConstants.CONTEXT_GETRESULT,   -1, 0, 0, 0, 0, 0, 0, null, null);
                        int high = execute(cio, ChannelConstants.CONTEXT_GETRESULT_2, -1, 0, 0, 0, 0, 0, 0, null, null);

                        if (timing != 0) {
                            end = System.currentTimeMillis();
                            timingInfo.execute += (end - start);
                            start = end;
                        }

                        if (DEBUG) System.err.print("IO server sending: ");
                        writeInt(out, "magic",  0xCAFEBABE);
                        writeInt(out, "status", status);
                        writeInt(out, "r-low ", low);
                        writeInt(out, "r-high", high);
                        writeInt(out, "resLth", buf.length);
                        out.write(buf);
                        out.flush();

                        if (timing != 0) {
                            end = System.currentTimeMillis();
                            timingInfo.send += (end - start);
                            timingInfo.count++;

                            if ((timingInfo.count % timing) == 0) {
                                timingInfo.dump(System.err);
                            }
                        }
                    }
                    //out.close();
                    //in.close();
                    //con.close();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
            return;
        }
    }

    /**
     * Read an int from the input stream
     *
     * @param in the stream
     * @param name debugging label
     * @return the int
     */
    private static int readInt(DataInputStream in, String name) throws IOException {
        int val = in.readInt();
        if (DEBUG) System.err.print(name + "=" + val+ " ");
        return val;
    }

    /**
     * Read an int from the input stream
     *
     * @param in the stream
     * @param name debugging label
     * @return the int
     */
    private static void writeInt(DataOutputStream out, String name, int val) throws IOException {
        out.writeInt(val);
        if (DEBUG) System.err.print(name + "=" + val+ " ");
    }

    /**
     * Read an array from the input stream
     *
     * @param in the stream
     * @return the array object
     */
    private static Object readObject(DataInputStream in) throws IOException {
        int cno = in.readInt();
        int lth = in.readInt();
        if (cno == 0) {
            return null;
        } else if (cno == CID.BYTE_ARRAY || cno == CID.STRING_OF_BYTES) {
            byte[] buf = new byte[lth];
            in.readFully(buf);
            return (cno == CID.BYTE_ARRAY) ? (Object)buf : new String(buf);
        } else if (cno == CID.CHAR_ARRAY || cno == CID.STRING) {
            char[] buf = new char[lth];
            for (int i = 0 ; i < lth ; i++) {
                buf[i] = in.readChar();
            }
            return (cno == CID.CHAR_ARRAY) ? (Object)buf : new String(buf);
        } else if (cno == CID.INT_ARRAY) {
            int[] buf = new int[lth];
            for (int i = 0 ; i < lth ; i++) {
                buf[i] = in.readInt();
            }
            return buf;
        } else {
            System.err.println("Bad object type "+cno);
            return null;
        }
    }
}
