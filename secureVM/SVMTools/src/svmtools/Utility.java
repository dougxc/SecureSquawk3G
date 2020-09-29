package svmtools;

import java.io.*;
import java.util.*;

/**
 * This class provides general use functions.
 */

public class Utility {

    /**
     * Handle displaying of an error message.
     * @param msg The error message to display.
     * @param exit Call System.exit if true after displaying the message.
     */
    static public void error(String msg, boolean exit) {
        System.err.println(msg);
        if (exit)
            System.exit(1);
    }
    static public void error(String msg) {
        error(msg,false);
    }

    /**
     * Reads user password from given input stream.
     * This method is cut-and-paste directly from sun.security.tools.KeyTool.
     */
    static public char[] readPasswd(InputStream in) throws IOException {
	char[] lineBuffer;
	char[] buf;
	int i;

	buf = lineBuffer = new char[128];

	int room = buf.length;
	int offset = 0;
	int c;

loop:	while (true) {
	    switch (c = in.read()) {
	      case -1:
	      case '\n':
		break loop;

	      case '\r':
		int c2 = in.read();
		if ((c2 != '\n') && (c2 != -1)) {
		    if (!(in instanceof PushbackInputStream)) {
			in = new PushbackInputStream(in);
		    }
		    ((PushbackInputStream)in).unread(c2);
		} else
		    break loop;

	      default:
		if (--room < 0) {
		    buf = new char[offset + 128];
		    room = buf.length - offset - 1;
		    System.arraycopy(lineBuffer, 0, buf, 0, offset);
		    Arrays.fill(lineBuffer, ' ');
		    lineBuffer = buf;
		}
		buf[offset++] = (char) c;
		break;
	    }
	}

	if (offset == 0) {
	    return null;
	}

	char[] ret = new char[offset];
	System.arraycopy(buf, 0, ret, 0, offset);
	Arrays.fill(buf, ' ');

	return ret;
    }

  /**
   * Convert an array of objects to a string.
   * @param arr The array to convert.
   * @param delim The delimiter used to separate the elements of the array.
   */
  static public String toString(Object[] arr, String delim) {
      if (arr == null || arr.length == 0)
          return null;
      StringBuffer buf = new StringBuffer(arr.length * 20);
      for (int i = 0; i != arr.length; i++) {
          buf.append(arr[i]);
          if (i != arr.length - 1)
              buf.append(delim);
      }
      return buf.toString();
  }

}
