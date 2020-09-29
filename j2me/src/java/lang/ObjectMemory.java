package java.lang;

/**
 * An ObjectMemory instance is an immutable wrapper for an object memory and
 * its metadata.
 *
 * @author  Doug Simon
 */
class ObjectMemory {

    /**
     * The hash object memory in canonical form.
     */
    private final int hash;

    /**
     * The URL from which this object memory file was loaded.
     */
    private final String url;

    /**
     * The start address of the object memory.
     */
    private final Address start;

    /**
     * The size (in bytes) of the object memory.
     */
    private final int size;

    /**
     * The root of the serialized graph in the object memory.
     */
    private final Object root;

    /**
     * The direct parent object memory.
     */
    private final ObjectMemory parent;

    /**
     * Constructs a new object memory file.
     */
    public ObjectMemory(Address start, int size, String url, Object root, int hash, ObjectMemory parent) {
        this.start = start;
        this.size = size;
        this.url = url;
        this.root = root;
        this.hash = hash;
        this.parent = parent;
    }

    /**
     * Creates an ObjectMemory that is a wrapper for the bootstrap suite.
     *
     * @return an ObjectMemory that is a wrapper for the bootstrap suite
     */
    static public ObjectMemory createBootstrapObjectMemory(Suite bootstrapSuite) {
        return new ObjectMemory(VM.getRomStart(), VM.getRomEnd().diff(VM.getRomStart()).toInt(), null, bootstrapSuite, VM.getRomHash(), null);
    }

    /**
     * Gets the canonical starting address of this object memory. This address is computed
     * as the sum of the size of all the parent object memories.
     *
     * @return  the canonical starting address of this object memory
     */
    public Address getCanonicalStart() {
        if (parent == null) {
            return Address.zero();
        } else {
            return parent.getCanonicalEnd();
        }
    }

    /**
     * Gets the address one byte past the end of the canonical object memory.
     *
     * @return the address one byte past the end of the canonical object memory
     */
    public Address getCanonicalEnd() {
        return getCanonicalStart().add(size);
    }

    /**
     * Gets the size (in bytes) of the object memory.
     *
     * @return the size of the object memory
     */
    public int getSize() {
        return size;
    }

    /**
     * Gets the root object in this object memory.
     *
     * @return the root object in this object memory
     */
    public Object getRoot() {
        return root;
    }

    /**
     * Gets the direct parent object memory of this object memory.
     *
     * @return the direct parent object memory of this object memory
     */
    public ObjectMemory getParent() {
        return parent;
    }

    /**
     * Gets the number of parents in the chain of parent object memories.
     *
     * @return the number of parents in the chain of parent object memories
     */
    public int getParentCount() {
        if (parent == null) {
            return 0;
        } else {
            return 1 + parent.getParentCount();
        }
    }

    /**
     * Gets the hash of the canonical form of this object memory.
     *
     * @return the hash of the canonical form of this object memory
     */
    public int getHash() {
        return hash;
    }

    /**
     * Gets the URL from which this object memory file was loaded.
     *
     * @return the URL from which this object memory file was loaded
     */
    public String getURL() {
        if (!VM.isHosted() && parent == null) {
            return "file://" + GC.convertCString(VM.getRomFileName());
        } else {
            return url;
        }
    }

    /**
     * Gets the start address of the object memory.
     *
     * @return the address of the object memory
     */
    public Address getStart() {
        if (!VM.isHosted() && parent == null) {
            return VM.getRomStart();
        }
        return start;
    }

    /**
     * Gets the address one byte past the end of the object memory.
     *
     * @return the address one byte past the end of the object memory
     */
    public Address getEnd() {
        return getStart().add(size);
    }

}
