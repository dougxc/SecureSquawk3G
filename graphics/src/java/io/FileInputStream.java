/*
 * @(#)FileInputStream.java 1.29 95/12/19 Arthur van Hoff
 *
 * Copyright (c) 1994 Sun Microsystems, Inc. All Rights Reserved.
 *
 * Permission to use, copy, modify, and distribute this software
 * and its documentation for NON-COMMERCIAL purposes and without
 * fee is hereby granted provided that this copyright notice
 * appears in all copies. Please refer to the file "copyright.html"
 * for further important copyright and licensing information.
 *
 * SUN MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY OF
 * THE SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
 * TO THE IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE, OR NON-INFRINGEMENT. SUN SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES.
 */

package java.io;

/**
 * File input stream, can be constructed from
 * a file descriptor or a file name.
 * @see FileOutputStream
 * @see File
 * @version     1.29, 19 Dec 1995
 * @author  Arthur van Hoff
 */
public class FileInputStream extends InputStream  {

    ByteArrayInputStream is;

    /**
     * Creates an input file with the specified system dependent file
     * name.
     * @param name the system dependent file name
     * @exception IOException If the file is not found.
     */
    public FileInputStream(String name) throws FileNotFoundException {
        int handle = FileInputStream_open(name);
        if (handle == 0) {
            throw new FileNotFoundException();
        }
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        int ch;
        while ((ch = FileInputStream_read()) != -1) {
//System.out.print("==="+ch);
//System.out.println(" os="+os);
            os.write((byte)ch);
        }
        FileInputStream_close();
        try { os.close(); } catch(IOException ex) {}
        is = new ByteArrayInputStream(os.toByteArray());

    }

    public int read() throws IOException {
        return is.read();
    }

    public void close() throws IOException {
        is.close();
    }

    public int available() {
        return is.available();
    }

    public boolean markSupported() {
        return is.markSupported();
    }

    public void mark(int readAheadLimit) {
        is.mark(readAheadLimit);
    }

    public synchronized void reset() {
        is.reset();
    }

    private /*native*/ int    FileInputStream_open(String name) {
        throw new Error("Unimplemeted native method");
    }
    private /*native*/ int    FileInputStream_read() {
        throw new Error("Unimplemeted native method");
    }
    private /*native*/ void   FileInputStream_close() {
        throw new Error("Unimplemeted native method");
    }


}


