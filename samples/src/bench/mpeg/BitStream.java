/*
 *   @(#) MPEG-I Vide Decoder 0.9 Yu Tianli (yu-tianli@usa.net)
 *
 *   BitStream.java   1999-2-24 09:11 am
 *
 *   Copyright (C) 1999  Yu Tianli
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation; either version 2 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program; if not, write to the Free Software
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 *
 *   The Video Stream Decoder part of this program is based on
 *   Carlos Hasan's MPEG-1 video stream decoder version 0.9 1998
 */

package bench.mpeg;
import java.io.*;

/**
 * MPEG-1 BitStream for the System Decoder
 */
class BitStream
{
    /**
     * The start code for every MPEG-1 stream start code
     */
    public static final int SYNC_START_CODE = 0x000001;
    public static final int ISO_11172_END_CODE = 0x000001b9;

    /**
     * The underlying MPEG-1 System Stream.
     */
    private InputStream mpegStream;

    /**
     * The bit buffer for bit operation of MPEG-1 System Stream.
     * The first bit of the stream is the Most Significant Bit of bitbuffer.
     */
    private int bitbuffer;

    /**
     * The length of the bits currently available in the bitbuffer.
     */
    private int bitlength;

    /**
     *   The next int data from the inputstream
     */
    private int nextdata = 0;

    /**
     *   If the nextdata is valid
     */
    private boolean available = false;

    /**
     *   Initializes the input bit stream.
     */
    public BitStream (InputStream inputstream)
    {
        mpegStream = inputstream;
        bitbuffer = bitlength = 0;
    }

    /**
     *   Reset the BitStream ( drop but didn't close the inputstream )
     * high layer should call setInputStream to set another inputstream
     */
    void reset()
    {
        mpegStream = null;
        bitbuffer = bitlength = 0;
        nextdata = 0;
        available = false;
    }

    /**
     *   Set the inputstream
     */
    void setInputStream(InputStream inputstream)
    {
        mpegStream = inputstream;
    }

    /**
     * Shows the next MPEG-1 layer start code.
     * @see getCode()
     * @return  the 32-bits start code
     */
    public int showCode() throws IOException
    {
        alignBits(8);
        while (showBits(24)!= SYNC_START_CODE)
            flushBits(8);
        return showBits(32);
    }

    /**
     * Gets the next MPEG-1 layer start code.
     * @see showCode()
     * @return  the 32-bits start code
     */
    public int getCode() throws IOException
    {
        alignBits(8);
        while (showBits(24)!= SYNC_START_CODE)
            flushBits(8);
        return getBits(32);
    }

    /**
     * Shows next nbit bits from the MPEG-1 system stream.
     * @see getBits(int)
     * @param   nbit        the number of bits to show
     * @return  the wanted bits in the Least Significant Bits of the int
     */
    public int showBits(int nbit) throws IOException
    {
        int bits = bitbuffer >>> (32 - nbit);
        if (nbit > bitlength) {
            bits |= show32Bits() >>> (32 + bitlength - nbit);
        }
        return bits;
    }

    /**
     * Gets next nbit bits from the MPEG-1 system stream.
     * @see showBits(int)
     * @param   nbit        the number of bits to get
     * @return  the wanted bits in the Least Significant Bits of the int
     */
    public int getBits(int nbit) throws IOException
    {
        int bits = bitbuffer >>> (32 - nbit);
        if (nbit <= bitlength) {
            bitlength -= nbit;
            bitbuffer <<= nbit;
        } else { // Additional bits needed
            bitbuffer = get32Bits();
            nbit -= bitlength;
            bitlength = 32 - nbit;
            bits |= bitbuffer >>> (bitlength);
            bitbuffer <<= nbit;
        }
        return bits;
    }

    /**
     * Flushes nbit bits from the MPEG-1 system stream.
     * @param   nbit        the number of bits to be flushed
     */
    public void flushBits(int nbit) throws IOException
    {
        if (nbit <= bitlength) {
            bitlength -= nbit;
            bitbuffer <<= nbit;
        } else {
            nbit -= bitlength;
            bitlength = 32 - nbit;
            bitbuffer = get32Bits() << nbit;
        }
    }

    /**
     * Aligns the MPEG-1 system stream to the given boundary .
     * @param   nbit        the number of bits to align the stream
     */
    public void alignBits(int nbit) throws IOException
    {
        flushBits(bitlength % nbit);
    }

    /**
     * Shows the next 32-bits data in the MPEG-1 system stream .
     * @return  the next 32-bits data
     */
    private int show32Bits() throws IOException
    {
        if (!available) {
            nextdata = read32Bits();
            available = true;
        }
        return nextdata;
    }

    /**
     * Gets the next 32-bits data from the MPEG-1 system stream .
     * @return  the next 32-bits data
     */
    private int get32Bits() throws IOException
    {
        if (!available)
            nextdata = read32Bits();
        available = false;
        return nextdata;
    }

    /**
     * Reads the next 32-bits data in the MPEG-1 system stream .
     * @return  the next 32-bits data
     */
    private int read32Bits() throws IOException
    {
        if (mpegStream.available()<=0) // End of the mpegStream
            return ISO_11172_END_CODE;

        int a0 = mpegStream.read() & 0xFF;
        int a1 = mpegStream.read() & 0xFF;
        int a2 = mpegStream.read() & 0xFF;
        int a3 = mpegStream.read() & 0xFF;
        // Change to network byte order
        return (a0 << 24) | (a1 << 16) | (a2 << 8) | a3 ;
    }

    /**
     *   Fill len length of data in to the buffer
     */
    int fillData(int len, byte[] buffer) throws IOException
    {
        if (len > buffer.length)
            len = buffer.length;

        // There's something left in bitbuffer

        int remain = bitlength / 8;
        for (int i=0;i<remain;i++)
            buffer[i] = (byte)((bitbuffer >>> ((4 - 1 - i) * 8)) & 0xff);

        bitbuffer = bitlength = 0;

        // if there are bytes in nextData field
        if (available) {
            remain += 4;
            for (int i=0;i<4;i++)
                buffer[i] = (byte)((nextdata >>> ((4 - 1 - i) * 8)) & 0xff);
            available = false;
        }

        len = mpegStream.read(buffer, remain , len - remain);
        return  (len + remain) ;
    }

    /**
     *   Skip len length of data
     */
    int skip(int len) throws IOException
    {
        int remain = bitlength / 8;
        bitbuffer = bitlength = 0;

        if (available) {
            remain += 4;
            available = false;
        }

        return (int)(mpegStream.skip(len - remain) + remain);
    }

    /**
     * BitStream Class test & demo
     */
    public static void main(String args[])
    {
        int table[] = { 0x12,0x34,0x56,0x78,0x9a,0xbc,0xde,0xff,0x00,0x00,0x01,0xb9 };
        byte line[] = new byte[table.length];
        for (int i=0;i<table.length;i++) line[i] = (byte)(table[i]&0xff);

        ByteArrayInputStream teststream = new ByteArrayInputStream(line);
        BitStream testBitStream = new BitStream(teststream);
        try {
            System.out.println( "Show 8 bits:"+Integer.toHexString(testBitStream.showBits(8)));
            System.out.println( "Get 8 bits:"+Integer.toHexString(testBitStream.getBits(8)));
            System.out.println( "Show 15 bits:"+Integer.toHexString(testBitStream.showBits(15)));
            System.out.println( "Get 15 bits:"+Integer.toHexString(testBitStream.getBits(15)));
            System.out.println( "Show the next bits:"+Integer.toHexString(testBitStream.showBits(1))+" Then align 8 bits:" );
            testBitStream.alignBits(8);
            System.out.println( "After aligning: "+ Integer.toHexString(testBitStream.getBits(8)));
            System.out.println( "Show code:" + Integer.toHexString(testBitStream.showCode()));
            System.out.println( "Get code:" + Integer.toHexString(testBitStream.getCode()));
            System.out.println( "The End");
        } catch (IOException e) {
            System.out.println( "IOException : " + e );
        }
    }
}