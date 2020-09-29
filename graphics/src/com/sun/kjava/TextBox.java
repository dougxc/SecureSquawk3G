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

import java.lang.Integer;

import java.util.Vector;

/**
 * A box displaying text on the screen. This class flows the
 * text in the box. It doesn't break words, and therefore isn't
 * graceful handling words larger than the width of the box.
 */
public class TextBox {

    private final int WIDTHOFFSET = 5; 

    protected String text;
    protected IntVector lineStarts = new IntVector();
    protected IntVector lineEnds = new IntVector();
    protected int xPos, yPos, width, height;
    protected Graphics g = Graphics.getGraphics();
    protected static int widthM = Graphics.getWidth("e");
    protected static int heightM = Graphics.getHeight("E");

    /**
     * Create a new TextBox object.
     */
    public TextBox() {    
    }

    /**
     * Create a new TextBox object.
     *
     * @param t the initial text
     * @param x the X coordinate of the ScrollTextBox's position
     * @param y the Y coordinate of the ScrollTextBox's position
     * @param w the width
     * @param h the height
     */
    public TextBox(String t, int x, int y, int w, int h) {

        xPos = x;
        yPos = y;
        width = w;
        height = h;
        text = t;
        if (text != null) {
            computeLineBreaks();
        }
    }

    /**
     * How many lines of text does the TextBox currently hold?
     *
     * @return the number of lines of text contained
     */
    public int getNumLines() {
        return lineStarts.size();
    }

    /**
     * Set the text. You need to call paint() on the TextBox to
     * get the new text displayed.
     *
     * @param t a String representing the new text.
     */
    public void setText(String t) {

        text = t;
        lineStarts.removeAllElements();
        lineEnds.removeAllElements();
        if(text!=null) {
            computeLineBreaks();
        }
    }

    /**
     * Gets the text entered into the textbox
     *
     * @return String containing the user's entry
     */
    public String getText() {
        return text;
    }

    /**
     * Reset the display bounds of the TextBox.
     *
     * @param x the new X coordinate of the ScrollTextBox's position
     * @param y the new Y coordinate of the ScrollTextBox's position
     * @param w the new width
     * @param h the new height
     */
    public void setBounds(int x, int y, int w, int h) {

        xPos = x;
        yPos = y;
        width = w;
        height = h;
        setText(text);
    }

    private void computeLineBreaks() {

        int start = 0;
        int lastSpace = 0;
        int pos = 0;
        int lastSpaceSave;
        char ch;
        int strWidth;

        // This is (hopefully) a faster strategy than computeLineBreaks.
        // Instead of computing character widths for every character
        // we just do it on each ' ', '\n' or '\r' in the string. 

        while( pos < text.length()) {
            
            ch = text.charAt(pos);

            if(ch == ' ' || ch == '\n' || ch == '\r' ) {
                strWidth = g.getWidth(text.substring( start, pos ));
                if(ch == ' ' ) {
                    lastSpaceSave = lastSpace;
                    lastSpace = pos;
                    strWidth = g.getWidth(text.substring( start, lastSpace ));

                    if ( strWidth >= width ) {
                        strWidth = 
                            g.getWidth(text.substring( start, lastSpaceSave ));
                        if ( strWidth <= width ) {
                            lineStarts.append( start );
                            lineEnds.append( lastSpaceSave );
                            start = lastSpaceSave;
                        }
                    } 
                } else if (( ch == '\n' || ch == '\r' ) && pos >= start ) {
                    // break the line
                    if ( strWidth >= width ) {
                        start = handleSpaces( start, lastSpace );
                        start = breakLongLine( start, pos );
                    } else {
                        lineStarts.append( start );
                        lineEnds.append( pos );
                        start = pos + 1;
                    }
                    lastSpace = pos + 1;
                }

                if ( strWidth > width ) {
                    start = handleSpaces( start, lastSpace );
                } else if ( strWidth == width ) {
                    lineStarts.append( start );
                    lineEnds.append( pos );
                    start = pos + 1;
                    lastSpace = pos + 1;
                }
            }
            pos++;
        }

        // there may not have been a space in the end of the string.
        // we need to check if we have to break the last line.

        strWidth = g.getWidth(text.substring( start, pos ));

        if ( strWidth > width ) {
            start = handleSpaces( start, lastSpace );
            start = breakLongLine( start, pos );
            pos = text.length();
        }
        lineStarts.append( start );
        lineEnds.append( pos );
    }

    private int handleSpaces( int start, int lastSpace ) {

        int strWidth;

        if ( lastSpace != 0 ) {
            strWidth = g.getWidth( text.substring( start, lastSpace ));
            if ( strWidth > width ) {
                return breakLongLine( start, lastSpace );
            } else {
                lineStarts.append( start );
                lineEnds.append( lastSpace );
                return lastSpace;
            }
        }
        return start;
    }

    /* 
     * Doesn't care about spaces just breaks a long line
     * Returns the new starting point after the lines are broken
     */
    private int breakLongLine( int start, int pos ) {

        int numTimes;
        int tmpPos;
        int tmpStart;
        int strWidth;

        // Calculate how many times the line can be broken

        strWidth = g.getWidth(text.substring( start, pos ));
        numTimes = strWidth/width;

        // Set the tmpPos to start so that counting character
        // will start counting upwards and strWidth is set to 0.
        // When the strWidth is near the width break the line
        // Not the most efficient method

        tmpStart = tmpPos = start;
        strWidth = 0;

        for ( int i = 1; i <= numTimes; i++ ) {
            while ( strWidth <= ( width * i )) {
                if (( strWidth > ( width * i ) - WIDTHOFFSET ) && 
                    ( strWidth <= ( width * i ) + WIDTHOFFSET )) {
                    lineStarts.append( tmpStart );
                    lineEnds.append( tmpPos );
                    tmpStart = tmpPos;
                    break;
                }
                strWidth = g.getWidth(text.substring( start, ++tmpPos ));
            }
        }
        return tmpStart;
    }

    /**
     * Paint the TextBox on the screen.
     */
    public void paint() {

        g.clearScreen();
        g.drawBorder( xPos, yPos, 
                      width, height, 
                      Graphics.PLAIN, Graphics.SIMPLE );
        g.setDrawRegion(xPos, yPos, width, height);
        
        int x = xPos;
        int y = yPos;
        int first = 0;
        int last = 0;
        for(int i=0; i<lineStarts.size(); i++) {
            first = lineStarts.valueAt(i);
            last =  lineEnds.valueAt(i);
            if(first != last) {
                if(text.charAt(first) == ' ') {
                    first++;
                }
                g.drawString(text.substring(first, last), x, y);
            } else {
                // Don't draw the line if first and last are the same
                continue;
            }
            y += heightM;
        }
        g.resetDrawRegion();
    }
}
