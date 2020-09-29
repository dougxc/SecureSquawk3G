/*
 *  @(#)Score.java  1.6 01/08/21
 *  Copyright (c) 2000-2001 Sun Microsystems, Inc., 901 San Antonio Road,
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

import java.io.*;
import javax.microedition.io.*;
import javax.microedition.midlet.*;
import javax.microedition.rms.*;

/**
 * This class keeps track of the current game score,
 * and a high score. The high score is kept in the RMS
 * so that it is persistent across different game invocations.
 */
public class Score {
    private int score;
    private int highScore;
    private int highScoreID;
    private RecordStore recordStore = null;

    /* Create a new instance of this class. */
    public Score() {
        // create/open record store
/**
        try {
            recordStore =
        RecordStore.openRecordStore("SpaceInvaderScores", true);
        } catch (Exception rse) {
            System.out.println("unable to save high score");
        }
**/
        score = 0;
        highScore = 0;
        load();
    }

    /** set score to 0 */
    public void reset() {
        score = 0;
    }

    /** Increment score by one */
    public void add() {
        add(1);
    }

    /** Increment score by x */
    public void add(int x) {
        score += x;

        if (score >= highScore)
        {
        highScore = score;
        save();
        }
    }

    /** Return the current score */
    public int getScore() {
        return score;
    }

    /** Return the high score */
    public int getHighScore() {
        return highScore;
    }

    /** load the hight score from the RMS */
    public void load()
    {
    byte[] data = null;
    if (recordStore == null)
        return;

    try {
        if (recordStore.getNumRecords() == 0) { // no high score record
        highScore = 0;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream das = new DataOutputStream(baos);
        try {
            das.writeInt(highScore);
        } catch (IOException ioe) {
            System.out.println("Can't save highScore "+ioe);
        }

        byte[] b = baos.toByteArray();
        highScoreID = recordStore.addRecord(b, 0, b.length);
        } else {  // there is a record
        highScoreID = recordStore.getNextRecordID();
        if (highScoreID > 0)
            highScoreID--;
        data = recordStore.getRecord(highScoreID);
        if (data != null) {
            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            DataInputStream dis = new DataInputStream(bais);
            highScore = dis.readInt();
        } else {
            highScore = 0;
        }
        } // else
    } catch (Exception rse) {
        System.out.println("unable to load high score "+rse);
    }
    }

    /** save the high score to the RMS */
    public void save() {
        if (recordStore == null)
            return;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream das = new DataOutputStream(baos);
        try {
            das.writeInt(highScore);
        } catch (IOException ioe) {
        System.out.println("Can't save highScore "+ioe);
    }

        byte[] b = baos.toByteArray();

        try {
            recordStore.setRecord(highScoreID, b, 0, b.length);
        } catch (RecordStoreException rse) {
        System.out.println("Unable to save the high score "+rse);
    }

    } // save

} // class Score
