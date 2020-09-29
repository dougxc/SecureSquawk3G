/*
 * @(#)ImageConsumer.java
 *
 * Copyright 1995-1999 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 */

package java.awt.image;


public class MemoryImageSource implements ImageProducer {

    public int hs;
    public int vs;
    public int[] rgb;
    public int stride;

    public void setAnimated(boolean x) {
    }


    public MemoryImageSource  (int hs ,int vs, DirectColorModel c, int[] rgb, int something, int stride) {
        this.hs = hs;
        this.vs = vs;
        this.rgb = rgb;
        this.stride = stride;
    }

}
