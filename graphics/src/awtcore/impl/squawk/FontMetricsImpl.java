
package awtcore.impl.squawk;

import java.awt.*;
import com.sun.squawk.vm.ChannelConstants;

public class FontMetricsImpl extends FontMetrics {

    static FontMetricsImpl defaultFontMetrics = FontMetricsImpl.create(new Font("plain", Font.PLAIN, 8));

    int fontIndex;

    static FontMetricsImpl create(Font font) {
        return new FontMetricsImpl(font);
    }

    private FontMetricsImpl(Font font) {
        super(font);
        fontIndex = createFontMetrics(font.getSize(), font.isBold() ? 1 : 0);
//if (font.getSize() == 0) throw new RuntimeException();
    }

    public int stringWidth(String s) {
        return fontStringWidth(fontIndex, s);
    }

    public int getHeight() {
        return fontGetHeight(fontIndex);
    }

    public int getAscent() {
        return fontGetAscent(fontIndex);
    }

    public int getDescent() {
        return fontGetDescent(fontIndex);
    }

    private int createFontMetrics(int size, int isBold) {
        return (int)VM.execGraphicsIO(ChannelConstants.CREATEFONTMETRICS, size, isBold, 0, 0, 0, 0, null, null);
    }
    private int fontStringWidth(int font, String string) {
        return (int)VM.execGraphicsIO(ChannelConstants.FONTSTRINGWIDTH, font, 0, 0, 0, 0, 0, string, null);
    }
    private int fontGetHeight(int font) {
        return (int)VM.execGraphicsIO(ChannelConstants.FONTGETHEIGHT, font, 0, 0, 0, 0, 0, null, null);
    }
    private int fontGetAscent(int font) {
        return (int)VM.execGraphicsIO(ChannelConstants.FONTGETASCENT, font, 0, 0, 0, 0, 0, null, null);
    }
    private int fontGetDescent(int font) {
        return (int)VM.execGraphicsIO(ChannelConstants.FONTGETDESCENT, font, 0, 0, 0, 0, 0, null, null);
    }

}
