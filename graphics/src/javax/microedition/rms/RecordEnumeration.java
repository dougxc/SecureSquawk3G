package javax.microedition.rms;

public interface RecordEnumeration {
    public int numRecords();
    public byte[] nextRecord()
	throws InvalidRecordIDException, RecordStoreNotOpenException,
	    RecordStoreException;
    public int nextRecordId() throws InvalidRecordIDException;
    public byte[] previousRecord()
	throws InvalidRecordIDException, RecordStoreNotOpenException,
	    RecordStoreException;
    public int previousRecordId() throws InvalidRecordIDException;
    public boolean hasNextElement();
    public boolean hasPreviousElement();
    public void reset();
    public void rebuild();
    public void keepUpdated(boolean keepUpdated);
    public boolean isKeptUpdated();
    public void destroy();
}
