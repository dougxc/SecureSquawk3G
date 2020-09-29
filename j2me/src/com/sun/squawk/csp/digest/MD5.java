//if[TRUSTED]
/* This will selectively exclude the entire file from the build */
/*
 * @(#)MD5.java	1.33 03/12/19
 *
 * Copyright 2004 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.sun.squawk.csp.digest;

/**
 * The MD5 class is used to compute an MD5 message digest over a given
 * buffer of bytes. It is an implementation of the RSA Data Security Inc
 * MD5 algorithim as described in internet RFC 1321.
 *
 * Slightly modified for use with the secure Squawk JVM.
 *
 * @version 	1.33 12/19/03
 * @author 	Chuck McManis
 * @author 	Benjamin Renaud
 * @author	Andreas Sterbenz
 * @author Andrew Crouch
 */
public final class MD5 extends DigestBase {

    private static final byte[] DIGEST_INFO = { 0x30, 0x20, 0x30, 0x0c, 0x06, 0x08,
                                               0x2a, (byte)0x86, 0x48, (byte)0x86, (byte)0xf7, 0x0d,
                                               0x02, 0x05, 0x05, 0x00, 0x04, 0x10 };



    // state of this object
    private final int[] state;

    // temporary buffer, used by implCompress()
    private final int[] x;

    // temporary buffer, used by implDigest()
    private final byte[] bits;

    // temporary buffer, used by implDigest()
    private final int[] intBits;

    // rotation constants
    private static final int S11 = 7;
    private static final int S12 = 12;
    private static final int S13 = 17;
    private static final int S14 = 22;
    private static final int S21 = 5;
    private static final int S22 = 9;
    private static final int S23 = 14;
    private static final int S24 = 20;
    private static final int S31 = 4;
    private static final int S32 = 11;
    private static final int S33 = 16;
    private static final int S34 = 23;
    private static final int S41 = 6;
    private static final int S42 = 10;
    private static final int S43 = 15;
    private static final int S44 = 21;

    // Standard constructor, creates a new MD5 instance.
    public MD5() {
        super("MD5", 16, 64);
        state = new int[4];
        x = new int[16];
        bits = new byte[8];
        intBits = new int[2];
        implReset();
    }

    /**
     * Reset the state of this object.
     */
    void implReset() {
        // Load magic initialization constants.
        state[0] = 0x67452301;
        state[1] = 0xefcdab89;
        state[2] = 0x98badcfe;
        state[3] = 0x10325476;
    }

    /**
     * Perform the final computations, any buffered bytes are added
     * to the digest, the count is added to the digest, and the resulting
     * digest is stored.
     */
    void implDigest(byte[] out, int ofs) {
        long bitsProcessed = bytesProcessed << 3;

        int index = (int)bytesProcessed & 0x3f;
        int padLen = (index < 56) ? (56 - index) : (120 - index);
        engineUpdate(padding, 0, padLen);

        intBits[0] = (int)bitsProcessed;
        intBits[1] = (int)(bitsProcessed >> 32);
        i2bLittle(intBits, 0, bits, 0, 8);
        engineUpdate(bits, 0, 8);

        i2bLittle(state, 0, out, ofs, 16);
    }

    /* **********************************************************
     * The MD5 Functions. The results of this
     * implementation were checked against the RSADSI version.
     * **********************************************************
     */

    private static int FF(int a, int b, int c, int d, int x, int s, int ac) {
        a += ((b & c) | ((~b) & d)) + x + ac;
        return ((a << s) | (a >>> (32 - s))) + b;
    }

    private static int GG(int a, int b, int c, int d, int x, int s, int ac) {
        a += ((b & d) | (c & (~d))) + x + ac;
        return ((a << s) | (a >>> (32 - s))) + b;
    }

    private static int HH(int a, int b, int c, int d, int x, int s, int ac) {
        a += ((b ^ c) ^ d) + x + ac;
        return ((a << s) | (a >>> (32 - s))) + b;
    }

    private static int II(int a, int b, int c, int d, int x, int s, int ac) {
        a += (c ^ (b | (~d))) + x + ac;
        return ((a << s) | (a >>> (32 - s))) + b;
    }

    /**
     * This is where the functions come together as the generic MD5
     * transformation operation. It consumes sixteen
     * bytes from the buffer, beginning at the specified offset.
     */
    void implCompress(byte[] buf, int ofs) {
        int a = state[0];
        int b = state[1];
        int c = state[2];
        int d = state[3];

        b2iLittle(buf, ofs, x, 0, 64);

        /* Round 1 */
        a = FF(a, b, c, d, x[0], S11, 0xd76aa478); /* 1 */
        d = FF(d, a, b, c, x[1], S12, 0xe8c7b756); /* 2 */
        c = FF(c, d, a, b, x[2], S13, 0x242070db); /* 3 */
        b = FF(b, c, d, a, x[3], S14, 0xc1bdceee); /* 4 */
        a = FF(a, b, c, d, x[4], S11, 0xf57c0faf); /* 5 */
        d = FF(d, a, b, c, x[5], S12, 0x4787c62a); /* 6 */
        c = FF(c, d, a, b, x[6], S13, 0xa8304613); /* 7 */
        b = FF(b, c, d, a, x[7], S14, 0xfd469501); /* 8 */
        a = FF(a, b, c, d, x[8], S11, 0x698098d8); /* 9 */
        d = FF(d, a, b, c, x[9], S12, 0x8b44f7af); /* 10 */
        c = FF(c, d, a, b, x[10], S13, 0xffff5bb1); /* 11 */
        b = FF(b, c, d, a, x[11], S14, 0x895cd7be); /* 12 */
        a = FF(a, b, c, d, x[12], S11, 0x6b901122); /* 13 */
        d = FF(d, a, b, c, x[13], S12, 0xfd987193); /* 14 */
        c = FF(c, d, a, b, x[14], S13, 0xa679438e); /* 15 */
        b = FF(b, c, d, a, x[15], S14, 0x49b40821); /* 16 */

        /* Round 2 */
        a = GG(a, b, c, d, x[1], S21, 0xf61e2562); /* 17 */
        d = GG(d, a, b, c, x[6], S22, 0xc040b340); /* 18 */
        c = GG(c, d, a, b, x[11], S23, 0x265e5a51); /* 19 */
        b = GG(b, c, d, a, x[0], S24, 0xe9b6c7aa); /* 20 */
        a = GG(a, b, c, d, x[5], S21, 0xd62f105d); /* 21 */
        d = GG(d, a, b, c, x[10], S22, 0x2441453); /* 22 */
        c = GG(c, d, a, b, x[15], S23, 0xd8a1e681); /* 23 */
        b = GG(b, c, d, a, x[4], S24, 0xe7d3fbc8); /* 24 */
        a = GG(a, b, c, d, x[9], S21, 0x21e1cde6); /* 25 */
        d = GG(d, a, b, c, x[14], S22, 0xc33707d6); /* 26 */
        c = GG(c, d, a, b, x[3], S23, 0xf4d50d87); /* 27 */
        b = GG(b, c, d, a, x[8], S24, 0x455a14ed); /* 28 */
        a = GG(a, b, c, d, x[13], S21, 0xa9e3e905); /* 29 */
        d = GG(d, a, b, c, x[2], S22, 0xfcefa3f8); /* 30 */
        c = GG(c, d, a, b, x[7], S23, 0x676f02d9); /* 31 */
        b = GG(b, c, d, a, x[12], S24, 0x8d2a4c8a); /* 32 */

        /* Round 3 */
        a = HH(a, b, c, d, x[5], S31, 0xfffa3942); /* 33 */
        d = HH(d, a, b, c, x[8], S32, 0x8771f681); /* 34 */
        c = HH(c, d, a, b, x[11], S33, 0x6d9d6122); /* 35 */
        b = HH(b, c, d, a, x[14], S34, 0xfde5380c); /* 36 */
        a = HH(a, b, c, d, x[1], S31, 0xa4beea44); /* 37 */
        d = HH(d, a, b, c, x[4], S32, 0x4bdecfa9); /* 38 */
        c = HH(c, d, a, b, x[7], S33, 0xf6bb4b60); /* 39 */
        b = HH(b, c, d, a, x[10], S34, 0xbebfbc70); /* 40 */
        a = HH(a, b, c, d, x[13], S31, 0x289b7ec6); /* 41 */
        d = HH(d, a, b, c, x[0], S32, 0xeaa127fa); /* 42 */
        c = HH(c, d, a, b, x[3], S33, 0xd4ef3085); /* 43 */
        b = HH(b, c, d, a, x[6], S34, 0x4881d05); /* 44 */
        a = HH(a, b, c, d, x[9], S31, 0xd9d4d039); /* 45 */
        d = HH(d, a, b, c, x[12], S32, 0xe6db99e5); /* 46 */
        c = HH(c, d, a, b, x[15], S33, 0x1fa27cf8); /* 47 */
        b = HH(b, c, d, a, x[2], S34, 0xc4ac5665); /* 48 */

        /* Round 4 */
        a = II(a, b, c, d, x[0], S41, 0xf4292244); /* 49 */
        d = II(d, a, b, c, x[7], S42, 0x432aff97); /* 50 */
        c = II(c, d, a, b, x[14], S43, 0xab9423a7); /* 51 */
        b = II(b, c, d, a, x[5], S44, 0xfc93a039); /* 52 */
        a = II(a, b, c, d, x[12], S41, 0x655b59c3); /* 53 */
        d = II(d, a, b, c, x[3], S42, 0x8f0ccc92); /* 54 */
        c = II(c, d, a, b, x[10], S43, 0xffeff47d); /* 55 */
        b = II(b, c, d, a, x[1], S44, 0x85845dd1); /* 56 */
        a = II(a, b, c, d, x[8], S41, 0x6fa87e4f); /* 57 */
        d = II(d, a, b, c, x[15], S42, 0xfe2ce6e0); /* 58 */
        c = II(c, d, a, b, x[6], S43, 0xa3014314); /* 59 */
        b = II(b, c, d, a, x[13], S44, 0x4e0811a1); /* 60 */
        a = II(a, b, c, d, x[4], S41, 0xf7537e82); /* 61 */
        d = II(d, a, b, c, x[11], S42, 0xbd3af235); /* 62 */
        c = II(c, d, a, b, x[2], S43, 0x2ad7d2bb); /* 63 */
        b = II(b, c, d, a, x[9], S44, 0xeb86d391); /* 64 */

        state[0] += a;
        state[1] += b;
        state[2] += c;
        state[3] += d;
    }


    /**
     * {@inheritDoc}
     *
     */
    public byte[] getAlgorithmID() {
        return DIGEST_INFO;
    }



}