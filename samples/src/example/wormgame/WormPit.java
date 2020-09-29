/*
 * @(#)WormPit.java	1.9 01/08/22 @(#)
 * Copyright (c) 2001 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the confidential and proprietary information of Sun
 * Microsystems, Inc. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Sun.
 *
 * SUN MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY OF THE
 * SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE, OR NON-INFRINGEMENT. SUN SHALL NOT BE LIABLE FOR ANY DAMAGES
 * SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR DISTRIBUTING
 * THIS SOFTWARE OR ITS DERIVATIVES.
 */

/*
 * WormPit.java
 *
 * Created on March 30, 2001, 16:15
 * @author Todd M Kennedy
 * @version 1.0
 */

package example.wormgame;

import java.lang.System;
import java.lang.Runnable;
import java.lang.InterruptedException;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Graphics;

import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;

/**
 * The WormPit contains a Worm and some WormFood. The Worm will
 * slither around the pit in search of WormFood. If the Worm eats
 * it will grow in length. The Worm will be killed if it attempts
 * to slither past the edge of the pit or if it eats itself.
 */
public class WormPit extends Canvas implements Runnable {

    /** Handle to target food. */
    private WormFood myFood;
    /** Handle to current worm object. */
    private Worm     myWorm;
    /** Flag to indicate when the game is finished. */
    private boolean gameOver = true;
    /** Flag to indicate if current game is paused. */
    private boolean gamePaused = false;
    /** Flag to indicate game is restarted. */
    private boolean gameRestart = false;
    /** Flag to indicate forced repaint is needed. */
    private boolean forceRedraw = true;
    /** Current game score. */
    private int score = 0;
    /** Current game level. */
    private int level = 7;
    /** Current screen width in pixels. */
    private int width;
    /** Current screen height in pixels. */
    private int height;
    /** Current screen size in cells. */
    static int CellWidth;
    /** Current screen height in cells. */
    static int CellHeight;
    /** Initial offset for screen rendering. */
    private static final int START_POS = 3;
    /** Font height for rendering the score message. */
    private static final int SCORE_CHAR_HEIGHT;
    /** Font width for rendering the score message. */
    private static final int SCORE_CHAR_WIDTH;
    /** Height for two lines of score message text. */
    private static final int SCORE_HEIGHT;
    /** Default delay between worm repaints. (400 milliseconds) */
    private static final int DEFAULT_WAIT = 400;
    /** Maximum number of difficulty levels. (10) */
    static final byte MAX_LEVELS = 10;
    /** Color for target food object. (0x00ff00) */
    static final int FOOD_COLOUR  = 0x0000ff00;
    /** Color for text objects. (0xff0000) */
    static final int TEXT_COLOUR  = 0x00ff0000;
    /** Color for erasing worm cells. (0xffffff) */
    static final int ERASE_COLOUR = 0x00ffffff;
    /** Color for drawing worm cells. (0x000000) */
    static final int DRAW_COLOUR  = 0x00000000;
    /** Size of a cell for worm rendering. */
    public  static final int CELL_SIZE = 5;

    static {
	Font defaultFont  = Font.getDefaultFont();
	SCORE_CHAR_WIDTH  = defaultFont.charWidth('S');
	SCORE_CHAR_HEIGHT = defaultFont.getHeight();
	SCORE_HEIGHT      = SCORE_CHAR_HEIGHT * 2;
    }

    /**
     * Default constructor for worm pit.
     * Initialized the food and worm objects.
     */
    public WormPit() {
	width  = round(getWidth());
	height = round(getHeight()-SCORE_HEIGHT);
	WormPit.CellWidth  = (width-(START_POS*2)) / WormPit.CELL_SIZE;
	WormPit.CellHeight = (height-(START_POS*2)) / WormPit.CELL_SIZE;
	myWorm = new Worm(this);

	/* Generate food for worm to eat */
	myFood  = new WormFood(this);
	int x = myFood.getX();
	int y = myFood.getY();
	while (myWorm.contains(x, y)) {
	    myFood.regenerate();        // regenerate if food placed under worm
	    x = myFood.getX();  y = myFood.getY();
	}
    }

    /**
     * Round the given value to next lowest number divisible by the
     * cell size.
     * @param val number to be rounded down
     * @return rounded down value
     */
    private int round(int val) {
	int delta = (val-(START_POS*2)) % CELL_SIZE;
	return (val - delta);
    }

    /**
     * Set the difficulty level. If the new level is different from the
     * current level, any game currently in progress is terminated and
     * the score is lost.
     * @param level value of new level requested
     * @see #getLevel
     */
    public void setLevel(int level) {
	if (this.level != level) {
	    this.level = level;
	    gameOver = true;
	    gamePaused = false;
	    score = 0;
	}
    }

    /**
     * Get the difficulty level.
     * @return current game level
     * @see #setLevel
     */
    public int getLevel() {
	return level;
    }

    /**
     * Get the score of this game.
     * @return current score
     */
    public int getScore() {
	return score;
    }

    /**
     * Returns true if the given point is within the bounds of the
     * worm pit.
     * @param x x coordinate of point to check (0 - CellWidth)
     * @param y y coordinate of point to check (0 - CellHeight)
     * @return true, if coordinate is in the worm pit
     */
    static boolean isInBounds(int x, int y) {
	if ((x < 0) || (x >= WormPit.CellWidth)) {
	    return false;
	}
	if ((y < 0) || (y >= WormPit.CellHeight)) {
	    return false;
	}
	return true;
    }

    /**
     * Restart the game.
     */
    void restart() {
	    if (gamePaused) {
		gamePaused = false;
	    } else {
		myWorm.regenerate();
		myFood.regenerate();
		score = 0;
		gameOver = false;
	    }
	    forceRedraw = true;
	synchronized (myWorm) {
	    myWorm.notifyAll();
	}
    }

    /**
     * Handle keyboard input. This is used for worm movement.
     * @param keyCode pressed key is either Canvas arrow key (UP,
     *  DOWN, LEFT, RIGHT) or simulated with KEY_NUM (2, 8, 4,6).
     */
    public void keyPressed(int keyCode) {
	switch (getGameAction(keyCode)) {
	case Canvas.UP:
	    myWorm.setDirection(Worm.UP);
	    break;
	case Canvas.DOWN:
	    myWorm.setDirection(Worm.DOWN);
	    break;
	case Canvas.LEFT:
	    myWorm.setDirection(Worm.LEFT);
	    break;
	case Canvas.RIGHT:
	    myWorm.setDirection(Worm.RIGHT);
	    break;
	case 0:
	    // There is no game action.. Use keypad constants instead
	    switch (keyCode) {
	    case Canvas.KEY_NUM2:
		myWorm.setDirection(Worm.UP);
		break;
	    case Canvas.KEY_NUM8:
		myWorm.setDirection(Worm.DOWN);
		break;
	    case Canvas.KEY_NUM4:
		myWorm.setDirection(Worm.LEFT);
		break;
	    case Canvas.KEY_NUM6:
		myWorm.setDirection(Worm.RIGHT);
		break;
	    }
	    break;
	}
    }

    /**
     * Paint anything currently in the pit.
     * Overrides Canvas.paint.
     * @param g graphics object to be rendered
     */
    private void paintPitContents(Graphics g) {
	try {
	    myWorm.update(g);    // update worm position
	    if (myFood.isAt(myWorm.getX(), myWorm.getY())) {
		myWorm.eat();
		score += level;

		g.setColor(WormPit.ERASE_COLOUR);
		g.fillRect((width - (SCORE_CHAR_WIDTH * 3))-START_POS,
			   height-START_POS,
			   (SCORE_CHAR_WIDTH * 3),
			   SCORE_CHAR_HEIGHT);
		g.setColor(WormPit.DRAW_COLOUR);

		// Display new score
		g.drawString("" + score,
			     width - (SCORE_CHAR_WIDTH * 3) - START_POS,
			     height - START_POS, g.TOP|g.LEFT);

		myFood.regenerate();
		int x = myFood.getX();
		int y = myFood.getY();
		while (myWorm.contains(x, y)) {
		// generate again if food placed under worm..
		    myFood.regenerate();
		    x = myFood.getX();  y = myFood.getY();
		}
	    }

	    myFood.paint(g);
	} catch (WormException se) {
	    gameOver = true;
	}
    }

    /**
     * Paint the worm pit and its components
     * @param g graphics object to be rendered
     */
    public void paint(Graphics g) {
	if (forceRedraw) {
	    // Redraw the entire screen
	    forceRedraw = false;

	    // Clear background
	    g.setColor(WormPit.ERASE_COLOUR);
	    g.fillRect(0, 0, width,
		       (height + SCORE_HEIGHT + WormPit.CELL_SIZE));

	    // Draw pit border
	    g.setColor(WormPit.DRAW_COLOUR);
	    g.drawRect(1, 1, (width - START_POS), (height - START_POS));

	    // Display current level
	    g.drawString("L: " + level, START_POS, height, g.TOP|g.LEFT);
	    g.drawString("" + score,
			 (width - (SCORE_CHAR_WIDTH * 3)),
			 height, g.TOP|g.LEFT);

	    // Display current score
	    g.drawString("S: ",
			 (width - (SCORE_CHAR_WIDTH * 4)),
			 height, g.TOP|g.RIGHT);
	    g.drawString("" + score,
			 (width - (SCORE_CHAR_WIDTH * 3)),
			 height, g.TOP|g.LEFT);

	    // Display highest score for this level
	    g.drawString("H: ",
			 (width - (SCORE_CHAR_WIDTH * 4)),
			 (height + SCORE_CHAR_HEIGHT),
			 g.TOP|g.RIGHT);
	    g.drawString("" + WormScore.getHighScore(level),
			 (width - (SCORE_CHAR_WIDTH * 3)),
			 (height + SCORE_CHAR_HEIGHT),
			 g.TOP|g.LEFT);

	    // Draw worm & food
	    g.translate(START_POS, START_POS);
	    myWorm.paint(g);
	    myFood.paint(g);
	} else {
	    // Draw worm & food
	    g.translate(START_POS, START_POS);
	}

	if (gamePaused) {
	    Font pauseFont = g.getFont();
	    int fontH = pauseFont.getHeight();
	    int fontW = pauseFont.stringWidth("Paused");
	    g.setColor(WormPit.ERASE_COLOUR);
	    g.fillRect((width-fontW)/2 - 1, (height-fontH)/2,
		       fontW + 2, fontH);
	    g.setColor(WormPit.TEXT_COLOUR);
	    g.setFont(pauseFont);
	    g.drawString("Paused", (width-fontW)/2, (height-fontH)/2,
			 g.TOP|g.LEFT);
	} else if (gameOver) {
	    Font overFont = g.getFont();
	    int fontH = overFont.getHeight();
	    int fontW = overFont.stringWidth("Game Over");

	    g.setColor(WormPit.ERASE_COLOUR);
	    g.fillRect((width-fontW)/2 - 1, (height-fontH)/2,
		       fontW + 2, fontH);
	    g.setColor(WormPit.TEXT_COLOUR);
	    g.setFont(overFont);
	    g.drawString("Game Over", (width-fontW)/2, (height-fontH)/2,
			 g.TOP|g.LEFT);
	} else {
	    paintPitContents(g);
	}
	g.translate(-START_POS, -START_POS);
    }

    /**
     * Notification handler when canvas hidden.
     * Forces a redraw when canvas is shown again.
     * Signals that the game is paused while the canvas is obscured.
     */
    protected void hideNotify() {
	super.hideNotify();
	forceRedraw = true;
	if (!gameOver) {
	    gamePaused = true;
	}
    }

    /**
     * The main execution loop.
     */
    public void run() {
	while (true) {
	    try {
		synchronized (myWorm) {
		    if (gameOver) {
			if (WormScore.getHighScore(level) < score) {
			    /* Display score screen */
			    WormScore.setHighScore(level, score, "me");
			}
			repaint();
			// serviceRepaints(); // Draw immediately
			myWorm.wait();    // Wait until user presses 'restart'
		    } else if (gamePaused) {
			repaint();
			// serviceRepaints(); // Draw immediately
			myWorm.wait();    // Wait until user presses 'restart'
		    } else {
			myWorm.moveOnUpdate();
			repaint();
			// serviceRepaints(); // Draw immediately
			myWorm.wait(DEFAULT_WAIT-(level*40));
		    }
		}
	    } catch (java.lang.InterruptedException ie) {
	    }
	}
    }

}

