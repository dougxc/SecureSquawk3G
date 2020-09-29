/*
 *   @(#) MPEG-I Vide Decoder 0.9 Yu Tianli (yu-tianli@usa.net)
 *
 *   BufferedBitStream.java   1999-2-24 09:11 am
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

package example.mpeg;

/**
 *   BufferedBitStream reads from a BitBuffer and provide data to audio and video decoder
 */
class BufferedBitStream
{
    /**
     *   Synchronizing start code
     */
    public static final int SYNC_START_CODE = 0x000001;

    /**
     *   The data queue to get data buffer
     */
    private BufferQueue dataQueue;

    /**
     *   The system queue to enqueue used buffer
     */
    private BufferQueue systemQueue;

    /**
     *   The buffer that currently in use
     */
    private ByteBuffer bytebuffer = null;

    /**
     *   The buffer for bit processing
     */
    private int bitbuffer;

    /**
     *   The remaining bit length in bitbuffer
     */
    private int bitlength;

    /**
     *   The next int data from the bytebuffer
     */
    private int nextdata;

    /**
     *   If the nextdata is valid
     */
    private boolean available;

    /**
     *   Initializes the input buffered stream.
     * Note: the BufferedBitStream should hold one empty buffer while constructing
     */
    public BufferedBitStream (BufferQueue dataQueue, BufferQueue systemQueue, ByteBuffer bytebuffer)
    {
        this.dataQueue = dataQueue;
        this.systemQueue = systemQueue;

        // The first bytebuffer should have len = 0
        // This is used to hack for speed and convenience in read8Bits()
        this.bytebuffer = bytebuffer;
        bitbuffer = bitlength = 0;
        nextdata = 0;
        available = false;
    }

    /**
     *   Reset the BufferedBitStream and its dataQueue
     */
    void reset()
    {
        bitbuffer = bitlength = 0;
        nextdata = 0;
        available = false;
        if (bytebuffer != null)
            bytebuffer.reset();
        // clear all the data in the dataQueue
        dataQueue.reset();
    }

    /**
     * Shows the next MPEG-1 layer start code.
     * @see getCode()
     * @return  the 32-bits start code
     */
    public int showCode() throws InterruptedException
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
    public int getCode() throws InterruptedException
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
    public int showBits(int nbit) throws InterruptedException
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
    public int getBits(int nbit) throws InterruptedException
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
    public void flushBits(int nbit) throws InterruptedException
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
    public void alignBits(int nbit) throws InterruptedException
    {
        flushBits(bitlength % nbit);
    }

    /**
     *   Change to the next buffer ( for audio decoder )
     */
    void nextBuffer() throws InterruptedException
    {
        systemQueue.enQueue(bytebuffer);

        bitlength = bitbuffer = 0;
        available = false;

        bytebuffer = dataQueue.deQueue();
    }

    /**
     *   Show the next 32 bit data from the bytebuffer
     */
    private int show32Bits() throws InterruptedException
    {
        if (!available) {
            int b0 = read8Bits();
            int b1 = read8Bits();
            int b2 = read8Bits();
            int b3 = read8Bits();
            nextdata = (b0 << 24) | (b1 << 16) | (b2 << 8) | b3;
            available = true;
        }
        return nextdata;
    }

    /**
     *   Get the next 32 bit data from the bytebuffer
     */
    private int get32Bits() throws InterruptedException
    {

        if (!available) {
            int b0 = read8Bits();
            int b1 = read8Bits();
            int b2 = read8Bits();
            int b3 = read8Bits();
            nextdata = (b0 << 24) | (b1 << 16) | (b2 << 8) | b3;
        }
        available = false;
        return nextdata;
    }

    /**
     *   Read next byte data from the bytebuffer
     */
    private int read8Bits() throws InterruptedException
    {
        while (bytebuffer.position >= bytebuffer.len) {
            // Get the data from queue
            systemQueue.enQueue(bytebuffer);

            bytebuffer = dataQueue.deQueue();
        }
        return (bytebuffer.buffer[bytebuffer.position++] & 0xff);
    }
}