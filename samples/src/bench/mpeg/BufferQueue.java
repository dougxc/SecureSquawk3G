/*
 *   @(#) MPEG-I Vide Decoder 0.9 Yu Tianli (yu-tianli@usa.net)
 *
 *   BufferQueue.java   1999-2-24 09:11 am
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

/**
 *   The Buffer queue to control the order of the buffer
 */
class BufferQueue
{
    /**
     *   Head and tail pointer
     */
    ByteBuffer head = null, tail = null;

    /**
     *   The base stream identifer
     */
    private int streamIdentifer = 0;

    /**
     *   Unused parameter
     */
    private int STDBufferUBMark;
    private int STDBufferSizeUB;
    private boolean systemLockFlag;

    /**
     *   Constructor
     */
    public BufferQueue()
    {
        head = tail = null;
    }

    /**
     *   Reset the BufferQueue and all buffer in it
     */
    synchronized void reset()
    {
        streamIdentifer = 0;
        ByteBuffer temp = head;
        while (temp != null) {
            temp.reset();
            temp = temp.nextBuffer;
        }
        STDBufferUBMark = STDBufferSizeUB = 0;
        systemLockFlag = false;
    }

    /**
     *   Dequeue one buffer from the queue, block the thread when no buffer is available
     */
    synchronized ByteBuffer deQueue() throws InterruptedException
    {
        while (head == null)
             wait(); // wait for a new buffer to enqueue

        // Now both head and tail != null
        ByteBuffer temp = head;
        if (tail == head) tail = head.getNext();
        head = head.getNext();
        temp.setNext(null);

        return temp;
    }

    /**
     *   Enqueue one buffer to the queue, wake up the waiting thread
     */
    synchronized void enQueue(ByteBuffer buffer)
    {
        if (buffer == null) return;

        if (tail == null) {
            head = tail = buffer;
            notify(); // notify the waiting thread
        } else {
            tail.setNext(buffer);
            tail = buffer;
        }
    }

    /**
     *   Get the queue's base stream identifer
     */
    int getStreamIdentifer()
    {
        return streamIdentifer;
    }

    /**
     *   Set the queue's base stream identifer
     */
    void setStreamIdentifer(int value)
    {
        streamIdentifer = value;
    }

    /**
     *   Set some unused parameters
     */
    void setSTDBufferUBMark(int value)
    {
        STDBufferUBMark = value;
    }

    void setSTDBufferSizeUB(int value)
    {
        STDBufferSizeUB = value;
    }

    void setSystemLockFlag ( boolean value )
    {
        systemLockFlag = value;
    }
}