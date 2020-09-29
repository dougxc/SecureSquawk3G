/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 */
package com.sun.squawk.util;

import java.io.*;

/**
 * An instance of <code>StructuredInputStream</code> is used to read a file that
 * must conform to some given format such as a class file or a suite file.
 *
 * @author  Doug Simon
 */
public abstract class StructuredFileInputStream {

    /**
     * The path of the file being read.
     */
    private final String filePath;

    /**
     * The DataInputStream that this class delegates to.
     */
    private final DataInputStream in;

    /**
     * The tracing feature that must be enabled if tracing is to be enabled for the
     * methods in this class.
     */
    private final String traceFeature;

    /**
     * Creates a <code>StructuredFileInputStream</code> that reads class components
     * from a given input stream.
     *
     * @param   in           the input stream
     * @param   filePath     the file from which <code>in</code> was created
     * @param   traceFeature the tracing feature that must be enabled if tracing is to be enabled
     */
    public StructuredFileInputStream(InputStream in, String filePath, String traceFeature) {
        if (in instanceof DataInputStream) {
            this.in = (DataInputStream)in;
        } else {
            this.in = new DataInputStream(in);
        }
        this.filePath = filePath;
        this.traceFeature = traceFeature;
    }

    /**
     * Gets the name of the file from which this reader is reading.
     *
     * @return  the name of the file from which this reader is reading
     */
    public final String getFileName() {
        return filePath;
    }

    /**
     * Throw a LinkageError to indicate there was an IO error or the file did not
     * conform to the structure expected by the client of this class.
     *
     * @param   msg  the cause of the error
     * @return  the LinkageError raised
     */
    public abstract LinkageError formatError(String msg);

    /**
     * Reads some bytes from the class file and stores them into the buffer
     * array <code>b</code>. The number of bytes read is equal to the
     * length of <code>b</code>.
     *
     * @param  b  the buffer to fill
     * @param   prefix  the optional prefix used when tracing this read
     */
    public final void readFully(byte[] b, String prefix) {
        try {
            in.readFully(b);
            if (Klass.DEBUG && prefix != null && Tracer.isTracing(traceFeature, filePath)) {
                Tracer.traceln(prefix + ":{read " + b.length + " bytes}");
            }
        } catch (EOFException oef) {
            throw formatError("truncated file");
        } catch (IOException ioe) {
            throw formatError(ioe.toString());
        }
    }

    /**
     * Reads an integer from the class file.
     *
     * @param   prefix  the optional prefix used when tracing this read
     * @return  the value read
     * @see     DataInputStream#readInt()
     */
    public final int readInt(String prefix) {
        try {
            int value = in.readInt();
            if (Klass.DEBUG && prefix != null && Tracer.isTracing(traceFeature, filePath)) {
                Tracer.traceln(prefix+":"+value);
            }
            return value;
        } catch (EOFException oef) {
            throw formatError("truncated file");
        } catch (IOException ioe) {
            throw formatError(ioe.toString());
        }
    }

    /**
     * Reads an unsigned short from the class file.
     *
     * @param   prefix  the optional prefix used when tracing this read
     * @return  the value read
     * @see     DataInputStream#readUnsignedShort()
     */
    public final int readUnsignedShort(String prefix) {
        try {
            int value = in.readUnsignedShort();
            if (Klass.DEBUG && prefix != null && Tracer.isTracing(traceFeature, filePath)) {
                Tracer.traceln(prefix+":"+value);
            }
            return value;
        } catch (EOFException oef) {
            throw formatError("truncated file");
        } catch (IOException ioe) {
            throw formatError(ioe.toString());
        }
    }

    /**
     * Reads an unsigned byte from the file.
     *
     * @param   prefix  the optional prefix used when tracing this read
     * @return  the value read
     * @see     DataInputStream#readUnsignedByte()
     */
    public final int readUnsignedByte(String prefix) {
        try {
            int value = in.readUnsignedByte();
            if (Klass.DEBUG && prefix != null && Tracer.isTracing(traceFeature, filePath)) {
                Tracer.traceln(prefix+":"+value);
            }
            return value;
        } catch (EOFException oef) {
            throw formatError("truncated file");
        } catch (IOException ioe) {
            throw formatError(ioe.toString());
        }
    }

    /**
     * Reads a signed short from the file.
     *
     * @param   prefix  the optional prefix used when tracing this read
     * @return  the value read
     * @see     DataInputStream#readShort()
     */
    public final int readShort(String prefix) {
        try {
            int value = in.readShort();
            if (Klass.DEBUG && prefix != null && Tracer.isTracing(traceFeature, filePath)) {
                Tracer.traceln(prefix+":"+value);
            }
            return value;
        } catch (EOFException oef) {
            throw formatError("truncated file");
        } catch (IOException ioe) {
            throw formatError(ioe.toString());
        }
    }

    /**
     * Reads a signed byte from the file.
     *
     * @param   prefix  the optional prefix used when tracing this read
     * @return  the value read
     * @see     DataInputStream#readByte()
     */
    public final int readByte(String prefix) {
        try {
            int value = in.readByte();
            if (Klass.DEBUG && prefix != null && Tracer.isTracing(traceFeature, filePath)) {
                Tracer.traceln(prefix+":"+value);
            }
            return value;
        } catch (EOFException oef) {
            throw formatError("truncated file");
        } catch (IOException ioe) {
            throw formatError(ioe.toString());
        }
    }

    /**
     * Reads a long value from the file.
     *
     * @param   prefix  the optional prefix used when tracing this read
     * @return  the value read
     * @see     DataInputStream#readLong()
     */
    public final long readLong(String prefix) {
        try {
            long value = in.readLong();
            if (Klass.DEBUG && prefix != null && Tracer.isTracing(traceFeature, filePath)) {
                Tracer.traceln(prefix+":"+value);
            }
            return value;
        } catch (EOFException oef) {
            throw formatError("truncated file");
        } catch (IOException ioe) {
            throw formatError(ioe.toString());
        }
    }

/*if[FLOATS]*/

    /**
     * Reads a float value from the file.
     *
     * @param   prefix  the optional prefix used when tracing this read
     * @return  the value read
     * @see     DataInputStream#readFloat()
     */
    public final float readFloat(String prefix) {
        try {
            float value = in.readFloat();
            if (Klass.DEBUG && prefix != null && Tracer.isTracing(traceFeature, filePath)) {
                Tracer.traceln(prefix+":"+value);
            }
            return value;
        } catch (EOFException oef) {
            throw formatError("truncated file");
        } catch (IOException ioe) {
            throw formatError(ioe.toString());
        }
    }

    /**
     * Reads a double value from the file.
     *
     * @param   prefix  the optional prefix used when tracing this read
     * @return  the value read
     * @see     DataInputStream#readDouble()
     */
    public final double readDouble(String prefix) {
        try {
            double value = in.readDouble();
            if (Klass.DEBUG && prefix != null && Tracer.isTracing(traceFeature, filePath)) {
                Tracer.traceln(prefix+":"+value);
            }
            return value;
        } catch (EOFException oef) {
            throw formatError("truncated file");
        } catch (IOException ioe) {
            throw formatError(ioe.toString());
        }
    }

/*end[FLOATS]*/

    /**
     * Reads a UTF-8 encoded string from the file.
     *
     * @param   prefix  the optional prefix used when tracing this read
     * @return  the value read
     * @see     DataInputStream#readUTF()
     */
    public final String readUTF(String prefix) {
        try {
            String value = com.sun.squawk.util.DataInputUTF8Decoder.readUTF(in, true);
            if (Klass.DEBUG && prefix != null && Tracer.isTracing(traceFeature, filePath)) {
                Tracer.traceln(prefix+":"+value);
            }
            return value;
        } catch (EOFException oef) {
            throw formatError("truncated file");
        } catch (IOException ioe) {
            throw formatError(ioe.toString());
        }
    }

    /**
     * Skips over and discards <code>n</code> bytes of data from the file.
     * <p>
     *
     * @param      n   the number of bytes to be skipped.
     * @param   prefix  the optional prefix used when tracing this read
     */
    public final void skip(int n, String prefix) {
        try {
            if (in.skip(n) != n) {
                throw formatError("truncated file");
            }
            if (Klass.DEBUG && prefix != null && Tracer.isTracing(traceFeature, filePath)) {
                Tracer.traceln(prefix+":{skipped " + n + " bytes}");
            }
        } catch (IOException ioe) {
            throw formatError(ioe.toString());
        }
    }


    /**
     * Ensures that the input stream is at the end of the file.
     *
     * @throws a LinkageError if the input stream is not at the end of the file
     */
    public void readEOF() {
        /*
         * Ensure there are no extra bytes
         */
        try {
            in.readByte();
        } catch (EOFException e) {
            /* normal case */
        } catch (IOException ioe) {
            throw formatError(ioe.toString());
        }

    }

    /**
     * Closes this reader and releases any system resources
     * associated with the underlying input stream.
     */
    public final void close() {
        try {
            in.close();
        } catch (IOException ioe) {
            throw formatError(ioe.toString());
        }
    }
}
