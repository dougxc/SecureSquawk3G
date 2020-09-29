/*
 *  @(#)SpaceInvaders.java	1.7 01/08/21
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
import javax.microedition.midlet.*;

import example.About;


public class SpaceInvaders extends MIDlet {

    private Engine engine;
    private SpaceScreen screen;
    private Display display;
    private boolean pausedByPlayer = false;

    private Command pause = new Command("Pause", Command.SCREEN, 1);
    private Command resume = new Command("Resume", Command.SCREEN, 1);
    private Command about = new Command("About", Command.HELP, 11);
    private Command bye = new Command("Exit", Command.EXIT, 12);

    private CommandListener commandListener = new CommandListener() {
	public void commandAction(Command c, Displayable s) {
	    if (c == pause) {
                screen.removeCommand(pause);
                screen.addCommand(resume);
                pausedByPlayer = true;
                pauseApp();
                return;
	    }

	    if (c == resume) {
                screen.removeCommand(resume);
                screen.addCommand(pause);
                pausedByPlayer = false;
                startApp();
                return;
	    }

	    if (c == about) {
                About.showAbout(display);
                return;
	    }

	    if (c == bye) {
                destroyApp(true);
                notifyDestroyed();
                return;
	    }
	}
    };


    /**
     * constructor
     */
    public SpaceInvaders() {
	display = Display.getDisplay(this);

        screen = new SpaceScreen(this);
	screen.addCommand(pause);
	screen.addCommand(about);
	screen.addCommand(bye);
	screen.setCommandListener(commandListener);

        engine = new Engine(screen);
    }

    /**
     * startApp
     */
    public void startApp() {
	display.setCurrent(screen);
        start();
    }

    /**
     * start
     */
    public void start() {
        if (pausedByPlayer || !engine.isStopped()) {
            return;
        }

        engine.start();
    }

    /**
     * pauseApp
     */
    public void pauseApp() {
        engine.stop();
    }

    /**
     * destroyApp
     */
    public void destroyApp(boolean unconditional) {
        engine.terminate();
    }

} // class SpaceInvaders


