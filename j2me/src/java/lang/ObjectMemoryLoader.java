package java.lang;

import java.io.*;
import javax.microedition.io.*;
import com.sun.squawk.vm.HDR;
import com.sun.squawk.util.*;

/**
 * This class facilitates loading a serialized object graph from a URL
 * and relocating it. The expected structure of the input is
 * described by the following pseudo C struct:
 *
 * <p><hr><blockquote><pre>
 *    ObjectMemory {
 *        u4 magic               // 0xDEADBEEF
 *        u2 minor_version;
 *        u2 major_version;
 *        u4 attributes;         // mask of the ATTRIBUTE_* constants in this class
 *        u4 parent_hash;
 *        utf8 parent_url;
 *        u4 root;               // offset (in bytes) in 'memory' of the root of the graph
 *        u4 size;               // size (in bytes) of memory
 *        u1 oopmap[((size / HDR.BYTES_PER_WORD) + 7) / 8];
 *        u1 padding[n];         // 0 <= n < HDR.BYTES_PER_WORD to align 'memory' on a word boundary
 *        u1 memory[size];
 *        u1 typemap[size];      // only present if ATTRIBUTE_TYPEMAP is set
 *    }
 * </pre></blockquote><hr><p>
 *
 * @author Doug Simon
 */
class ObjectMemoryLoader {

    /**
     * Denotes a object memory file that has a type map describing the type of the value at every
     * address in the 'memory' component. The entries in the map are described in
     * {@link com.sun.squawk.vm.AddressType}.
     */
    public static final int ATTRIBUTE_TYPEMAP = 0x01;

    /**
     * Denotes a object memory file that is only compatible with a 32 bit system. Otherwise the object memory
     * file is only compatible with a 64 bit system.
     */
    public static final int ATTRIBUTE_32BIT = 0x02;

    /**
     * An error thrown during relocation to indicate the buffer containing the pointers
     * being relocated has moved due to a garbage collection.
     */
    static class GCDuringRelocationError extends Error {
    }

    /**
     * Calculates the hash of a an array of bytes.
     *
     * @param arr   the byte array to hash
     * @return      the hash of <code>arr</code>
     */
    private static int hash(byte[] arr) {
        int hash = arr.length;
        for (int i = 0; i != arr.length; ++i) {
            hash += arr[i];
        }
        return hash;
    }

    /*---------------------------------------------------------------------------*\
     *                                 Loading                                   *
    \*---------------------------------------------------------------------------*/

    /**
     * The validating reader used to read the components of an object memory from
     * an input stream.
     */
    protected final ObjectMemoryReader reader;

    /**
     * Specifies if the object memory is to be moved to read-only memory once it
     * has been loaded and relocated.
     */
    private final boolean loadIntoReadOnlyMemory;

    /**
     * Constructor.
     *
     * @param reader
     * @param loadIntoReadOnlyMemory
     */
    protected ObjectMemoryLoader(ObjectMemoryReader reader, boolean loadIntoReadOnlyMemory) {
        this.reader = reader;
        this.loadIntoReadOnlyMemory = loadIntoReadOnlyMemory;
    }

    /**
     * Loads an object memory from a given URL. If the given URL corresponds to the
     * URL of an object memory already present in the system, then that object
     * memory is returned instead.
     *
     * @param url                     the URL from which to load the object memory
     * @param loadIntoReadOnlyMemory  specifies if the object memory should be put into read-only memory
     * @return the ObjectMemory instance encapsulating the loaded/resolved object memory
     */
    public static ObjectMemory load(String url, boolean loadIntoReadOnlyMemory) {
        ObjectMemory om = GC.lookupObjectMemoryBySourceURL(url);
        if (om != null) {
            return om;
        }

        int attempt = 0;
        while (attempt < 5) {
            try {
                return load0(url, loadIntoReadOnlyMemory);
            } catch (GCDuringRelocationError e) {
                ++attempt;
                if (VM.isVeryVerbose()) {
                    VM.print("[failed attempt ");
                    VM.print(attempt);
                    VM.print(" to load and relocate memory from ");
                    VM.print(url);
                    VM.println(" - trying again]");
                }
            }
        }
        throw new OutOfMemoryError("while loading object memory from " + url);
    }

    /**
     * The restartable entry point to loading.
     *
     * @param url                     the URL from which to load the object memory
     * @param loadIntoReadOnlyMemory  specifies if the object memory should be put into read-only memory
     * @return the ObjectMemory instance encapsulating the loaded/resolved object memory
     */
    private static ObjectMemory load0(String url, boolean loadIntoReadOnlyMemory) {
        ObjectMemoryReader reader = null;
        try {
            ObjectMemoryLoader loader = null;
            try {
/*if[FLASH_MEMORY]*/
                if (url.startsWith("flash:")) {
                    InputStream input = Connector.openInputStream(url);
                    reader = new FlashObjectMemoryReader(input, url);
                    loader = new FlashObjectMemoryLoader(reader, loadIntoReadOnlyMemory);
                } else
/*end[FLASH_MEMORY]*/
                {
                    DataInputStream dis;
                    // Hack to enable loading of an object memory from a URL expressing a server socket
                    if (url.startsWith("serversocket://")) {
                        StreamConnectionNotifier scn = (StreamConnectionNotifier)Connector.open(url, Connector.READ);
                        dis = scn.acceptAndOpen().openDataInputStream();
                        scn.close();
                    } else {
                        dis = Connector.openDataInputStream(url);
                    }
                    reader = new ObjectMemoryReader(dis, url);
                    loader = new ObjectMemoryLoader(reader, loadIntoReadOnlyMemory);
                }
            } catch (IOException ioe) {
                throw new LinkageError("I/O error while trying to open " + url + ": " + ioe.toString());
            }
            ObjectMemory om = loader.load();

            if (VM.isVerbose()) {
                VM.print("[loaded object memory from '");
                VM.print(url);
                VM.println("']");
            }

            if (loadIntoReadOnlyMemory) {
                GC.addObjectMemory(om);
            }
            return om;
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }

/*if[TYPEMAP]*/
    /**
     * Loads the type map describing the type of every address in an object memory.
     *
     * @param start   the start address of the object memory
     * @param size    the size address of the object memory
     */
    private void loadTypeMap(Address start, int size) {
        Address p = start;
        for (int i = 0; i != size; ++i) {
            byte type = (byte)reader.readByte(null);
            Unsafe.setType(p, type, 1);
            p = p.add(1);
        }
        if (Klass.DEBUG && Tracer.isTracing("oms")) {
            Tracer.traceln("typemap:{size = " + size + "}");
        }
    }
/*end[TYPEMAP]*/

    /**
     * Loads the complete object memory from the input stream.
     */
    private ObjectMemory load() {

        if (Klass.DEBUG && Tracer.isTracing("oms")) {
            Tracer.traceln("Loading object memory from " + reader.getFileName());
        }

        // Load magic
        int magic = reader.readInt("magic");
        if (magic != 0xdeadbeef) {
            throw new LinkageError("invalid magic file identifier: expected 0xdeadbeef, received 0x" + Integer.toHexString(magic));
        }

        // Load and ignore version numbers for now
        reader.readShort("minor_version");
        reader.readShort("major_version");

        // Load attributes
        int attributes = reader.readInt("attributes");
        boolean hasTypemap = (attributes & ATTRIBUTE_TYPEMAP) != 0;
        boolean is32Bit = (attributes & ATTRIBUTE_32BIT) != 0;

        // Load the word size
        if (is32Bit != (HDR.BYTES_PER_WORD == 4)) {
            throw new LinkageError("invalid word size in object memory: expected " +
                                   (is32Bit ? "32 bit" : "64 bit") + ", received " + (is32Bit ? "64 bit" : "32 bit"));
        }

        // Load the parent of this object memory file
        ObjectMemory parent = loadParent();

        // Load the object memory file and relocate the object memory
        ObjectMemory om = loadThis(parent, hasTypemap);

        // Ensure there are no extra bytes at the end of the object memory file
        reader.readEOF();

        // Tracing
        if (Klass.DEBUG && Tracer.isTracing("oms")) {
            Tracer.traceln("Loaded object memory from " + reader.getFileName());
        }

        return om;
    }

    /**
     * An output stream that counts and then discards the bytes written to it.
     */
    static final class OutputStreamSink extends OutputStream {
        private int length;

        public int getLength() {
            return length;
        }

        public void write(int b) throws IOException {
            ++length;
        }
    }

    /**
     * Calculates the number of bytes that are required fro representing a given string
     * in a DataOutputStream of DataInputStream.
     *
     * @param s   the string value to test
     * @return    the number of bytes in the UTF8 representation of <code>s</code>
     */
    static int getUTF8Length(String s) {
        OutputStreamSink oss = new OutputStreamSink();
        DataOutputStream dos = new DataOutputStream(oss);
        try {
            dos.writeUTF(s);
        } catch (IOException ioe) {
            Assert.shouldNotReachHere();
        }
        return oss.getLength();
    }

    /**
     * Calculates the padding that precedes the 'memory' item to ensure that it is word aligned
     * with respect to the start of the object memory file.
     *
     * @param parentURL   the value of the 'parent_url' item in the object memory file
     * @param memorySize  the value of the 'size' item in the object memory file
     */
    static int calculateMemoryPadding(String parentURL, int memorySize) {
        int sizeSoFar = 4 +   // u4 magic
                        2 +   // u2 minor_version
                        2 +   // major_version
                        4 +   // u4 attributes
                        4 +   // u4 parent_hash
                        getUTF8Length(parentURL) + // utf8 parent_url
                        4 +   // u4 root
                        4 +   // u4 size
                        GC.calculateOopMapSizeInBytes(memorySize); // u1 oopmap[]

        int pad = sizeSoFar % HDR.BYTES_PER_WORD;
        if (pad != 0) {
            pad = HDR.BYTES_PER_WORD - pad;
        }
        return pad;
    }

    /**
     * Skips the padding that precedes the 'memory' item to ensure that it is word aligned
     * with respect to the start of the object memory file.
     *
     * @param parentURL   the value of the 'parent_url' item in the object memory file
     * @param memorySize  the value of the 'size' item in the object memory file
     */
    private void skipMemoryPadding(String parentURL, int memorySize) {
        int pad = calculateMemoryPadding(parentURL, memorySize);
        reader.skip(pad, "padding");
    }

    /**
     * Loads the non-parent components of an object memory from the input stream.
     *
     * @param parent     the parent object memory
     */
    private ObjectMemory loadThis(ObjectMemory parent, boolean hasTypemap) {

        String url = reader.getFileName();

        // Load the offset to the root object
        int root = reader.readInt("root");

        // Load the size of the memory
        int size = reader.readInt("size");

        // Load the oop map
        BitSet oopMap = loadOopMap(size);

        // Skip the padding
        skipMemoryPadding(parent == null ? "" : parent.getURL(), size);

        // Load the object memory
        byte[] buffer = loadMemory(parent, size);

        // Calculate the hash of the object memory while it is in canonical form
        int hash = hash(buffer);

        // Relocate the pointers in the memory and move the buffer into read-only memory if necessary
        Address relocatedBuffer = relocateMemory(parent, buffer, oopMap);

        // Need to do this one more time
        if (!VM.isHosted() && !loadIntoReadOnlyMemory) {
            if (buffer != relocatedBuffer.toObject()) {
                throw new GCDuringRelocationError();
            }
        }

        // Set the pointer to the root object
        Object rootObject = relocatedBuffer.add(root).toObject();
        ObjectMemory om = new ObjectMemory(relocatedBuffer, size, url, rootObject, hash, parent);

        // Load the typemap
        if (hasTypemap) {
/*if[TYPEMAP]*/
            if (VM.usingTypeMap()) {
                loadTypeMap(om.getStart(), om.getSize());
            }
/*end[TYPEMAP]*/
        }

        if (Klass.DEBUG && !VM.isHosted()) {
            boolean trace = Tracer.isTracing("oms");
            GC.traverseMemory(om.getStart(), om.getEnd(), trace);
        }

        return om;
    }

    /**
     * Loads the parent components of an object memory from the input stream. If the
     * parent components specify that there is a parent, then this parent and its
     * parents are loaded recursively.
     */
    private ObjectMemory loadParent() {
        int parentHash = reader.readInt("parent_hash");
        String parentURL = reader.readUTF("parent_url");
        if (parentURL.length() != 0) {
            ObjectMemory parent;
            parent = load(parentURL, loadIntoReadOnlyMemory);
            if (parent.getHash() != parentHash) {
                throw new LinkageError("invalid hash for parent: expected " + parentHash + ", received " + parent.getHash());
            }
            return parent;
        } else {
            return null;
        }
    }

    /**
     * Loads the 'oopmap' component of the object memory from the input stream.
     *
     * @param size   the size of memory as specified by the 'size' element of the objct memory
     * @return the bit set encapsulating the oop map
     */
    protected BitSet loadOopMap(int size) {
        // Load the oop map
        byte[] bits = new byte[GC.calculateOopMapSizeInBytes(size)];
        reader.readFully(bits, "oopmap");
        BitSet oopMap = new BitSet(bits);
        if (Klass.DEBUG && Tracer.isTracing("oms")) {
            Tracer.traceln("oopmap:{cardinality = " + oopMap.cardinality() + "}");
        }
        return oopMap;
    }

    /**
     * Loads the 'memory' component of the object memory from the input stream.
     *
     * @param parent    the loaded/resolved parent object memory
     * @param size      the size of memory as specified by the 'size' element of the objct memory
     * @return          the contents of the 'memory' component
     */
    protected byte[] loadMemory(ObjectMemory parent, int size) {
       // Load the 'memory' component into a byte array
        byte[] buffer = new byte[size];
        reader.readFully(buffer, "memory");
        return buffer;
    }

    /**
     * Relocates the memory.
     *
     * @param parent    the loaded/resolved parent object memory
     * @param buffer    the contents of the 'memory' component
     * @param oopMap    the bit set encapsulating the 'oopmap' component
     * @return the address of the relocated memory buffer
     */
    protected Address relocateMemory(ObjectMemory parent, byte[] buffer, BitSet oopMap) {

        String url = reader.getFileName();
        int size = buffer.length;

        // Calculate the canonical starting address of the memory about to be loaded
        // based on the canonical starting address and size of the parent
        Address canonicalStart = parent == null ? Address.zero() : parent.getCanonicalEnd();

        // If this is the mapper, then the memory model in java.lang.Address needs to be initialized/appended to
        if (VM.isHosted()) {
            Unsafe.initialize(buffer, oopMap, parent != null);
        }

        // Set up the address at which the object memory will finally reside
        final Address bufferAddress = VM.isHosted() ? canonicalStart : Address.fromObject(buffer);
        final Address relocatedBufferAddress = (loadIntoReadOnlyMemory) ? Address.fromObject(GC.allocateNvmBuffer(size)) : bufferAddress;

        // Null the buffer object as there is no need for the relocation to test whether
        // or not the relocated buffer has moved which it won't have if it is in read-only
        // memory or this host environment is not Squawk
        if (VM.isHosted() || loadIntoReadOnlyMemory) {
            buffer = null;
        }

        // Relocate the pointers to the other object memories against which this object memory is bound
        relocateParents(url, buffer, bufferAddress, oopMap, parent, false);

        // Relocate the pointers within this object memory
        relocate(url, buffer, bufferAddress, oopMap, relocatedBufferAddress, canonicalStart, size, url, false);

        // Copy a relocated object memory into read-only memory if necessary
        if (loadIntoReadOnlyMemory) {
            VM.copyBytes(bufferAddress, 0, relocatedBufferAddress, 0, size, true);
/*if[TYPEMAP]*/
            if (VM.usingTypeMap()) {
                Unsafe.copyTypes(bufferAddress, relocatedBufferAddress, size);
            }
/*end[TYPEMAP]*/
        }

        // Fix up the object memory buffer if it is in RAM so that it now looks
        // like a zero length byte array to the garbage collector
        if (!loadIntoReadOnlyMemory && !VM.isHosted()) {
            GC.setHeaderLength(Address.fromObject(buffer), 0);
        }

        return relocatedBufferAddress;
    }

    /*---------------------------------------------------------------------------*\
     *                                  Relocation                               *
    \*---------------------------------------------------------------------------*/

    /**
     * Relocate all the pointers in a range of memory that point to a memory against which it is bound.
     *
     * @param url         the URL of the source memory whose pointers are being relocated
     * @param startBuffer the object containing the source memory if it is in a RAM buffer otherwise null
     * @param start       the start address of the source memory
     * @param oopMap      the oop map describing where all the pointers in the source memory are
     * @param parent      the direct parent memory of the given memory
     * @param toCanonical specifies the direction of the relocation
     */
    public static void relocateParents(String url, Object startBuffer, Address start, BitSet oopMap, ObjectMemory parent, boolean toCanonical) {
        while (parent != null) {
            relocate(url, startBuffer, start, oopMap, parent.getStart(), parent.getCanonicalStart(), parent.getSize(), parent.getURL(), toCanonical);
            parent = parent.getParent();
        }
    }

    /**
     * Relocates the pointers in a range of memory that point to some target range of memory.
     * If <code>toCanonical</code> is true, then the pointers are currently relative to the
     * real address of the target memory and are adjusted to be relative to its canonical address.
     * Otherwise, the pointers are currently relative to the canonical address of the target memory
     * and are adjusted to be relative to its real address.
     *
     * @param url               the URL of the source memory whose pointers are being relocated
     * @param sourceStartBuffer the object containing the source memory if it is in a RAM buffer otherwise null
     * @param sourceStart       the start address of the source memory
     * @param oopMap            the oop map describing where all the pointers in the source memory are
     * @param targetStart       the real start address of the target space
     * @param targetCanonicalStart  the canonical start address of the target space
     * @param targetSize        the size of the target space
     * @param targetSourceURL   the URL that identifies the target space
     * @param toCanonical       specifies the direction of the relocation
     * @throws GCDuringRelocation if 'sourceStartBuffer' is not null and some point has a different value than
     *                          'sourceStart' which implies that a garbage collection occurred
     */
    public static void relocate(String url,
                                Object sourceStartBuffer,
                                Address sourceStart,
                                BitSet oopMap,
                                Address targetStart,
                                Address targetCanonicalStart,
                                int targetSize,
                                String targetSourceURL,
                                boolean toCanonical)
    {
        final Address start;
        final Offset delta;
        if (toCanonical) {
            start = targetStart;
            delta = targetCanonicalStart.diff(targetStart);
        } else {
            start = targetCanonicalStart;
            delta = targetStart.diff(targetCanonicalStart);
        }
        final Address end = start.add(targetSize);

        if (Klass.DEBUG && Tracer.isTracing("oms")) {
            VM.print("Relocating pointers from ");
            VM.print(url);
            VM.print(" into ");
            VM.print(targetSourceURL);
            VM.println(":");
        }

        for (int offset = oopMap.nextSetBit(0); offset != -1; offset = oopMap.nextSetBit(offset + 1)) {
            if (sourceStartBuffer != null) {
                if (sourceStartBuffer != sourceStart.toObject()) {
                    throw new GCDuringRelocationError();
                }
            }
            Address pointerAddress = sourceStart.add(offset * HDR.BYTES_PER_WORD);
            Address pointer = Address.fromObject(Unsafe.getObject(pointerAddress, 0));
            if (!pointer.isZero() && pointer.hi(start) && pointer.loeq(end)) {
                Address relocatedPointer = pointer.addOffset(delta);
                Unsafe.setObject(pointerAddress, 0, relocatedPointer);
                if (Klass.DEBUG && Tracer.isTracing("oms")) {
                    VM.print("  relocated pointer @ ");
                    VM.printAddress(pointerAddress);
                    VM.print(" [offset ");
                    VM.print(offset * HDR.BYTES_PER_WORD);
                    VM.print("] from ");
                    VM.printAddress(pointer);
                    VM.print(" to ");
                    VM.printAddress(relocatedPointer);
                    VM.println();
                }
            }
        }
    }
}

/**
 * An instance of <code>ObjectMemoryReader</code> is used to read an input
 * stream opened on an object memory file.
 *
 * @author  Doug Simon
 */
class ObjectMemoryReader extends StructuredFileInputStream {

    /**
     * Creates a <code>ObjectMemoryReader</code> that reads object memory file components
     * from a given input stream.
     *
     * @param   in        the input stream
     * @param   filePath  the file from which <code>in</code> was created
     */
    public ObjectMemoryReader(InputStream in, String filePath) {
        super(in, filePath, "oms");
    }


    /**
     * {@inheritDoc}
     */
    public LinkageError formatError(String msg) {
        if (msg == null) {
            throw new LinkageError(getFileName());
        } else {
            throw new LinkageError(getFileName() + ": " + msg);
        }
    }
}
