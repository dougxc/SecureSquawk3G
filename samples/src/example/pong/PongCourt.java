/*
 * @(#)PongCourt.java	1.7 01/08/21
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

package example.pong;

import javax.microedition.lcdui.*;

/*
 * PongCourt is a Canvas that controls and displays the game.
 */
public class PongCourt extends javax.microedition.lcdui.Canvas implements
CommandListener, Runnable {

    /* Most balls that can be on the court a one time */
    private static final int MAX_BALLS = 5;

    private static final int MAX_DELAY = 100;
    private static final int MIN_DELAY = 0;
    private static final int DEFAULT_DELAY = 50;

    /* the instead of GUI buttons use Commands */
    private Command startCommand = new Command("Start",
                                               Command.SCREEN, 1);
    private Command stopCommand = new Command("Stop",
                                               Command.SCREEN, 1);
    private Command fasterCommand = new Command("Faster", Command.SCREEN, 101);
    private Command slowerCommand = new Command("Slower", Command.SCREEN, 102);
    private Command moreCommand = new Command("More", Command.SCREEN, 103);
    private Command fewerCommand = new Command("Fewer", Command.SCREEN, 104);
    private Command largerCommand = new Command("Larger", Command.SCREEN, 105);
    private Command smallerCommand = new Command("Smaller",
                                                 Command.SCREEN, 106);

    /*
     * we want to listen for commands here and let the midlet listen,
     * so we save the midlet's listener here and forward any commands
     * we do not handle
     */
    private CommandListener commandListener;

    /*
     * control the speed of all balls
     *   delay is in milliseconds
     */
    private int delay = DEFAULT_DELAY;

    /* the count of hits on the paddle */
    private int score = 0;

    private int width;
    private int height;

    /* the game starts with one ball, and the user can request more */
    private int ballsRequested = 1;

    /* we need to know the total number of balls in play when restarting. */
    private int ballsInPlay = 0;

    /* we need to know when the user stopped, the game. */
    private boolean stopped = true;

    /* we need to know when the system paused, the game. */
    private boolean paused = true;

    private PongBall[] balls;
    private PongPaddle paddle;

    public PongCourt(Display d) {
        width = getWidth();
        height = getHeight() - Font.getDefaultFont().getHeight();

        /* account for the court border */
        paddle = new PongPaddle(0, 1, height - 2);

        /* account for the border and paddle thickness */
        balls = new PongBall[MAX_BALLS];
        
        for (int i = 0; i < balls.length; i++) {
            balls[i] = new PongBall(this, PongPaddle.THICKNESS, 1,
                            width - PongPaddle.THICKNESS - 1, height - 2);
        }

        ballsInPlay = 1;
        balls[0].putInPlay();

        /*
         * the stop command will be added when start is selected and the
         * start command removed.
         */
        addCommand(startCommand);
        addCommand(fasterCommand);
        addCommand(slowerCommand);
        addCommand(moreCommand);
        addCommand(fewerCommand);
        addCommand(largerCommand);
        addCommand(smallerCommand);
        super.setCommandListener(this);
    }

    public void setCommandListener(CommandListener l) {
        commandListener = l;
    }

    public void run() {
        paused = false;

        /* loop while the thread is active and the ball is in play */
        while (!paused) {
            repaint();

            /* use the delay to control the speed of the ball */
            try {
                Thread.currentThread().sleep(delay);
            } catch (InterruptedException e) {}

            for (int i = 0; i < balls.length; i++) {
                balls[i].move();
            }
        }
    }


    /*
     * isPaddleHit determines if ball hit the paddle assuming the ball is
     * at the left side of the court. It also keeps changes the score and
     * number of balls left.
     */
    boolean isPaddleHit(int ballYPos, int ballHeight) {
        if (paddle.isHit(ballYPos, ballHeight)) {
            score++;
            return true;
        }

        ballsInPlay--;
        if (ballsInPlay == 0) {
            stopGame();
        }

        return false;
    }


    /*
     * Start creates a thread for the ball and starts it.
     */
    void start() {
        Thread gameThread;

        /* do not resume the thread if the game was stopped by the user */ 
        if (stopped) {
            return;
        }

        gameThread = new Thread(this);
        gameThread.start();
    }


    /*
     * Pause the game thread by signaling it to stop.
     * If any ball objects are still in play they hold their current position
     * so they may be restarted later.
     */
    void pause() {
        paused = true;
    }


    void resetGame() {
        score = 0;

        for (int i = 0; i < ballsRequested; i++) {
            balls[i].putInPlay();
            ballsInPlay++;
        }
    }

    
    /*
     * starts the game, resume where the player stopped if balls are in play
     */
    void startGame() {
        if (!stopped) {
            return;
        }

        removeCommand(startCommand);
        addCommand(stopCommand);

        if (ballsInPlay == 0) {
            resetGame();
        }

        stopped = false;
        start();
    }


    /*
     * Lets the player stop the game to be restarted later.
     */
    void stopGame() {
        removeCommand(stopCommand);
        addCommand(startCommand);

        stopped = true;
        pause();
    }


    void speedUpGame() {
        paddle.faster();

        delay -= 10;
        if (delay < MIN_DELAY) {
            delay = MIN_DELAY;
        }
    }


    void slowDownGame() {
        paddle.slower();

        delay += 10;
        if (delay > MAX_DELAY) {
            delay = MAX_DELAY;
        }
    }


    void addBall() {
        /* do not give out more balls that we allocated */
        if (ballsRequested >= balls.length) {
            return;
        }

        ballsRequested++;

        /* put the a dead ball in play, if we have a ball already in play */
        if (ballsInPlay > 0) {
            for (int i = 0; i < balls.length; i++) {
                if (!balls[i].inPlay()) {
                    balls[i].putInPlay();
                    ballsInPlay++;

                    /*
                     * if we are stopped, we should repaint to show
                     * the new ball
                     */
                    if (stopped) {
                        repaint();
                    }

                    return;
                }
            }
        }
    }


    void removeBall() {
        /* do not remove the last ball */
        if (ballsRequested <= 1) {
            return;
        }

        ballsRequested--;

        /* remove the last ball put in play */
        for (int i = balls.length - 1; i >= 0; i--) {
            if (balls[i].inPlay()) {
                balls[i].takeOutOfPlay();
                ballsInPlay--;

                /*
                 * if we are stopped, we should repaint to erase
                 * the ball in play
                 */
                if (stopped) {
                    repaint();
                }

                return;
            }
        }
    }


    /*
     * This is called when a command menu is shown
     */
    protected void hideNotify() {
        pause();
    }


    /*
     * This is called after a command menu is shown
     */
    protected void showNotify() {
        start();
    }


    /*
     * Destroy is the same a pause for now.
     */
    void destroy() {
        pause();
    }

    /*
     * Draws the pong court, the score, the ball, and the paddle.
     */
    protected void paint(Graphics g) {
        /* clear the canvas */
        g.setColor(0xffffff);
        g.fillRect(0, 0, getWidth(), getHeight());

        /* Draw the court open on the left side */
        g.setColor(0);
        g.drawLine(0, 0, width - 1, 0);
        g.drawLine(width - 1, 0, width - 1, height - 1);
        g.drawLine(0, height - 1, width - 1, height - 1);

        paddle.paint(g);

        /*
         * To let the court be larger, put the score in the court
         * but let the balls paint over the score
         */
        displayScore(g);

        for (int i = 0; i < balls.length; i++) {
            balls[i].paint(g);
        }
    }


    /*
     * display the score and balls remaining
     */
    private void displayScore(Graphics g) {
        char digits[] = new char[3];

        /* compute the ones digit */
        digits[2] = (char) ((score % 10) + (int) '0');

        /* compute the tens digit */
        digits[1] = (char) (((score / 10) % 10) + (int) '0');

        /* compute the hundreds digit */
        digits[0] = (char) (((score / 100) % 10) + (int) '0');

        /* print the new score */
        g.drawString("Score: " + new String(digits), width - 1, height,
                     Graphics.TOP | Graphics.RIGHT);
    }


    /*
     * Respond to a command issued.
     */
    public void commandAction(Command c, Displayable s) {

        if (c == startCommand) {
            startGame();
            return;
        }

        if (c == stopCommand) {
            stopGame();
            return;
        }

        if (c == fasterCommand) {
            speedUpGame();
            return;
        }

        if (c == slowerCommand) {
            slowDownGame();
            return;
        }

        if (c == moreCommand) {
            addBall();
            return;
        }

        if (c == fewerCommand) {
            removeBall();
            return;
        }

        if (c == largerCommand) {
            paddle.bigger();
            repaint();
            return;
        }

        if (c == smallerCommand) {
            paddle.smaller();
            repaint();
            return;
        }

        /* forward any commands we do not handle */
        if (commandListener == null) {
            return;
        }

        commandListener.commandAction(c, s);
    }

    /*
     * This method will be called when a key is repeated.
     */
    public void keyRepeated(int keyCode) {
        keyPressed(keyCode);
    }


    /*
     * This method will be called when a key is pressed.
     */
    public void keyPressed(int keyCode) {

        int action;

        /* ignore keys when the game is stopped */
        if (stopped) {
            return;
        }

        action = getGameAction(keyCode);

        switch (action) {
        case UP:
            paddle.up();
            repaint();
            break;

        case DOWN:
            paddle.down();
            repaint();
            break;
        }
    }

}
