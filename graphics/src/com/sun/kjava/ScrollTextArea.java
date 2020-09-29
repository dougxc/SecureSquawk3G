/*
 * Copyright (c) 1999 Sun Microsystems, Inc., 901 San Antonio Road,
 * Palo Alto, CA 94303, U.S.A.  All Rights Reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating
 * to the technology embodied in this software.  In particular, and
 * without limitation, these intellectual property rights may include
 * one or more U.S. patents, foreign patents, or pending
 * applications.  Sun, Sun Microsystems, the Sun logo, Java, KJava,
 * and all Sun-based and Java-based marks are trademarks or
 * registered trademarks of Sun Microsystems, Inc.  in the United
 * States and other countries.
 *
 * This software is distributed under licenses restricting its use,
 * copying, distribution, and decompilation.  No part of this
 * software may be reproduced in any form by any means without prior
 * written authorization of Sun and its licensors, if any.
 *
 * FEDERAL ACQUISITIONS:  Commercial Software -- Government Users
 * Subject to Standard License Terms and Conditions
 */

package com.sun.kjava;

public class ScrollTextArea extends ScrollTextBox {

    private Caret caret;

    private int cursor;
    private String data;

    private boolean caretVisable;
    private int caretX, caretY;

    private int saveCurVal;
    private int i = 0;

    /**
     * Create a new ScrollTextArea object.
     *
     * @param t the initial text
     * @param x the X coordinate of the ScrollTextArea's position
     * @param y the Y coordinate of the ScrollTextArea's position
     * @param w the width
     * @param h the height
     */
    public ScrollTextArea(String t, int x, int y, int w, int h) {
        super(t, x, y, w, h);
        this.data = t;

        caretX = 0;
        caretY = 0;
        caret = new Caret(150, 0, 0);
        caret.blinking = false;
        caret.start();

        curVal = 0;
        saveCurVal = 0;
        setText(data);
        cursor = data.length();

        setCaret();
        paint();
    }

    /**
     * The pen has gone down at (x, y).  Do the right thing.
     *
     * @param x the X coordinate of the pen position
     * @param y the Y coordinate of the pen position
     */
    public void handlePenDown(int x, int y) {
        if(vsb.contains(x, y)) {
            vsb.handlePenDown(x, y);
        } else {
            setCursor(x, y);
        }
    }

    /**
     * The user pressed a key.  Do the right thing.
     *
     * @param keyCode a code representing the key the user pressed
     */
    public void handleKeyDown(int keyCode) {

        String s = new Character((char) keyCode ).toString();

        if( data == null ) {
            return;
        }

        if( cursor > 0 && data.length() == 0 ) {
            return;
        }


        switch(keyCode) {
            case Spotlet.PAGEUP:
                vsb.handleKeyDown(keyCode);
                break;

            case Spotlet.PAGEDOWN:
                vsb.handleKeyDown(keyCode);
                break;

            // Delete Key
            case 0x7F:
                if(cursor < data.length()) {
                    data = data.substring(0, cursor)+data.substring(cursor+1);
                    setText(data);
                }
                scrollPaint(-1, true);
                break;

            // Backspace Key
            case 0x08:
                if(cursor > 0) {
                    cursor--;
                    data = data.substring(0, cursor)+data.substring(cursor+1);
                    setText(data);

                }
                scrollPaint(-1, true);
                break;

            // All other keys
            default:

                data = data.substring(0, cursor)+s+data.substring(cursor);
                setText(data);

                cursor++;
                scrollPaint(1, true);
                break;
         }
    }


    /**
     * scrollPaint()
     * Determines if the screen needs to be scrolled down or up
     * and paints the screen
     */
    private void scrollPaint(int direction, boolean paintIt) {
         if (!setCaret()) {
             autoScroll(direction);
             setCaret();
         }
         if(paintIt) {
             paint();
         }
    }

    /**
     * autoScroll
     * Scroll the screen either up or down
     */
    private void autoScroll(int direction) {
        vsb.handleKeyDown((direction == 1) ? Spotlet.PAGEDOWN : Spotlet.PAGEUP);
    }

    /**
     * setCaret
     * Set the caret on the screen
     */
    public boolean setCaret() {

        int x = xPos;
        int y = yPos;
        int first = 0;
        int last = 0;
        int numLines = 0;
        int count = 0;

        if (lineStarts.size() > curVal + visibleLines) {
            numLines = curVal + visibleLines;
        } else {
            numLines = lineStarts.size();
        }

        for (int i = curVal; i < numLines; i++) {
            first = lineStarts.valueAt(i);
            last =  lineEnds.valueAt(i);

            if (first != last) {
                if (data.charAt( first ) == ' ') {
                    first++;
                }
                if (first <= cursor && last >= cursor) {
                    x += Graphics.getWidth(data.substring(first, cursor));
                    break;
                }
                y += heightM;
            }
        }


        /*
         * If there is no more room on the bottom return false and
         * scroll down
         */
        if (y + heightM > height - heightM) {
            caretVisable = false;
            return false;
        } else {
            caretVisable = true;
            caretX = x;
            caretY = y;
            return true;
        }
    }

    /**
     * setCursor
     * set the cursor on the screen by determining
     * the closest x and y positions.
     */
    public void setCursor(int x, int y) {

        int first = 0;
        int last = 0;
        int numLines = 0;
        int count = 0;

        int xx = 0;
        int yy = 0;
        x -= xPos;
        y -= yPos;


        if (lineStarts.size() > curVal + visibleLines) {
            numLines = curVal + visibleLines;
        } else {
            numLines = lineStarts.size();
        }

        for (int i = curVal ; i < numLines ; i++) {
            first = lineStarts.valueAt(i);
            last =  lineEnds.valueAt(i);

            if (first != last) {
                if (Math.abs(yy-y) < heightM) {
                    if (data.charAt( first ) == ' ') {
                        first++;
                    }
                    for (int j = first ; i < last ; j++) {
                        xx = Graphics.getWidth(data.substring(first, j));
                        if (Math.abs(xx-x) < 5) {
                            cursor = j;
                            caret.setPosition(x, y);
                            return;
                        }
                    }
                }
                yy += heightM;
            }
        }
    }

    /**
     * paint()
     * Paints the text onto the screen.
     */
    public void paint() {
        super.paint();
        if (caretVisable) {
            caret.setPosition(caretX, caretY);
            caret.blinking = true;
        } else {
            caret.blinking = false;
        }
    }

    /**
     * kill()
     * Stops the caret from blinking.
     */
    public void kill() {
        caret.eraseCaret();
        caret.stop = true;
    }

    /**
     * getText()
     * Returns the text.
     */
    public String getText() {
        return data;
    }
}
