package com.sun.squawk.util;

import java.io.*;

/**
 * This class provides for reading lines from a reader. This is functionality
 * normally provided by the non-J2ME class BufferedReader.
 *
 * @author  Doug Simon
 */
public final class LineReader {

    /**
     * The source reader.
     */
    private Reader in;

    /**
     * Creates a new LineReader to parse the lines from a given Reader.
     *
     * @param reader   the reader providing the input to be parsed into lines
     */
    public LineReader(Reader reader) {
        in = reader;
    }

    /**
     * Read a line of text.  A line is considered to be terminated by any one
     * of a line feed ('\n'), a carriage return ('\r'), or a carriage return
     * followed immediately by a linefeed.
     *
     * @return     A String containing the contents of the line, not including
     *             any line-termination characters, or null if the end of the
     *             stream has been reached
     *
     * @throws  IOException  If an I/O error occurs
     */
    public String readLine() throws IOException {
        boolean trucking = true;
        boolean eol = true;
        StringBuffer sb = new StringBuffer();
        while (trucking) {
            int c = in.read();
            if (c == '\n' || c == -1) {
                trucking = false;
                eol = eol && (c == -1);
            } else {
                eol = false;
                if (c != '\r') {
                    sb.append((char)c);
                }

            }
        }
        if (eol) {
            return null;
        }
        return sb.toString();
    }

    /**
     * Read all the lines from the input stream and add them to a given Vector.
     *
     * @param v   the vector to add to or null if it should be created first by this method.
     * @return the vector to which the lines were added
     *
     * @throws  IOException  If an I/O error occurs
     */
    public Vector readLines(Vector v) throws IOException {
        if (v == null) {
            v = new Vector();
        }
        for (String line = readLine(); line != null; line = readLine()) {
            v.addElement(line);
        }
        return v;
    }
}
