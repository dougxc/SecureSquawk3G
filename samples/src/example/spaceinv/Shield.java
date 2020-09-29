/*
 *  @(#)Shield.java	1.3 01/08/21
 *  Copyright (c) 2000-2001 Sun Microsystems, Inc., 901 San Antonio Road,
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
/**
 *  Sheild for player protection.
 */
public class Shield {
    /** Sheild width (10 pixels). */
    private static final int WIDTH = 10;
    /** Sheild height (6 pixels). */
    private static final int HEIGHT = 6;
    /** Handle for graphics object while rendering. */
    private Graphics graphics;
    /** Coordinates for top of sheild. */
    private int[] top;
    /** Coordinates for bottom of sheild. */
    private int[] bottom;
    /** Current sprite location x coordinate in pixels. */
    private int sx;
    /** Current shield location x coordinate in pixels. */
    private int x;
    /** Current shield location x coordinate in pixels. */
    private int y;
    /** Flag to indicate if the player has been killed. */
    private boolean dead = false;

    /**
     * Sheild object constructor.
     * @param x x coordinate of sheild in pixels
     * @param y y coordinate of sheild in pixels
     */
    public Shield(int x, int y) {
        int i;

        this.x = x;
        this.y = y;

        top = new int[WIDTH];
        bottom = new int[WIDTH];

        top[0] = top[WIDTH - 1] = 1;
        bottom[0] = bottom[WIDTH - 1] = HEIGHT;
        for (i = 1; i < WIDTH-1; i++) bottom[i] = HEIGHT - 1;
    }

    /**
     * Check for intersection of sheild with Fleet of objects.
     *
     * @param fleet objects to check for collisions.
     */
    public void intersect(Fleet fleet) {
        int i;
        Sprite s;

        for (i = 0; i < fleet.size(); i++) {
            s = (Sprite)fleet.elementAt(i);

            if (intersects(s, fleet.getVerticalStep())) {
                fleet.removeElement(s);
                redraw();
            }
        }
    }

    /** 
     * Check for intersection with specific Sprite item.
     *
     * @param sprite graphic sprite to test
     * @param direction current sprite movement. 
     *  If direction > 0 test top of sprite, 
     *  otherwise test bottom of sprite.
     * @return true, if intersection has happened
     */
    public boolean intersects(Sprite sprite, int direction) {
        int sy;

        sx = sprite.getX() - x;
        // sy = sprite.getY() - y;

        if (sx < 0 || sx >= WIDTH) {
	    return false;
	}

        if (direction > 0) {
            sy = sprite.getY() - (y + top[sx]);
            if (top[sx] < 0 || sy < 0) {
		return false;
	    }
            top[sx] += 2;
        } else {
            sy = sprite.getY() - (y + bottom[sx]);
            if (top[sx] < 0 || sy > 0) {
		return false;
	    }
            bottom[sx] -= 2;
        }

        if (bottom[sx] - top[sx] < 1) {
            top[sx] = -1;
        }

        return true;
    }
    /** 
     * Repaint the sheild lines with black and gray lines.
     */
    private void redraw() {
        if (sx < 0 || sx >= WIDTH) {
	    return;
	}

        if (top[sx] < 0) {
            graphics.setColor(Engine.BLACK);
            graphics.drawLine(x + sx, y, x + sx, y + HEIGHT);
        } else {
            graphics.setColor(Engine.BLACK);
            graphics.drawLine(x + sx, y, x + sx, y + top[sx] - 1);

            graphics.setColor(Engine.GREY);
            graphics.drawLine(x + sx, y + top[sx], x + sx, y + bottom[sx]);

            graphics.setColor(Engine.BLACK);
            graphics.drawLine(x + sx, y + bottom[sx] + 1, x + sx, y + HEIGHT);
        }
    }
    /**
     * Render the specified graphic object.
     *
     * @param g graphic object to be rendered
     */
    public void paint(Graphics g) {
        int i;

        graphics = g;

        g.setColor(Engine.GREY);
        for (i = 0; i < WIDTH; i++) {
            if (top[i] < 0) continue;
            g.drawLine(x + i, y + top[i], x + i, y + bottom[i]);
        }
    }
    /**
     * Blacken the screen, clear the top array, and
     * set the flag indicating death of player.
     */
    public void cleanup() {
        if (!dead) {
            graphics.setColor(Engine.BLACK);
            graphics.fillRect(x, y, WIDTH, HEIGHT + 1);
            for (int i = 0; i < WIDTH; top[i++] = -1);
            dead = true;
        }
    }

}
