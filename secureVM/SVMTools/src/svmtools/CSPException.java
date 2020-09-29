package svmtools;

/**
 * The exception class used when an error occurs in a CSP implementation.
 */
public class CSPException extends Exception {

    public CSPException() {
    }
    public CSPException(String msg) {
        super(msg);
    }
}
