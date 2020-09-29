/*
 *   @(#) MPEG-I Vide Decoder 0.9 Yu Tianli (yu-tianli@usa.net)
 *
 *   DisplayPanel.java   1999-2-24 09:11 am
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
import java.awt.*;
import java.awt.image.*;

/**
 *   DisplayPanel is a simple Panel that implements the Displayable interface
 * to draw images on the Panel
 */
public class DisplayPanel extends Panel implements Displayable
{
    /**
     *   The image to display
     */
    private Image img = null;

    // methods

    /**
     *   Constructor
     */
    public DisplayPanel()
    {
        super();
        img = null;
    }

    /**
     *   Set the panels size
     */
    public void setDisplaySize(int width, int height)
    {
        setSize( width, height);
    }

    /**
     *   Set the image source for the image
     */
    public void setImageSource(ImageProducer imagesource)
    {
        if (img != null)
            img.flush();
        img = Toolkit.getDefaultToolkit().createImage(imagesource);
        getGraphics().drawImage(img, 0, 0 , null);
    }

    /**
     *   Get next image from the imagesource and draw it
     */
    public void nextImage()
    {
        if (img != null) {
            img.flush();
            getGraphics().drawImage(img, 0, 0 , null);
        }

        ++paints;
    }

    int paints = 0;

    /**
     *   Override the original update() to prevent clear of the screen before paint()
     */
    public void update(Graphics g)
    {
        paint(g);
    }

    /**
     *   Do nothing
     */
    public void updateTime(int hour, int minute, int second)
    {
    }

    /**
     *   Do nothing
     */
    public void updateFrame()
    {
    }
}