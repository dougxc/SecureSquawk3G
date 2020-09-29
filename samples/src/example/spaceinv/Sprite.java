/*
 *  @(#)Sprite.java	1.5 01/08/21
 *  Copyright (c) 1999,2001 Sun Microsystems, Inc., 901 San Antonio Road,
 *  Palo Alto, CA 94303, U.S.A.  All Rights Reserved.
 *
 *  Sun Microsystems, Inc. has intellectual property rights relating
 *  to the technology embodied in this software.  In particular, and
 *  without limitation, these intellectual property rights may include
 *  one or more U.S. patents, foreign patents, or pending
 *  applications.  Sun, Sun Microsystems, the Sun logo, Java, KJava,
 *  and all Sun-based and Java-based marks are trademarks or
 *  registered trademarks of Sun Microsystems, Inc.  in the United
 *  States and other countries.
 *
 *  This software is distributed under licenses restricting its use,
 *  copying, distribution, and decompilation.  No part of this
 *  software may be reproduced in any form by any means without prior
 *  written authorization of Sun and its licensors, if any.
 *
 *  FEDERAL ACQUISITIONS:  Commercial Software -- Government Users
 *  Subject to Standard License Terms and Conditions
 */
package example.spaceinv;

import javax.microedition.lcdui.*;

public class Sprite
{
    private Image[] image;
    private Graphics graphics;
    private Fleet fleet;
    private int x;
    private int y;
    private int paint_x;    // x location of last paint
    private int paint_y;    // y location of last paint
    private int height;
    private int width;
    private int index;
    private int color;
    private int bg_color;

    /**
     * constructor
     */
    public Sprite(int x, int y, int width, int height, int color)
    {
        move(x, y);
        this.width = width;
        this.height = height;
        this.color = color;

        paint_x = x;
        paint_y = y;
        bg_color = Engine.BLACK;
    }

    /**
     * constructor
     */
    public Sprite(int x, int y, Image[] image, int color)
    {
        this(x, y, image, null);
    }

    /**
     * constructor
     */
    public Sprite(int x, int y, Image[] image, Fleet fleet)
    {
        this.image = image;
        index = 0;
        move(x, y);
        this.width = image[0].getWidth();
        this.height = image[0].getHeight();
        this.fleet = fleet;

        paint_x = x;
        paint_y = y;
        bg_color = Engine.BLACK;
    } // sprite()

    public void setBackgroundColor(int bg_color) {
        this.bg_color = bg_color;
    }

    /**
     * getX
     */
    public int getX()
    {
        return x;
    }

    /**
     * setX
     */
    public void setX(int x) {
        this.x = x;
    }

    /**
     * getY
     */
    public int getY()
    {
        return y;
    }

    /**
     * setY
     */
    public void setY(int y) {
        this.y = y;
    }

    /**
     * move
     */
    public void move(int x, int y)
    {
        setX(x);
        setY(y);
    }

    /**
     * getWidth
     */
    public int getWidth()
    {
        return width;
    }

    /**
     * getHeight
     */
    public int getHeight()
    {
        return height;
    }

    /**
     * getImage
     */
    public Image getImage()
    {
        if (image == null)
            return null;

        return image[index];
    } // getImage()

    /**
     * tick
     */
    public void tick()
    {
        if ((index+1) == image.length)
        {
            if (fleet == null)
                index = 0;
            else
            {
                fleet.removeElement(this);
            }
        }
	else
	    index++;
    } // tick()

    public void remove() {
        if (graphics != null) {
            erase(graphics);
            graphics.setColor(bg_color);
            graphics.fillRect(x, y, width, height);
        }
    }

    public void erase(Graphics g) {
        int erase_width, erase_height, erase_x, erase_y;

        g.setColor(bg_color);

        if (x != paint_x) {
            if (x > paint_x) erase_x = paint_x;
            else {
                if (Math.abs(x - paint_x) <= width) {
                    erase_x = x + width;
                } else {
                    erase_x = paint_x;
                }
            }

            erase_width = Math.min(width, Math.abs(x - paint_x));

            g.fillRect(erase_x, y, erase_width, height);
        }

        if (y != paint_y) {
            if (y > paint_y) erase_y = paint_y;
            else {
                if (Math.abs(y - paint_y) <= height) {
                    erase_y = y + height;
                } else {
                    erase_y = paint_y;
                }
            }

            erase_height = Math.min(height, Math.abs(y - paint_y));

            g.fillRect(x, erase_y, width, erase_height);
        }
    }

    /**
     * paint
     */
    public void paint(Graphics g) {
        graphics = g;

        erase(g);

        if (image == null) {
            g.setColor(color);
            g.fillRect(x, y, width, height);
        } else {
            g.drawImage(image[index], x, y, Graphics.TOP|Graphics.LEFT);
        }

        paint_x = x;
        paint_y = y;
    }


    /**
     * intersects
     */
    public boolean intersects(Sprite o)
    {
        if (fleet != null || o.fleet != null)
            return false;

        int right = x + width;
        int bottom = y + height;
        int o_right = o.x + o.width;
        int o_bottom = o.y + o.height;

        if ((o.x >= x && o.x < right
            || o_right >= x && o_right < right
            || x >= o.x && x < o_right
            || right >= o.x && right < o_right)
            && (o.y >= y && o.y < bottom
                || o_bottom >= y && o_bottom < bottom
                || y >= o.y && y < o_bottom
                || bottom >= o.y && bottom < o_bottom))
            return true;
        else
            return false;
    } // intersects()

} // class Sprite
