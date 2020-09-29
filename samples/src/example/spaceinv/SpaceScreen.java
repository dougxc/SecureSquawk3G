/*
 *  @(#)SpaceScreen.java    1.6 01/08/21
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

import java.util.Vector;
import javax.microedition.lcdui.*;

/**
 * Background screen for snake game.
 */
public class SpaceScreen extends javax.microedition.lcdui.Canvas
{
    /** Driver for all game motion. */
    private Engine engine;
    /** Space screen parent is the space invaders game itself. */
    private SpaceInvaders parent;
    /** Current score in the game. */
    private int score;
    /** Highest score for the game. */
    private int hi_score;
    /** Flag to signal full repaint operation. */
    public boolean complete_redraw = true;

    /**
     * constructor
     * @param si space invaders to record as parent screen
     */
    SpaceScreen(SpaceInvaders si)
    {
        parent = si;
    }


    /**
     * setEngine
     * @param engine  driver for game
     */
    public void setEngine(Engine engine)
    {
        this.engine = engine;
    }


    /**
     * keyRepeated
     * @param key key to be repeated via forced release
     */
    protected void keyRepeated(int key)
    {
        keyReleased(key);
    }


    /**
     * keyPressed
     * @param key user initiated action key
     */
    protected void keyPressed(int key)
    {
        int action = getGameAction(key); // get action associated with key
        if (engine != null)
        engine.processAction(action);
    }

    /**
     * paint
     * @param g graphics object to be rendered
     */
    public void paint(Graphics g)
    {
        Fleet fleet;
        Vector shields;
        Sprite s;
        int i = 0, j;
        int sc;
        int hisc;

        if (engine == null)
            return;

        sc = engine.getScore().getScore();
        hisc = engine.getScore().getHighScore();

        //if (complete_redraw) {
            g.setColor(Engine.BLACK); // background color
            g.fillRect(0, 0, getWidth(), getHeight());

            g.setColor(Engine.YELLOW);
            g.drawString("S: " + Integer.toString(sc), 10, 0,
                                    Graphics.TOP|Graphics.LEFT);
            g.drawString("H: " + Integer.toString(hisc), 50, 0,
                                    Graphics.TOP|Graphics.LEFT);
            complete_redraw = false;
        //}

        shields = engine.getShields();

        for (i = 0; i < shields.size(); i++) {
            Shield shield = (Shield)shields.elementAt(i);
            shield.paint(g);
        }

        g.setColor(Engine.GREY); // foreground color

        synchronized (engine)
        {
            if (sc != score || hisc != hi_score) {
                score = sc;
                hi_score = hisc;

                g.setColor(Engine.BLACK);
                g.fillRect(10, 0, 70, 10);
            }

            g.setColor(Engine.YELLOW);
            g.drawString("S: " + Integer.toString(sc), 10, 0,
                                    Graphics.TOP|Graphics.LEFT);
            g.drawString("H: " + Integer.toString(hisc), 50, 0,
                                    Graphics.TOP|Graphics.LEFT);

            // paint player bombs
            fleet = engine.getPlayerBombs();
            for (i = 0; i < fleet.size(); i++)
            {
                s = (Sprite)fleet.elementAt(i);
                s.paint(g);
            }

            // paint aliens bombs
            fleet = engine.getAlienBombs();
            for (i = 0; i < fleet.size(); i++)
            {
                s = (Sprite)fleet.elementAt(i);
                s.paint(g);
            }

            // paint alien
            fleet = engine.getAliens();
            for (i = 0; i < fleet.size(); i++)
            {
                s = (Sprite)fleet.elementAt(i);
                s.paint(g);
            }

            // paint ufo
            s = engine.getUFO();
            s.paint(g);

            // paint gun
            s = engine.getGun();
            s.paint(g);

            // paint booms
            fleet = engine.getBooms();
            for (i = 0; i < fleet.size(); i++)
            {
                s = (Sprite)fleet.elementAt(i);
                s.paint(g);
            }
        } // synchronize engine

        ++paints;
    } // paint()


    int paints = 0;


    /**
     * This is called when a command menu is shown
     */
    protected void hideNotify()
    {
        parent.pauseApp();
    }


    /**
     * This is called after a command menu is shown
     */
    protected void showNotify()
    {
        complete_redraw = true;
        parent.start();
    }
} // class screen
