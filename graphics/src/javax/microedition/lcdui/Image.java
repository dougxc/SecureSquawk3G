package javax.microedition.lcdui;

public class Image {
    private static java.awt.Toolkit toolkit = java.awt.Toolkit.getDefaultToolkit();
   // private static java.awt.MediaTracker tracker
   //     = new java.awt.MediaTracker(mojo.Main.getScreen());
    java.awt.Image proxy;
    private Graphics graphics;
    private static java.util.zip.CRC32 crc = new java.util.zip.CRC32();
    private static final byte[] png_sig = {
        (byte)137, (byte)80, (byte)78,
        (byte)71, (byte)13, (byte)10,
        (byte)26, (byte)10
    };

    Image() {
    }

    private synchronized static void waitForImage(java.awt.Image img) {
     //   tracker.addImage(img, 0);
     //   try {tracker.waitForID(0);}
     //   catch (InterruptedException e) {e.printStackTrace();}
    }
/*
    public static Image createImage(String path) throws java.io.IOException {
        Image img = new Image();
        img.proxy = toolkit.createImage(path);
        waitForImage(img.proxy);
        System.out.println("Image.createImage(path): " + img.proxy);
        System.out.println("width = " + img.proxy.getWidth(null));
        System.out.println("height = " + img.proxy.getHeight(null));
        img.graphics = new Graphics();
        // img.graphics.setGraphicsImpl(img.proxy.getGraphics());
        return img;
    }
*/
    private static int getInteger(byte[] data, int index) {
        int n = 0;

        for (int i=0; i<4; i++) {
            n <<= 8;
            n |= (data[index++] & 0xff);
        }

        return n;
    }

    private static void putInteger(byte[] data, int index, int n) {
        for (int i=3; i>=0; i--) {
            data[index + i] = (byte)(n & 0xff);
            n >>= 8;
        }
    }

    // Major hack to accomodate midp 1.x brokenness wrt transparency.
    // Since midp does not support transparency, and we want the images
    // in mojo to look exactly like they do in midp, we need to remove
    // all transparency from the image data before it is passed to
    // j2se's createImage(). We iterate through all png chunks looking
    // for 'tRNS' chunk types. If we find one, we set the opacity value
    // for each palette index to 255. We also recalculate the checksum
    // for each changed block.
    private static void removeTransparency(byte[] data, int offset, int length) {
        int i, pos, data_length;
        String chunk_type;

        // make sure we're dealing with a png image
        if (length < png_sig.length) return;
        for (i=0; i< png_sig.length; i++) {
            if (data[offset + i] != png_sig[i]) return;
        }

        pos = offset + png_sig.length;

        for (;;) {
            data_length = getInteger(data, pos);
            chunk_type = new String(data, pos + 4, 4);

            if (chunk_type.equals("tRNS")) {
                for (i=0; i<data_length; i++) {
                    data[pos + i + 8] = (byte)255;
                }

                crc.reset();
                crc.update(data, pos + 4, 4 + data_length);
                putInteger(data, pos + 8 + data_length, (int)crc.getValue());
            }

            if (chunk_type.equals("IEND")) {
                break;
            }

            pos += 12 + data_length;
        }
    }

    public static Image createImage(byte[] data, int offset, int length) {
//throw new RuntimeException();

//System.out.println("----------------------------------------------");
//        for (int i = 0 ; i < length ; i++) {
//System.out.print(data[i+offset]+" ");
///        }
//System.out.println();




        Image img = new Image();

        removeTransparency(data, offset, length);

        img.proxy = toolkit.createImage(data, offset, length);
        waitForImage(img.proxy);

        img.graphics = new Graphics();
        //img.graphics.setGraphicsImpl(img.proxy.getGraphics());
        return img;

    }

    public static Image createImage(int x, int y) {
throw new RuntimeException();
/*
        Image img = new Image();
        img.proxy = mojo.Main.getScreen().createImage(x, y);
        waitForImage(img.proxy);

        // midp expects double buffer images to
        // have all pixels set to white
        java.awt.Graphics g = img.proxy.getGraphics();
        g.setColor(java.awt.Color.white);
        g.fillRect(0, 0, img.proxy.getWidth(null), img.proxy.getHeight(null));

        img.graphics = new Graphics();
        img.graphics.setGraphicsImpl(g);
        return img;
*/
    }

    public int getWidth() {
        return proxy.getWidth(null);
    }

    public int getHeight() {
        return proxy.getHeight(null);
    }

    public Graphics getGraphics() {
        return graphics;
    }
}
