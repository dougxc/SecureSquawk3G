package javax.microedition.rms;

class RecordStoreFile extends java.io.RandomAccessFile {
	RecordStoreFile(String filename) throws java.io.IOException {
	    super(filename, "rw");
	}

	public static boolean exists(String filename) {
	    java.io.File file = new java.io.File(filename);
	    return file.exists();
	}

    public static boolean deleteFile(String filename) {
        java.io.File file = new java.io.File(filename);
        return file.delete();
    }

    public static int spaceAvailable() {
        return 1000000;
    }

    public void truncate(int n) throws java.io.IOException {
        setLength(n);
    }

    public static String[] listRecordStores() {
        return null;
    }
}
