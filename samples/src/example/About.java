/*
 * @(#)About.java   1.11 01/08/23
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

package example;

import javax.microedition.lcdui.*;

/**
 * Typical about box with a string and an image.
 * In this case the Sun copyright and logo.
 */

public class About {

    private static String copyright =
        "Copyright 2000-2001 Sun Microsystems Inc. All Rights Reserved.\n"
      + "Sun, Sun Microsystems, the Sun logo, Java, and all Sun-based and"
      + "Java-based marks are trademarks or registered trademarks of Sun "
      + "Microsystems, Inc. in the United States and other countries.\n"
      + "\n"
      + "This product meets the Mobile Information Device Profile "
      + "specification, containing tested and compliant "
      + "Mobile Information Device Profile "
      + "software from Sun Microsystems, Inc.\n"
      + "\n"
      + "Use is subject to license terms and limited to demonstration only. "
      + "Sun Microsystems, Inc.  has intellectual property rights relating "
      + "to the technology embodied in this software.  In particular, and "
      + "without limitation, these intellectual property rights may include "
      + "one or more U.S. patents, foreign patents, or pending applications.\n"
      + "U.S. Government approval required when exporting the product.\n"
      + "\n"
      + "FEDERAL ACQUISITIONS\n"
      + "Commercial Software -- Government Users Subject to Standard "
      + "License Terms and Conditions.";

    private Displayable previous; // the previous screen to go back to

    private About() {};     // no instances

    /**
     * Put up the About box and when the use click ok return
     * to the previous screen.
     */
    public static void showAbout(Display display) {

    Alert alert = new Alert("About MIDP");
    alert.setTimeout(Alert.FOREVER);
/*
    if (display.numColors() > 2) {
        String icon = (display.isColor()) ?
        "/icons/JavaPowered-8.png" : "/icons/JavaPowered-2.png";

        try {
            Image image = Image.createImage(icon);
        alert.setImage(image);
        } catch (java.io.IOException x) {
        // just don't append the image.
        }
    }
*/
    // Add the copyright
    alert.setString(copyright);

    display.setCurrent(alert);
    }

}
