/*
 *  @(#)PongBall.java	1.3 00/12/11
 *  Copyright (c) 1999 Sun Microsystems, Inc., 901 San Antonio Road,
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
 * A PongBall is a lightweight animated ball that runs in its own thread.
 * It moves within a rectangular region, bouncing off the walls at 45 degree
 * angles.
 * The ball is a Runnable object so it can be restarted after stopping if
 * system wants the game to pause.
 */
class PongBall {

    /* random number generator */
    static java.util.Random random = new java.util.Random(); 

    /* the region in which the ball moves */
    private int minX;
    private int minY;
    private int maxX;
    private int maxY;

    /* the position of the ball */
    private int posX;
    private int posY;

    /* the size of the ball */
    private int radius = 2;
    private int diameter = radius * 2;

    /* the direction of the ball */
    private int deltaX;
    private int deltaY;

    /* the court to draw on and to inform about paddle ball interaction. */
    private PongCourt court;
    
    /* variable to tell us not to draw the ball when it is out of play */
    private boolean outOfPlay = true;

    /*
     * construct a new PongBall
     *   the arguments define the active region
     */
    PongBall(PongCourt pongCourt, int left, int top,
             int width, int height) {

        super();

        /* We need the court so the ball can move on its own. */
        court = pongCourt;

        /* set the ball's region */
        minX = left;
        minY = top;
        maxX = minX + width - diameter;
        maxY = minY + height - diameter;
    }

    /*
     * resets the ball to a random start position
     */
    void putInPlay() {

        outOfPlay = false;

        /*
         * set the ball's position at almost at the right side of the court
         * (use positive random #s)
         */
        posX = maxX - 1;
        posY = ((random.nextInt()>>>1) % (maxY - minY)) + minY;

        /* set the ball's direction, the ball starts from right to left */
        deltaX = -1;

        deltaY = random.nextInt() & 1;
        if (deltaY == 0) {
            deltaY = -1;
        }
    }

    /*
     * takes the ball out of play
     */
    void takeOutOfPlay() {
        outOfPlay = true;
    }


    public boolean inPlay() {
        return !outOfPlay;
    }

    /* moves the ball */
    public void move() {
        if (outOfPlay) {
            return;
        }

        /* check for ball-paddle interaction */
        if ((posX <= minX) && (!court.isPaddleHit(posY, diameter))) {
            outOfPlay = true;
            court.repaint();
            return;
        }

        /*
         * if the ball hit a wall then change direction and since
         * our walls are in x and y plains we can just flip the
         * right delta.
         */
        if (posX <= minX || posX >= maxX) {
            deltaX = -deltaX;
        }

        if (posY <= minY || posY >= maxY) {
            deltaY = -deltaY;
        }

        /* calculate the new position */
        posX = posX + deltaX;
        posY = posY + deltaY;
    }


    /*
     * Paint the ball.
     */
    void paint(Graphics g) {
        /* don't draw the ball if it is out of play */
        if (outOfPlay) {
            return;
        }

        g.setColor(0);
        g.fillArc(posX, posY, diameter, diameter, 0, 360);
    }

    public String toString() {
        return super.toString() + " x = " + posX + ", y = " + posY;
    }

}
