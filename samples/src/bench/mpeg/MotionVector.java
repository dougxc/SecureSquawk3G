/*
 *   @(#) MPEG-I Vide Decoder 0.9 Yu Tianli (yu-tianli@usa.net)
 *
 *   MotionVector.java   1999-2-24 09:11 am
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
 *   Motion Vector
 */
class MotionVector
{
    /**
     *   Right and down value (in half pixels)
     */
    int right,down;

    /**
     *   If the vector is in full pixel
     */
    private boolean fullPel;

    /**
     *   motion_f value
     */
    private int motionF;

    /**
     *   motion_horizontal_code
     */
    private int motionHorizontalCode;

    /**
     *   motion_vertical_code
     */
    private int motionVerticalCode;

    /**
     *   motion_r_size
     */
    private int motionRSize;

    /**
     *   motion_horizontal_r
     */
    private int motionHorizontalR;

    /**
     *   motion_vertical_r
     */
    private int motionVerticalR;

    /**
     *   Unreachable code
     */
    private static final int UR = 0;

    /**
     *   Motion horizontal & vertical code look up table
     */
    private static final int MVTable[][] = {

        // xxxx x... ...
        { UR , UR }, { UR , UR }, { 3  , 5  }, { -3 , 5  },
        { 2  , 4  }, { 2  , 4  }, { -2 , 4  }, { -2 , 4  },
        { 1  , 3  }, { 1  , 3  }, { 1  , 3  }, { 1  , 3  },
        { -1 , 3  }, { -1 , 3  }, { -1 , 3  }, { -1 , 3  },

        { 0  , 1  }, { 0  , 1  }, { 0  , 1  }, { 0  , 1  },
        { 0  , 1  }, { 0  , 1  }, { 0  , 1  }, { 0  , 1  },
        { 0  , 1  }, { 0  , 1  }, { 0  , 1  }, { 0  , 1  },
        { 0  , 1  }, { 0  , 1  }, { 0  , 1  }, { 0  , 1  },

        // 0000 xxxx xxx
        { UR , UR }, { UR , UR }, { UR , UR }, { UR , UR },
        { UR , UR }, { UR , UR }, { UR , UR }, { UR , UR },
        { UR , UR }, { UR , UR }, { UR , UR }, { UR , UR },
        { UR , UR }, { UR , UR }, { UR , UR }, { UR , UR },

        { UR , UR }, { UR , UR }, { UR , UR }, { UR , UR },
        { UR , UR }, { UR , UR }, { UR , UR }, { UR , UR },
        { 16 , 11 }, { -16, 11 }, { 15 , 11 }, { -15, 11 },
        { 14 , 11 }, { -14, 11 }, { 13 , 11 }, { -13, 11 },

        { 12 , 11 }, { -12, 11 }, { 11 , 11 }, { -11, 11 },
        { 10 , 10 }, { 10 , 10 }, { -10, 10 }, { -10, 10 },
        { 9  , 10 }, { 9  , 10 }, { -9 , 10 }, { -9 , 10 },
        { 8  , 10 }, { 8  , 10 }, { -8 , 10 }, { -8 , 10 },

        { 7  , 8  }, { 7  , 8  }, { 7  , 8  }, { 7  , 8  },
        { 7  , 8  }, { 7  , 8  }, { 7  , 8  }, { 7  , 8  },
        { -7 , 8  }, { -7 , 8  }, { -7 , 8  }, { -7 , 8  },
        { -7 , 8  }, { -7 , 8  }, { -7 , 8  }, { -7 , 8  },

        { 6  , 8  }, { 6  , 8  }, { 6  , 8  }, { 6  , 8  },
        { 6  , 8  }, { 6  , 8  }, { 6  , 8  }, { 6  , 8  },
        { -6 , 8  }, { -6 , 8  }, { -6 , 8  }, { -6 , 8  },
        { -6 , 8  }, { -6 , 8  }, { -6 , 8  }, { -6 , 8  },

        { 5  , 8  }, { 5  , 8  }, { 5  , 8  }, { 5  , 8  },
        { 5  , 8  }, { 5  , 8  }, { 5  , 8  }, { 5  , 8  },
        { -5 , 8  }, { -5 , 8  }, { -5 , 8  }, { -5 , 8  },
        { -5 , 8  }, { -5 , 8  }, { -5 , 8  }, { -5 , 8  },

        { 4  , 7  }, { 4  , 7  }, { 4  , 7  }, { 4  , 7  },
        { 4  , 7  }, { 4  , 7  }, { 4  , 7  }, { 4  , 7  },
        { 4  , 7  }, { 4  , 7  }, { 4  , 7  }, { 4  , 7  },
        { 4  , 7  }, { 4  , 7  }, { 4  , 7  }, { 4  , 7  },

        { -4 , 7  }, { -4 , 7  }, { -4 , 7  }, { -4 , 7  },
        { -4 , 7  }, { -4 , 7  }, { -4 , 7  }, { -4 , 7  },
        { -4 , 7  }, { -4 , 7  }, { -4 , 7  }, { -4 , 7  },
        { -4 , 7  }, { -4 , 7  }, { -4 , 7  }, { -4 , 7  },
    };

    /**
     *   Get the Motion code
     */
    private int getMotionCode(BufferedBitStream stream) throws InterruptedException
    {
        int code = stream.showBits(11);
        int data[];

        if (code >= 128)
            data = MVTable [ code >>> 6 ];
        else
            data = MVTable [ code + 32 ];

        stream.flushBits(data[1]);
        return data[0];
    }

    /**
     *   Constructor
     */
    MotionVector()
    {
        right = down = 0;
    }

    /**
     *   Parsing the motion vector
     */
    void getMotionVector(BufferedBitStream stream) throws InterruptedException
    {
        motionHorizontalCode = getMotionCode(stream);

        if ((motionF != 1) && (motionHorizontalCode != 0))
            motionHorizontalR = stream.getBits(motionRSize);

        motionVerticalCode = getMotionCode(stream);

        if ((motionF != 1) && (motionVerticalCode != 0))
            motionVerticalR = stream.getBits(motionRSize);

        // Compute the right and down value:

        int complementHorizontalR, complementVerticalR;

        if ((motionF == 1) || (motionHorizontalCode == 0))
            complementHorizontalR = 0;
        else
            complementHorizontalR = motionF - 1 - motionHorizontalR;

        if ((motionF == 1) || (motionVerticalCode == 0))
            complementVerticalR = 0;
        else
            complementVerticalR = motionF -1 - motionVerticalR;

        int rightLittle = motionHorizontalCode * motionF;
        int rightBig = 0;

        if (rightLittle == 0) {
            rightBig = 0;
        } else if (rightLittle > 0) {
            rightLittle = rightLittle - complementHorizontalR;
            rightBig = rightLittle - (32 * motionF);
        } else {
            rightLittle = rightLittle + complementHorizontalR;
            rightBig = rightLittle + (32 * motionF);
        }

        int downLittle = motionVerticalCode * motionF;
        int downBig = 0;

        if (downLittle == 0) {
            downBig = 0;
        } else if (downLittle > 0) {
            downLittle = downLittle - complementVerticalR;
            downBig = downLittle - (32 * motionF);
        } else {
            downLittle = downLittle + complementVerticalR;
            downBig = downLittle + (32 * motionF);
        }

        int max = (16 * motionF) - 1;
        int min = -16 * motionF;

        int newVector = right + rightLittle;

        if ((newVector <= max) && (newVector >= min))
            right = newVector;
        else right = right + rightBig;

        if (fullPel)
            right <<= 1;

        newVector = down + downLittle;

        if ((newVector <= max) && (newVector >= min))
            down = newVector;
        else down = down + downBig;

        if (fullPel)
            down <<= 1;
        // Vector computing finished. right and down are in half pixel value.
    }

    /**
     *   Reset the motion vector
     */
    void resetMotionVector()
    {
        right = down = 0;
    }

    /**
     *   Set the full_pel flag
     */
    void setFullPel( int value )
    {
        fullPel = (value!=0);
    }

    /**
     *   Set motion_f_code
     */
    void setMotionFCode ( int code )
    {
        motionRSize = code - 1;
        motionF = 1 << motionRSize;
    }
}