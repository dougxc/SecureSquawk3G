/*
 *  @(#)Pong.java	1.9 01/08/21
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
package example.pong;
 
import javax.microedition.midlet.*;
import javax.microedition.lcdui.*;
import example.About;

/*
 * The Pong class is in charge of the active state of the Pong game.
 */
public class Pong extends MIDlet {

    static final String DIRECTIONS_TEXT = "Welcome to the Pong game!\n" +
    "The object of Pong is to keep the ball within the box and not " +
    "let them get past your paddle.  Each time you hit the ball(s) back " +
    "you get a point (shown in the lower right hand corner of the " +
    "screen).\nThere options to change the size of the paddle " +
    "and alter the speed at which the balls are bouncing.\n" +
    "Good luck!";


    /*
     * the since Pong is a UI MIDlet, the PongCourt being a canvas is the
     * central part of the game.
     */
    private PongCourt court;

    private Alert directions;

    /*
     * This only initializes the resources, startApp will be called to get
     * things going.
     */
    public Pong() {
        /*
         * Create the PongCourt, which is the canvas for our application
         * then setup the commands.
         */
        court = new PongCourt(Display.getDisplay(this));

        directions = new Alert("Help");
	directions.setTimeout(Alert.FOREVER);
        directions.setString(DIRECTIONS_TEXT);

        /*
         * hookup this MIDlet to the Court so the destroyApp method will
         * get called when the user selects exit command
         */
        ExitHookup.hookup(this, court);
    }


    /*
     * let the game begin or resume
     */
    public void startApp() throws MIDletStateChangeException {
        Display.getDisplay(this).setCurrent(court);
        court.start();
    }
    

    /*
     * the system is telling us to pause
     */
    public void pauseApp() {
        court.pause();
    }

    /*
     * the system is telling us to exit
     */
    public void destroyApp(boolean unconditional) 
        throws MIDletStateChangeException {
        court.destroy();
    }


    /*
     * display some help
     */
    void help() {
        Display.getDisplay(this).setCurrent(directions);
    }
}


class ExitHookup implements CommandListener {

    static void hookup(Pong p, Displayable d) {
        ExitHookup exitHookup = new ExitHookup(p);

        d.addCommand(exitHookup.helpCommand);
        d.addCommand(exitHookup.aboutCommand);
        d.addCommand(exitHookup.exitCommand);
        d.setCommandListener(exitHookup);  
    }


    private Pong parent;

    /* the instead of GUI buttons use Commands */
    private Command helpCommand = new Command("Help", Command.SCREEN, 1001);
    private Command aboutCommand = new Command("About", Command.HELP, 1002);
    private Command exitCommand = new Command("Exit", Command.EXIT, 1003);


    ExitHookup(Pong p) {
        parent = p;
    }

    /*
     * Respond to a command issued by the Canvas.
     */
    public void commandAction(Command c, Displayable s) {

        if (c == helpCommand) {
            parent.help();
            return;
        }
        
        if (c == aboutCommand) {
            About.showAbout(Display.getDisplay(parent));
            return;
        }

        try {
            if (c == exitCommand) {
                parent.destroyApp(false);

                /* tell the system we exited */
                parent.notifyDestroyed();
            }
        } catch (MIDletStateChangeException ex) {}
    }
}
