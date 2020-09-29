package tests;


class App {
    public static void main (String[] args) throws Exception {
        System.out.println("started App");
        Thread.currentThread().getIsolate().hibernate();
        System.out.println("re-awoke App");
    }
}

public class Test1 {

    public static void main (String[] args) {
        if (args.length > 0) {
            String name = args[0];
            System.err.println("About to restart " + name + ".isolate after " + VM.branchCount() + " branches");

            Isolate isolate = null;
            String url = "file://" + name + ".isolate";

            do {
                isolate = Isolate.load(url);
                isolate.unhibernate();
                isolate.join();
                url = save(isolate);
            } while (isolate.isHibernated());
        }

        System.out.println("started Test1");

        if (args.length == 0) {
            String cp = Thread.currentThread().getIsolate().getClassPath();
            Isolate isolate = new Isolate("tests.App", new String[0], cp, null);
            isolate.start();
            isolate.join();

            save(isolate);
        }
    }

    private static String save(Isolate isolate) {
        if (isolate.isHibernated()) {
            try {
                String url = isolate.save();
                System.out.println("saved isolate tests.Test0 to " + url);
                return url;
            } catch (java.io.IOException ioe) {
                System.err.println("I/O error while trying to save isolate: ");
                ioe.printStackTrace();
            }
        }
        return null;
    }

}
