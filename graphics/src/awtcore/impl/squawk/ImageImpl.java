
package awtcore.impl.squawk;

import java.awt.*;
import java.awt.image.*;
import com.sun.squawk.vm.ChannelConstants;

public class ImageImpl extends Image {

    int   imageIndex;
    int[] imageRgb;

    ImageImpl(MemoryImageSource memoryImage) {
        imageIndex = createMemoryImage(
                                        memoryImage.hs,
                                        memoryImage.vs,
                                        memoryImage.rgb.length,
                                        memoryImage.stride
                                      );
        imageRgb = memoryImage.rgb;
    }

    ImageImpl(byte[] data, int offset, int length) {
        imageIndex = createImage(data, offset, length);
    }

    ImageImpl(String ressourceName) {
        imageIndex = getImage(ressourceName);
    }

    public int getWidth (ImageObserver o) {
        return imageWidth(imageIndex);
    }

    public int getHeight (ImageObserver o) {
        return imageHeight(imageIndex);
    }

    public void flush () {
        flush0(imageIndex, imageRgb);
    }


    private int createImage(byte[] data, int offset, int length) {
        return (int)VM.execGraphicsIO(ChannelConstants.CREATEIMAGE, offset, length, 0, 0, 0, 0, data, null);
    }
    private int createMemoryImage(int hs, int vs, int length, int stride) {
        return (int)VM.execGraphicsIO(ChannelConstants.CREATEMEMORYIMAGE, hs, vs, length, stride, 0, 0, null, null);
    }
    private int getImage(String ressourceName) {
        return (int)VM.execGraphicsIO(ChannelConstants.GETIMAGE, 0, 0, 0, 0, 0, 0, ressourceName, null);
    }
    private int imageWidth(int number) {
        return (int)VM.execGraphicsIO(ChannelConstants.IMAGEWIDTH, number, 0, 0, 0, 0, 0, null, null);
    }
    private int imageHeight(int number) {
        return (int)VM.execGraphicsIO(ChannelConstants.IMAGEHEIGHT, number, 0, 0, 0, 0, 0, null, null);
    }
    private void flush0(int number, int[] image) {
        VM.execGraphicsIO(ChannelConstants.FLUSHIMAGE, number, 0, 0, 0, 0, 0, image, null);
    }

}

