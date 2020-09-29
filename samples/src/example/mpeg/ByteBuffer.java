/*
 *   @(#) MPEG-I Vide Decoder 0.9 Yu Tianli (yu-tianli@usa.net)
 *
 *   ByteBuffer.java   1999-2-24 09:11 am
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
 *   Byte buffer for storing the video and audio stream data
 */
class ByteBuffer
{
    /**
     *   Buffer size const ( Note, one ByteBuffer could only hold one group data
     * so a big buffer size may lead to a waste of memory with no enhanced performance )
     */
    public static final int BUFFER_SIZE = 4096;

    /**
     *   STD buffer control parameter ( unused )
     */
    private int stdBufferMark, stdBufferSize;

    /**
     *   display and decode timestamp ( unused )
     */
    private long displayTimeStamp = 0, decodeTimeStamp = 0;

    /**
     *   The buffer array
     */
    byte buffer[];

    /**
     *   The position of the current unreaded data
     */
    int position = 0;

    /**
     * The length of the buffer
     */
    int len = 0;

    /**
     * Pointer to the next BitBuffer
     */
    ByteBuffer nextBuffer = null;

    /**
     *   Constructor
     */
    public ByteBuffer()
    {
        buffer = new byte[BUFFER_SIZE];
        position = 0;
        len = 0;
    }

    /**
     *   Reset the ByteBuffer
     */
    void reset()
    {
        position = len = 0;
        displayTimeStamp = decodeTimeStamp = 0;
        stdBufferMark = stdBufferSize = 0;
    }

    /**
     *   Set the next buffer
     */
    void setNext(ByteBuffer next)
    {
        nextBuffer = next;
    }

    /**
     *   Get the next buffer
     */
    ByteBuffer getNext()
    {
        return nextBuffer;
    }

    /**
     *   Set some unused parameter
     */
    void setSTDBufferInfo(int stdBufferMark, int stdBufferSize)
    {
        this.stdBufferMark = stdBufferMark;
        this.stdBufferSize = stdBufferSize;
    }

    void setTimeStamp(long displayTimeStamp, long decodeTimeStamp)
    {
        this.displayTimeStamp = displayTimeStamp;
        this.decodeTimeStamp = decodeTimeStamp;
    }
}