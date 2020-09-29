/*
 * @(#)TilePuzzle.java	1.9	01/08/22
 * Copyright (c) 2000-2001 Sun Microsystems, Inc. All Rights Reserved.
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

package example.tilepuzzle;

import javax.microedition.midlet.MIDlet;
import javax.microedition.lcdui.Display;

public class TilePuzzle extends MIDlet {

	Board b;

	public TilePuzzle() {
		b = new Board(this);
	}

	public void startApp() {
		Display.getDisplay(this).setCurrent(b);
	}

	public void pauseApp() { }

	public void destroyApp(boolean unc) { }
		
}
