package javax.microedition.lcdui;

public class Font {
    public static final int STYLE_PLAIN       = 0;
    public static final int STYLE_BOLD        = 1;
    public static final int STYLE_ITALIC      = 2;
    public static final int STYLE_UNDERLINED  = 4;
    public static final int SIZE_SMALL        = 8;
    public static final int SIZE_MEDIUM       = 0;
    public static final int SIZE_LARGE        = 16;
    public static final int FACE_SYSTEM       = 0;
    public static final int FACE_MONOSPACE    = 32;
    public static final int FACE_PROPORTIONAL = 64;

    private java.awt.Font proxy;
    private java.awt.FontMetrics fm;

    private Font(int face, int style, int size) {
        proxy = new java.awt.Font("TimesRoman", java.awt.Font.PLAIN, 14);
        fm = mojo.Main.getScreen().getFontMetrics(proxy);
    }

    public static Font getFont(int face, int style, int size) {
        return getDefaultFont();
    }

    public static Font getDefaultFont() {
        return new Font(FACE_SYSTEM, STYLE_PLAIN, SIZE_MEDIUM);
    }

    public int getHeight() {
        return fm.getHeight();
    }

    public int charWidth(char c) {
        //return fm.charWidth(c);
        return fm.stringWidth(""+c);
    }

    public int stringWidth(String s) {
        return fm.stringWidth(s);
    }
}
