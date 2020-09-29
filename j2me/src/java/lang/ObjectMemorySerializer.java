 package java.lang;

import java.io.*;
import javax.microedition.io.*;
import com.sun.squawk.vm.HDR;
import com.sun.squawk.util.*;

/**
 * This class facilitates saving a serialized object graph to a URL.
 * The format of the output is described by {@link ObjectMemoryLoader}.
 *
 * @author Doug Simon
 */
class ObjectMemorySerializer {

    /**
     * A ControlBlock instance is used to pass parameters in both directions when
     * calling the {@link CheneyCollector#copyObjectGraph(Address, ObjectMemorySerializer.ControlBlock, boolean) low level routine}
     * that serializes an object graph.
     */
    static final class ControlBlock {
        /**
         * The address of the buffer into which the serialized object graph is copied.
         */
        public Object memory;

        /**
         * The size of the memory buffer which will be modified after returning from 'copyObjectGraph'
         * to specifiy the size of the serialized graph.
         */
        public int size;

        /**
         * The oop map that describes which words in the serialized graph are pointers.
         */
        public BitSet oopMap;

        /**
         * The offset in the serialized graph to the root of the graph.
         */
        public int root;
    }


    /*---------------------------------------------------------------------------*\
     *                                  Saving                                   *
    \*---------------------------------------------------------------------------*/

    /**
     * Write a serialized object graph to a given URL.
     *
     * @param    url     where to write the serialized graph
     * @param    cb      the control block describing the serialized object graph
     * @param    parent  the object memory to which the serialized object memory is bound
     * @throws IOException     if there is an IO error
     */
    public static void save(final String url, final ControlBlock cb, final ObjectMemory parent) throws IOException {
        Assert.that(parent != null  || VM.isHosted());
        ObjectMemoryOutputStream sfos = new ObjectMemoryOutputStream(Connector.openDataOutputStream(url));

        // Tracing
        if (Klass.DEBUG && Tracer.isTracing("oms")) {
            Tracer.traceln("Saving object memory to " + url);
        }

        // Write the magic file number
        sfos.writeInt(0xdeadbeef, "magic");

        // Write the version numbers
        sfos.writeShort(1, "minor_version");
        sfos.writeShort(1, "major_version");

        // Write the attributes
        int attributes = 0;
        if (VM.usingTypeMap()) {
            attributes |= ObjectMemoryLoader.ATTRIBUTE_TYPEMAP;
        }
        if (!Klass.SQUAWK_64) {
            attributes |= ObjectMemoryLoader.ATTRIBUTE_32BIT;
        }
        sfos.writeInt(attributes, "attributes");

        if (parent == null) {
            sfos.writeInt(0, "parent_hash");
            sfos.writeUTF("", "parent_url");
        } else {
            sfos.writeInt(parent.getHash(), "parent_hash");
            sfos.writeUTF(parent.getURL(), "parent_url");
        }

        final int size = cb.size;
        sfos.writeInt(cb.root, "root");
        sfos.writeInt(cb.size, "size");

        // Write the oop map
        byte[] bits = new byte[GC.calculateOopMapSizeInBytes(size)];
        cb.oopMap.copyInto(bits);
        sfos.write(bits, "oopmap");
        if (Klass.DEBUG && Tracer.isTracing("oms")) {

            final int canonicalStart = (parent == null ? 0 : parent.getCanonicalEnd().toUWord().toInt());
            PrintStream out = new PrintStream(Connector.openOutputStream(url + ".pointers"));

            for (int offset = cb.oopMap.nextSetBit(0); offset != -1; offset = cb.oopMap.nextSetBit(offset + 1)) {
                int pointerAddress = canonicalStart + (offset * HDR.BYTES_PER_WORD);
                Address pointer = Address.fromObject(Unsafe.getObject(cb.memory, offset));
                out.println(pointerAddress + " [offset " + (offset * HDR.BYTES_PER_WORD) + "] : " + pointer.toUWord().toPrimitive());
            }
            out.close();
            Tracer.traceln("oopmap:{cardinality = " + cb.oopMap.cardinality() + "}");
        }

        // Write the padding to ensure 'memory' is word aligned
        int pad = ObjectMemoryLoader.calculateMemoryPadding(parent == null ? "" : parent.getURL(), size);
        while (pad-- != 0) {
            sfos.writeByte(0);
        }

        // Write the object memory itself.
        for (int i = 0; i != size; ++i) {
            sfos.writeByte(Unsafe.getAsByte(cb.memory, i));
        }
        if (Klass.DEBUG && Tracer.isTracing("oms")) {
            Tracer.traceln("memory:{wrote " + size + " bytes}");
        }

/*if[TYPEMAP]*/
        if (VM.usingTypeMap()) {
            writeTypeMap(sfos, Address.fromObject(cb.memory), size);
        }
/*end[TYPEMAP]*/

        sfos.close();

        // Tracing
        if (Klass.DEBUG && Tracer.isTracing("oms")) {
            Tracer.traceln("Saved object memory to " + url);
        }
    }

/*if[TYPEMAP]*/
    /**
     * Writes the type map describing the type of every address in an object memory.
     *
     * @param sfos    where to write the map
     * @param start   the start address of the object memory
     * @param size    the size address of the object memory
     */
    private static void writeTypeMap(ObjectMemoryOutputStream sfos, Address start, int size) throws IOException {
        Address p = start;
        for (int i = 0; i != size; ++i) {
            byte type = Unsafe.getType(p);
            sfos.writeByte(type);
            p = p.add(1);
        }
        if (Klass.DEBUG && Tracer.isTracing("oms")) {
            Tracer.traceln("typemap:{size = " + size + "}");
        }
    }
/*end[TYPEMAP]*/

}

/**
 * An instance of <code>ObjectMemoryOutputStream</code> is used to write the components of
 * a object memory file to an output stream.
 *
 * @author  Doug Simon
 */
final class ObjectMemoryOutputStream extends DataOutputStream {

    /**
     * Creates a <code>ObjectMemoryOutputStream</code>.
     *
     * @param   os        the output stream
     */
    public ObjectMemoryOutputStream(OutputStream os) {
        super(os);
    }

    /**
     * Writes a byte array to the stream.
     *
     * @param   value   the value to write
     * @param   prefix  the optional prefix used when tracing this read
     * @see     DataOutputStream#writeByte(int)
     */
    public final void write(byte[] value, String prefix) throws IOException {
        super.write(value);
        if (Klass.DEBUG && prefix != null && Tracer.isTracing("oms")) {
            Tracer.traceln(prefix+":{wrote " + value.length + " bytes}");
        }
    }

    /**
     * Writes a byte to the stream.
     *
     * @param   value   the value to write
     * @param   prefix  the optional prefix used when tracing this read
     * @see     DataOutputStream#writeByte(int)
     */
    public final void writeByte(int value, String prefix) throws IOException {
        super.writeByte(value);
        if (Klass.DEBUG && prefix != null && Tracer.isTracing("oms")) {
            Tracer.traceln(prefix+":"+(value&0xFF));
        }
    }

    /**
     * Writes a short to the stream.
     *
     * @param   value   the value to write
     * @param   prefix  the optional prefix used when tracing this read
     * @see     DataOutputStream#writeShort(int)
     */
    public final void writeShort(int value, String prefix) throws IOException {
        super.writeShort(value);
        if (Klass.DEBUG && prefix != null && Tracer.isTracing("oms")) {
            Tracer.traceln(prefix+":"+value);
        }
    }

    /**
     * Writes a int to the stream.
     *
     * @param   value the value to write
     * @param   prefix  the optional prefix used when tracing this read
     * @see     DataOutputStream#writeInt(int)
     */
    public final void writeInt(int value, String prefix) throws IOException {
        super.writeInt(value);
        if (Klass.DEBUG && prefix != null && Tracer.isTracing("oms")) {
            Tracer.traceln(prefix+":"+value);
        }
    }

    /**
     * Writes a string to the stream.
     *
     * @param   value   the value to write
     * @param   prefix  the optional prefix used when tracing this read
     * @see     DataOutputStream#writeUTF(String)
     */
    public final void writeUTF(String value, String prefix) throws IOException {
        super.writeUTF(value);
        if (Klass.DEBUG && prefix != null && Tracer.isTracing("oms")) {
            Tracer.traceln(prefix+":\""+value+"\"");
        }
    }
}
