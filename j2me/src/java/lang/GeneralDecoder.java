/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */
package java.lang;

import com.sun.squawk.util.Assert;

/**
 * A byte buffer dencoder can be used to decode a byte array of values encoded
 * with a {@link ByteBufferEncoder byte buffer encoder}.
 *
 * @author  Doug Simon
 */
abstract class GeneralDecoder {

    /**
     * Get the next byte
     *
     * @return the next byte
     */
    abstract int nextByte();

    /**
     * Decodes an unsigned integer from the current decoding position.
     *
     * @return the decoded value
     * @see    ByteBufferEncoder#addUnsignedInt(int)
     */
    public final int readUnsignedInt() {
        int lo = nextByte() & 0xFF;
        if (lo < 128) {
            /* 0xxxxxxx */
            return lo;
        }
        lo &= 0x7f;
        int mid = nextByte() & 0xFF;
        if (mid < 128) {
            /* 1xxxxxxx 0xxxxxxx */
            return ((mid << 7) + lo);
        }
        mid &= 0x7f;
        int hi = nextByte() & 0xFF;
        if (hi < 128) {
            /* 1xxxxxxx 1xxxxxxx 0xxxxxxx */
            return ((hi << 14) + (mid << 7) + lo);
        }
        hi &= 0x7f;
        int last = nextByte() & 0xFF;
        if (last < 128) {
            /* 1xxxxxxx 1xxxxxxx 1xxxxxxx 0xxxxxxx */
            return ((last << 21) + (hi << 14) + (mid << 7) + lo);
        }
        throw new Error();
    }

    /**
     * Decode an encoded short from the current position.
     *
     * @return the decoded value
     * @see    ByteBufferEncoder#addUnsignedShort(int)
     */
    public final int readUnsignedShort() {
        int x = readUnsignedInt();
        Assert.that(x >= 0 && x <= 0xFFFF);
        return x;
    }

    /**
     * Decode a UTF8 encoded character from the current position.
     *
     * @return  the decoded character
     */
    public final char readChar() {
        byte c = (byte)nextByte();
        if (c > 0) {
            /* 0xxxxxxx*/
            return (char)(c & 0xFF);
        } else if ((c & 0xF0) == 0xC0) {
            /* 110xxxxx 10xxxxxx*/
            int char2 = (int)nextByte();
            Assert.that((char2 & 0xC0) == 0x80);
            return (char)(((c & 0x1F) << 6) | (char2 & 0x3F));
        } else {
            /* 1110xxxx 10xxxxxx 10xxxxxx */
            Assert.that((c & 0xF0) == 0xE0);
            int char2 = (int)nextByte();
            Assert.that((char2 & 0xC0) == 0x80);
            int char3 = (int)nextByte();
            Assert.that((char3 & 0xC0) == 0x80);
            return (char)(((c & 0x0F) << 12) | ((char2 & 0x3F) << 6) | ((char3 & 0x3F) << 0));

        }
    }
}