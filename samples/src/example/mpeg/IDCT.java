/*
 *   @(#) MPEG-I Vide Decoder 0.9 Yu Tianli (yu-tianli@usa.net)
 *
 *   IDCT.java   1999-2-24 09:11 am
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
 * Fast inverse two-dimensional discrete cosine transform algorithm
 * by Chen-Wang using 32 bit integer arithmetic (8 bit coefficients).
 */
class IDCT {
  /**
   * The basic DCT block is 8x8 samples
   */
  private final static int DCTSIZE = 8;

  /**
   * Integer arithmetic precision constants
   */
  private final static int PASS_BITS = 3;
  private final static int CONST_BITS = 11;

  /**
   * Precomputed DCT cosine kernel functions:
   *  Ci = (2^CONST_BITS)*sqrt(2.0)*cos(i * PI / 16.0)
   */
  private final static int C1 = 2841;
  private final static int C2 = 2676;
  private final static int C3 = 2408;
  private final static int C5 = 1609;
  private final static int C6 = 1108;
  private final static int C7 = 565;

  public static void transform(int block[]) {
    /* pass 1: process rows */
    for (int i = 0, offset = 0; i < DCTSIZE; i++, offset += DCTSIZE) {

      /* get coefficients */
      int d0 = block[offset + 0];
      int d4 = block[offset + 1];
      int d3 = block[offset + 2];
      int d7 = block[offset + 3];
      int d1 = block[offset + 4];
      int d6 = block[offset + 5];
      int d2 = block[offset + 6];
      int d5 = block[offset + 7];
      int d8;

      /* AC terms all zero? */
      if ((d1 | d2 | d3 | d4 | d5 | d6 | d7) == 0) {
    d0 <<= PASS_BITS;
    block[offset + 0] = d0;
    block[offset + 1] = d0;
    block[offset + 2] = d0;
    block[offset + 3] = d0;
    block[offset + 4] = d0;
    block[offset + 5] = d0;
    block[offset + 6] = d0;
    block[offset + 7] = d0;
    continue;
      }

      /* first stage */
      d8 = (d4 + d5) * C7;
      d4 = d8 + d4 * (C1 - C7);
      d5 = d8 - d5 * (C1 + C7);
      d8 = (d6 + d7) * C3;
      d6 = d8 - d6 * (C3 - C5);
      d7 = d8 - d7 * (C3 + C5);

      /* second stage */
      d8 = ((d0 + d1) << CONST_BITS) + (1 << (CONST_BITS - PASS_BITS - 1));
      d0 = ((d0 - d1) << CONST_BITS) + (1 << (CONST_BITS - PASS_BITS - 1));
      d1 = (d2 + d3) * C6;
      d2 = d1 - d2 * (C2 + C6);
      d3 = d1 + d3 * (C2 - C6);
      d1 = d4 + d6;
      d4 = d4 - d6;
      d6 = d5 + d7;
      d5 = d5 - d7;

      /* third stage */
      d7 = d8 + d3;
      d8 = d8 - d3;
      d3 = d0 + d2;
      d0 = d0 - d2;
      d2 = ((d4 + d5) * 181) >> 8;
      d4 = ((d4 - d5) * 181) >> 8;

      /* output stage */
      block[offset + 0] = (d7 + d1) >> (CONST_BITS - PASS_BITS);
      block[offset + 7] = (d7 - d1) >> (CONST_BITS - PASS_BITS);
      block[offset + 1] = (d3 + d2) >> (CONST_BITS - PASS_BITS);
      block[offset + 6] = (d3 - d2) >> (CONST_BITS - PASS_BITS);
      block[offset + 2] = (d0 + d4) >> (CONST_BITS - PASS_BITS);
      block[offset + 5] = (d0 - d4) >> (CONST_BITS - PASS_BITS);
      block[offset + 3] = (d8 + d6) >> (CONST_BITS - PASS_BITS);
      block[offset + 4] = (d8 - d6) >> (CONST_BITS - PASS_BITS);
    }

    /* pass 2: process columns */
    for (int i = 0, offset = 0; i < DCTSIZE; i++, offset++) {

      /* get coefficients */
      int d0 = block[offset + DCTSIZE*0];
      int d4 = block[offset + DCTSIZE*1];
      int d3 = block[offset + DCTSIZE*2];
      int d7 = block[offset + DCTSIZE*3];
      int d1 = block[offset + DCTSIZE*4];
      int d6 = block[offset + DCTSIZE*5];
      int d2 = block[offset + DCTSIZE*6];
      int d5 = block[offset + DCTSIZE*7];
      int d8;

      /* AC terms all zero? */
      if ((d1 | d2 | d3 | d4 | d5 | d6 | d7) == 0) {
    d0 >>= PASS_BITS + 3;
    block[offset + DCTSIZE*0] = d0;
    block[offset + DCTSIZE*1] = d0;
    block[offset + DCTSIZE*2] = d0;
    block[offset + DCTSIZE*3] = d0;
    block[offset + DCTSIZE*4] = d0;
    block[offset + DCTSIZE*5] = d0;
    block[offset + DCTSIZE*6] = d0;
    block[offset + DCTSIZE*7] = d0;
    continue;
      }

      /* first stage */
      d8 = (d4 + d5) * C7;
      d4 = (d8 + d4 * (C1 - C7)) >> 3;
      d5 = (d8 - d5 * (C1 + C7)) >> 3;
      d8 = (d6 + d7) * C3;
      d6 = (d8 - d6 * (C3 - C5)) >> 3;
      d7 = (d8 - d7 * (C3 + C5)) >> 3;

      /* second stage */
      d8 = ((d0 + d1) << (CONST_BITS - 3)) + (1 << (CONST_BITS + PASS_BITS-1));
      d0 = ((d0 - d1) << (CONST_BITS - 3)) + (1 << (CONST_BITS + PASS_BITS-1));
      d1 = (d2 + d3) * C6;
      d2 = (d1 - d2 * (C2 + C6)) >> 3;
      d3 = (d1 + d3 * (C2 - C6)) >> 3;
      d1 = d4 + d6;
      d4 = d4 - d6;
      d6 = d5 + d7;
      d5 = d5 - d7;

      /* third stage */
      d7 = d8 + d3;
      d8 = d8 - d3;
      d3 = d0 + d2;
      d0 = d0 - d2;
      d2 = ((d4 + d5) * 181) >> 8;
      d4 = ((d4 - d5) * 181) >> 8;

      /* output stage */
      block[offset + DCTSIZE*0] = (d7 + d1) >> (CONST_BITS + PASS_BITS);
      block[offset + DCTSIZE*7] = (d7 - d1) >> (CONST_BITS + PASS_BITS);
      block[offset + DCTSIZE*1] = (d3 + d2) >> (CONST_BITS + PASS_BITS);
      block[offset + DCTSIZE*6] = (d3 - d2) >> (CONST_BITS + PASS_BITS);
      block[offset + DCTSIZE*2] = (d0 + d4) >> (CONST_BITS + PASS_BITS);
      block[offset + DCTSIZE*5] = (d0 - d4) >> (CONST_BITS + PASS_BITS);
      block[offset + DCTSIZE*3] = (d8 + d6) >> (CONST_BITS + PASS_BITS);
      block[offset + DCTSIZE*4] = (d8 - d6) >> (CONST_BITS + PASS_BITS);
    }
  }
}

