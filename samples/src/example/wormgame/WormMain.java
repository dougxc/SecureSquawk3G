/*
 * @(#)WormMain.java	1.6 01/08/22 @(#)
 * Copyright (c) 1999-2001 Sun Microsystems, Inc. All Rights Reserved.
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
 * WormMain.java
 *
 * Created on March 30, 2001, 16:15
 * @author Todd M Kennedy
 * @version 1.0
 */

package example.wormgame;

import java.lang.Thread;

import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.Gauge;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
/**
 * Main routine for worm MIDlet.
 */
public class WormMain extends MIDlet implements CommandListener {
    /** Current game board for worm pit. */
    private WormPit theGame;
    /** Button for exiting the game. */
    private Command exitCmd  = new Command("Exit", Command.SCREEN, 3);
    /** Menu item for changing game levels. */
    private Command levelCmd = new Command("Change Level", Command.SCREEN, 3);
    /** Menu item for starting a new game. */
    private Command startCmd = new Command("Start", Command.SCREEN, 1);
    /** Menu item to restart another game. */
    private Command restartCmd = new Command("Restart", Command.SCREEN, 1);
    /** Menu item to cancel current pausedmenu dialog. */
    private Command cancelCmd = new Command("Cancel", Command.ITEM, 1);
    /** Menu item to confirm current selected operation. */
    private Command OKCmd = new Command("OK", Command.OK, 1);
    /**
     * Default mconstructor for worm MIDlet game.
     * Creates the initial graphics objects and sets the command
     * listener.
     */
    public WormMain() {
	theGame = new WormPit();
	theGame.addCommand(exitCmd);
	theGame.addCommand(levelCmd);
	theGame.addCommand(startCmd);
	theGame.setCommandListener(this);
    }

    /**
     * Destroy must cleanup everything. Only objects exist so the GC
     * will do all the cleanup after the last reference is removed.
     * @param unconditional if true, force MIDlet destroy processing
     */
    protected void destroyApp(boolean unconditional) {
	Display.getDisplay(this).setCurrent(null);
    }

    /**
     * Pause signals the thread to stop by clearing the thread field.
     * If stopped before done with the iterations it will be restarted
     * from scratch later.
     */
    protected void pauseApp() {
    }

    /**
     * Start creates the thread to do the timing. It should return
     * immediately to keep the dispatcher from hanging.
     */
    protected void startApp() {
	Display.getDisplay(this).setCurrent(theGame);

	try {
	    // Start the game in its own thread
	    Thread myThread = new Thread(theGame);
	    myThread.start();
	} catch (Error e) {
	    destroyApp(false);
	    notifyDestroyed();
	}
    }

    /**
     * Respond to a commands issued on any Screen.
     * @param c command object source of action
     * @param d screen object containing actioned item
     */
    public void commandAction(Command c, Displayable d) {
        if (c == restartCmd) {
	    theGame.restart();
        } else if (c == levelCmd) {
	    Item[] levelItem = {
		new Gauge("Level", true, 9, theGame.getLevel())};
	    Form f = new Form("Change Level", levelItem);
	    f.addCommand(OKCmd);
	    f.addCommand(cancelCmd);
	    f.setCommandListener(this);
	    Display.getDisplay(this).setCurrent(f);
        } else if (c == exitCmd) {
	    destroyApp(false);
	    notifyDestroyed();
        } else if (c == startCmd) {
	    theGame.removeCommand(startCmd);
	    theGame.addCommand(restartCmd);
	    theGame.restart();
        } else if (c == OKCmd) {
	    Form f = (Form)d;
	    Gauge g = (Gauge)f.get(0);
	    theGame.setLevel(g.getValue());
	    Display.getDisplay(this).setCurrent(theGame);
        } else if (c == cancelCmd) {
	    Display.getDisplay(this).setCurrent(theGame);
	}
    }

}


