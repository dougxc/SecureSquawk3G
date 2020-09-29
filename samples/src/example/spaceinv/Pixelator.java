/*
 *  @(#)Pixelator.java	1.5 01/08/21
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

import java.util.Hashtable;
import javax.microedition.lcdui.*;
import java.io.*;

public class Pixelator 
{

    private static Hashtable cache;
    static 
    {
        cache = new Hashtable();
    }

    /**
     * createImage
     */
    public static Image createImage(String pattern, int scanline) 
    {
        if (scanline % 8 != 0) 
            throw new IllegalArgumentException();

        Image image = (Image)cache.get(pattern);
        if (image != null) 
            return image;        

	try
        {
	    if (pattern.equals(Engine.ALIEN1_1)) 
		image = Image.createImage(Images.alien_1_1, 0, 
					Images.alien_1_1.length);

	    if (pattern.equals(Engine.ALIEN1_2)) 
		image = Image.createImage(Images.alien_1_2, 0, 
					  Images.alien_1_2.length);

	    if (pattern.equals(Engine.ALIEN2_1)) 
		image = Image.createImage(Images.alien_2_1, 0, 
					  Images.alien_2_1.length);

	    if (pattern.equals(Engine.ALIEN2_2)) 
		image = Image.createImage(Images.alien_2_2, 0, 
					  Images.alien_2_2.length);

	    if (pattern.equals(Engine.ALIEN3_1))
		image = Image.createImage(Images.alien_3_1, 0, 
					  Images.alien_3_1.length);

	    if (pattern.equals(Engine.ALIEN3_2))
		image = Image.createImage(Images.alien_3_2, 0,
					  Images.alien_3_2.length);

	    if (pattern.equals(Engine.BOOM_1))
		image = Image.createImage(Images.boom_1, 0, 
					  Images.boom_1.length);

	    if (pattern.equals(Engine.BOOM_2))
		image = Image.createImage(Images.boom_2, 0, 
					  Images.boom_2.length);

	    if (pattern.equals(Engine.BOOM_3))
		image = Image.createImage(Images.boom_3, 0, 
					  Images.boom_3.length);

	    if (pattern.equals(Engine.UFO_1)) 
		image = Image.createImage(Images.ufo_1, 0, 
					  Images.ufo_1.length);

	    if (pattern.equals(Engine.UFO_2))
		image = Image.createImage(Images.ufo_2, 0, 
					  Images.ufo_2.length);

	    if (pattern.equals(Engine.GUN)) 
		image = Image.createImage(Images.gun, 0, 
					  Images.gun.length);

        } catch (IllegalArgumentException iae)
            {
                System.out.println("Can't create image "+iae);
            }

	if (image == null) 
        {
            System.out.println("Pixelator.createImage(): invalid pattern");
            return null;
        }

        cache.put(pattern, image);

        return image;

    } // createImage()

} // class Pixelator

