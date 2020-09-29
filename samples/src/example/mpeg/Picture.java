/*
 *   @(#) MPEG-I Vide Decoder 0.9 Yu Tianli (yu-tianli@usa.net)
 *
 *   Picture.java   1999-2-24 09:11 am
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
import java.io.*;

/**
 *   One Frame of MPEG Video
 */
class Picture
{
    /**
     *   Slice Start code
     */
    private static final int SLICE_MAX_CODE = 0x000001AF;
    private static final int SLICE_MIN_CODE = 0x00000101;

    /**
     *   Picture type const
     */
    static final int FORBIDDEN_TYPE = 0x00;
    static final int I_TYPE = 0x01;
    static final int P_TYPE = 0x02;
    static final int B_TYPE = 0x03;
    static final int D_TYPE = 0x04;
    static final int RESERVED_TYPE = 0x05;

    /**
     *   The clip table to map the Y, Cb, and Cr value to 0 -> 255
     */
    private static int clip [] = new int[1024];

    /**
     *   Initial the value for clip[]
     */
    static {
        for (int i=0;i<1024;i++)
            clip[i] = Math.min(Math.max(i-512, 0), 255);
    }

    // The Macro Block Address Increment VLC

    /**
     *   Unreachable code in table
     */
    private static final int UR = 0;

    /**
     *   Macro Block Escape
     */
    private static final int ESC = -2;

    /**
     *   Macro Block Stuffing
     */
    private static final int STF = -3;

    /**
     *   Macro Block Address Increment look up table
     */
    private static final int MBACode[][] = {

        // xxxx x... ...
        { UR , UR }, { UR , UR }, { 7  , 5  }, { 6  , 5  },
        { 5  , 4  }, { 5  , 4  }, { 4  , 4  }, { 4  , 4  },
        { 3  , 3  }, { 3  , 3  }, { 3  , 3  }, { 3  , 3  },
        { 2  , 3  }, { 2  , 3  }, { 2  , 3  }, { 2  , 3  },
        { 1  , 1  }, { 1  , 1  }, { 1  , 1  }, { 1  , 1  },
        { 1  , 1  }, { 1  , 1  }, { 1  , 1  }, { 1  , 1  },
        { 1  , 1  }, { 1  , 1  }, { 1  , 1  }, { 1  , 1  },
        { 1  , 1  }, { 1  , 1  }, { 1  , 1  }, { 1  , 1  },

        // 0000 xxxx ...
        { UR , UR }, { UR , UR }, { UR , UR }, { UR , UR },
        { UR , UR }, { UR , UR }, { 15 , 8  }, { 14 , 8  },
        { 13 , 8  }, { 12 , 8  }, { 11 , 8  }, { 10 , 8  },
        { 9  , 7  }, { 9  , 7  }, { 8  , 7  }, { 8  , 7  },

        // 0000 0xxx xxx
        { UR , UR }, { UR , UR }, { UR , UR }, { UR , UR },
        { UR , UR }, { UR , UR }, { UR , UR }, { UR , UR },
        { ESC, 11 }, { UR , UR }, { UR , UR }, { UR , UR },
        { UR , UR }, { UR , UR }, { UR , UR }, { STF, 11 },
        { UR , UR }, { UR , UR }, { UR , UR }, { UR , UR },
        { UR , UR }, { UR , UR }, { UR , UR }, { UR , UR },
        { 33 , 11 }, { 32 , 11 }, { 31 , 11 }, { 30 , 11 },
        { 29 , 11 }, { 28 , 11 }, { 27 , 11 }, { 26 , 11 },
        { 25 , 11 }, { 24 , 11 }, { 23 , 11 }, { 22 , 11 },
        { 21 , 10 }, { 21 , 10 }, { 20 , 10 }, { 20 , 10 },
        { 19 , 10 }, { 19 , 10 }, { 18 , 10 }, { 18 , 10 },
        { 17 , 10 }, { 17 , 10 }, { 16 , 10 }, { 16 , 10 },
        { UR , UR }, { UR , UR }, { UR , UR }, { UR , UR },
        { UR , UR }, { UR , UR }, { UR , UR }, { UR , UR },
        { UR , UR }, { UR , UR }, { UR , UR }, { UR , UR },
        { UR , UR }, { UR , UR }, { UR , UR }, { UR , UR },
    };

    /**
     *   Decode the Macro Block Address Increment code
     */
    private int getMBAddress(BufferedBitStream stream) throws InterruptedException
    {
        int addressIncrement = 0;
        // Macro Block Escape
        while (stream.showBits(11) == 0x08) {
            stream.flushBits(11);
            addressIncrement += 33;
        }

        int code = stream.showBits(11);

        int data[];

        if (code >= 128) data = MBACode [code >>> 6];
        else if (code >= 48) data = MBACode [(code >>> 3) + 32];
        else data = MBACode [code + 48];

        addressIncrement += data[0];
        stream.flushBits(data[1]);

        return addressIncrement;
    }

    /**
     *   Picture's Height and Width in Macro Blocks
     */
    private int mbHeight, mbWidth;

    /**
     *   Picture temporal reference
     */
    private int temporalReference;

    /**
     *   Picture Type
     */
    private int pictureType;

    /**
     *   VBV buffer delay
     */
    private int vbvDelay;

    /**
     *   Forward, backward prediction frame and current decoding frame data
     */
    private int forward[] , current[] , backward[];

    /**
     *   The frame data size
     */
    private int bufferSize = 0;

    /**
     *   One Macro Block
     */
    private MacroBlock macroblock;

    // methods

    /**
     *   Constructor
     */
    Picture()
    {
        macroblock = new MacroBlock();
    }

    /**
     *   Reset the picture
     */
    void reset()
    {
        if (forward != null) {
            for (int i=0;i<forward.length;i++)
                backward[i] = 0;
        }
        macroblock.reset();
    }

    /**
     *   Set the Picture's Macro Block size
     */
    void setPictureMBSize(int mbWidth, int mbHeight)
    {
        this.mbWidth = mbWidth;
        this.mbHeight = mbHeight;

        if ( bufferSize != mbHeight*mbWidth*256 ) {

            bufferSize = mbHeight*mbWidth*256;

            forward = new int[bufferSize];
            current = new int[bufferSize];
            backward = new int[bufferSize];
        }
    }

    /**
     *   Get the Macro Block
     */
    MacroBlock getMacroBlock()
    {
        return macroblock;
    }

    /**
     *   Reads the Intra Quantize Matrix data
     */
    void setIntraQuantMatrix(BufferedBitStream stream) throws InterruptedException
    {
        macroblock.setIntraQuantMatrix(stream);
    }

    /**
     *   Set to use the default Intra Quantize Matrix
     */
    void setDefaultIntraQuantMatrix()
    {
        macroblock.setDefaultIntraQuantMatrix();
    }

    /**
     *   Reads the Inter Quantize Matrix data
     */
    void setInterQuantMatrix(BufferedBitStream stream) throws InterruptedException
    {
        macroblock.setInterQuantMatrix(stream);
    }

    /**
     *   Set to use the default Inter Quantize Matrix
     */
    void setDefaultInterQuantMatrix()
    {
        macroblock.setDefaultInterQuantMatrix();
    }

    /**
     *   Get the Picture type
     */
    int getPictureType()
    {
        return pictureType;
    }

    /**
     *   Get the backward prediction frame
     */
    int[] getBackwardFrame()
    {
        return backward;
    }

    /**
     *   Get the current frame
     */
    int[] getCurrentFrame()
    {
        return current;
    }

    /**
     *   Get the correct frame after decoding I or P frame
     */
    int[] getIPFrame()
    {
        int[] temp;
        temp = backward;
        backward = current;
        current = temp;
        return forward;
    }

    /**
     *   Decoding one picture
     */
    void getPicture(BufferedBitStream stream, boolean skipB) throws InterruptedException,PictureFormatException
    {
        // Parse off the picture start code
        stream.flushBits(32);

        setTemporalReference(stream.getBits(10));
        setPictureType(stream.getBits(3));

        // For Fast Forwarding , skip the B-frame
        if ((pictureType == Picture.B_TYPE) && skipB)
            return;

        setVBVDelay(stream.getBits(16));

        // Frame re-order
        if (pictureType != B_TYPE)
            reorderBackToFor();

        if ((pictureType == P_TYPE)||(pictureType == B_TYPE))
            setForwardVectorInfo(stream);

        if (pictureType == B_TYPE)
            setBackwardVectorInfo(stream);

        // Extra Information Picture
        while (stream.showBits(1)==1)
            stream.flushBits(9);

        stream.flushBits(1);

        // By pass extension data
        if (stream.showCode()==VideoDecoder.EXTENSION_START_CODE)
            stream.flushBits(32);

        // By pass user data
        if (stream.showCode()==VideoDecoder.USER_DATA_START_CODE)
            stream.flushBits(32);

        while ((stream.showCode() >= SLICE_MIN_CODE) && (stream.showCode() <= SLICE_MAX_CODE)) {
            try {
                getSlice(stream);
            } catch (ArrayIndexOutOfBoundsException e) {
                //System.out.println("ArrayIndexOutOfBoundsException : "+e+" Go on to the next slice!");
            }
        }
        // Parsing Picture finished!
    }

    /**
     *   Decoding one slice
     */
    private void getSlice(BufferedBitStream stream) throws InterruptedException
    {
        int sliceVerticalPosition = stream.getBits(32) & 0xFF;
        int lastMBAddress = (sliceVerticalPosition - 1)*mbWidth - 1;

        // The first Macro Block flag;
        boolean firstMB = true;

        macroblock.setQuantizerScale(stream.getBits(5));

        // Extra Information Slice
        while (stream.showBits(1)==1)
            stream.flushBits(9);

        stream.flushBits(1);

        macroblock.resetDCPredictor();
        macroblock.resetMotionVector();

        do {
            // Macro Block stuffing
            while (stream.showBits(11) == 0x0F)
                stream.flushBits(11);

            int address = lastMBAddress + getMBAddress(stream);

            // Processing the skipped Macro Block
            if (firstMB) {
                firstMB = false;
                lastMBAddress = address;
            } else {
                while ( ++lastMBAddress < address ) {
                    // Motion Prediction for the skipped Macro Block

                    macroblock.resetDCPredictor();

                    // Set Motion Vector to 0 if a P Frame
                    if (pictureType == P_TYPE)
                        macroblock.resetMotionVector();

                    if (macroblock.isBidirPredicted()) // B type
                        motionPrediction(lastMBAddress, forward, macroblock.forwardVector, backward, macroblock.backwardVector);
                    else if (macroblock.isBackwardPredicted()) // B type
                        motionPrediction(lastMBAddress, backward, macroblock.backwardVector);
                    else // B or P type picture's Forward prediction
                        motionPrediction(lastMBAddress, forward, macroblock.forwardVector);
                }
            }

            macroblock.getMacroBlock(stream);

            if (macroblock.isMBIntra())
                fillPixels( address , macroblock.getData());
            else {
                if (macroblock.isBidirPredicted())
                    motionPrediction(address, forward, macroblock.forwardVector, backward, macroblock.backwardVector);
                else if (macroblock.isBackwardPredicted())
                    motionPrediction(address, backward, macroblock.backwardVector);
                else
                    motionPrediction(address, forward, macroblock.forwardVector);

                motionCompensation( address, macroblock.getData());
            }

        } while (stream.showBits(23)!=0);

        stream.showCode();
        // Parsing Slice Finished!
    }

    /**
     *   Set the Picture Type
     */
    private void setPictureType( int code ) throws PictureFormatException
    {
        if ( (code == FORBIDDEN_TYPE)||(code >= RESERVED_TYPE) )
            throw new PictureFormatException(" Unknown Picture Coding Type ");

        if (code == D_TYPE)
            throw new PictureFormatException(" D Type Frame encountered! ");

        pictureType = code;
        macroblock.setPictureType( code );
    }

    /**
     *   Set the Forward Motion Vector info
     */
    private void setForwardVectorInfo(BufferedBitStream stream) throws InterruptedException
    {
        macroblock.setFullPelForwardVector(stream.getBits(1));
        macroblock.setForwardFCode(stream.getBits(3));
    }

    /**
     *   Set the Backward Motion Vector info
     */
    private void setBackwardVectorInfo(BufferedBitStream stream) throws InterruptedException
    {
        macroblock.setFullPelBackwardVector(stream.getBits(1));
        macroblock.setBackwardFCode(stream.getBits(3));
    }

    /**
     *   Frame reorder, change backward frame to forward frame
     */
    private void reorderBackToFor()
    {
        int[] temp = forward;
        forward = backward;
        backward = temp;
    }

    /**
     *   Bidirection Motion Prediction
     */
    private void motionPrediction (int mbAddress, int forw[], MotionVector forwVector, int back[], MotionVector backVector)
    {
        int width = mbWidth << 4;
        int offset = ((mbAddress % mbWidth) + width * (mbAddress / mbWidth)) << 4;

        int deltaA = (forwVector.right >> 1) + width * (forwVector.down >> 1);
        int deltaB = (backVector.right >> 1) + width * (backVector.down >> 1);
        int deltaC = (forwVector.right & 1) + width * (forwVector.down & 1);
        int deltaD = (backVector.right & 1) + width * (backVector.down & 1);

        int i,j;
        int d0,d1,d2,d3,d4,d5;

        if ((deltaC == 0) && (deltaD == 0)) {
            for (i=0;i<16;i++) {
                for (j=0;j<16;j++) {
                    d0 = forw[offset + deltaA];
                    d1 = back[offset + deltaB];
                    d2 = (d0 & 0xfefefe) + (d1 & 0xfefefe);
                    d3 = (d0 & d1) & 0x010101;
                    current[offset++] = (d2 >> 1) + d3;
                }
                offset += width - 16;
            }
        } else {
            deltaC += deltaA;
            deltaD += deltaB;
            for (i=0;i<16;i++) {
                for (j=0;j<16;j++) {
                    d0 = forw[offset + deltaA];
                    d1 = back[offset + deltaB];
                    d2 = forw[offset + deltaC];
                    d3 = back[offset + deltaD];
                    d4 = ((d0 & 0xfcfcfc) + (d1 & 0xfcfcfc) + (d2 & 0xfcfcfc) + (d3 & 0xfcfcfc));
                    d5 = (d0 + d1 + d2 + d3 - d4) & 0x040404;
                    current[offset++] = (d4 + d5) >> 2;
                }
                offset += width -16;
            }
        }
    }

    /**
     *   Forward Motion Prediction
     */
    private void motionPrediction (int mbAddress, int frame[], MotionVector vector)
    {
        int width = mbWidth << 4;
        int offset = ((mbAddress % mbWidth) + width * (mbAddress / mbWidth)) << 4;
        int deltaA = (vector.right >> 1) + width * (vector.down >> 1);
        int deltaB = (vector.right & 1) + width * (vector.down & 1);

        int i,j;
        int d0,d1,d2,d3;

        if (deltaB == 0) {
            for (i=0;i<16;i++) {
                System.arraycopy(frame, offset+deltaA, current, offset, 16);
                offset += width;
            }
        } else {
            deltaB += deltaA;
            for (i=0;i<16;i++) {
                for (j=0;j<16;j++) {
                    d0 = frame[offset + deltaA];
                    d1 = frame[offset + deltaB];
                    d2 = (d0 & 0xfefefe) + (d1 & 0xfefefe);
                    d3 = (d0 & d1) & 0x010101;
                    current[offset++] = (d2 >> 1) + d3;
                }
                offset += width - 16;
            }
        }
    }

    /**
     *   Fill the intra block data in current frame
     */
    private void fillPixels( int mbAddress, int[][] block)
    {
        int width = mbWidth << 4;

        int offset[] = new int[4];

        offset[0] = ((mbAddress % mbWidth) + width * (mbAddress / mbWidth)) << 4;
        offset[1] = offset[0] + 8;
        offset[2] = offset[0] + (width << 3);
        offset[3] = offset[2] + 8;
        int offsetInc = width + width -8;

        int chromIndex[] = new int[4];
        chromIndex[0] = 0;
        chromIndex[1] = chromIndex[0] + 4;
        chromIndex[2] = 8 * 4;
        chromIndex[3] = chromIndex[2] + 4;
        int chromIndexInc = 8 - 4;

        int lumIndexInc = 8 + 8 - 8;

        int i,j,k;
        int off, index, lumIndex;
        int CbCr;

        for (i=0;i<4;i++) {
            off = offset[i];
            index = chromIndex[i];
            lumIndex = 0;

            for (j=0;j<4;j++) {
                for (k=0;k<4;k++) {
                    CbCr = (clip[512 + block[4][index]] << 8) | (clip[512 + block[5][index]] << 16);
                    current[off] = clip[512 + block[i][lumIndex]] | CbCr;
                    current[off + 1] = clip[512 + block[i][lumIndex + 1]] | CbCr;
                    current[off + width] = clip[512 + block[i][lumIndex + 8]] | CbCr;
                    current[off + width + 1] = clip[512 + block[i][lumIndex + 8 + 1]] | CbCr;
                    off += 2;
                    index++;
                    lumIndex += 2;
                }
                off += offsetInc;
                lumIndex += lumIndexInc;
                index += chromIndexInc;
            }
        }
    }

    /**
     *   Motion Compensation
     */
    private void motionCompensation( int mbAddress, int[][] block )
    {
        int width = mbWidth << 4;

        int offset[] = new int[4];

        offset[0] = ((mbAddress % mbWidth) + width * (mbAddress / mbWidth)) << 4;
        offset[1] = offset[0] + 8;
        offset[2] = offset[0] + (width << 3);
        offset[3] = offset[2] + 8;
        int offsetInc = width + width -8;

        int chromIndex[] = new int[4];
        chromIndex[0] = 0;
        chromIndex[1] = chromIndex[0] + 4;
        chromIndex[2] = 8 * 4;
        chromIndex[3] = chromIndex[2] + 4;
        int chromIndexInc = 8 - 4;

        int lumIndexInc = 8 + 8 - 8;

        int i,j,k;
        int off, index, lumIndex;

        int Cb, Cr;
        int CbCr;
        int orgPixel;

        for (i=0;i<4;i++) {
            off = offset[i];
            index = chromIndex[i];
            lumIndex = 0;

            for (j=0;j<4;j++) {
                for (k=0;k<4;k++) {
                    orgPixel = current[off];
                    Cb = (orgPixel >> 8) & 0xff;
                    Cr = (orgPixel >> 16) & 0xff;
                    CbCr = (clip[512 + block[4][index] + Cb] << 8) | (clip[512 + block[5][index] + Cr] << 16);
                    current[off] = clip[512 + (orgPixel & 0xff) + block[i][lumIndex]] | CbCr;

                    orgPixel = current[off + 1];
                    current[off + 1] = clip[512 + (orgPixel & 0xff) + block[i][lumIndex + 1]] | CbCr;

                    orgPixel = current[off + width];
                    current[off + width] = clip[512 + (orgPixel & 0xff) + block[i][lumIndex + 8]] | CbCr;

                    orgPixel = current[off + width + 1];
                    current[off + width + 1] = clip[512 + (orgPixel & 0xff) + block[i][lumIndex + 8 + 1]] | CbCr;

                    off += 2;
                    index++;
                    lumIndex += 2;
                }
                off += offsetInc;
                lumIndex += lumIndexInc;
                index += chromIndexInc;
            }
        }
    }


    /**
     *   Set unused information
     */
    private void setTemporalReference(int value)
    {
        temporalReference = value;
    }

    private void setVBVDelay( int value )
    {
        vbvDelay = value;
    }


}
