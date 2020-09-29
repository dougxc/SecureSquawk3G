/*
 *   @(#) MPEG-I Vide Decoder 0.9 Yu Tianli (yu-tianli@usa.net)
 *
 *   AudioDecoder.java   1999-2-24 09:11 am
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
 *   MPEG-I Audio Stream Decoder
 * Note: This class is for future extension, now it just kick the buffer back to system queue
 */
class AudioDecoder implements Runnable
{
    /**
     *   The Buffered bit stream where all the audio data comes
     */
    private BufferedBitStream audioStream;

    /**
     *   Constructor
     */
    AudioDecoder( BufferedBitStream stream )
    {
        audioStream = stream;
    }

    /**
     *   Reset the audio stream decoder
     */
    void reset()
    {
        audioStream.reset();
    }

    /**
     *   The main Audio decoding process ( just kicking back the buffer )
     */
    public void run()
    {
Trace.println("AudioDecode::run()++");

        try {
            int code = audioStream.showBits(32);
            //while ( code != VideoDecoder.SEQUENCE_END_CODE) {
            // Note: Change to ISO_11172_END_CODE to force the system decoder die first
            while ( code != BitStream.ISO_11172_END_CODE) {
Trace.println("AudioDecode::run() nextBuffer()");
                audioStream.nextBuffer();
                code = audioStream.showBits(32);
            }

        } catch (InterruptedException e) {
            System.out.println(" InterruptedException in AudioDecoder Thread: " + e);
        }
Trace.println("AudioDecode::run()--");

    }
}
