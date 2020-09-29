package example.cubes;

import java.util.Hashtable;

public class ColorTable {
    public static final int black = rgb(0, 0, 0);
    public static final int red = rgb(255, 0, 0);
    public static final int orange = rgb(255, 200, 0);
    public static final int yellow = rgb(255, 255, 0);
    public static final int white = rgb(255, 255, 255);
    public static final int green = rgb(0, 255, 0);
    public static final int blue = rgb(0, 0, 255);
    public static final int lightGray = rgb(192, 192, 192);
    public static final int gray = rgb(128, 128, 128);
    public static final int darkGray = rgb(64, 64, 64);

    public static int[][] color;
    public static final int MAX_SHADES = 32;
    private static Hashtable hashtable;

    static {
        int red_f, green_f, blue_f, i, j;

        int[] base = {
            blue,
            gray,
            red,
            yellow
        };

        hashtable = new Hashtable();
        for (i=0; i<base.length; i++) {
            hashtable.put(new Integer(base[i]), new Integer(i));
        }

        color = new int[base.length][32];

        for (i=0; i<base.length; i++) {
            red_f   = red(base[i]) / MAX_SHADES;
            green_f = green(base[i]) / MAX_SHADES;
            blue_f  = blue(base[i]) / MAX_SHADES;

            for (j=0; j<MAX_SHADES; j++) {
                color[i][j] = rgb(
                    red_f * j,
                    green_f * j,
                    blue_f * j
                );

                //if (j % 8 == 0) System.out.println();
                //System.out.print("0x" + pad(((red_f * j) << 16) | ((green_f * j) << 8) | (blue_f * j)) + ", ");
           }
        }
    }

    private static String pad(int n) {
        String hex = Integer.toHexString(n);
        return "0000000".substring(0, 7 - hex.length()) + hex;
    }

    private static final int rgb(int r, int g, int b) {
        return ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
    }

    private static final int red(int color) {
        return (color >> 16) & 0xff;
    }

    private static final int green(int color) {
        return (color >> 8) & 0xff;
    }

    private static final int blue(int color) {
        return color & 0xff;
    }

    public static int getIndexForColor(int c) {
        Integer n = (Integer)hashtable.get(new Integer(c));

        if (n == null) return -1;
        else return n.intValue();
    }
}
