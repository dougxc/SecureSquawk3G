package javax.microedition.lcdui;

public class TextField extends Item {
    public final static int ANY = 0;
    public final static int EMAILADDR = 1;
    public final static int NUMERIC = 2;
    public final static int PHONENUMBER = 3;
    public final static int URL = 4;
    public final static int PASSWORD = 0x10000;
    public final static int CONSTRAINT_MASK = 0xFFFF;

    public TextField(String label, String text, int maxSize, int constraints) {
        super(label);
    }
}
