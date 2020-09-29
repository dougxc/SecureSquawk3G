/*
 *  @(#)Fleet.java	1.5 01/08/21
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
import java.util.*;

public class Fleet extends Vector
{
    private int left_edge;
    private int right_edge;
    private int bottom_edge;
    private int horizontal_step;
    private int direction;
    private int vertical_step;
    private int left_boundary;
    private int right_boundary;
    private int bottom_boundary;
    private int max_count;
    private Random rnd;

    /**
     * constructor
     */
    public Fleet()
    {
        horizontal_step = 1;
        direction = 1;
        vertical_step = 1;
        left_edge = Integer.MAX_VALUE;
        right_edge = Integer.MIN_VALUE;
        bottom_edge = Integer.MIN_VALUE;
        rnd = new Random();
    } // ctor

    /**
     * addSprite
     */
    public void addSprite(Sprite s)
    {
        addElement(s);
    }

    public void addElement(Object o) {
        if (o instanceof Sprite) {
            checkEdges((Sprite)o);
        }

        super.addElement(o);
        max_count++;
    }

    /**
     * removeSprite
     */
    public boolean removeSprite(Sprite s)
    {
        if (s.getX() == left_edge)
            resync();
        if (s.getX() + s.getWidth() == right_edge)
            resync();
        if (s.getY() + s.getHeight() == bottom_edge)
            resync();

        return removeElement(s);
    } // removeSprite()

    public boolean removeElement(Object o) {
        if (o instanceof Sprite) {
            ((Sprite)o).remove();
        }

        return super.removeElement(o);
    }

    /**
     * moveVertical
     */
    public void moveVertical(Fleet targets, Fleet booms)
    {
        moveVertical(targets, booms, null);
    }

    /**
     * moveVertical
     */
    public void moveVertical(Fleet targets, Fleet booms, Score score)
    {
        left_edge = Integer.MAX_VALUE;
        right_edge = Integer.MIN_VALUE;
        bottom_edge = Integer.MIN_VALUE;

        for (int i = 0; i < size(); i++)
        {
            Sprite s = (Sprite)elementAt(i);
            s.move(s.getX(), s.getY() + vertical_step);

            if (s.getY() + s.getHeight() <= 0 || s.getY() >= bottom_boundary)
            {
                removeSprite(s);
                continue;
            } // if

            checkEdges(s);

            Sprite t = targets.intersect(s);
            if (t != null)
            {
                targets.removeSprite(t);
                booms.addSprite(new Sprite(t.getX(), t.getY(),
					   Engine.BOOM_IMGS, booms));
                removeSprite(s);
                if (score != null)
                    score.add();
            } // if
        } // for

    } // moveVertical()

    /**
     * moveHorizontal
     */
    public void moveHorizontal(Fleet bombs)
    {
        int vstep = 0;
        int hstep = direction;

        if (bottom_edge >= bottom_boundary)
            return;

        if (direction < 0 && left_edge - horizontal_step < left_boundary ||
            direction > 0 && right_edge + horizontal_step > right_boundary)
        {
            direction = -direction;
            vstep = vertical_step;
            hstep = 0;
        } // if

        left_edge = Integer.MAX_VALUE;
        right_edge = Integer.MIN_VALUE;
        bottom_edge = Integer.MIN_VALUE;

        for (int i = 0; i < size(); i++)
        {
            Sprite s = (Sprite)elementAt(i);
            s.move(s.getX() + hstep, s.getY() + vstep);
            s.tick();
            checkEdges(s);

            if (bombs != null && Engine.throwDice(900) < max_count + 1 - size())
            {
                bombs.addSprite(new Sprite(s.getX() + 3,
                                           s.getY(),
                                           1,
                                           2,
                                           Engine.YELLOW));
            } // if
        } // for

    } // moveHorizontal()

    /**
     * intersect
     */
    public Sprite intersect(Sprite o)
    {
        for (int i = 0; i  < size(); i++)
        {
            Sprite s = (Sprite)elementAt(i);
            if (o.intersects(s))
		return s;
        }

        return null;
    } // intersect()


    /**
     * resync
     */
    private void resync()
    {
        for (int i = 0; i < size(); i++)
        {
            checkEdges((Sprite)elementAt(i));
        }
    } // resync()

    /**
     * checkEdges
     */
    private void checkEdges(Sprite s)
    {
        if (s.getX() < left_edge)
            left_edge = s.getX();
        if (s.getX() + s.getWidth() > right_edge)
            right_edge = s.getX() + s.getWidth();
        if (s.getY() + s.getHeight() > bottom_edge)
            bottom_edge = s.getY() + s.getHeight();
    } // checkEdges()

    /**
     * getLEftBoundary
     */
    public int getLeftBoundary()
    {
        return left_boundary;
    }

    /**
     * setLeftBoundary
     */
    public void setLeftBoundary(int left)
    {
        left_boundary = left;
    }

    /**
     * getRightBoundary
     */
    public int getRightBoundary()
    {
        return right_boundary;
    }

    /**
     * setRightBoundary
     */
    public void setRightBoundary(int right)
    {
        right_boundary = right;
    }

    /**
     * getBottomBaoundary
     */
    public int getBottomBoundary()
    {
        return bottom_boundary;
    }

    public int getBottomEdge() {
        return bottom_edge;
    }

    /**
     * setBottomBoundary
     */
    public void setBottomBoundary(int bottom)
    {
        bottom_boundary = bottom;
    }

    /**
     * getHorizontalStep
     */
    public int getHorizontalStep()
    {
        return horizontal_step;
    }

    /**
     * setHorizontalStep
     */
    public void setHorizontalStep(int step)
    {
        horizontal_step = step;
        direction = step;
    }

    /**
     * getVerticalStep
     */
    public int getVerticalStep()
    {
        return vertical_step;
    }

    /**
     * setVerticalStep
     */
    public void setVerticalStep(int step)
    {
        vertical_step = step;
    }

} // class Fleet
