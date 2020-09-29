package bench.mpeg;

public class Main {

    public static void main(String[] args) {
        try {
            MPEGMidlet app = new MPEGMidlet();
            app.init();
            app.runMain();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

}





