/*
 *   @(#) MPEG-I Vide Decoder 0.9 Yu Tianli (yu-tianli@usa.net)
 *
 *   SystemDecoder.java   1999-2-24 09:11 am
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
 *   MPEG-I System Stream Decoder
 */
public class SystemDecoder implements Runnable
{

    /**
     *   System Stream start code
     */
    public final static int PACKET_START_CODE = 0x000001BA;
    public final static int SYSTEM_HEADER_START_CODE = 0x000001BB;

    /**
     *   Base Stream Identifer
     */
    private final static int MIN_GROUP_START_CODE = 0x000001BC;
    private final static int ALL_VIDEO_STREAM_IDENTIFER = 0xB8;
    private final static int RESERVED_STREAM_IDENTIFER = 0xBC;
    private final static int SPECIFIC_STREAM_1_IDENTIFER = 0xBD;
    private final static int FILL_STUFF_IDENTIFER = 0xBD;
    private final static int SPECIFIC_STREAM_2_IDENTIFER = 0xBF;
    private final static int FIRST_AUDIO_STREAM_IDENTIFER = 0xC0;
    private final static int FIRST_VIDEO_STREAM_IDENTIFER = 0xE0;
    private final static int FIRST_RESERVED_DATA_STREAM_IDENTIFER = 0xF0;

    /**
     *   Time_reference_code
     */
    private final static int TIME_REFERENCE_CODE = 2; // 0010b

    /**
     *   Video Queue where video group data should be sent
     */
    private BufferQueue videoQueue;

    /**
     *   Video Group Identifer
     */
    private int videoStreamIdentifer = 0;

    /**
     *   Audio Queue where audio group data should be sent
     */
    private BufferQueue audioQueue;

    /**
     *   Audio Group Identifer
     */
    private int audioStreamIdentifer = 0;

    /**
     *   System Queue where to find the empty buffer
     */
    private BufferQueue systemQueue;

    /**
     *   Underlying data stream
     */
    private BitStream systemStream = null;

    /**
     *   Flag to check if systemStream should stop
     */
    private boolean shouldStop = false;

    /**
     *   Unused system stream info
     */
    private long systemClockReference;
    private int streamBitRate;
    private int bitRateUpBound;
    private int audioUpBound;
    private int videoUpBound;
    private boolean fixedBitRateFlag;
    private boolean cspsFlag;

    /**
     *   Constructor (no inputstream, application should call setSystemStream before starting the thread )
     */
    public SystemDecoder(BufferQueue systemQueue, BufferQueue vQueue, BufferQueue aQueue)
    {
        systemStream = null;
        videoQueue = vQueue;
        audioQueue = aQueue;
        this.systemQueue = systemQueue;
    }

    /**
     *   Constructor (ready to start)
     */
/*
    public SystemDecoder(InputStream inputstream, BufferQueue systemQueue, BufferQueue vQueue, BufferQueue aQueue)
    {
        systemStream = new BitStream(inputstream);
        videoQueue = vQueue;
        audioQueue = aQueue;
        this.systemQueue = systemQueue;
    }
*/
    /**
     *   Set the underlying MPEG data inputstream
     */
    void setSystemStream(InputStream inputstream)
    {
        if (systemStream == null)
            systemStream = new BitStream(inputstream);
        else
            systemStream.setInputStream(inputstream);
    }

    /**
     *   Reset the system decoder
     * SystemDecoder's reset will not close or reset the inputstream
     * application should either close or reset the inputstream
     * and call setSystemStream to change the inputstream
     */
    void reset()
    {
        if (systemStream != null)
            systemStream.reset();
        //systemQueue.reset();
        videoStreamIdentifer = 0;
        audioStreamIdentifer = 0;
    }

    /**
     *   Set the stop flag to indicate that system decoder should stop
     */
    synchronized void stop()
    {
        shouldStop = true;
    }

    /**
     *   The main system stream decoding process
     */
    public void run()
    {
Trace.println("SystemDecode::run()++");

        shouldStop = false;

        try {
            while ((systemStream.showCode() != BitStream.ISO_11172_END_CODE) && (!shouldStop)) {

                if (systemStream.showCode() == PACKET_START_CODE) {
                    // a standard 11172 system stream
                    try {
Trace.println("SystemDecode::run() getPacket()");
                        getPacket();
                    } catch (PacketFormatException e) {
                        System.out.println("PacketFormatException: "+e);
                        System.out.println("Go on to the next packet");
                    }
                } else if (systemStream.showCode() == VideoDecoder.SEQUENCE_HEADER_CODE) {
                    // a layer II video stream
Trace.println("SystemDecode::run() getVideoStream()");
                    getVideoStream();
                } else systemStream.flushBits(32);
            }

            sayGoodNight(videoQueue);
            sayGoodNight(audioQueue);

        } catch ( IOException e ) {
            System.out.println( "Unrecoverable IOException:" + e);
            System.exit(-1);
        } catch ( InterruptedException e ) {
            System.out.println( "InterruptedException in SystemDecoder: " + e);
            System.exit(-1);
        }
Trace.println("SystemDecode::run()--");

    }

    /**
     *   Sent the end code to both audio and video queue to stop them
     */
    private void sayGoodNight(BufferQueue queue) throws InterruptedException
    {
        ByteBuffer buf;
        // fill the last buf with the SEQUENCE_END_CODE
        buf = systemQueue.deQueue();
        buf.setSTDBufferInfo(0,0);
        buf.setTimeStamp(0,0);
        for (int i=0;i<4;i++)
            buf.buffer[i] = (byte)((BitStream.ISO_11172_END_CODE >> ((4 - 1 - i) * 8)) & 0xff);

        // In case the Video Stream pay no attention to the above
        // More powerful killer data
        for (int i=4;i<128;i++)
            buf.buffer[i] = 0;

        for (int i=120;i<124;i++)
            buf.buffer[i] = (byte)((BitStream.ISO_11172_END_CODE >> ((4 - 1 - (i % 4)) * 8)) & 0xff);

        // We are sure the video stream will end up in with a ArrayIndexOutOfBoundsException now
        // Don't worry , we will catch it and the video stream will die gracefully  :)

        buf.len = 128;
        buf.position = 0;
        queue.enQueue(buf);
    }


    /**
     *   Passing all the data to videoQueue in case the inputstream is a MPEG-I video stream
     */
    private void getVideoStream() throws IOException, InterruptedException
    {
        ByteBuffer buf;

        do {
            if (shouldStop)
                return;
            buf = systemQueue.deQueue();
            buf.setSTDBufferInfo(0,0);
            buf.setTimeStamp(0,0);
            buf.len = systemStream.fillData(buf.buffer.length, buf.buffer) ;
            buf.position = 0;
            videoQueue.enQueue(buf);
        } while (buf.len == buf.buffer.length);
    }

    /**
     *   Parsing the Packet layer
     */
    private void getPacket() throws IOException, InterruptedException, PacketFormatException
    {
        // parse off the PACKET_START_CODE
        systemStream.flushBits(32);

        // the system clock reference 0010b
        systemStream.flushBits(4);

        // 32 - 30 bit of systemClockReference
        systemClockReference = systemStream.getBits(3) << (33-3);

        // Marker bit
        systemStream.flushBits(1);

        // 29 - 15 bit of systemClockReference
        systemClockReference += systemStream.getBits(15) << (30-15);

        // Marker bit
        systemStream.flushBits(1);

        // 14 - 0 bit of systemClockReference
        systemClockReference += systemStream.getBits(15);

        // Marker bit
        systemStream.flushBits(2);

        // The BitRate of the multiplex stream
        setStreamBitRate(systemStream.getBits(22));

        // Marker bit
        systemStream.flushBits(1);

        if (systemStream.showCode() == SYSTEM_HEADER_START_CODE)
            getSystemHeader();

        while ((systemStream.showCode() >= MIN_GROUP_START_CODE) && (!shouldStop)) {

            try {
                // A new group stream
                getGroupStream();
            } catch (GroupFormatException e) {
                System.out.println("GroupFormatException: "+e+" Go on to the next Group!");
            }
        }
    }

    /**
     *   Parsing the System Header
     */
    private void getSystemHeader() throws IOException, PacketFormatException
    {
        // parses the SYSTEM_HEADER_START_CODE
        systemStream.flushBits(32);

        // The header Length
        int headerLength = systemStream.getBits(16);

        // Marker bit
        systemStream.flushBits(1);

        // Bit Rate Up Bound
        setBitRateUpBound(systemStream.getBits(22));

        // Marker bit
        systemStream.flushBits(1);

        // Audio Up Bound
        setAudioUpBound(systemStream.getBits(6));

        // Fixed Bit Rate Flag
        setFixedBitRateFlag(systemStream.getBits(1)==1);

        // The CSPS Flag
        setCSPSFlag(systemStream.getBits(1)==1);

        // System_Audio_Lock_Flag
        audioQueue.setSystemLockFlag((systemStream.getBits(1)==1));

        // System_Video_Lock_Flag
        videoQueue.setSystemLockFlag((systemStream.getBits(1)==1));

        // Video Up Bound
        setVideoUpBound(systemStream.getBits(5));

        // Marker bit
        systemStream.flushBits(1);

        // Reserved byte
        systemStream.flushBits(8);

        headerLength -= 6;

        while (systemStream.showBits(1) == 1) {
            // The Stream Identifer
            int sysIdent = systemStream.getBits(8);

            if (((sysIdent & 0xF0) == (FIRST_VIDEO_STREAM_IDENTIFER & 0xF0)) || (sysIdent==ALL_VIDEO_STREAM_IDENTIFER)) {

                // Video Stream
                if (videoQueue.getStreamIdentifer() == 0) {
                // This is the first video stream
                    // Marker bit
                    systemStream.flushBits(2);

                    if (sysIdent==ALL_VIDEO_STREAM_IDENTIFER) videoQueue.setStreamIdentifer(0xE0);
                    else videoQueue.setStreamIdentifer(sysIdent);

                    videoQueue.setSTDBufferUBMark(systemStream.getBits(1));
                    videoQueue.setSTDBufferSizeUB(systemStream.getBits(13));

                } else systemStream.flushBits(24);

            } else if ((sysIdent & 0xC0) == (FIRST_AUDIO_STREAM_IDENTIFER & 0xE0)) {

                // Audio Stream
                if (audioQueue.getStreamIdentifer() == 0) {
                // This is the first audio stream
                    // Marker bit
                    systemStream.flushBits(2);

                    audioQueue.setStreamIdentifer(sysIdent);
                    audioQueue.setSTDBufferUBMark(systemStream.getBits(1));
                    audioQueue.setSTDBufferSizeUB(systemStream.getBits(13));

                } else systemStream.flushBits(24);

            } else if (((sysIdent & 0xF0) == (RESERVED_STREAM_IDENTIFER & 0xF0))|| ((sysIdent & 0xF0) == (FIRST_RESERVED_DATA_STREAM_IDENTIFER & 0xF0))) {
                // Discard other stream identifer
                systemStream.flushBits(24);

            } else throw new PacketFormatException("Unknown Stream Identifer in the System Header");

            headerLength -= 3;
        }

        while (headerLength > 2) {
            systemStream.flushBits(16);
            headerLength -= 2;
        }

        if (headerLength > 0) systemStream.flushBits(headerLength*8);
        // Parsing System Header finished
    }

    /**
     *   Parsing the Group Layer
     */
    private void getGroupStream() throws IOException, InterruptedException, GroupFormatException
    {
        // parses off the GROUP_START_CODE_PREFIX
        systemStream.flushBits(24);

        // The stream identifer
        int streamIdent = systemStream.getBits(8);

        BufferQueue queue = null;

        if ((streamIdent & 0xF0) == (videoQueue.getStreamIdentifer() & 0xF0))
            queue = videoQueue;
        else if ((streamIdent & 0xE0) == (audioQueue.getStreamIdentifer() & 0xE0))
            queue = audioQueue;

        // The Group Length IN BYTES
        int groupLength = systemStream.getBits(16);
        if (queue == null) { // by pass this base stream

            groupLength -= systemStream.skip(groupLength);

        } else { // Audio stream or Video Stream

            int stdBufferMark = 0;
            int stdBufferSize = 0;
            long displayTimeStamp = 0;
            long decodeTimeStamp = 0;

            // Fill stuff
            while (systemStream.showBits(8) == 0xFF) {
                systemStream.flushBits(8);
                groupLength--;
            }

            if (systemStream.showBits(2) == 0x01) {
                systemStream.flushBits(2);
                stdBufferMark = systemStream.getBits(1);
                stdBufferSize = systemStream.getBits(13);
                groupLength -= 2;
            }

            if (systemStream.showBits(4) == 0x02) {
                systemStream.flushBits(4);

                // DisplayTimeStamp
                displayTimeStamp = getTimeStamp();
                groupLength -= 5;

            } else if (systemStream.showBits(4) == 0x03) {
                systemStream.flushBits(4);

                // DisplayTimeStamp
                displayTimeStamp = getTimeStamp();

                // Marker bits
                systemStream.flushBits(4);

                // DecodeTimeStamp
                decodeTimeStamp = getTimeStamp();

                groupLength -= 10;

            } else if (systemStream.showBits(8) == 0x0F) {
                systemStream.flushBits(8);
                groupLength--;
            } else throw new GroupFormatException(" Marker bits = 00001111 expected!");

            // Enqueue the data
            enQueueData(queue, stdBufferMark, stdBufferSize, displayTimeStamp, decodeTimeStamp, groupLength);
        }

        // Group decoding Finished
    }

    /**
     *   Enqueue the Group layer into the specific queue
     */
    private void enQueueData(BufferQueue queue, int stdBufferMark, int stdBufferSize, long displayTimeStamp, long decodeTimeStamp, int dataLength) throws InterruptedException, IOException
    {
        while (dataLength > 0) {
            ByteBuffer buf;
            buf = systemQueue.deQueue();

            buf.setSTDBufferInfo(stdBufferMark, stdBufferSize);
            buf.setTimeStamp(displayTimeStamp, decodeTimeStamp);
            buf.len = systemStream.fillData(dataLength, buf.buffer) ;
            buf.position = 0;
            dataLength -= buf.len;

            queue.enQueue(buf);
        }
    }

    /**
     *   Parsing the Time Stamp
     */
    private long getTimeStamp () throws IOException,GroupFormatException
    {
        // TimeStamp bit 33 - 30
        long timestamp = systemStream.getBits(3) << (33-3);

        // Marker bit
        systemStream.flushBits(1);

        // bit 29 - 15
        timestamp |= systemStream.getBits(15) << (30 - 15);

        // Marker bit
        systemStream.flushBits(1);

        // bit 14 - 0
        timestamp |= systemStream.getBits(15) << (15 - 15);

        // Marker bit
        systemStream.flushBits(1);
        return timestamp;
    }

    /**
     *   Set the unused parameters
     */
    private void setStreamBitRate( int value )
    {
        streamBitRate = value;
    }

    private void setAudioUpBound( int value )
    {
        audioUpBound = value;
    }

    private void setBitRateUpBound(int value)
    {
        bitRateUpBound = value;
    }

    private void setFixedBitRateFlag(boolean flag)
    {
        fixedBitRateFlag = flag;
    }

    private void setCSPSFlag(boolean flag)
    {
        cspsFlag = flag;
    }

    private void setVideoUpBound(int value)
    {
        videoUpBound = value;
    }
}


