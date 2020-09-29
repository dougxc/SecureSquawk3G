/*
 *   @(#) MPEG-I Vide Decoder 0.9 Yu Tianli (yu-tianli@usa.net)
 *
 *   MacroBlock.java   1999-2-24 09:11 am
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
 *  The Macro Block
 */
class MacroBlock
{
    /**
     *   Macroblock type bit fields
     */
    public final static int INTRA    = 0x01;
    public final static int PATTERN  = 0x02;
    public final static int BACKWARD = 0x04;
    public final static int FORWARD  = 0x08;
    public final static int QUANT    = 0x10;

    /**
     *   Intra Quantize Matrix
     */
    private int intraQuantMatrix[];

    /**
     *   Inter Quantize Matrix
     */
    private int interQuantMatrix[];

    /**
     *   Quantizer scale
     */
    private int quantizerScale;

    /**
     * Default quantization matrix for intra coded macro blocks
     */
    private final static int defaultIntraQuantMatrix[] = {
        8, 16, 16, 19, 16, 19, 22, 22,
        22, 22, 22, 22, 26, 24, 26, 27,
        27, 27, 26, 26, 26, 26, 27, 27,
        27, 29, 29, 29, 34, 34, 34, 29,
        29, 29, 27, 27, 29, 29, 32, 32,
        34, 34, 37, 38, 37, 35, 35, 34,
        35, 38, 38, 40, 40, 40, 48, 48,
        46, 46, 56, 56, 58, 69, 69, 83
    };

    /**
     *   Picture type
     */
    private int pictureType;

    /**
     *   Motion Vector info
     */
    private int fullPelForwardVector;
    private int forwardFCode;
    private int fullPelBackwardVector;
    private int backwardFCode;

    /**
     *   Forward and backward Motion Vector
     */
    MotionVector forwardVector, backwardVector;

    /**
     *   Macro Block type
     */
    private int mbType;

    /**
     *   Coded Block Pattern
     */
    private int codedBlockPattern;

    /**
     *   Past DCT dc predict value
     */
    private int dctDCYPast = 128*8;
    private int dctDCCrPast = 128*8;
    private int dctDCCbPast = 128*8;

    /**
     *   6 block data
     */
    private int block[][];

    /**
     * Mapping for zig-zag scan ordering
     */
    private final static int zigzag[] = {
        0,  1,  8, 16,  9,  2,  3, 10,
        17, 24, 32, 25, 18, 11,  4,  5,
        12, 19, 26, 33, 40, 48, 41, 34,
        27, 20, 13,  6,  7, 14, 21, 28,
        35, 42, 49, 56, 57, 50, 43, 36,
        29, 22, 15, 23, 30, 37, 44, 51,
        58, 59, 52, 45, 38, 31, 39, 46,
        53, 60, 61, 54, 47, 55, 62, 63
    };

    /**
     *   Unreachable Table Code
     */
    private static final int UR = 0;

    /**
     *   I-frame Macro Block type look up table
     */
    private static final int IMBTable [][] = {

        // xx
        { UR , UR }, { 17 , 2  }, { 1  , 1  }, { 1  , 1  }
    };

    /**
     *   P-frame Macro Block type look up table
     */
    private static final int PMBTable [][] = {

        // xxx. ..
        { UR , UR }, { 8  , 3  }, { 2  , 2  }, { 2  , 2  },
        { 10 , 1  }, { 10 , 1  }, { 10 , 1  }, { 10 , 1  },

        // 000x xx
        { UR , UR }, { 17 , 6  }, { 18 , 5  }, { 18 , 5  },
        { 26 , 5  }, { 26 , 5  }, { 1  , 5  }, { 1  , 5  }
    };

    /**
     *   P-frame Macro Block type look up table
     */
    private static final int BMBTable [][] = {

        // xxxx ..
        { UR , UR }, { UR , UR }, { 8  , 4  }, { 10 , 4  },
        { 4  , 3  }, { 4  , 3  }, { 6  , 3  }, { 6  , 3  },
        { 12 , 2  }, { 12 , 2  }, { 12 , 2  }, { 12 , 2  },
        { 14 , 2  }, { 14 , 2  }, { 14 , 2  }, { 14 , 2  },

        // 000x xx
        { UR , UR }, { 17 , 6  }, { 22 , 6  }, { 26 , 6  },
        { 30 , 5  }, { 30 , 5  }, { 1  , 5  }, { 1  , 5  }
    };

    /**
     *   Get the I-frame Macro Block type
     */
    private void getIMBType(BufferedBitStream stream) throws InterruptedException
    {
        int data[] = IMBTable[stream.showBits(2)];
        stream.flushBits(data[1]);
        mbType = data[0];
    }

    /**
     *   Get the I-frame Macro Block type
     */
    private void getPMBType(BufferedBitStream stream) throws InterruptedException
    {
        int code = stream.showBits(6);
        int data[];

        if (code >= 8) data = PMBTable[ code >>> 3];
        else data = PMBTable [ code + 8 ];

        stream.flushBits(data[1]);
        mbType = data[0];
    }

    /**
     *   Get the B-frame Macro Block type
     */
    private void getBMBType(BufferedBitStream stream) throws InterruptedException
    {
        int code = stream.showBits(6);
        int data[];

        if (code >= 8) data = BMBTable[ code >>> 2 ];
        else data = BMBTable [ code + 16 ];

        stream.flushBits(data[1]);
        mbType = data[0];
    }

    /**
     *   Coded Block Pattern look up table
     */
    private static final int CBPCode [][] = {

        // xxxx x... .

        { UR , UR }, { UR , UR }, { UR , UR }, { UR , UR },
        { UR , UR }, { UR , UR }, { UR , UR }, { UR , UR },
        { 62 , 5  }, { 2  , 5  }, { 61 , 5  }, { 1  , 5  },
        { 56 , 5  }, { 52 , 5  }, { 44 , 5  }, { 28 , 5  },

        { 40 , 5  }, { 20 , 5  }, { 48 , 5  }, { 12 , 5  },
        { 32 , 4  }, { 32 , 4  }, { 16 , 4  }, { 16 , 4  },
        { 8  , 4  }, { 8  , 4  }, { 4  , 4  }, { 4  , 4  },
        { 60 , 3  }, { 60 , 3  }, { 60 , 3  }, { 60 , 3  },

        // 00xx xxxx x

        { UR , UR }, { UR , UR }, { 39 , 9  }, { 27 , 9  },
        { 59 , 9  }, { 55 , 9  }, { 47 , 9  }, { 31 , 9  },
        { 58 , 8  }, { 58 , 8  }, { 54 , 8  }, { 54 , 8  },
        { 46 , 8  }, { 46 , 8  }, { 30 , 8  }, { 30 , 8  },

        { 57 , 8  }, { 57 , 8  }, { 53 , 8  }, { 53 , 8  },
        { 45 , 8  }, { 45 , 8  }, { 29 , 8  }, { 29 , 8  },
        { 38 , 8  }, { 38 , 8  }, { 26 , 8  }, { 26 , 8  },
        { 37 , 8  }, { 37 , 8  }, { 25 , 8  }, { 25 , 8  },

        { 43 , 8  }, { 43 , 8  }, { 23 , 8  }, { 23 , 8  },
        { 51 , 8  }, { 51 , 8  }, { 15 , 8  }, { 15 , 8  },
        { 42 , 8  }, { 42 , 8  }, { 22 , 8  }, { 22 , 8  },
        { 50 , 8  }, { 50 , 8  }, { 14 , 8  }, { 14 , 8  },

        { 41 , 8  }, { 41 , 8  }, { 21 , 8  }, { 21 , 8  },
        { 49 , 8  }, { 49 , 8  }, { 13 , 8  }, { 13 , 8  },
        { 35 , 8  }, { 35 , 8  }, { 19 , 8  }, { 19 , 8  },
        { 11 , 8  }, { 11 , 8  }, { 7  , 8  }, { 7  , 8  },

        { 34 , 7  }, { 34 , 7  }, { 34 , 7  }, { 34 , 7  },
        { 18 , 7  }, { 18 , 7  }, { 18 , 7  }, { 18 , 7  },
        { 10 , 7  }, { 10 , 7  }, { 10 , 7  }, { 10 , 7  },
        { 6  , 7  }, { 6  , 7  }, { 6  , 7  }, { 6  , 7  },

        { 33 , 7  }, { 33 , 7  }, { 33 , 7  }, { 33 , 7  },
        { 17 , 7  }, { 17 , 7  }, { 17 , 7  }, { 17 , 7  },
        { 9  , 7  }, { 9  , 7  }, { 9  , 7  }, { 9  , 7  },
        { 5  , 7  }, { 5  , 7  }, { 5  , 7  }, { 5  , 7  },

        { 63 , 6  }, { 63 , 6  }, { 63 , 6  }, { 63 , 6  },
        { 63 , 6  }, { 63 , 6  }, { 63 , 6  }, { 63 , 6  },
        { 3  , 6  }, { 3  , 6  }, { 3  , 6  }, { 3  , 6  },
        { 3  , 6  }, { 3  , 6  }, { 3  , 6  }, { 3  , 6  },

        { 36 , 6  }, { 36 , 6  }, { 36 , 6  }, { 36 , 6  },
        { 36 , 6  }, { 36 , 6  }, { 36 , 6  }, { 36 , 6  },
        { 24 , 6  }, { 24 , 6  }, { 24 , 6  }, { 24 , 6  },
        { 24 , 6  }, { 24 , 6  }, { 24 , 6  }, { 24 , 6  },
    };

    /**
     *   Get the Coded Block Pattern
     */
    private void getMBPattern (BufferedBitStream stream) throws InterruptedException
    {
        int data[];
        int code = stream.showBits(9);

        if (code >= 128)
            data = CBPCode[code >>> 4];
        else data = CBPCode[(code) + 32];

        stream.flushBits(data[1]);
        codedBlockPattern = data[0];
    }

    /**
     *   Reset the coded block pattern
     */
    private void resetMBPattern()
    {
        codedBlockPattern = 0;
    }

    /**
     *   dct_dc_size_luminance look up table
     */
    private static final int DCSizeLuminanceTable[][] = {

        // xxxx x..
        { 1  , 2  }, { 1  , 2  }, { 1  , 2  }, { 1  , 2  },
        { 1  , 2  }, { 1  , 2  }, { 1  , 2  }, { 1  , 2  },
        { 2  , 2  }, { 2  , 2  }, { 2  , 2  }, { 2  , 2  },
        { 2  , 2  }, { 2  , 2  }, { 2  , 2  }, { 2  , 2  },

        { 0  , 3  }, { 0  , 3  }, { 0  , 3  }, { 0  , 3  },
        { 3  , 3  }, { 3  , 3  }, { 3  , 3  }, { 3  , 3  },
        { 4  , 3  }, { 4  , 3  }, { 4  , 3  }, { 4  , 3  },
        { 5  , 4  }, { 5  , 4  }, { 6  , 5  }, { 7  , 6  },

        // 1111 1xx
        { 7  , 6  }, { 7  , 6  }, { 8  , 7  }, { UR , UR },
    };

    /**
     *   get the dct_dc_luminance
     */
    private int getDCLuminance(BufferedBitStream stream) throws InterruptedException
    {
        int code = stream.showBits(7);
        int data[];

        if (code <= 123)
            data = DCSizeLuminanceTable[code >>> 2];
        else
            data = DCSizeLuminanceTable[ (code & 0x03) + 32 ];

        stream.flushBits(data[1]);

        if (data[0] == 0) return 0;

        int dctDCDifferential = stream.getBits(data[0]);

        if ((dctDCDifferential & (1 << (data[0]-1)))!=0)
            return dctDCDifferential;
        else return ((-1) << (data[0]))|(dctDCDifferential + 1);
    }

    /**
     *   dct_dc_size_chrominance look up table
     */
    private static final int DCSizeChrominanceTable[][] = {

        // xxxx x...
        { 0  , 2  }, { 0  , 2  }, { 0  , 2  }, { 0  , 2  },
        { 0  , 2  }, { 0  , 2  }, { 0  , 2  }, { 0  , 2  },
        { 1  , 2  }, { 1  , 2  }, { 1  , 2  }, { 1  , 2  },
        { 1  , 2  }, { 1  , 2  }, { 1  , 2  }, { 1  , 2  },

        { 2  , 2  }, { 2  , 2  }, { 2  , 2  }, { 2  , 2  },
        { 2  , 2  }, { 2  , 2  }, { 2  , 2  }, { 2  , 2  },
        { 3  , 3  }, { 3  , 3  }, { 3  , 3  }, { 3  , 3  },
        { 4  , 4  }, { 4  , 4  }, { 5  , 5  }, { UR , UR },

        // 1111 1xxx
        { 6  , 6  }, { 6  , 6  }, { 6  , 6  }, { 6  , 6  },
        { 7  , 7  }, { 7  , 7  }, { 8  , 8  }, { UR , UR }
    };

    /**
     *   Get the dct_dc_size_chrominance
     */
    private int getDCChrominance(BufferedBitStream stream) throws InterruptedException
    {
        int code = stream.showBits(8);
        int data[];

        if (code <= 247)
            data = DCSizeChrominanceTable[code >>> 3];
        else
            data = DCSizeChrominanceTable[(code & 0x07) + 32];

        // data[0] is dc_size_luminance
        // data[1] is bits of dc_size_luminance

        stream.flushBits(data[1]);

        if (data[0] == 0) return 0;

        int dctDCDifferential = stream.getBits(data[0]);

        if ((dctDCDifferential & (1 << (data[0]-1))) != 0)
            return dctDCDifferential;
        else return ((-1) << (data[0]))|(dctDCDifferential + 1);
    }

    /**
     *   End of Block
     */
    private static final int EOB = -1;

    /**
     *   Macro Block Escape
     */
    private static final int ESC = -2;

    /**
     *   dct_first and dct_next coefficient look up table ( a little bit long )
     */
    private static final int DCTCoefficientTable[][] = {

        // { run , value , length }

        // xxxx xxxx x... .... .
        { UR , UR , UR }, { UR , UR , UR }, { UR , UR , UR }, { UR , UR , UR },
        { UR , UR , UR }, { UR , UR , UR }, { UR , UR , UR }, { UR , UR , UR },
        { ESC, ESC, 6  }, { ESC, ESC, 6  }, { ESC, ESC, 6  }, { ESC, ESC, 6  },
        { ESC, ESC, 6  }, { ESC, ESC, 6  }, { ESC, ESC, 6  }, { ESC, ESC, 6  },

        { 2  , 2  , 8  }, { 2  , 2  , 8  }, { 2  , -2 , 8  }, { 2  , -2 , 8  },
        { 9  , 1  , 8  }, { 9  , 1  , 8  }, { 9  , -1 , 8  }, { 9  , -1 , 8  },
        { 0  , 4  , 8  }, { 0  , 4  , 8  }, { 0  , -4 , 8  }, { 0  , -4 , 8  },
        { 8  , 1  , 8  }, { 8  , 1  , 8  }, { 8  , -1 , 8  }, { 8  , -1 , 8  },

            // 8
        { 7  , 1  , 7  }, { 7  , 1  , 7  }, { 7  , 1  , 7  }, { 7  , 1  , 7  },
        { 7  , -1 , 7  }, { 7  , -1 , 7  }, { 7  , -1 , 7  }, { 7  , -1 , 7  },
        { 6  , 1  , 7  }, { 6  , 1  , 7  }, { 6  , 1  , 7  }, { 6  , 1  , 7  },
        { 6  , -1 , 7  }, { 6  , -1 , 7  }, { 6  , -1 , 7  }, { 6  , -1 , 7  },

        { 1  , 2  , 7  }, { 1  , 2  , 7  }, { 1  , 2  , 7  }, { 1  , 2  , 7  },
        { 1  , -2 , 7  }, { 1  , -2 , 7  }, { 1  , -2 , 7  }, { 1  , -2 , 7  },
        { 5  , 1  , 7  }, { 5  , 1  , 7  }, { 5  , 1  , 7  }, { 5  , 1  , 7  },
        { 5  , -1 , 7  }, { 5  , -1 , 7  }, { 5  , -1 , 7  }, { 5  , -1 , 7  },

            // 16
        { 13 , 1  , 9  }, { 13 , -1 , 9  }, { 0  , 6  , 9  }, { 0  , -6 , 9  },
        { 12 , 1  , 9  }, { 12 , -1 , 9  }, { 11 , 1  , 9  }, { 11 , -1 , 9  },
        { 3  , 1  , 9  }, { 3  , -1 , 9  }, { 1  , 3  , 9  }, { 1  , -3 , 9  },
        { 0  , 5  , 9  }, { 0  , -5 , 9  }, { 10 , 1  , 9  }, { 10 , -1 , 9  },

        { 0  , 3  , 6  }, { 0  , 3  , 6  }, { 0  , 3  , 6  }, { 0  , 3  , 6  },
        { 0  , 3  , 6  }, { 0  , 3  , 6  }, { 0  , 3  , 6  }, { 0  , 3  , 6  },
        { 0  , -3 , 6  }, { 0  , -3 , 6  }, { 0  , -3 , 6  }, { 0  , -3 , 6  },
        { 0  , -3 , 6  }, { 0  , -3 , 6  }, { 0  , -3 , 6  }, { 0  , -3 , 6  },

            // 24
        { 4  , 1  , 6  }, { 4  , 1  , 6  }, { 4  , 1  , 6  }, { 4  , 1  , 6  },
        { 4  , 1  , 6  }, { 4  , 1  , 6  }, { 4  , 1  , 6  }, { 4  , 1  , 6  },
        { 4  , -1 , 6  }, { 4  , -1 , 6  }, { 4  , -1 , 6  }, { 4  , -1 , 6  },
        { 4  , -1 , 6  }, { 4  , -1 , 6  }, { 4  , -1 , 6  }, { 4  , -1 , 6  },

        { 3  , 1  , 6  }, { 3  , 1  , 6  }, { 3  , 1  , 6  }, { 3  , 1  , 6  },
        { 3  , 1  , 6  }, { 3  , 1  , 6  }, { 3  , 1  , 6  }, { 3  , 1  , 6  },
        { 3  , -1 , 6  }, { 3  , -1 , 6  }, { 3  , -1 , 6  }, { 3  , -1 , 6  },
        { 3  , -1 , 6  }, { 3  , -1 , 6  }, { 3  , -1 , 6  }, { 3  , -1 , 6  },

            // 32
        { 0  , 2  , 5  }, { 0  , 2  , 5  }, { 0  , 2  , 5  }, { 0  , 2  , 5  },
        { 0  , 2  , 5  }, { 0  , 2  , 5  }, { 0  , 2  , 5  }, { 0  , 2  , 5  },
        { 0  , 2  , 5  }, { 0  , 2  , 5  }, { 0  , 2  , 5  }, { 0  , 2  , 5  },
        { 0  , 2  , 5  }, { 0  , 2  , 5  }, { 0  , 2  , 5  }, { 0  , 2  , 5  },

        { 0  , -2 , 5  }, { 0  , -2 , 5  }, { 0  , -2 , 5  }, { 0  , -2 , 5  },
        { 0  , -2 , 5  }, { 0  , -2 , 5  }, { 0  , -2 , 5  }, { 0  , -2 , 5  },
        { 0  , -2 , 5  }, { 0  , -2 , 5  }, { 0  , -2 , 5  }, { 0  , -2 , 5  },
        { 0  , -2 , 5  }, { 0  , -2 , 5  }, { 0  , -2 , 5  }, { 0  , -2 , 5  },

            // 40
        { 2  , 1  , 5  }, { 2  , 1  , 5  }, { 2  , 1  , 5  }, { 2  , 1  , 5  },
        { 2  , 1  , 5  }, { 2  , 1  , 5  }, { 2  , 1  , 5  }, { 2  , 1  , 5  },
        { 2  , 1  , 5  }, { 2  , 1  , 5  }, { 2  , 1  , 5  }, { 2  , 1  , 5  },
        { 2  , 1  , 5  }, { 2  , 1  , 5  }, { 2  , 1  , 5  }, { 2  , 1  , 5  },

        { 2  , -1 , 5  }, { 2  , -1 , 5  }, { 2  , -1 , 5  }, { 2  , -1 , 5  },
        { 2  , -1 , 5  }, { 2  , -1 , 5  }, { 2  , -1 , 5  }, { 2  , -1 , 5  },
        { 2  , -1 , 5  }, { 2  , -1 , 5  }, { 2  , -1 , 5  }, { 2  , -1 , 5  },
        { 2  , -1 , 5  }, { 2  , -1 , 5  }, { 2  , -1 , 5  }, { 2  , -1 , 5  },

            // 48
        { 1  , 1  , 4  }, { 1  , 1  , 4  }, { 1  , 1  , 4  }, { 1  , 1  , 4  },
        { 1  , 1  , 4  }, { 1  , 1  , 4  }, { 1  , 1  , 4  }, { 1  , 1  , 4  },
        { 1  , 1  , 4  }, { 1  , 1  , 4  }, { 1  , 1  , 4  }, { 1  , 1  , 4  },
        { 1  , 1  , 4  }, { 1  , 1  , 4  }, { 1  , 1  , 4  }, { 1  , 1  , 4  },

        { 1  , 1  , 4  }, { 1  , 1  , 4  }, { 1  , 1  , 4  }, { 1  , 1  , 4  },
        { 1  , 1  , 4  }, { 1  , 1  , 4  }, { 1  , 1  , 4  }, { 1  , 1  , 4  },
        { 1  , 1  , 4  }, { 1  , 1  , 4  }, { 1  , 1  , 4  }, { 1  , 1  , 4  },
        { 1  , 1  , 4  }, { 1  , 1  , 4  }, { 1  , 1  , 4  }, { 1  , 1  , 4  },

            // 56
        { 1  , -1 , 4  }, { 1  , -1 , 4  }, { 1  , -1 , 4  }, { 1  , -1 , 4  },
        { 1  , -1 , 4  }, { 1  , -1 , 4  }, { 1  , -1 , 4  }, { 1  , -1 , 4  },
        { 1  , -1 , 4  }, { 1  , -1 , 4  }, { 1  , -1 , 4  }, { 1  , -1 , 4  },
        { 1  , -1 , 4  }, { 1  , -1 , 4  }, { 1  , -1 , 4  }, { 1  , -1 , 4  },

        { 1  , -1 , 4  }, { 1  , -1 , 4  }, { 1  , -1 , 4  }, { 1  , -1 , 4  },
        { 1  , -1 , 4  }, { 1  , -1 , 4  }, { 1  , -1 , 4  }, { 1  , -1 , 4  },
        { 1  , -1 , 4  }, { 1  , -1 , 4  }, { 1  , -1 , 4  }, { 1  , -1 , 4  },
        { 1  , -1 , 4  }, { 1  , -1 , 4  }, { 1  , -1 , 4  }, { 1  , -1 , 4  },

            // 64
        { EOB, EOB, 2  }, { EOB, EOB, 2  }, { EOB, EOB, 2  }, { EOB, EOB, 2  },
        { EOB, EOB, 2  }, { EOB, EOB, 2  }, { EOB, EOB, 2  }, { EOB, EOB, 2  },
        { EOB, EOB, 2  }, { EOB, EOB, 2  }, { EOB, EOB, 2  }, { EOB, EOB, 2  },
        { EOB, EOB, 2  }, { EOB, EOB, 2  }, { EOB, EOB, 2  }, { EOB, EOB, 2  },

        { EOB, EOB, 2  }, { EOB, EOB, 2  }, { EOB, EOB, 2  }, { EOB, EOB, 2  },
        { EOB, EOB, 2  }, { EOB, EOB, 2  }, { EOB, EOB, 2  }, { EOB, EOB, 2  },
        { EOB, EOB, 2  }, { EOB, EOB, 2  }, { EOB, EOB, 2  }, { EOB, EOB, 2  },
        { EOB, EOB, 2  }, { EOB, EOB, 2  }, { EOB, EOB, 2  }, { EOB, EOB, 2  },

            // 72
        { EOB, EOB, 2  }, { EOB, EOB, 2  }, { EOB, EOB, 2  }, { EOB, EOB, 2  },
        { EOB, EOB, 2  }, { EOB, EOB, 2  }, { EOB, EOB, 2  }, { EOB, EOB, 2  },
        { EOB, EOB, 2  }, { EOB, EOB, 2  }, { EOB, EOB, 2  }, { EOB, EOB, 2  },
        { EOB, EOB, 2  }, { EOB, EOB, 2  }, { EOB, EOB, 2  }, { EOB, EOB, 2  },

        { EOB, EOB, 2  }, { EOB, EOB, 2  }, { EOB, EOB, 2  }, { EOB, EOB, 2  },
        { EOB, EOB, 2  }, { EOB, EOB, 2  }, { EOB, EOB, 2  }, { EOB, EOB, 2  },
        { EOB, EOB, 2  }, { EOB, EOB, 2  }, { EOB, EOB, 2  }, { EOB, EOB, 2  },
        { EOB, EOB, 2  }, { EOB, EOB, 2  }, { EOB, EOB, 2  }, { EOB, EOB, 2  },

            // 80
        { EOB, EOB, 2  }, { EOB, EOB, 2  }, { EOB, EOB, 2  }, { EOB, EOB, 2  },
        { EOB, EOB, 2  }, { EOB, EOB, 2  }, { EOB, EOB, 2  }, { EOB, EOB, 2  },
        { EOB, EOB, 2  }, { EOB, EOB, 2  }, { EOB, EOB, 2  }, { EOB, EOB, 2  },
        { EOB, EOB, 2  }, { EOB, EOB, 2  }, { EOB, EOB, 2  }, { EOB, EOB, 2  },

        { EOB, EOB, 2  }, { EOB, EOB, 2  }, { EOB, EOB, 2  }, { EOB, EOB, 2  },
        { EOB, EOB, 2  }, { EOB, EOB, 2  }, { EOB, EOB, 2  }, { EOB, EOB, 2  },
        { EOB, EOB, 2  }, { EOB, EOB, 2  }, { EOB, EOB, 2  }, { EOB, EOB, 2  },
        { EOB, EOB, 2  }, { EOB, EOB, 2  }, { EOB, EOB, 2  }, { EOB, EOB, 2  },

            // 88
        { EOB, EOB, 2  }, { EOB, EOB, 2  }, { EOB, EOB, 2  }, { EOB, EOB, 2  },
        { EOB, EOB, 2  }, { EOB, EOB, 2  }, { EOB, EOB, 2  }, { EOB, EOB, 2  },
        { EOB, EOB, 2  }, { EOB, EOB, 2  }, { EOB, EOB, 2  }, { EOB, EOB, 2  },
        { EOB, EOB, 2  }, { EOB, EOB, 2  }, { EOB, EOB, 2  }, { EOB, EOB, 2  },

        { EOB, EOB, 2  }, { EOB, EOB, 2  }, { EOB, EOB, 2  }, { EOB, EOB, 2  },
        { EOB, EOB, 2  }, { EOB, EOB, 2  }, { EOB, EOB, 2  }, { EOB, EOB, 2  },
        { EOB, EOB, 2  }, { EOB, EOB, 2  }, { EOB, EOB, 2  }, { EOB, EOB, 2  },
        { EOB, EOB, 2  }, { EOB, EOB, 2  }, { EOB, EOB, 2  }, { EOB, EOB, 2  },

            // 96
        { 0  , 1  , 3  }, { 0  , 1  , 3  }, { 0  , 1  , 3  }, { 0  , 1  , 3  },
        { 0  , 1  , 3  }, { 0  , 1  , 3  }, { 0  , 1  , 3  }, { 0  , 1  , 3  },
        { 0  , 1  , 3  }, { 0  , 1  , 3  }, { 0  , 1  , 3  }, { 0  , 1  , 3  },
        { 0  , 1  , 3  }, { 0  , 1  , 3  }, { 0  , 1  , 3  }, { 0  , 1  , 3  },

        { 0  , 1  , 3  }, { 0  , 1  , 3  }, { 0  , 1  , 3  }, { 0  , 1  , 3  },
        { 0  , 1  , 3  }, { 0  , 1  , 3  }, { 0  , 1  , 3  }, { 0  , 1  , 3  },
        { 0  , 1  , 3  }, { 0  , 1  , 3  }, { 0  , 1  , 3  }, { 0  , 1  , 3  },
        { 0  , 1  , 3  }, { 0  , 1  , 3  }, { 0  , 1  , 3  }, { 0  , 1  , 3  },

            // 104
        { 0  , 1  , 3  }, { 0  , 1  , 3  }, { 0  , 1  , 3  }, { 0  , 1  , 3  },
        { 0  , 1  , 3  }, { 0  , 1  , 3  }, { 0  , 1  , 3  }, { 0  , 1  , 3  },
        { 0  , 1  , 3  }, { 0  , 1  , 3  }, { 0  , 1  , 3  }, { 0  , 1  , 3  },
        { 0  , 1  , 3  }, { 0  , 1  , 3  }, { 0  , 1  , 3  }, { 0  , 1  , 3  },

        { 0  , 1  , 3  }, { 0  , 1  , 3  }, { 0  , 1  , 3  }, { 0  , 1  , 3  },
        { 0  , 1  , 3  }, { 0  , 1  , 3  }, { 0  , 1  , 3  }, { 0  , 1  , 3  },
        { 0  , 1  , 3  }, { 0  , 1  , 3  }, { 0  , 1  , 3  }, { 0  , 1  , 3  },
        { 0  , 1  , 3  }, { 0  , 1  , 3  }, { 0  , 1  , 3  }, { 0  , 1  , 3  },

            // 112
        { 0  , -1 , 3  }, { 0  , -1 , 3  }, { 0  , -1 , 3  }, { 0  , -1 , 3  },
        { 0  , -1 , 3  }, { 0  , -1 , 3  }, { 0  , -1 , 3  }, { 0  , -1 , 3  },
        { 0  , -1 , 3  }, { 0  , -1 , 3  }, { 0  , -1 , 3  }, { 0  , -1 , 3  },
        { 0  , -1 , 3  }, { 0  , -1 , 3  }, { 0  , -1 , 3  }, { 0  , -1 , 3  },

        { 0  , -1 , 3  }, { 0  , -1 , 3  }, { 0  , -1 , 3  }, { 0  , -1 , 3  },
        { 0  , -1 , 3  }, { 0  , -1 , 3  }, { 0  , -1 , 3  }, { 0  , -1 , 3  },
        { 0  , -1 , 3  }, { 0  , -1 , 3  }, { 0  , -1 , 3  }, { 0  , -1 , 3  },
        { 0  , -1 , 3  }, { 0  , -1 , 3  }, { 0  , -1 , 3  }, { 0  , -1 , 3  },

            // 120
        { 0  , -1 , 3  }, { 0  , -1 , 3  }, { 0  , -1 , 3  }, { 0  , -1 , 3  },
        { 0  , -1 , 3  }, { 0  , -1 , 3  }, { 0  , -1 , 3  }, { 0  , -1 , 3  },
        { 0  , -1 , 3  }, { 0  , -1 , 3  }, { 0  , -1 , 3  }, { 0  , -1 , 3  },
        { 0  , -1 , 3  }, { 0  , -1 , 3  }, { 0  , -1 , 3  }, { 0  , -1 , 3  },

        { 0  , -1 , 3  }, { 0  , -1 , 3  }, { 0  , -1 , 3  }, { 0  , -1 , 3  },
        { 0  , -1 , 3  }, { 0  , -1 , 3  }, { 0  , -1 , 3  }, { 0  , -1 , 3  },
        { 0  , -1 , 3  }, { 0  , -1 , 3  }, { 0  , -1 , 3  }, { 0  , -1 , 3  },
        { 0  , -1 , 3  }, { 0  , -1 , 3  }, { 0  , -1 , 3  }, { 0  , -1 , 3  },

            // 128

        // 0000 00xx xxxx xx.. .

        { UR , UR , UR }, { UR , UR , UR }, { UR , UR , UR }, { UR , UR , UR },
        { UR , UR , UR }, { UR , UR , UR }, { UR , UR , UR }, { UR , UR , UR },
        { UR , UR , UR }, { UR , UR , UR }, { UR , UR , UR }, { UR , UR , UR },
        { UR , UR , UR }, { UR , UR , UR }, { UR , UR , UR }, { UR , UR , UR },

        { UR , UR , UR }, { UR , UR , UR }, { UR , UR , UR }, { UR , UR , UR },
        { UR , UR , UR }, { UR , UR , UR }, { UR , UR , UR }, { UR , UR , UR },
        { UR , UR , UR }, { UR , UR , UR }, { UR , UR , UR }, { UR , UR , UR },
        { UR , UR , UR }, { UR , UR , UR }, { UR , UR , UR }, { UR , UR , UR },

            // 136
        { 10 , 2  , 14 }, { 10 , -2 , 14 }, { 9  , 2  , 14 }, { 9  , -2 , 14 },
        { 5  , 3  , 14 }, { 5  , -3 , 14 }, { 3  , 4  , 14 }, { 3  , -4 , 14 },
        { 2  , 5  , 14 }, { 2  , -5 , 14 }, { 1  , 7  , 14 }, { 1  , -7 , 14 },
        { 1  , 6  , 14 }, { 1  , -6 , 14 }, { 0  , 15 , 14 }, { 0  , -15, 14 },

        { 0  , 14 , 14 }, { 0  , -14, 14 }, { 0  , 13 , 14 }, { 0  , -13, 14 },
        { 0  , 12 , 14 }, { 0  , -12, 14 }, { 26 , 1  , 14 }, { 26 , -1 , 14 },
        { 25 , 1  , 14 }, { 25 , -1 , 14 }, { 24 , 1  , 14 }, { 24 , -1 , 14 },
        { 23 , 1  , 14 }, { 23 , -1 , 14 }, { 22 , 1  , 14 }, { 22 , -1 , 14 },

            // 144
        { 0  , 11 , 13 }, { 0  , 11 , 13 }, { 0  , -11, 13 }, { 0  , -11, 13 },
        { 8  , 2  , 13 }, { 8  , 2  , 13 }, { 8  , -2 , 13 }, { 8  , -2 , 13 },
        { 4  , 3  , 13 }, { 4  , 3  , 13 }, { 4  , -3 , 13 }, { 4  , -3 , 13 },
        { 0  , 10 , 13 }, { 0  , 10 , 13 }, { 0  , -10, 13 }, { 0  , -10, 13 },

        { 2  , 4  , 13 }, { 2  , 4  , 13 }, { 2  , -4 , 13 }, { 2  , -4 , 13 },
        { 7  , 2  , 13 }, { 7  , 2  , 13 }, { 7  , -2 , 13 }, { 7  , -2 , 13 },
        { 21 , 1  , 13 }, { 21 , 1  , 13 }, { 21 , -1 , 13 }, { 21 , -1 , 13 },
        { 20 , 1  , 13 }, { 20 , 1  , 13 }, { 20 , -1 , 13 }, { 20 , -1 , 13 },

            // 152
        { 0  , 9  , 13 }, { 0  , 9  , 13 }, { 0  , -9 , 13 }, { 0  , -9 , 13 },
        { 19 , 1  , 13 }, { 19 , 1  , 13 }, { 19 , -1 , 13 }, { 19 , -1 , 13 },
        { 18 , 1  , 13 }, { 18 , 1  , 13 }, { 18 , -1 , 13 }, { 18 , -1 , 13 },
        { 1  , 5  , 13 }, { 1  , 5  , 13 }, { 1  , -5 , 13 }, { 1  , -5 , 13 },

        { 3  , 3  , 13 }, { 3  , 3  , 13 }, { 3  , -3 , 13 }, { 3  , -3 , 13 },
        { 0  , 8  , 13 }, { 0  , 8  , 13 }, { 0  , -8 , 13 }, { 0  , -8 , 13 },
        { 6  , 2  , 13 }, { 6  , 2  , 13 }, { 6  , -2 , 13 }, { 6  , -2 , 13 },
        { 17 , 1  , 13 }, { 17 , 1  , 13 }, { 17 , -1 , 13 }, { 17 , -1 , 13 },

            // 160
        { 16 , 1  , 11 }, { 16 , 1  , 11 }, { 16 , 1  , 11 }, { 16 , 1  , 11 },
        { 16 , 1  , 11 }, { 16 , 1  , 11 }, { 16 , 1  , 11 }, { 16 , 1  , 11 },
        { 16 , -1 , 11 }, { 16 , -1 , 11 }, { 16 , -1 , 11 }, { 16 , -1 , 11 },
        { 16 , -1 , 11 }, { 16 , -1 , 11 }, { 16 , -1 , 11 }, { 16 , -1 , 11 },

        { 5  , 2  , 11 }, { 5  , 2  , 11 }, { 5  , 2  , 11 }, { 5  , 2  , 11 },
        { 5  , 2  , 11 }, { 5  , 2  , 11 }, { 5  , 2  , 11 }, { 5  , 2  , 11 },
        { 5  , -2 , 11 }, { 5  , -2 , 11 }, { 5  , -2 , 11 }, { 5  , -2 , 11 },
        { 5  , -2 , 11 }, { 5  , -2 , 11 }, { 5  , -2 , 11 }, { 5  , -2 , 11 },

            // 168
        { 0  , 7  , 11 }, { 0  , 7  , 11 }, { 0  , 7  , 11 }, { 0  , 7  , 11 },
        { 0  , 7  , 11 }, { 0  , 7  , 11 }, { 0  , 7  , 11 }, { 0  , 7  , 11 },
        { 0  , -7 , 11 }, { 0  , -7 , 11 }, { 0  , -7 , 11 }, { 0  , -7 , 11 },
        { 0  , -7 , 11 }, { 0  , -7 , 11 }, { 0  , -7 , 11 }, { 0  , -7 , 11 },

        { 2  , 3  , 11 }, { 2  , 3  , 11 }, { 2  , 3  , 11 }, { 2  , 3  , 11 },
        { 2  , 3  , 11 }, { 2  , 3  , 11 }, { 2  , 3  , 11 }, { 2  , 3  , 11 },
        { 2  , -3 , 11 }, { 2  , -3 , 11 }, { 2  , -3 , 11 }, { 2  , -3 , 11 },
        { 2  , -3 , 11 }, { 2  , -3 , 11 }, { 2  , -3 , 11 }, { 2  , -3 , 11 },

            // 176
        { 1  , 4  , 11 }, { 1  , 4  , 11 }, { 1  , 4  , 11 }, { 1  , 4  , 11 },
        { 1  , 4  , 11 }, { 1  , 4  , 11 }, { 1  , 4  , 11 }, { 1  , 4  , 11 },
        { 1  , -4 , 11 }, { 1  , -4 , 11 }, { 1  , -4 , 11 }, { 1  , -4 , 11 },
        { 1  , -4 , 11 }, { 1  , -4 , 11 }, { 1  , -4 , 11 }, { 1  , -4 , 11 },

        { 15 , 1  , 11 }, { 15 , 1  , 11 }, { 15 , 1  , 11 }, { 15 , 1  , 11 },
        { 15 , 1  , 11 }, { 15 , 1  , 11 }, { 15 , 1  , 11 }, { 15 , 1  , 11 },
        { 15 , -1 , 11 }, { 15 , -1 , 11 }, { 15 , -1 , 11 }, { 15 , -1 , 11 },
        { 15 , -1 , 11 }, { 15 , -1 , 11 }, { 15 , -1 , 11 }, { 15 , -1 , 11 },

            // 184
        { 14 , 1  , 11 }, { 14 , 1  , 11 }, { 14 , 1  , 11 }, { 14 , 1  , 11 },
        { 14 , 1  , 11 }, { 14 , 1  , 11 }, { 14 , 1  , 11 }, { 14 , 1  , 11 },
        { 14 , -1 , 11 }, { 14 , -1 , 11 }, { 14 , -1 , 11 }, { 14 , -1 , 11 },
        { 14 , -1 , 11 }, { 14 , -1 , 11 }, { 14 , -1 , 11 }, { 14 , -1 , 11 },

        { 4  , 2  , 11 }, { 4  , 2  , 11 }, { 4  , 2  , 11 }, { 4  , 2  , 11 },
        { 4  , 2  , 11 }, { 4  , 2  , 11 }, { 4  , 2  , 11 }, { 4  , 2  , 11 },
        { 4  , -2 , 11 }, { 4  , -2 , 11 }, { 4  , -2 , 11 }, { 4  , -2 , 11 },
        { 4  , -2 , 11 }, { 4  , -2 , 11 }, { 4  , -2 , 11 }, { 4  , -2 , 11 },

            // 192

        // 0000 0000 0xxx xxxx x

        { UR , UR , UR }, { UR , UR , UR }, { UR , UR , UR }, { UR , UR , UR },
        { UR , UR , UR }, { UR , UR , UR }, { UR , UR , UR }, { UR , UR , UR },
        { UR , UR , UR }, { UR , UR , UR }, { UR , UR , UR }, { UR , UR , UR },
        { UR , UR , UR }, { UR , UR , UR }, { UR , UR , UR }, { UR , UR , UR },

        { UR , UR , UR }, { UR , UR , UR }, { UR , UR , UR }, { UR , UR , UR },
        { UR , UR , UR }, { UR , UR , UR }, { UR , UR , UR }, { UR , UR , UR },
        { UR , UR , UR }, { UR , UR , UR }, { UR , UR , UR }, { UR , UR , UR },
        { UR , UR , UR }, { UR , UR , UR }, { UR , UR , UR }, { UR , UR , UR },

            // 200
        { 1  , 18 , 17 }, { 1  , -18, 17 }, { 1  , 17 , 17 }, { 1  , -17, 17 },
        { 1  , 16 , 17 }, { 1  , -16, 17 }, { 1  , 15 , 17 }, { 1  , -15, 17 },
        { 6  , 3  , 17 }, { 6  , -3 , 17 }, { 16 , 2  , 17 }, { 16 , -2 , 17 },
        { 15 , 2  , 17 }, { 15 , -2 , 17 }, { 14 , 2  , 17 }, { 14 , -2 , 17 },

        { 13 , 2  , 17 }, { 13 , -2 , 17 }, { 12 , 2  , 17 }, { 12 , -2 , 17 },
        { 11 , 2  , 17 }, { 11 , -2 , 17 }, { 31 , 1  , 17 }, { 31 , -1 , 17 },
        { 30 , 1  , 17 }, { 30 , -1 , 17 }, { 29 , 1  , 17 }, { 29 , -1 , 17 },
        { 28 , 1  , 17 }, { 28 , -1 , 17 }, { 27 , 1  , 17 }, { 27 , -1 , 17 },


            // 208
        { 0  , 40 , 16 }, { 0  , 40 , 16 }, { 0  , -40, 16 }, { 0  , -40, 16 },
        { 0  , 39 , 16 }, { 0  , 39 , 16 }, { 0  , -39, 16 }, { 0  , -39, 16 },
        { 0  , 38 , 16 }, { 0  , 38 , 16 }, { 0  , -38, 16 }, { 0  , -38, 16 },
        { 0  , 37 , 16 }, { 0  , 37 , 16 }, { 0  , -37, 16 }, { 0  , -37, 16 },

        { 0  , 36 , 16 }, { 0  , 36 , 16 }, { 0  , -36, 16 }, { 0  , -36, 16 },
        { 0  , 35 , 16 }, { 0  , 35 , 16 }, { 0  , -35, 16 }, { 0  , -35, 16 },
        { 0  , 34 , 16 }, { 0  , 34 , 16 }, { 0  , -34, 16 }, { 0  , -34, 16 },
        { 0  , 33 , 16 }, { 0  , 33 , 16 }, { 0  , -33, 16 }, { 0  , -33, 16 },

            // 216
        { 0  , 32 , 16 }, { 0  , 32 , 16 }, { 0  , -32, 16 }, { 0  , -32, 16 },
        { 1  , 14 , 16 }, { 1  , 14 , 16 }, { 1  , -14, 16 }, { 1  , -14, 16 },
        { 1  , 13 , 16 }, { 1  , 13 , 16 }, { 1  , -13, 16 }, { 1  , -13, 16 },
        { 1  , 12 , 16 }, { 1  , 12 , 16 }, { 1  , -12, 16 }, { 1  , -12, 16 },

        { 1  , 11 , 16 }, { 1  , 11 , 16 }, { 1  , -11, 16 }, { 1  , -11, 16 },
        { 1  , 10 , 16 }, { 1  , 10 , 16 }, { 1  , -10, 16 }, { 1  , -10, 16 },
        { 1  , 9  , 16 }, { 1  , 9  , 16 }, { 1  , -9 , 16 }, { 1  , -9 , 16 },
        { 1  , 8  , 16 }, { 1  , 8  , 16 }, { 1  , -8 , 16 }, { 1  , -8 , 16 },

            // 224
        { 0  , 31 , 15 }, { 0  , 31 , 15 }, { 0  , 31 , 15 }, { 0  , 31 , 15 },
        { 0  , -31, 15 }, { 0  , -31, 15 }, { 0  , -31, 15 }, { 0  , -31, 15 },
        { 0  , 30 , 15 }, { 0  , 30 , 15 }, { 0  , 30 , 15 }, { 0  , 30 , 15 },
        { 0  , -30, 15 }, { 0  , -30, 15 }, { 0  , -30, 15 }, { 0  , -30, 15 },

        { 0  , 29 , 15 }, { 0  , 29 , 15 }, { 0  , 29 , 15 }, { 0  , 29 , 15 },
        { 0  , -29, 15 }, { 0  , -29, 15 }, { 0  , -29, 15 }, { 0  , -29, 15 },
        { 0  , 28 , 15 }, { 0  , 28 , 15 }, { 0  , 28 , 15 }, { 0  , 28 , 15 },
        { 0  , -28, 15 }, { 0  , -28, 15 }, { 0  , -28, 15 }, { 0  , -28, 15 },

            // 232
        { 0  , 27 , 15 }, { 0  , 27 , 15 }, { 0  , 27 , 15 }, { 0  , 27 , 15 },
        { 0  , -27, 15 }, { 0  , -27, 15 }, { 0  , -27, 15 }, { 0  , -27, 15 },
        { 0  , 26 , 15 }, { 0  , 26 , 15 }, { 0  , 26 , 15 }, { 0  , 26 , 15 },
        { 0  , -26, 15 }, { 0  , -26, 15 }, { 0  , -26, 15 }, { 0  , -26, 15 },

        { 0  , 25 , 15 }, { 0  , 25 , 15 }, { 0  , 25 , 15 }, { 0  , 25 , 15 },
        { 0  , -25, 15 }, { 0  , -25, 15 }, { 0  , -25, 15 }, { 0  , -25, 15 },
        { 0  , 24 , 15 }, { 0  , 24 , 15 }, { 0  , 24 , 15 }, { 0  , 24 , 15 },
        { 0  , -24, 15 }, { 0  , -24, 15 }, { 0  , -24, 15 }, { 0  , -24, 15 },

            // 240
        { 0  , 23 , 15 }, { 0  , 23 , 15 }, { 0  , 23 , 15 }, { 0  , 23 , 15 },
        { 0  , -23, 15 }, { 0  , -23, 15 }, { 0  , -23, 15 }, { 0  , -23, 15 },
        { 0  , 22 , 15 }, { 0  , 22 , 15 }, { 0  , 22 , 15 }, { 0  , 22 , 15 },
        { 0  , -22, 15 }, { 0  , -22, 15 }, { 0  , -22, 15 }, { 0  , -22, 15 },

        { 0  , 21 , 15 }, { 0  , 21 , 15 }, { 0  , 21 , 15 }, { 0  , 21 , 15 },
        { 0  , -21, 15 }, { 0  , -21, 15 }, { 0  , -21, 15 }, { 0  , -21, 15 },
        { 0  , 20 , 15 }, { 0  , 20 , 15 }, { 0  , 20 , 15 }, { 0  , 20 , 15 },
        { 0  , -20, 15 }, { 0  , -20, 15 }, { 0  , -20, 15 }, { 0  , -20, 15 },

            // 248
        { 0  , 19 , 15 }, { 0  , 19 , 15 }, { 0  , 19 , 15 }, { 0  , 19 , 15 },
        { 0  , -19, 15 }, { 0  , -19, 15 }, { 0  , -19, 15 }, { 0  , -19, 15 },
        { 0  , 18 , 15 }, { 0  , 18 , 15 }, { 0  , 18 , 15 }, { 0  , 18 , 15 },
        { 0  , -18, 15 }, { 0  , -18, 15 }, { 0  , -18, 15 }, { 0  , -18, 15 },

        { 0  , 17 , 15 }, { 0  , 17 , 15 }, { 0  , 17 , 15 }, { 0  , 17 , 15 },
        { 0  , -17, 15 }, { 0  , -17, 15 }, { 0  , -17, 15 }, { 0  , -17, 15 },
        { 0  , 16 , 15 }, { 0  , 16 , 15 }, { 0  , 16 , 15 }, { 0  , 16 , 15 },
        { 0  , -16, 15 }, { 0  , -16, 15 }, { 0  , -16, 15 }, { 0  , -16, 15 },

            // 256
    };

    /**
     *   Get dct_coeff_first
     */
    private void getDCTCoeffFirst(int[] coeff, BufferedBitStream stream) throws InterruptedException
    {

        if (stream.showBits(1) == 1) {
            coeff[0] = 0;
            coeff[1] = (stream.getBits(2) == 2) ? 1 : -1 ;
        } else {
            int code = stream.showBits(17);
            int data[];

            if (code >= 2048)
                data = DCTCoefficientTable[code >>> 8];
            else if (code >= 256)
                data = DCTCoefficientTable[ (code >>> 3) + 512];
            else
                data = DCTCoefficientTable[(code) + 768];

            if (data[0] == ESC) {
                // More decoding
                stream.flushBits(data[2]);
                int run = stream.getBits(6);
                int value = stream.showBits(16);


                if ((value & 0xFF00) == 0x8000) {
                    stream.flushBits(16);
                    value = (value | 0xFFFFFF00);
                } else if  ((value & 0xFF00) == 0x0000) {
                    stream.flushBits(16);
                    value = (value & 0x000000FF);
                } else {
                    stream.flushBits(8);
                    value = (value << 16) >> 24;
                }
                coeff[0] = run;
                coeff[1] = value;
            } else {
                coeff[0] = data[0];
                coeff[1] = data[1];
                stream.flushBits(data[2]);
            }
        }
    }


    /**
     *   Contructor
     */
    MacroBlock()
    {
        intraQuantMatrix = new int[8*8];
        interQuantMatrix = new int[8*8];

        forwardVector = new MotionVector();
        backwardVector = new MotionVector();

        block = new int[6][8*8];
        //dctZZ = new int[8*8];
    }

    /**
     *   Reset the Macro Block
     */
    void reset()
    {

    }

    /**
     *   Parsing the Macro Block
     */
    protected void getMacroBlock(BufferedBitStream stream) throws InterruptedException
    {
        // Starting from the Macroblock type
        /* read macro block bit flags */

        switch (getPictureType()) {

        case Picture.I_TYPE:
            getIMBType(stream);
            break;
        case Picture.P_TYPE:
            getPMBType(stream);
            if (!isForwardPredicted())
                resetMotionVector();
            if (!isMBIntra())
                resetDCPredictor();
            break;
        case Picture.B_TYPE:
            getBMBType(stream);
            if (isMBIntra())
                resetMotionVector();
            else
                resetDCPredictor();
            break;
        }

        if (haveMBQuant())
            setQuantizerScale(stream.getBits(5));

        if (isForwardPredicted())
            forwardVector.getMotionVector(stream);

        if (isBackwardPredicted())
            backwardVector.getMotionVector(stream);

        if (haveMBPattern())
            getMBPattern(stream);
        else resetMBPattern();

        int i,j;

        // Clear the block data
        for (i=0;i<6;i++)
            for (j=0;j<64;j++)
                block[i][j] = 0;

        // Read DCT coefficients
        if (isMBIntra()) {

            for (i=0;i<4;i++) {
                dctDCYPast = getIntraBlock(block[i],stream, dctDCYPast,getDCLuminance(stream));
                IDCT.transform(block[i]);
            }

            dctDCCbPast = getIntraBlock(block[4],stream, dctDCCbPast,getDCChrominance(stream));
            IDCT.transform(block[4]);

            dctDCCrPast = getIntraBlock(block[5],stream, dctDCCrPast,getDCChrominance(stream));
            IDCT.transform(block[5]);

        } else {
            for (i=0;i<6;i++) {

                if ((codedBlockPattern & (1 << (5-i))) != 0) {

                    getInterBlock(block[i], stream);
                    IDCT.transform(block[i]);
                }
            }
        }

        if (getPictureType() == Picture.D_TYPE)
            stream.flushBits(1);
        // Parsing Macro Block finished!
    }

    /**
     *   Set Default Intra Quantize Matrix
     */
    void setDefaultIntraQuantMatrix()
    {
        for (int i=0;i<defaultIntraQuantMatrix.length;i++)
            intraQuantMatrix[i] = defaultIntraQuantMatrix[i];
    }

    /**
     *   Set Intra Quantize Matrix
     */
    void setIntraQuantMatrix(BufferedBitStream stream) throws InterruptedException
    {
        for (int i=0;i<defaultIntraQuantMatrix.length;i++)
            intraQuantMatrix[i] = stream.getBits(8);
    }

    /**
     *   Set Default Inter Quantize Matrix
     */
    void setDefaultInterQuantMatrix()
    {
        for (int i=0;i<interQuantMatrix.length;i++)
            interQuantMatrix[i] = 16;
    }

    /**
     *   Set Inter Quantize Matrix
     */
    void setInterQuantMatrix(BufferedBitStream stream) throws InterruptedException
    {
        for (int i=0;i<interQuantMatrix.length;i++)
            interQuantMatrix[i] = stream.getBits(8);
    }

    /**
     *   Set the Picture type
     */
    void setPictureType ( int code )
    {
        pictureType = code;
    }

    /**
     *   Set the Forward Vector's full_pel
     */
    void setFullPelForwardVector(int value)
    {
        forwardVector.setFullPel(value);
    }

    /**
     *   Set the Forward Vector's f_code
     */
    void setForwardFCode(int value)
    {
        forwardVector.setMotionFCode(value);
    }

    /**
     *   Set the Backward Vector's full_pel
     */
    void setFullPelBackwardVector(int value)
    {
        backwardVector.setFullPel(value);
    }

    /**
     *   Set the Backward Vector's f_code
     */
    void setBackwardFCode(int value)
    {
        backwardVector.setMotionFCode(value);
    }

    /**
     *   Set the Quantizer Scale
     */
    void setQuantizerScale(int value)
    {
        quantizerScale = value;
    }

    /**
     *   Reset the DCT dc past predictor
     */
    void resetDCPredictor()
    {
        dctDCYPast = 128*8;
        dctDCCrPast = 128*8;
        dctDCCbPast = 128*8;
    }

    /**
     *   If the Macro Block is bidirectional predicted
     */
    boolean isBidirPredicted()
    {
        return ((mbType & FORWARD)!=0) && ((mbType & BACKWARD)!=0);
    }

    /**
     *   If the Macro Block is backward predicted
     */
    boolean isBackwardPredicted()
    {
        return (mbType & BACKWARD)!=0;
    }

    /**
     *   If the Macro Block is forward predicted
     */
    boolean isForwardPredicted()
    {
        return ((mbType & FORWARD) != 0);
    }

    /**
     *   If the Macro Block have Quantizer scale
     */
    boolean haveMBQuant()
    {
        return ((mbType & QUANT) != 0);
    }

    /**
     *   If the Macro Block is intra coded
     */
    boolean isMBIntra()
    {
        return ((mbType & INTRA) != 0);
    }

    /**
     *   Reset the Motion Vector
     */
    void resetMotionVector()
    {
        forwardVector.resetMotionVector();
        backwardVector.resetMotionVector();
    }


    /**
     *   Get the block data
     */
    int[][] getData()
    {
        return block;
    }

    /**
     *   Get the Picture type
     */
    private int getPictureType()
    {
        return pictureType;
    }

    /**
     *   Get one Intra coded Block
     */
    private int getIntraBlock( int block[], BufferedBitStream stream, int dctDCPast, int dcValue) throws InterruptedException
    {
        int i, position;
        int data0,data1;

        int code, temp[];

        int blocktemp;
        boolean endOfBlock = false;

        block[0] = dctDCPast + (dcValue * 8);

        i = 0;

        while ( ! endOfBlock ) {

            { // DCT Next Coefficient decode
                code = stream.showBits(17);

                if (code >= 2048)
                    temp = DCTCoefficientTable[code >>> 8];
                else if (code >= 256)
                    temp = DCTCoefficientTable[ (code >>> 3) + 512];
                else
                    temp = DCTCoefficientTable[ (code) + 768];

                if (temp[0] == ESC) {
                    // More decoding
                    stream.flushBits(temp[2]);
                    int run = stream.getBits(6);
                    int value = stream.showBits(16);

                    if ((value & 0xFF00) == 0x8000) {
                        stream.flushBits(16);
                        value = (value | 0xFFFFFF00);
                    } else if  ((value & 0xFF00) == 0x0000) {
                        stream.flushBits(16);
                        value = (value & 0x000000FF);
                    } else {
                        stream.flushBits(8);
                        value = (value << 16) >> 24;
                    }
                    data0 = run;
                    data1 = value;

                } else {
                    stream.flushBits(temp[2]);
                    data0 = temp[0];
                    data1 = temp[1];
                }

            } // End of DCT Next Coeffcient decode


            if (data0 != EOB) {
                i = i + data0 + 1;
                position = zigzag[i];

                blocktemp =  data1 * quantizerScale * intraQuantMatrix[i] / 8;

                if ((blocktemp & 1) == 0)
                    if (blocktemp > 0)
                        blocktemp -= 1;
                    else if (blocktemp < 0)
                        blocktemp += 1;

                if (blocktemp > 2047) blocktemp = 2047;
                else if (blocktemp < -2048) blocktemp = -2048;

                block[position] = blocktemp;
            } else endOfBlock = true;
        }
        return block[0];
    }

    /**
     *   Get one Inter Coded Block
     */
    private void getInterBlock(int block[], BufferedBitStream stream) throws InterruptedException
    {
        int i, position;
        int blocktemp=0;
        int data[] = new int[2];
        int data0, data1;
        int sign = 0;

        int code, temp[];
        boolean endOfBlock = false;

        i = -1;

        getDCTCoeffFirst(data, stream);
        data0 = data[0];
        data1 = data[1];

        do {
            i = i + data0 + 1;
            position = zigzag[i];

            if ( data1 > 0 )  sign = 1;
            else sign = -1;
            blocktemp = ((2 * data1 + sign) * quantizerScale * interQuantMatrix[i])/16;

            if ((blocktemp & 1) == 0)
                if (blocktemp > 0)
                    blocktemp -= 1;
                else if (blocktemp < 0)
                    blocktemp += 1;

            if (blocktemp > 2047) blocktemp = 2047;
            else if (blocktemp < -2048) blocktemp = -2048;

            block[position] = blocktemp;

            { // DCT Next Coefficient decode
                code = stream.showBits(17);

                if (code >= 2048)
                    temp = DCTCoefficientTable[code >>> 8];
                else if (code >= 256)
                    temp = DCTCoefficientTable[ (code >>> 3) + 512];
                else
                    temp = DCTCoefficientTable[ (code) + 768];

                if (temp[0] == ESC) {
                    // More decoding
                    stream.flushBits(temp[2]);
                    int run = stream.getBits(6);
                    int value = stream.showBits(16);

                    if ((value & 0xFF00) == 0x8000) {
                        stream.flushBits(16);
                        value = (value | 0xFFFFFF00);
                    } else if  ((value & 0xFF00) == 0x0000) {
                        stream.flushBits(16);
                        value = (value & 0x000000FF);
                    } else {
                        stream.flushBits(8);
                        value = (value << 16) >> 24;
                    }

                    data0 = run;
                    data1 = value;

                } else {
                    stream.flushBits(temp[2]);
                    data0 = temp[0];
                    data1 = temp[1];
                }

            } // End of DCT Next Coeffcient decode
        } while (data0 != EOB);
    }

    /**
     *   If have coded Macro Block pattern
     */
    private boolean haveMBPattern()
    {
        return ((mbType & PATTERN) !=0 );
    }

}
