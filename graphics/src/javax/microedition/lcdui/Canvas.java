package javax.microedition.lcdui;

public class Canvas extends Displayable {
    public static final int UP = 1;
    public static final int DOWN = 6;
    public static final int LEFT = 2;
    public static final int RIGHT = 5;
    public static final int FIRE = 8;
    public static final int GAME_A = 9;
    public static final int GAME_B = 10;
    public static final int GAME_C = 11;
    public static final int GAME_D = 12;
    public static final int KEY_NUM0  =  48;
    public static final int KEY_NUM1  =  49;
    public static final int KEY_NUM2  =  50;
    public static final int KEY_NUM3 = 51;
    public static final int KEY_NUM4 = 52;
    public static final int KEY_NUM5 = 53;
    public static final int KEY_NUM6 = 54;
    public static final int KEY_NUM7 = 55;
    public static final int KEY_NUM8 = 56;
    public static final int KEY_NUM9 = 57;
    public static final int KEY_STAR = 42;
    public static final int KEY_POUND = 35;

    protected void hideNotify()  {
    }

    public int getGameAction(int n) {
        int game_action = 0;

        switch (n) {
            case '0': game_action = KEY_NUM0; break;
            case '1': game_action = KEY_NUM1; break;
            case '2': game_action = KEY_NUM2; break;
            case '3': game_action = KEY_NUM3; break;
            case '4': game_action = KEY_NUM4; break;
            case '5': game_action = KEY_NUM5; break;
            case '6': game_action = KEY_NUM6; break;
            case '7': game_action = KEY_NUM7; break;
            case '8': game_action = KEY_NUM8; break;
            case '9': game_action = KEY_NUM9; break;
            case '*': game_action = KEY_STAR; break;
            case '#': game_action = KEY_POUND; break;
            case 0xa: game_action = FIRE; break;
            case KEYCODE_F1: game_action = GAME_A; break;
            case KEYCODE_F2: game_action = GAME_B; break;
            case KEYCODE_F3: game_action = GAME_C; break;
            case KEYCODE_F4: game_action = GAME_D; break;
            case KEYCODE_UP: game_action = UP; break;
            case KEYCODE_DOWN: game_action = DOWN; break;
            case KEYCODE_LEFT: game_action = LEFT; break;
            case KEYCODE_RIGHT: game_action = RIGHT; break;
        }

        return game_action;
    }
}
