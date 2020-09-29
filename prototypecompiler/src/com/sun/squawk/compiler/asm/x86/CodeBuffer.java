/*
 * @(#)CodeBuffer.java                  1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package com.sun.squawk.compiler.asm.x86;

/**
 * Represents a buffer into which assembly code is generated. The code buffer
 * also stores information about instructions that must be patched when objects
 * or code move.
 *
 * @see      Assembler
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class CodeBuffer {

    /**
     * The byte array that stores the assembly instructions.
     */
    private byte[] code;

    /**
     * The number of bytes in the code buffer.
     */
    private int count;

    /**
     * Current write position in the code buffer.
     */
    private int pos;

    /**
     * The relocation address
     */
    private int address;

    /**
     * Constructs an empty code buffer with the specified capacity.
     *
     * @param  codeSize   size of the code buffer
     */
    public CodeBuffer(int codeSize) {
        this.code = new byte[codeSize];
        this.count = 0;
        this.pos = 0;
    }

    /**
     * Constructs an empty code buffer with an initial capacity of 4096 bytes.
     */
    public CodeBuffer() {
        this(4096);
    }

    /**
     * Returns the address of the first byte of the code buffer.
     *
     * @return  start address of the code
     */
    public int getCodeBegin() {
        return 0;
    }

    /**
     * Returns the current code generation position.
     *
     * @see #setCodePos
     * @return  current code generation position
     */
    public int getCodePos() {
        return pos;
    }

    /**
     * Returns the current code generation end point.
     *
     * @return  current code generation position
     */
    public int getCodeEnd() {
        return count;
    }

    /**
     * Sets the current code generation position.
     *
     * @see #getCodePos
     * @param p current code generation position
     */
    public void setCodePos(int p) {
        pos = p;
    }

    /**
     * Returns the number of bytes in the code buffer.
     *
     * @return  size of the code
     */
    public int getCodeSize() {
        return count;
    }

    /**
     * Returns the current capacity of the code buffer.
     *
     * @return  the current capacity
     */
    public int getCodeLimit() {
        return code.length;
    }

    /**
     * Add space in the code buffer.
     */
    private void addCodespace() {
        int capacity = code.length * 2 + 1024;
        byte[] newCode = new byte[capacity];
        System.arraycopy(code, 0, newCode, 0, count);
        code = newCode;
    }

    /**
     * Returns the array of bytes in this code buffer.
     *
     * @return  array of bytes in the buffer
     */
    public byte[] getBytes() {
        return code;
    }

    /**
     * Appends the specified byte to the end of the code buffer.
     *
     * @param  x  byte value to be appended
     */
    public void emit(int x) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that((address == 0), "code emited after relocation");
        }
        if (pos == code.length) {
            addCodespace();
        }
        code[pos++] = (byte) x;
        if (pos > count) {
           count = pos;
        }
    }

    /**
     * Returns the byte at the specified position in the code buffer.
     *
     * @param   pos  index into the code buffer
     * @return  the byte value at the specified index
     */
    public int byteAt(int pos) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that((pos >= 0) && (pos < count), "index out of bounds");
        }
        return (int) code[pos] & 0xff;
    }

    /**
     * Sets the byte at the specified position to the new value.
     *
     * @param  pos  index of the byte to be replaced
     * @param  x    the new byte value
     */
    public void setByteAt(int pos, int x) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that((pos >= 0) && (pos < count), "index out of bounds");
            Assert.that((address == 0), "code emited after relocation");
        }
        code[pos] = (byte) x;
    }

    /**
     * Sets the relocation address.
     *
     * @see #getRelocation
     * @param  address  the address
     */
    public void setRelocation(int address) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that((this.address == 0), "second relocation");
        }
        this.address = address;
    }

    /**
     * Gets the relocation address.
     *
     * @see #setRelocation
     * @return  the address
     */
    public int getRelocation() {
        return address;
    }

}
