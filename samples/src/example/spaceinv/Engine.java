/*
 *  @(#)Engine.java 1.8 01/08/21
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

import java.util.Random;
import java.util.Vector;
import javax.microedition.lcdui.*;
/**
 * Driving class to put snake in motion.
 */
public class Engine implements Runnable {
    /** Array of alien graphic objects. */
    private Fleet aliens;
    /** Array of player bomb graphic objects. */
    private Fleet player_bombs;
    /** Array of alien bomb graphic objects. */
    private Fleet alien_bombs;
    /** Array of explosion graphic objects. */
    private Fleet booms;
    /** Array of player shield graphic objects. */
    private Vector shields;
    /** Array of player gun graphic objects. */
    private Fleet gunfleet;
    /** Background graphic for game screen. */
    private SpaceScreen screen;
    /** Sprite handle for individual gun. */
    private Sprite gun;
    /** Sprite handle for UFO object. */
    private Sprite ufo;
    /** Handle for game score graphic. */
    private Score score;
    /** Random number seed for game behavior. */
    private static Random rnd;
    /** Delay in milliseconds between alien moves. */
    private int alien_delay = 5;
    /** Intermediate variable for calculating alien wall bounce. */
    private int divider;
    /**
     * Time of last processed user action used to limit user actions
     * to one per 5 millisecond interval.
     */
    private long last;
    /** Border constraint is 25 pixels below screen max height. */
    private int shield_top;
    /** Flag to indicate the current game is stopped. */
    private boolean stopped = true;
    /** Red color (0xff0000). */
    public final static int RED = 0x00FF0000;
    /** Green color (0x0000ff). */
    public final static int GREEN = 0x0000FF00;
    /** Blue color (0x00ff00). */
    public final static int BLUE = 0x000000FF;
    /** White color (0xffffff). */
    public final static int WHITE = 0x00FFFFFF;
    /** Black color (0x000000). */
    public final static int BLACK = 0x00000000;
    /** Grey color (0x808080). */
    public final static int GREY = 0X00808080;
    /** Yellow color (0xffff00). */
    public final static int YELLOW = 0X00FFFF00;
    /** Array of explosion images. */
    public final static Image[] BOOM_IMGS;
    /** UFo offset from top of screen (20 pixels). */
    private static final int UFO_Y = 20;
    /** First alien in first position. */
    public static final String ALIEN1_1 = ""
        + "___rr___"
        + "__rrrr__"
        + "_r_rr_r_"
        + "_rrrrrr_"
        + "__r__r__"
        + "_r____r_"
        + "r______r";

    /** First alien in second position. */
    public static final String ALIEN1_2 = ""
        + "___rr___"
        + "__rrrr__"
        + "_r_rr_r_"
        + "_rrrrrr_"
        + "__r__r__"
        + "_r____r_"
        + "__r__r__";

    /** Second alien in first position. */
    public static final String ALIEN2_1 = ""
        + "__y__y__"
        + "y__yy__y"
        + "y_y__y_y"
        + "_yyyyyy_"
        + "__yyyy__"
        + "__y__y__"
        + "_y____y_";

    /** Second alien in second position. */
    public static final String ALIEN2_2 = ""
        + "__y__y__"
        + "___yy___"
        + "__y__y__"
        + "_yyyyyy_"
        + "y_yyyy_y"
        + "y_y__y_y"
        + "___yy___";

    /** Third alien in first position. */
    public static final String ALIEN3_1 = ""
        + "___bb___"
        + "_bbbbbb_"
        + "bb_bb_bb"
        + "bbbbbbbb"
        + "__b__b__"
        + "_b_bb_b_"
        + "b______b";

    /** Third alien in second position. */
    public static final String ALIEN3_2 = ""
        + "___bb___"
        + "_bbbbbb_"
        + "bb_bb_bb"
        + "bbbbbbbb"
        + "__b__b__"
        + "_b_bb_b_"
        + "__b__b__";

    /** UFO in first position. */
    public static final String UFO_1 = ""
        + "___yy___"
        + "_yyyyyy_"
        + "_y_y__y_"
        + "_y_y__y_"
        + "_yyyyyy_"
        + "___yy___";

    /** UFO in second position. */
    public static final String UFO_2 = ""
        + "___yy___"
        + "_yyyyyy_"
        + "yy__yy_y"
        + "yy__yy_y"
        + "_yyyyyy_"
        + "___yy___";

    /** Gun image. */
    public static final String GUN = ""
        + "___gg___"
        + "__gggg__"
        + "__gggg__"
        + "__gggg__"
        + "__gggg__"
        + "gggggggg"
        + "gggggggg";

    /** First explosion image. */
    public static final String BOOM_1 = ""
        + "__w____w"
        + "_w___w__"
        + "__w_ww__"
        + "_w_w__w_"
        + "__w_ww__"
        + "_w_w__w_"
        + "w____w__";

    /** Second explosion image. */
    public static final String BOOM_2 = ""
        + "W_______"
        + "_w_____w"
        + "w__w__ww"
        + "w_w____w"
        + "_w___w__"
        + "___w____"
        + "w______w";

    /** THird explosion image. */
    public static final String BOOM_3 = ""
        + "____w___"
        + "_w__w___"
        + "w___w__w"
        + "___w___w"
        + "______w_"
        + "_w____W_"
        + "___w____";

    static
    {
        BOOM_IMGS = new Image[]
        {
        Pixelator.createImage(Engine.BOOM_1, 8),
        Pixelator.createImage(Engine.BOOM_2, 8),
        Pixelator.createImage(Engine.BOOM_3, 8)
        };
    }

    /**
     * constructor
     * @param screen default screen to use for this game
     */
    public Engine(SpaceScreen screen) {
        this.screen = screen;
        screen.setEngine(this);
        score = new Score();
        rnd = new Random();
        init();
    }

    /**
     * init
     */
    private void init() {
        Image[] image;
        int i;
        int fleet_width = screen.getWidth() / 20;
        int shield_offset = 15;

        shield_top = screen.getHeight() - 25;
        if (screen.getWidth() > 100) shield_offset += 5;

        gun = new Sprite(screen.getWidth()/2-6,
                         screen.getHeight()-8,
                         new Image[] {Pixelator.createImage(GUN, 8)},
             GREEN);
        gunfleet = new Fleet();
        gunfleet.addElement(gun);
        aliens = new Fleet();
        aliens.setHorizontalStep(2);
        aliens.setVerticalStep(2);
        aliens.setLeftBoundary(0);
        aliens.setRightBoundary(screen.getWidth());
        aliens.setBottomBoundary(screen.getHeight());

        image = new Image[] {Pixelator.createImage(ALIEN1_1, 8),
                             Pixelator.createImage(ALIEN1_2, 8)};
        for (i = 0; i < fleet_width; i++)
            aliens.addElement(new Sprite(15 * i + 1, 30, image, RED));

        image = new Image[] {Pixelator.createImage(ALIEN2_1, 8),
                             Pixelator.createImage(ALIEN2_2, 8)};
        for (i = 0; i < fleet_width; i++)
            aliens.addElement(new Sprite(15 * i, 45, image, YELLOW));

        image = new Image[] {Pixelator.createImage(ALIEN1_2, 8),
                             Pixelator.createImage(ALIEN1_1, 8)};
        for (i = 0; screen.getHeight() > 100 && i < fleet_width; i++)
            aliens.addElement(new Sprite(15 * i + 1, 60, image, RED));

        image = new Image[] {Pixelator.createImage(ALIEN3_1, 8),
                             Pixelator.createImage(ALIEN3_2, 8)};
        for (i = 0; screen.getHeight() > 120 && i < fleet_width; i++)
            aliens.addElement(new Sprite(15 * i, 75, image, BLUE));

        divider = aliens.size() / (screen.getWidth() > 100 ? 2 : 4);

        ufo = new Sprite(0, 500, new Image[] {Pixelator.createImage(UFO_1, 8),
                                              Pixelator.createImage(UFO_2, 8)},
                                              YELLOW);

        booms = new Fleet();
        booms.setHorizontalStep(0);
        booms.setVerticalStep(0);
        booms.setBottomBoundary(screen.getHeight());

        player_bombs = new Fleet();
        player_bombs.setVerticalStep(-4);
        player_bombs.setBottomBoundary(screen.getHeight());

        alien_bombs = new Fleet();
        alien_bombs.setVerticalStep(3);
        alien_bombs.setBottomBoundary(screen.getHeight());

        shields = new Vector();
        for (i = 0; i < 3; i++) {
            shields.addElement(new Shield(shield_offset + 56 * i, shield_top));
        }

        screen.complete_redraw = true;
    } // init()

    /**
     * getAliens
     * @return Fleet of alien objects
     */
    public Fleet getAliens() {
        return aliens;
    }

    /**
     * getPlayerBombs
     * @return Fleet of player bombs deployed
     */
    public Fleet getPlayerBombs() {
        return player_bombs;
    }

    /**
     * getAliensBombs
     * @return Fleet of alien bombs deployed
     */
    public Fleet getAlienBombs() {
        return alien_bombs;
    }

    /**
     * getBooms
     * @return Fleet of explosions in progress
     */
    public Fleet getBooms() {
        return booms;
    }

    /**
     * getGun
     * @return Sprite object for gun
     */
    public Sprite getGun() {
        return gun;
    }

    /**
     * getUFO
     * @return Sprite object for UFO
     */
    public Sprite getUFO() {
        return ufo;
    }

    /**
     * getShields
     * @return Vector of current shields
     */
    public Vector getShields() {
        return shields;
    }

    /**
     * getScore
     * @return current game score
     */
    public Score getScore() {
        return score;
    }

    /**
     * reset
     */
    private void reset() {
    init();
        score.reset();
    }

    /**
     * start
     */
    public void start() {
        stopped = false;
        Thread runner = new Thread(this);
        runner.start();
    }

    /**
     * stop
     */
    public void stop() {
        stopped = true;
    }

    /**
     * isStopped
     * @return true if game is stopped
     */
    public boolean isStopped() {
        return stopped;
    }

    /**
     * throwDice
     * @param range limit of valid dice value to return
     * @return random dice value
     */
    public static int throwDice(int range) {
    int dice = rnd.nextInt() % (range/2) + range/2;
    return dice;
    }

    /**
     * run
     */
    public void run() {
        int i, n = 0;

        while (!stopped)
        {
            synchronized (this)
            {
                try
                {
                    Thread.sleep(screen.getWidth() > 100 ? 30 : 40);
                }
                catch (Exception e) {}

                alien_delay  = aliens.size() / divider;

                if (n++ % (alien_delay + 1) == 0)
                    aliens.moveHorizontal(alien_bombs);

                if (ufo.getY() == 500 /*&& throwDice(300) < 5*/)
                    ufo.move(0, UFO_Y);

                if (ufo.getY() == UFO_Y)
                {
                    ufo.move(ufo.getX() + 1, UFO_Y);
                    ufo.tick();

                    if (throwDice(300) < 10)
                    {
                        alien_bombs.addElement(new Sprite(ufo.getX() + 3,
                                                           ufo.getY(),
                                                           1, 2,
                                                           YELLOW));
                    }

                    if (ufo.getX() > screen.getWidth())
                        ufo.move(0, 500);

                    if (player_bombs.intersect(ufo) != null)
                    {
                        booms.addElement(new Sprite(ufo.getX(),
                                                    ufo.getY(),
                                                    BOOM_IMGS,
                                                    booms));
                        ufo.move(0, 500);
                        score.add(5);
                    }
                } // if(ufo.getY..

                booms.moveHorizontal(null);
                player_bombs.moveVertical(aliens, booms, score);
                alien_bombs.moveVertical(gunfleet, booms);

                for (i = 0; i < shields.size(); i++) {
                    Shield shield = (Shield)shields.elementAt(i);
                    shield.intersect(alien_bombs);
                    shield.intersect(player_bombs);
                    if (aliens.getBottomEdge() >= shield_top) shield.cleanup();
                }

                if (gunfleet.isEmpty())
                {
                    try {Thread.sleep(2000); }
                    catch (InterruptedException ie) {}

                     reset();
                    continue;
                }

                if (aliens.isEmpty() && booms.isEmpty() && ufo.getY() == 500)
                {
                    init();
                    continue;
                }

                if (aliens.intersect(gun) != null)
                {
                    try {Thread.sleep(2000); }
                    catch (InterruptedException ie) {}

                    reset();
                    continue;
                }
            } // synchronize

            screen.repaint();
        } // while
    } // run()

    /**
     * processAction
     * @param action user designated action (LEFT, RIGHT, FIRE)
     */
    public void processAction(int action) {
        stopped = false;
        long diff = System.currentTimeMillis() - last;
        last = System.currentTimeMillis();

        if (diff < 5)
        return;

        int x = gun.getX();
        int y = gun.getY();

        // identify action
        if (action == screen.LEFT && x > 0)
    {
            gun.move(x - 4, y);
        }

        if (action == screen.RIGHT && x + gun.getWidth() < screen.getWidth())
    {
            gun.move(x + 4, y);
        }

        if (action == screen.FIRE && player_bombs.isEmpty())
    {
            player_bombs.addElement(new Sprite(gun.getX() + 3,
                                               gun.getY(),
                                               1,
                                               2,
                           YELLOW));
        }
    } // processAction()

    /**
     * terminate
     */
    public void terminate() {
        stop();
    }

} // class engine

