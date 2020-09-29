/*
 *   @(#) MPEG-I Vide Decoder 0.9 Yu Tianli (yu-tianli@usa.net)
 *
 *   PictureFormatException.java   1999-2-24 09:11 am
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
 *   Format Exception in Picture Layer
 */
class Trace
{

    //static StringBuffer sb = new StringBuffer(5000);

    static void println(String s) {
         System.out.println(s);
//        sb.append(s);
//        sb.append('\n');
    }

    static void print(String s) {
         System.out.print(s);
//        sb.append(s);
    }

    static void write(PrintStream ps) {
//        ps.print(sb.toString());
//        sb = new StringBuffer(5000);
    }

}