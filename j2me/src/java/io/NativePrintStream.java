/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */
package java.io;

public class NativePrintStream extends PrintStream {


    public NativePrintStream() {
    }

    public void flush() {
    }

    public void close() {
    }

    public boolean checkError() {
        return false;
    }

    protected void setError() {
        throw new Error();
    }

    public void write(int b) {
        throw new Error();
    }

    public void write(byte buf[], int off, int len) {
        throw new Error();
    }

    protected void write(char buf[]) {
        throw new Error();
    }

    protected void write(String s) {
        VM.print(s);
    }

    protected void newLine() {
        VM.println();
    }



}


