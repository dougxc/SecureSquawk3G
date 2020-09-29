

package awtcore.impl.squawk;

import java.awt.*;
import java.util.Vector;
import java.awt.image.ImageObserver;
import com.sun.squawk.vm.ChannelConstants;

public class GraphicsImpl extends Graphics {

    static Graphics current;
    static boolean dirty;

    private int orgX = 0;
    private int orgY = 0;

    Color color = Color.black;

    FontMetricsImpl fontMetrics = FontMetricsImpl.defaultFontMetrics;
    int clipX;
    int clipY;
    int clipW;
    int clipH;

    public GraphicsImpl() {
        clipW = ToolkitImpl.screenWidth();
        clipH = ToolkitImpl.screenHeight();
    }


    public Graphics create() {
        GraphicsImpl g = new GraphicsImpl();
        g.orgX  = orgX;
        g.orgY  = orgY;
        g.clipX = clipX;
        g.clipY = clipY;
        g.clipW = clipW;
        g.clipH = clipH;
        g.color = color;
        return g;
    }

    private static int lastColor = -1;
    private static int lastFont  = -1;

    void checkContext() {
        dirty = true;
        if (current != this) {
            setClip0(clipX, clipY, clipW, clipH);

            if (lastColor != color.getRGB()) {
                lastColor = color.getRGB();
                setColor0(lastColor);
            }

            if (lastFont != fontMetrics.fontIndex) {
                lastFont = fontMetrics.fontIndex;
                setFont0(lastFont);
            }

            current = this;
        }
    }

    public void clipRect(int x, int y, int w, int h) {
//prtn("***clipRect "+x+":"+y+":"+w+":"+h);
//if(w <= 0) throw new RuntimeException();
        current = null; // force setDrawRegion

        x += orgX;
        y += orgY;

        if (x > clipX) {
            clipW -= x - clipX;
            clipX = x;
        }

        if (y > clipY) {
            clipH -= y - clipY;
            clipY = y;
        }

        if (x + w < clipX + clipW)
            clipW = x + w - clipX;

        if (y + h < clipY + clipH)
            clipH = y + h - clipY;
    }


    public void setClip(int x, int y, int w, int h) {
//prtn("***setClip "+x+":"+y+":"+w+":"+h);
        current = null;
        clipX = x + orgX;
        clipY = y + orgY;
        clipW = w;
        clipH = h;
    }


    public void clearRect(int x, int y, int w, int h) {
        checkContext();
        setColor0(0x0ffffff);
        fillRect0(orgX+x, orgY+y, w, h);
        setColor0(color.getRGB ());
    }


    public boolean drawImage(Image image, int x, int y, ImageObserver observer) {
        checkContext ();
        ImageImpl imageImpl = (ImageImpl)image;
        drawImage0(imageImpl.imageIndex, x+orgX, y+orgY);
        return true;
    }


    public void dispose() {
    }


    public void drawLine(int x1, int y1, int x2, int y2) {
        checkContext();
        drawLine0(x1+orgX, y1+orgY, x2+orgX, y2+orgY);
    }


    public void drawPolyline(int[] xPoints, int[] yPoints, int nPoints) {
        checkContext();
        for (int i = 0; i < nPoints - 1; i++) {
            drawLine0(xPoints[i]+orgX, yPoints[i]+orgY, xPoints[i+1]+orgX, yPoints[i+1]+orgY);
        }
    }


    public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
        checkContext();
        drawRoundRect0(orgX+x, orgY+y, width, height, arcWidth, arcHeight);
    }


    public void drawString(String text, int x, int y) {
        checkContext();
        drawString0(text, orgX+x, orgY+y);
    }

    public void drawOval(int x, int y, int w, int h) {
        checkContext();
        drawOval0(x+orgX, y+orgY, w, h);
    }


    public void drawRect(int x,  int y, int width, int height) {
        checkContext();
        drawRect0(x+orgX, y+orgY, width, height);
    }


    public void fillRect (int x, int y, int width, int height) {
        checkContext();
        fillRect0(orgX+x, orgY+y, width, height);
    }


    public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
        checkContext();
        fillRoundRect0(orgX+x, orgY+y, width, height, arcWidth, arcHeight);
    }

    public Color getColor() {
        return color;
    }


    public Font getFont() {
        return fontMetrics.getFont();
    }

    public Rectangle getClipBounds() {
        return new Rectangle (clipX-orgX, clipY-orgY, clipW, clipH);
    }

    public Rectangle getClipBounds(Rectangle r) {
        r.x      = clipX - orgX;
        r.y      = clipY - orgY;
        r.width  = clipW;
        r.height = clipH;
        return r;
    }

    public boolean hitClip(int x, int y, int w, int h) {
        x += orgX;
        y += orgY;
        return ((clipX <= x + w) && (clipX + clipW >= x) && (clipY <= y + h) && (clipY + clipH >= y));
    }

    public FontMetrics getFontMetrics() {
        return fontMetrics;
    }

    public FontMetrics getFontMetrics(Font font) {
        return FontMetricsImpl.create(font);
    }

    public void setColor(Color c) {
        color = c;
        setColor0(color.getRGB());
    }

    public void setFont(Font font) {
        fontMetrics = FontMetricsImpl.create(font);
        setFont0(fontMetrics.fontIndex);
    }

    public void setPaintMode() {
    }

    public void setXORMode(Color c2) {
    }

    public void translate(int x, int y) {
        orgX += x;
        orgY += y;
    }

    public void fillArc(int x, int y, int w, int h, int ba, int ea) {
        checkContext();
        fillArc0(orgX+x, orgY+y, w, h, ba, ea);
    }

    public void fillPolygon(int[] x, int[] y, int count) {
        checkContext();
        fillPolygon0(x, y, count);
    }





    private void setFont0(int fontIndex) {
        VM.execGraphicsIO(ChannelConstants.SETFONT, fontIndex, 0, 0, 0, 0, 0, null, null);
    }
    private void setColor0(int rgb) {
        VM.execGraphicsIO(ChannelConstants.SETCOLOR, rgb, 0, 0, 0, 0, 0, null, null);
    }
    private void setClip0(int x, int y, int width, int height) {
        VM.execGraphicsIO(ChannelConstants.SETCLIP, x, y, width, height, 0, 0, null, null);
    }
    private void drawString0(String text, int x, int y) {
        VM.execGraphicsIO(ChannelConstants.DRAWSTRING, x, y, 0, 0, 0, 0, text, null);
    }
    private void drawLine0(int x1, int y1, int x2, int y2) {
        VM.execGraphicsIO(ChannelConstants.DRAWLINE, x1, y1, x2, y2, 0, 0, null, null);
    }
    private void drawOval0(int x, int y, int width, int height) {
        VM.execGraphicsIO(ChannelConstants.DRAWOVAL, x, y, width, height, 0, 0, null, null);
    }
    private void drawRect0(int x,  int y, int width, int height) {
        VM.execGraphicsIO(ChannelConstants.DRAWRECT, x, y, width, height, 0, 0, null, null);
    }
    private void fillRect0(int x, int y, int width, int height) {
        VM.execGraphicsIO(ChannelConstants.FILLRECT, x, y, width, height, 0, 0, null, null);
    }
    private void drawRoundRect0(int x, int y, int width, int height, int arcWidth, int arcHeight) {
        VM.execGraphicsIO(ChannelConstants.DRAWROUNDRECT, x, y, width, height, arcWidth, arcHeight, null, null);
    }
    private void fillRoundRect0(int x, int y, int width, int height, int arcWidth, int arcHeight) {
        VM.execGraphicsIO(ChannelConstants.FILLROUNDRECT, x, y, width, height, arcWidth, arcHeight, null, null);
    }
    private void drawImage0(int imageIndex, int x, int y) {
        VM.execGraphicsIO(ChannelConstants.DRAWIMAGE, imageIndex, x, y, 0, 0, 0, null, null);
    }
    private void fillArc0(int x, int y, int width, int height, int ba, int ea) {
        VM.execGraphicsIO(ChannelConstants.FILLARC, x, y, width, height, ba, ea, null, null);
    }
    private void fillPolygon0(int[] x, int[] y, int count) {
        int[] comb = new int[x.length + y.length];
        for (int i = 0 ; i < x.length ; i++) {
            comb[i] = x[i];
        }
        for (int i = 0 ; i < y.length ; i++) {
            comb[x.length+i] = y[i];
        }
        VM.execGraphicsIO(ChannelConstants.FILLPOLYGON, count, 0, 0, 0, 0, 0, comb, null);
    }
}






