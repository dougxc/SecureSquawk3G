/*
 *  @(#)PongPaddle.java	1.5 01/04/09
 *  Copyright (c) 1999-2001 Sun Microsystems, Inc., 901 San Antonio Road,
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
 
package example.pong;

import javax.microedition.lcdui.*;

/*
 * The PongPaddle is synchronized because the ball must query it from
 * from the ball thread.
 */
class PongPaddle {

    /* defaults for the paddle, the PongCourt needs to know the thickness */
    private static final int MAX_PADDLE_SPEED = 20;
    private static final int MIN_PADDLE_SPEED = 5;
    private static final int DEFAULT_SPEED = 10;
    private static final int MAX_PADDLE_HEIGHT = 30;
    private static final int MIN_PADDLE_HEIGHT = 10;
    private static final int DEFAULT_HEIGHT = 20;
    public static final int THICKNESS = 2;

    /* the region in which the paddle moves and its speed and height */
    private int minY;
    private int maxY;

    private int posX;
    private int posY;

    private int speed = DEFAULT_SPEED;
    private int height = DEFAULT_HEIGHT;

    private int courtHeight;

    /*
     * construct a new PongPaddle
     *   the arguments define the active region
     */
    PongPaddle(int left, int top, int canvasHeight) {

        super();

        courtHeight = canvasHeight;

        /* set the paddle's region */
        minY = top;
        maxY = minY + courtHeight - height;

        posX = left;
        posY = maxY / 2;
    }

    /*
     * moves the paddle up
     */
    synchronized void up() {
        posY -= speed;

        /* check for upper boundry */
        if (posY < minY) {
            posY = minY;
        }
    }

    /*
     * moves the paddle down
     */
    synchronized void down() {
        posY += speed;

        /* check for upper boundry */
        if (posY > maxY) {
            posY = maxY;
        }
    }

    /*
     * makes the paddle bigger
     */
    synchronized void bigger() {
        height += 2;

        /* check for upper boundry */
        if (height > MAX_PADDLE_HEIGHT) {
            height = MAX_PADDLE_HEIGHT;
        }

        maxY = minY + courtHeight - height;
        if (posY > maxY) {
            posY = maxY;
        }
    }

    /*
     * makes the paddle bigger
     */
    synchronized void smaller() {
        height -= 2;

        /* check for lower boundry */
        if (height < MIN_PADDLE_HEIGHT) {
            height = MIN_PADDLE_HEIGHT;
        }
    }

    /*
     * make the paddle faster
     */
    synchronized void faster() {
        speed *= 2;

        /* check for upper boundry */
        if (speed > MAX_PADDLE_SPEED) {
            speed = MAX_PADDLE_SPEED;
        }
    }

    /*
     * make the paddle slower
     */
    synchronized void slower() {
        speed /= 2;

        /* check for lower boundry */
        if (speed < MIN_PADDLE_SPEED) {
            speed = MIN_PADDLE_SPEED;
        }
    }

    /*
     * an atomic check to see if the ball has hit the paddle
     */
    synchronized boolean isHit(int ballYPos, int ballHeight) {
        int paddleBottom = posY + height;
        int ballBottom = ballYPos + ballHeight;

        /* it is easier to check if we missed and return the opposite */
        return !((ballYPos > paddleBottom) || (ballBottom < posY));
    }

    /*
     * Paint the paddle.
     */
    synchronized void paint(Graphics g) {
        g.setColor(0);
        g.fillRect(posX, posY, THICKNESS, height);
    }

    public String toString() {
        return "paddle at = " + posY;
    }

}
