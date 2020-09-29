/*
 *   @(#) MPEG-I Vide Decoder 0.9 Yu Tianli (yu-tianli@usa.net)
 *
 *   Displayable.java   1999-2-24 09:11 am
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
 *   The Display interface that a displayer must implement.
 */
public interface Displayable
{
    /**
     *   Set the display size
     */
    void setDisplaySize(int width, int height);

    /**
     *   Set the video image data source
     */
    //void setImageSource(ImageProducer imagesource);

    /**
     *   Update the time info
     */
    void updateTime(int hour, int minute, int second);

    /**
     *   Update the frame info
     */
    void updateFrame();

    /**
     *   Next image in the imagesource is ready to display
     */
    void nextImage();
}