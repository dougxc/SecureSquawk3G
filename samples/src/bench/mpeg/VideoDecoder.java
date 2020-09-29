/*
 *   @(#) MPEG-I Vide Decoder 0.9 Yu Tianli (yu-tianli@usa.net)
 *
 *   VideoDecoder.java   1999-2-24 09:11 am
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
 *   MPEG-I Video stream decoder
 */
class VideoDecoder implements Runnable
{

    /**
     *   MPEG Video layer start code
     */
    static final int USER_DATA_START_CODE = 0x000001B2;
    static final int SEQUENCE_HEADER_CODE = 0x000001B3;
    static final int EXTENSION_START_CODE = 0x000001B5;
    static final int SEQUENCE_END_CODE = 0x000001B7;
    private static final int PICTURE_START_CODE = 0x00000100;
    private static final int GOP_START_CODE = 0x000001B8;

    /**
     *   The buffered bit stream where all the video data comes from
     */
    private BufferedBitStream videoStream = null;

    /**
     *   One Frame in the stream
     */
    private Picture picture;

    /**
     *   The picture rate code of the stream
     */
    private int pictureRate = 0;

    /**
     *   The picture rate look up table
     */
/*
    private static final double [] picRateTable = {
        0,                 24000.0/1001.0,     24,              25,
        30000.0/1001.0,    30,                 50,              60000.0/1001.0,
        60,                0,                  0,               0,
        0,                 0,                  0,               0
    };
*/

    private static final int [] picRateTable = {
        0,                 24,                 24,              25,
        30,                30,                 50,              60,
        60,                0,                  0,               0,
        0,                 0,                  0,               0
    };



    /**
     *   Interval between frames calc from picture rate, in millisecond
     */
    private long intervalPerFrame = 0;

    /**
     *   Systime when the last frame was displayed, in millisecond, used in frame rate control
     */
    private long lastTime = 0;

    /**
     *   Horizontal and Vertical size of the video image
     */
    private int horizontalSize = 0, verticalSize = 0;

    /**
     *   The actual width of data in the rgbFrame buffer
     */
    private int stride = 0;

    /**
     *   The time and frame information
     */
    private int hours,minutes,seconds,pictures;


    /**
     *   One frame of RGB data
     */
    private int rgbFrame[];

    /**
     *   The interface to send the image data
     */
    private Displayable screen;

    /**
     *   MemoryImageSource to hold the memory image data
     */
    //private MemoryImageSource imageSource;

    /**
     *   DirectColorModel for 24-bit RGB color
     */
    //private DirectColorModel colormodel;

    /**
     *   Converting and clip table for  Y Cb Cr  to  RGB  conversion
     */
    private static int clip[], cbtable[], crtable[];

    /**
     *   Initial value for clip[], cbtable[], crtable[]
     */
    static {
        clip = new int[1024];
        cbtable = new int[256];
        crtable = new int[256];
        for (int i=0;i<1024;i++)
            clip[i] = Math.min(Math.max(i-512,0), 255);
        for (int i=0;i<256;i++) {
            int level = i-128;
            cbtable[i] = (((int)((1772 * level)/1000)) << 16) - ((int)((344 * level)/1000));
            crtable[i] = (((int)((1402 * level)/1000)) << 16) - ((int)((714 * level)/1000));
        }
    }


    /**
     *   Control Flag const
     */
    private static final int RESETED = 1;
    private static final int PLAYING = 2;
    private static final int PAUSE = 3;
    private static final int FAST_FORW = 4;

    /**
     *   Control Flag
     */
    private int controlFlag;

    /**
     *   Other unused information
     */
    private int pelAspectRatio = 0;
    private int bitRate = 0;
    private int vbvBufferSize = 0;
    private boolean constrainedParamFlag = false;
    private boolean dropFrameFlag = false;
    private boolean closedGOP = false;
    private boolean brokenLink = false;

    /**
     *   Constructor
     * @param   stream      The BufferedBitStream where the video data comes from
     * @param   screen      The displayable interface to send the image data
     */
    VideoDecoder( BufferedBitStream stream, Displayable screen )
    {
Trace.println("VideoDecode::init()++");
        videoStream = stream;
        picture = new Picture();
        this.screen = screen;
Trace.println("VideoDecode::init()--");
    }

    /**
     *   Reset the VideoDecoder
     */
    void reset()
    {
        videoStream.reset();
        hours = minutes = seconds = pictures = 0;
        screen.updateTime(hours, minutes, seconds);
        intervalPerFrame = 0;
        controlFlag = RESETED;
        if (rgbFrame != null) {
            for (int i=0;i < rgbFrame.length;i++)
                rgbFrame[i] = 0;
        }

        picture.reset();
        screen.nextImage();
    }

    /**
     *   Tell video decoder it's time to pause
     */
    void setPause()
    {
        controlFlag = PAUSE;
    }

    /**
     *   Tell video decoder to skip B-frame
     */
    void setFastPlay()
    {
        controlFlag = FAST_FORW;
    }

    /**
     *   Tell video to play at normal speed
     */
    void setNormalPlay()
    {
        controlFlag = PLAYING;
    }

    /**
     *   Wake up the Video Decoder that paused
     */
    synchronized void wakeup()
    {
        notify();
    }

    /**
     *   Pause decoding
     */
    synchronized private void pause()
    {
Trace.println("pause()");
/*
        try {
            wait();
        } catch (InterruptedException e) {
        }
*/
    }

    /**
     *   One round of video decoding
     */
    public void run()
    {
Trace.println("VideoDecode::run()++");
        try{
            // Note: Decoding the video_sequence;
            //while (videoStream.showCode() != SEQUENCE_END_CODE) {
            // Note: Change to ISO_11172_END_CODE to force the system decoder die first
            while (videoStream.showCode() != BitStream.ISO_11172_END_CODE) {
                if (videoStream.showCode() == SEQUENCE_HEADER_CODE) {

                    getSequenceHeader();
                    while (videoStream.showCode() == GOP_START_CODE) {
Trace.println("VideoDecode::run() calling getGOP()");
                        getGOP();
                    }
                } else {
                    videoStream.flushBits(32);
                }
            }

            // Draw the last frame that is still in buffer.
//            if (imageSource != null)
//                drawFrame(picture.getBackwardFrame());

        } catch (InterruptedException e) {
            System.out.println("InterruptedException: "+e);
            System.exit(-1);
        }
Trace.println("VideoDecode::run()--");

    }

    /**
     *   Parsing the Sequence Header
     */
    private void getSequenceHeader() throws InterruptedException
    {
        // Parses off the sequence header code
        videoStream.flushBits(32);

        // horizontal and vertical size resize the screen and the imagesource
        setDisplaySize();

        setPelAspectRatio(videoStream.getBits(4));

        setPictureRate(videoStream.getBits(4));

        setBitRate(videoStream.getBits(18));

        // Marker bit
        videoStream.flushBits(1);

        setVBVBufferSize(videoStream.getBits(10));

        setConstrainedParamFlag(videoStream.getBits(1)==1);

        // Load intra quantitizer matrix??
        if (videoStream.getBits(1)==1)
            // Load
            picture.setIntraQuantMatrix(videoStream);
        else picture.setDefaultIntraQuantMatrix();

        // Load inter quantitizer matrix??
        if (videoStream.getBits(1)==1)
            // Load
            picture.setInterQuantMatrix(videoStream);
        else picture.setDefaultInterQuantMatrix();

        // By pass sequence extension data
        if (videoStream.showCode()==EXTENSION_START_CODE)
            videoStream.flushBits(32);

        // By pass user data
        if (videoStream.showCode()==USER_DATA_START_CODE)
            videoStream.flushBits(32);

        videoStream.showCode();
        // Parsing Sequence Header Finished!
    }

    /**
     *   Parsing the Group of Picture
     */
    private void getGOP() throws InterruptedException
    {
//Trace.println("getGOP() 1");
        // Parsing off the GOP Start code
        videoStream.flushBits(32);

        setTimeCode();

        setClosedGOP(videoStream.getBits(1)==1);

        setBrokenLink(videoStream.getBits(1)==1);

        // By pass sequence extension data junks
        if (videoStream.showCode()==EXTENSION_START_CODE)
            videoStream.flushBits(32);

        // By pass user data
        if (videoStream.showCode()==USER_DATA_START_CODE)
            videoStream.flushBits(32);

        int code = videoStream.showCode();
//Trace.println("getGOP() 2");


        while ((code != GOP_START_CODE) && (code != SEQUENCE_HEADER_CODE) && (code != BitStream.ISO_11172_END_CODE)) {//SEQUENCE_END_CODE)) {
//Trace.println("getGOP() 3");

            if (code == PICTURE_START_CODE) {
//Trace.println("getGOP() 3a");

                // For pause
                if (controlFlag == PAUSE)
                    pause();
                try {
//Trace.println("getGOP() 3b");

                    screen.updateFrame();
//Trace.println("getGOP() 3c");
                    picture.getPicture(videoStream, (controlFlag == FAST_FORW));
//Trace.println("getGOP() 3d");
                } catch (PictureFormatException e) {
                    System.out.println("PictureFormatException: "+e +" Go on to the next picture");
                }
//Trace.println("getGOP() 3e");
                if (picture.getPictureType() == Picture.B_TYPE) {
//Trace.println("getGOP() 3f");
                    if (controlFlag != FAST_FORW) {
                        drawFrame(picture.getCurrentFrame());
                    }
//Trace.println("getGOP() 3G");
                } else {
                    drawFrame(picture.getIPFrame());
                }
//Trace.println("getGOP() 3H");
            } else {
                videoStream.flushBits(32);
            }
            code = videoStream.showCode();
        }
//Trace.println("getGOP() 4");

        // Parsing GOP finished!
    }

    /**
     *   Draw the next frame ( YCbCr conversion and frame control )
     */
    private void drawFrame(int frame[])
    {
        int offset = 0;
        int YCbCr = 0;
        int yValue = 0;
        int cbValue = 0;
        int crValue = 0;

        for ( int x = 0; x < horizontalSize ; x++ ) {
            for (int y = 0; y < verticalSize ; y++ ) {

                YCbCr = frame[offset];
                yValue = (YCbCr & 0xff)  + 512;
                cbValue = cbtable[ (YCbCr >> 8) & 0xff ];
                crValue = crtable[ (YCbCr >> 16) & 0xff ];
//Trace.println("x "+x+" y "+y);
                rgbFrame[offset++] = (clip[yValue + (crValue >> 16)]) + (clip[yValue + (((cbValue + crValue) << 16) >> 16)] << 8) + (clip[yValue + (cbValue >> 16)] << 16);
            }
            offset += stride - horizontalSize;
        }


/**
        // Frame rate Control. (The thread will never sleep unless ... urh , it's a P III/1000MHz ??? )
        long now = System.currentTimeMillis();
        try {
            Thread.sleep(Math.max(intervalPerFrame - (now - lastTime), 0));
        } catch (InterruptedException e) {
        }

        lastTime = now;
*/
        screen.nextImage();
    }

    /**
     *   Set the Display size
     */
    private void setDisplaySize() throws InterruptedException
    {
        int horizontal = videoStream.getBits(12);
        int vertical = videoStream.getBits(12);

        if ((horizontalSize!=horizontal)||(verticalSize!=vertical)) {
            horizontalSize = horizontal;
            verticalSize = vertical;

            int mbWidth = (horizontal +15) >>> 4;
            int mbHeight = (vertical + 15) >>> 4;

            stride = mbWidth << 4;

            picture.setPictureMBSize(mbWidth, mbHeight);

            rgbFrame = new int[256 * mbWidth * mbHeight];
/*
                colormodel = new DirectColorModel(24, 0x0000ff, 0x00ff00, 0xff0000);
            imageSource = new MemoryImageSource(horizontalSize,verticalSize,colormodel,rgbFrame,0, stride);
            imageSource.setAnimated(true);
            screen.setImageSource( imageSource );
*/
        }
        screen.setDisplaySize(horizontalSize, verticalSize);
    }

    /**
     *   Parsing the time code
     */
    private void setTimeCode() throws InterruptedException
    {
        dropFrameFlag = (videoStream.getBits(1)==1);
        hours = videoStream.getBits(5);
        minutes = videoStream.getBits(6);

        // Marker bit
        videoStream.flushBits(1);

        seconds = videoStream.getBits(6);
        pictures = videoStream.getBits(6);
        screen.updateTime(hours, minutes, seconds);
    }

    /**
     *   Set the frame rate
     */
    private void setPictureRate(int value)
    {
        lastTime = System.currentTimeMillis();
        pictureRate = value;

        if (picRateTable[pictureRate] == 0)
            intervalPerFrame = 0;
        else
            intervalPerFrame = (long)(1000/picRateTable[pictureRate]);
    }

    /**
     *   Set other unused information
     */
    private void setPelAspectRatio(int value)
    {
        pelAspectRatio = value;
    }

    private void setBitRate(int value)
    {
        bitRate = value;
    }

    private void setVBVBufferSize(int value)
    {
        vbvBufferSize = value;
    }

    private void setConstrainedParamFlag(boolean flag)
    {
        constrainedParamFlag = flag;
    }

    private void setClosedGOP(boolean flag)
    {
        closedGOP = flag;
    }

    private void setBrokenLink(boolean flag)
    {
        brokenLink = flag;
    }
}


