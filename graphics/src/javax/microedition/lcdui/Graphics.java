package javax.microedition.lcdui;

public class Graphics {
    public static final int HCENTER = 1;
    public static final int VCENTER = 2;
    public static final int LEFT = 4;
    public static final int RIGHT = 8;
    public static final int TOP = 16;
    public static final int BOTTOM = 32;
    public static final int BASELINE = 64;
    public static final int SOLID = 0;
    public static final int DOTTED = 1;

    private static boolean extended = false;
    private java.awt.Graphics proxy;
    //private java.awt.Rectangle clip = new java.awt.Rectangle(0,0,0,0);
    private Font font = Font.getDefaultFont();
    private int translate_x;
    private int translate_y;

    static {
        //if ("true".equals(System.getProperty("mojo.graphics.extended"))) {
            extended = true;
        //}
    }

    Graphics() {
        translate_x = 0;
        translate_y = 0;
    }

    public void setGraphicsImpl(java.awt.Graphics g) {
        proxy = g;
    }

    public void setColor(int color) {
        proxy.setColor(new java.awt.Color(color));
    }

    public int getClipX() {
        //return (int)proxy.getClipBounds(clip).getX();
        return 0;
    }

    public int getClipY() {
        //return (int)proxy.getClipBounds(clip).getY();
        return 0;
    }

    public int getClipWidth() {
        //return (int)proxy.getClipBounds(clip).getWidth();
        return Display.WIDTH;
    }

    public int getClipHeight() {
        //return (int)proxy.getClipBounds(clip).getHeight();
        return Display.HEIGHT;
    }

    public void drawString(String s, int x, int y, int anchor) {
        java.awt.FontMetrics fm = proxy.getFontMetrics();
        int sw = fm.stringWidth(s);
        int sh = proxy.getFont().getSize();

        y += (sh - 1);

        if ((anchor & Graphics.HCENTER) != 0) x -= sw/2;
        if ((anchor & Graphics.RIGHT) != 0) x -= sw;
        if ((anchor & Graphics.VCENTER) != 0) y -= sh/2;
        if ((anchor & Graphics.BOTTOM) != 0) y -= sh;

        proxy.drawString(s, x, y);
    }

    public void fillArc(int x, int y, int w, int h, int ba, int ea) {
        proxy.fillArc(x, y, w, h, ba, ea);
    }

    public void drawRect(int x, int y, int w, int h) {
        proxy.drawRect(x, y, w,h);
    }

    public void fillRect(int x, int y, int w, int h) {
        proxy.fillRect(x, y, w,h);
    }

    public void fillPolygon(int[] x, int[] y, int count) {
        if (!extended) {
            throw new RuntimeException("fillPolygon() not supported int MIDP 1.x");
        } else {
            proxy.fillPolygon(x, y, count);
        }
    }

    public void setClip(int x, int y, int w, int h) {
        if (proxy != null) {
            proxy.setClip(x, y, w,h);
        }
    }

    public void drawImage(Image img, int x, int y, int anchor) {
        if ((anchor & Graphics.HCENTER) != 0) x -= img.getWidth()/2;
        if ((anchor & Graphics.RIGHT) != 0) x -= img.getWidth();
        if ((anchor & Graphics.VCENTER) != 0) y -= img.getHeight()/2;
        if ((anchor & Graphics.BOTTOM) != 0) y -= img.getHeight();

        proxy.drawImage(img.proxy, x, y, null);
    }

    public void drawLine(int x1, int y1, int x2, int y2) {
        proxy.drawLine(x1, y1, x2, y2);
    }

    public void translate(int x, int y) {
        translate_x += x;
        translate_y += y;

        proxy.translate(x, y);
    }

    public int getTranslateX() {
        return translate_x;
    }

    public int getTranslateY() {
        return translate_y;
    }

    public Font getFont() {
        return font;
    }

    public void setFont(Font font) {
        this.font = font;
    }

    public void setStrokeStyle(int style) {
    }
}
