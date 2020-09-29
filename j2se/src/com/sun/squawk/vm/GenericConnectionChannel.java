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
import com.sun.squawk.vm.ChannelConstants;

/**
 * Channel for stream IO.
 */
public class GenericConnectionChannel extends Channel {

    /**
     * Flag to enable really non-blocking input.
     */
    private static boolean NON_BLOCKING_INPUT = true;

    /**
     * The open generic connection held by this channel (if any).
     */
    Connection con;

    /**
     * The data input stream held by this channel (if any).
     */
    private DataInputStream dis;

    /**
     * Used for non-blocking I/0.
     */
    PushbackInputStream pbis;

    /**
     * DataSucker EOF flag.
     */
    boolean eofSeen = false;

    /**
     * DataSucker pending exception.
     */
    IOException pendingException;

    /**
     * The connection asynchronously found for accept().
     */
    StreamConnection acceptConnection;

    /**
     * The data output stream held by this channel (if any).
     */
    private DataOutputStream dos;

    /**
     * The logging stream for input operations.
     */
    private DataOutputStream inLog;

    /**
     * The logging stream for output operations.
     */
    private DataOutputStream outLog;

    /**
     * Constructor.
     *
     * @param cio        the owner of this channel
     * @param channelID  the identifier of this channel
     * @param logging    specifies if the channel is to log its operations
     */
    public GenericConnectionChannel(ChannelIO cio, int channelID) {
        super(cio, channelID);
        if (ChannelIO.LOGGING_ENABLED) {
            try {
                this.inLog = new DataOutputStream(new FileOutputStream("channel"+channelID+".input"));
                this.outLog = new DataOutputStream(new FileOutputStream("channel"+channelID+".output"));
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }


    /**
     * Throw a pending exception if one is there.
     */
    private void throwPendingException() throws IOException {
        IOException ex = pendingException;
        pendingException = null;
        if (ex != null) {
            throw ex;
        }
    }

    /**
     * Determin if N bytes can be read from the input stream without blocking.
     * We need this to make genuine non-blocking input I/O calls. If the input
     * is not known to be at EOF and the number of bytes needed is less than
     * the number available then we start a seporate thread to read the number
     * of bytes we need and return true. Which will tell the caller that the
     * channel should be blocked using the channel ID as the event number.
     * When the seporate thread has read the number of bytes we need then the
     * event will be sent to restart the Squawk thread.
     *
     * @param n the number of bytes needed
     * @return true is the requested number of bytes are available
     */
    private boolean available(int n) throws IOException {
        if (!NON_BLOCKING_INPUT) {
            return true;
        }
        throwPendingException();
        if (eofSeen || n <= dis.available()) {
            return true;
        }
        new DataSucker(this, n).start();
        return false;
    }

    /**
     * Executes an operation on this channel.
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
     * @return the result
     */
    int execute(
                 int op,
                 int i1,
                 int i2,
                 int i3,
                 int i4,
                 int i5,
                 int i6,
                 Object o1,
                 Object o2
               ) throws IOException {

        switch (op) {

            case ChannelConstants.OPENCONNECTION: {
                String protAndName = (String)o1;
                int mode = i1;
                int tmo = i2;
//System.err.println("ChannelConstants.OPENCONNECTION: " + " name=" + protAndName);
                con = Connector.open(protAndName, mode, tmo == 1);
                break;
            }

            case ChannelConstants.CLOSECONNECTION: {
//System.err.println("ChannelConstants.CLOSECONNECTION:");
                con.close();
                con = null;
                break;
            }

            case ChannelConstants.ACCEPTCONNECTION: {
                throwPendingException();
                if (acceptConnection == null) {
                    new Acceptor(this).start();
//System.err.println("ChannelConstants.ACCEPTCONNECTION: waiting...");
                    return getEventNumber();
                }
                GenericConnectionChannel channel = cio.createGenericConnectionChannel();
                channel.con = acceptConnection;
                acceptConnection = null;
                result = channel.getChannelID();
//System.err.println("ChannelConstants.ACCEPT: "+result);
                break;
            }

            case ChannelConstants.OPENINPUT: {
                InputStream is = ((InputConnection)con).openInputStream();
                pbis = new PushbackInputStream(is, 8);
                dis  = new DataInputStream(pbis);
                break;
            }

            case ChannelConstants.CLOSEINPUT: {
                dis.close();
                dis = null;
                break;
            }

            case ChannelConstants.READBYTE: {
                if (!available(1)) {
                    return getEventNumber();
                }
                result = dis.read();
                if (inLog != null) {
                    inLog.writeByte((int)result);
                }
                break;
            }

            case ChannelConstants.READSHORT: {
                if (!available(2)) {
                    return getEventNumber();
                }
                result = dis.readShort();
                if (inLog != null) {
                    inLog.writeShort((int)result);
                }
                break;
            }

            case ChannelConstants.READINT: {
                if (!available(4)) {
                    return getEventNumber();
                }
                result = dis.readInt();
                if (inLog != null) {
                    inLog.writeInt((int)result);
                }
                break;
            }

            case ChannelConstants.READLONG: {
                if (!available(8)) {
                    return getEventNumber();
                }
                result = dis.readLong();
                if (inLog != null) {
                    inLog.writeLong(result);
                }
                break;
            }

            case ChannelConstants.READBUF: {
                if (!available(1)) {
                    return getEventNumber();
                }
                int    off = i1;
                int    len = Math.min(i2, dis.available());
                byte[] buf = (byte[])o2;
                result = dis.read(buf, off, len);
                if (inLog != null) {
                    for (int i = off; i < off + len; i++) {
                        inLog.writeByte(buf[i]);
                    }
                }
                break;
            }

            case ChannelConstants.SKIP: {
                long l = ((long)i1 << 32) + (i2 & 0xFFFFFFFFL);
                result = dis.skip(l);
                break;
            }

            case ChannelConstants.AVAILABLE: {
                result = dis.available();
                break;
            }

            case ChannelConstants.MARK: {
                int limit = i1;
                dis.mark(limit);
                break;
            }

            case ChannelConstants.RESET: {
                dis.reset();
                break;
            }

            case ChannelConstants.MARKSUPPORTED: {
                result = dis.markSupported() ? 1 : 0;
                break;
            }

            case ChannelConstants.OPENOUTPUT: {
                dos = ((OutputConnection)con).openDataOutputStream();
                break;
            }

            case ChannelConstants.FLUSH: {
                dos.flush();
                break;
            }

            case ChannelConstants.CLOSEOUTPUT: {
                dos.close();
                dos = null;
                break;
            }

            case ChannelConstants.WRITEBYTE: {
                int ch = i1;
                dos.write(ch);
                dos.flush();
                if (outLog != null) {
                    outLog.writeByte(ch);
                }
                break;
            }

            case ChannelConstants.WRITESHORT: {
                int val = i1;
                dos.writeShort(val);
                dos.flush();
                if (outLog != null) {
                    outLog.writeShort(val);
                }
                break;
            }

            case ChannelConstants.WRITEINT: {
                int val = i1;
                dos.writeInt(val);
                dos.flush();
                if (outLog != null) {
                    outLog.writeInt(val);
                }
                break;
            }

            case ChannelConstants.WRITELONG: {
                long l = ((long)i1 << 32) + (i2 & 0xFFFFFFFFL);
                dos.writeLong(l);
                dos.flush();
                if (outLog != null) {
                    outLog.writeLong(l);
                }
                break;
            }

            case ChannelConstants.WRITEBUF: {
                byte[] buf = (byte[])o1;
                int off = i1;
                int len = i2;
                dos.write(buf, off, len);
                dos.flush();
                if (outLog != null) {
                    for (int i = off; i < off + len; i++) {
                        outLog.writeByte(buf[i]);
                    }
                }
                break;
            }
            default: {
                throw new RuntimeException("Invalid channel opcode: " + op);
            }
        }
        return 0;
    }

    /**
     * Closes this channel.
     */
    public void close() {
        close(dos);
        close(outLog);
        close(dis);
        close(inLog);
        if (con != null) {
            try {
                con.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void close(OutputStream out) {
        if (out != null) {
            try {
                out.close();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }

    private void close(InputStream in) {
        if (in != null) {
            try {
                in.close();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }
}

/**
 * Service thread to read data from an input stream
 */
class DataSucker extends Thread {
    GenericConnectionChannel chan;
    int size;

    DataSucker(GenericConnectionChannel chan, int size) {
        this.chan = chan;
        this.size = size;
    }

    public void run() {
//System.err.println("**Sucking "+size);
        byte[] buf = new byte[size];
        int i = 0;
        try {
            for (; i < size ; i++) {
                int ch = chan.pbis.read();
//System.err.println("**read "+ch);
                if (ch == -1) {
                    chan.eofSeen = true;
                    break;
                } else {
                    buf[i] = (byte)(ch & 0xFF);
                }
            }
        } catch (IOException ex) {
//System.err.println("**IOException "+ex);
            chan.pendingException = ex;
        }
        try {
//System.err.println("**pushed back "+i+" buf[0] = "+buf[0]);
            chan.pbis.unread(buf, 0, i);
        } catch (IOException ex) {
            System.err.println("unread failure "+ ex);
        }
//System.err.println("**Sucked "+i+" for "+chan.getEventNumber());
        chan.cio.unblock(chan.getEventNumber());
    }
}

/**
 * Service thread to wait for a connection to be accepted.
 */
class Acceptor extends Thread {
    GenericConnectionChannel chan;

    Acceptor(GenericConnectionChannel chan) {
        this.chan = chan;
    }

    public void run() {
//System.err.println("Acceptor");
        try {
            chan.acceptConnection = ((StreamConnectionNotifier)chan.con).acceptAndOpen();
        } catch (IOException ex) {
            chan.pendingException = ex;
        }
//System.err.println("Accepted");
        chan.cio.unblock(chan.getEventNumber());
    }
}
