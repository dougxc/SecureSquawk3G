package awtcore;

import java.awt.*;

public class WordWrap {

    FontMetrics fm;
    int width;
    int maxWidth;
    String txt;
    int pos;

    public WordWrap (FontMetrics fm, String txt, int width) {

    this.fm = fm;
    this.txt = txt;
    this.width = width;
    }


    /** returns -1 if no text is left */

    public int next () {

    int i = pos;
    int len = txt.length ();

    if (pos >= len) return -1;

    int start = pos;

    while (true) {
        while (i < len && txt.charAt (i) > ' ')
        i++;

        int w = fm.stringWidth (txt.substring (start, i));
        if (pos == start  || w <= width) {
        if (w > maxWidth) maxWidth = w;
        pos = i;
        }

        if (w > width || i >= len || txt.charAt(i) == '\n') break;
        i++;
    }

    return pos >= len ? pos : ++pos;
    }


    public int getMaxWidth () {
    return maxWidth;
    }
}
