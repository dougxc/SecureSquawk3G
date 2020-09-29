/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */
package java.lang;

import java.util.*;
import com.sun.squawk.vm.*;
import com.sun.squawk.util.*;

/**
 * This class provides a mechanism for copying an object graph from the host
 * VM (e.g. Hotspot) into a Squawk memory.
 *
 * @author  Doug Simon
 */
final class ObjectGraphSerializer {

    private Romizer romizer;

    /**
     * This maps host objects to their serialized addresses.
     */
    private ArrayHashtable objectMap;

    /**
     * Counts the number of "java.lang.VM.do_..." methods. These are the methods
     * that the VM needs to know about as there are entry points back into Java code.
     */
    private int entryPointCounter;

    /**
     * Serializes a graph of host objects into the object memory format used by the Squawk VM.
     * Each call to this method resets the memory model in {@link Address} so that the objects
     * are serialized relative to address 0.
     *
     * @param object  the root of the object graph to serialize
     * @return the address of the root in the serialized memory
     */
    public static Object serialize(Object object, Romizer romizer) {
        // Reset the memory model
        Unsafe.setMemorySize(0);

        // Initialize the allocator
        GC.initialize(VM.getCurrentIsolate().getBootstrapSuite());

        // Serialize the graph
        ObjectGraphSerializer serializer = new ObjectGraphSerializer(object, romizer);

        return serializer.objectMap.get(object);
    }

    /**
     * Creates an ObjectGraphSerializer instance and serializes a given object graph.
     *
     * @param object  the root of the object graph to serialize
     */
    private ObjectGraphSerializer(Object object, Romizer romizer) {
        this.romizer = romizer;

        // Reset the entry point counter and object map
        entryPointCounter = 0;
        objectMap = new ArrayHashtable();

        // Now serialize the given graph
        save(object);

        // Fix up the class pointers of the objects in the Squawk memory
        Unsafe.resolveClasses(objectMap);
    }

    /**
     * Trace the allocation of a serialized object if tracing of the image building is enabled.
     *
     * @param klass             the class of the object
     * @param object            the host object
     * @param serializedObject  the serialized copy of <code>object</code>
     */
    private void traceAllocation(Klass klass, Object object, Address serializedObject) {
        if (Klass.DEBUG && Tracer.isTracing("image")) {
            Address block = GC.oopToBlock(klass, serializedObject);
            int bodySize = GC.getBodySize(klass, serializedObject);
            Tracer.traceln("[allocated " + klass.getName() + " instance @ " + serializedObject +
                           " (block=" + block + ", body size=" + bodySize + ")" + " (object.toString()=\"" + object + "\")]");

        }
    }

    /**
     * Guards against re-entry to the alloc method.
     */
    private boolean inAlloc = false;

    /**
     * Allocates a copy of a host object in the Squawk memory. The copy allocated
     * will have the same size and type as <code>object</code> and all of its fields
     * or components will be initialized to their default values.<p>
     *
     * There must be no object in the Squawk memory {@link Arrays#equals(Object) equal} to
     * <code>object</code> prior to this call.
     *
     * @param   object  the host object for which a Squawk copy is to be allocated
     * @param   klass   the class of <code>object</code>
     * @param   length  the number of elements in the array being allocated or -1 if a non-array object is being allocated
     * @return  the copy allocated in Squawk memory
     */
    private Object alloc(Object object, Klass klass, int length) {
        Assert.that(!inAlloc);
        inAlloc = true;

        Object serializedObject;

        /*
         * Allocate the Squawk object
         */
        if (klass.isArray()) {
            serializedObject = GC.newArray(klass, length);
        } else if (klass == Klass.STRING) {
            serializedObject = GC.newArray(Klass.CHAR_ARRAY, length);
            GC.setHeaderClass((Address)serializedObject, Klass.STRING);
        } else if (klass == Klass.STRING_OF_BYTES) {
            serializedObject = GC.newArray(Klass.BYTE_ARRAY, length);
            GC.setHeaderClass((Address)serializedObject, Klass.STRING_OF_BYTES);
        } else if (object instanceof MethodBody) {
            MethodBody mbody = (MethodBody)object;
            Klass definingClass = mbody.getDefiningClass();
            serializedObject = GC.newMethod(definingClass, mbody);

            /*
             * Write special symbol table entries for all the methods in java.lang.VM
             * whose names start "do_"
             */
            if (definingClass.getName().equals("java.lang.VM")) {
                String methodName = mbody.getDefiningMethod().getName();
                if (methodName.startsWith("do_")) {
                    int old = VM.setStream(VM.STREAM_SYMBOLS);
                    VM.println("ENTRYPOINT."+entryPointCounter+".NAME=java_lang_VM_"+methodName);
                    VM.println("ENTRYPOINT."+entryPointCounter+".ADDRESS="+serializedObject);
                    entryPointCounter++;
                    VM.setStream(old);
                }
            }
        } else {
            serializedObject = GC.newInstance(klass);
        }

        // Add the mapping between the host object and its serialized copy
        Object previous = objectMap.put(object, serializedObject);
        Assert.that(previous == null);

        traceAllocation(klass, object, Address.fromObject(serializedObject));

        inAlloc = false;
        return serializedObject;
    }

    /**
     * Determines if all the elements in a specified character array can be
     * encoded in 8 bits.
     *
     * @param   chars  the character array to check
     * Return   true if all the characters in <code>chars</code> can be
     *                 encoded in 8 bits
     */
    private boolean isEightBitEnc(char[] chars) {
        int length = chars.length;
        for (int i = 0; i < length; i++) {
            if (chars[i] > 0xFF) {
                return false;
            }
        }
        return true;
    }

    /**
     * Copies an object graph from the host memory to the Squawk memory.
     *
     * @param   object  the root of the host object graph to be copied
     * @return  the copy of <code>object</code> in Squawk memory
     */
    private Object save(Object object) {
        return save(object, classToKlass(object.getClass()));
    }

    /**
     * Copies an object graph from the host memory to the Squawk memory.
     *
     * @param   object  the root of the host object graph to be copied
     * @param   klass   the class of <code>object</code>
     * @return  the copy of <code>object</code> in Squawk memory
     */
    private Object save(Object object, Klass klass) {
        Object serializedObject = objectMap.get(object);
        if (serializedObject == null) {
            /*
             * Initialize the fields
             */
            if (klass.isSquawkArray()) {
                switch (klass.getClassID()) {
                    case CID.BOOLEAN_ARRAY: {
                        boolean[] array = (boolean[])object;
                        serializedObject = alloc(object, klass, array.length);
                        for (int i = 0 ; i < array.length ; i++) {
                            Unsafe.setByte(serializedObject, i, array[i] ? 1 : 0);
                        }
                        break;
                    }
                    case CID.BYTE_ARRAY: {
                        byte[] array = (byte[])object;
                        serializedObject = alloc(object, klass, array.length);
                        for (int i = 0 ; i < array.length ; i++) {
                            Unsafe.setByte(serializedObject, i, array[i]);
                        }
                        break;
                    }
                    case CID.SHORT_ARRAY: {
                        short[] array = (short[])object;
                        serializedObject = alloc(object, klass, array.length);
                        for (int i = 0 ; i < array.length ; i++) {
                            Unsafe.setShort(serializedObject, i, array[i]);
                        }
                        break;
                    }
                    case CID.CHAR_ARRAY: {
                        char[] array = (char[])object;
                        serializedObject = alloc(object, klass, array.length);
                        for (int i = 0 ; i < array.length ; i++) {
                            Unsafe.setChar(serializedObject, i, array[i]);
                        }
                        break;
                    }
                    case CID.STRING_OF_BYTES: {
                        char[] value = ((String)object).toCharArray();
                        serializedObject = alloc(object, Klass.STRING_OF_BYTES, value.length);
                        for (int i = 0; i != value.length; ++i) {
                            Unsafe.setByte(serializedObject, i, (byte)value[i]);
                        }
                        break;
                    }
                    case CID.STRING: {
                        String str = (String)object;
                        char[] value = str.toCharArray();
                        if (isEightBitEnc(value)) { // Will only occur during romizing
                            boolean done = false;
                            while (!done) {
                                done = true;
                                String buildProp = "${build.properties:";
                                int start = str.indexOf(buildProp);
                                if (start >= 0) {

                                    String head = str.substring(0, start);
                                    String rest = str.substring(start);
                                    int end = rest.indexOf('}');
                                    Assert.that(end > 0);
                                    String symbol = rest.substring(buildProp.length(), end);
                                    String tail   = rest.substring(end+1);


                                    Object prop = Romizer.buildProperties.get(symbol);
                                    if (prop == null) {
                                         throw new RuntimeException("Cannot find build property: "+symbol);
                                    }
                                    str = head+prop+tail;
                                    done = false;
//System.out.println("str="+str);
//System.out.println("head="+head);
//System.out.println("rest="+rest);
//System.out.println("symbol="+symbol);
//System.out.println("tail="+tail);
//System.out.println(str+"->"+object);
                                }
                                object = str;
                            }
                            return save(object, Klass.STRING_OF_BYTES);
                        }
                        serializedObject = alloc(object, Klass.STRING, value.length);
                        for (int i = 0; i != value.length; ++i) {
                            Unsafe.setChar(serializedObject, i, value[i]);
                        }
                        break;
                    }
                    case CID.INT_ARRAY: {
                        int[] array = (int[])object;
                        serializedObject = alloc(object, klass, array.length);
                        for (int i = 0 ; i < array.length ; i++) {
                            Unsafe.setInt(serializedObject, i, array[i]);
                        }
                        break;
                    }
                    case CID.UWORD_ARRAY: {
                        UWord[] array = (UWord[])object;
                        serializedObject = alloc(object, klass, array.length);
                        for (int i = 0 ; i < array.length ; i++) {
                            Unsafe.setUWord(serializedObject, i, array[i]);
                        }
                        break;
                    }
                    case CID.FLOAT_ARRAY: {
                        float[] array = (float[])object;
                        serializedObject = alloc(object, klass, array.length);
                        for (int i = 0 ; i < array.length ; i++) {
                            Unsafe.setInt(serializedObject, i, Float.floatToIntBits(array[i]));
                        }
                        break;
                    }
                    case CID.LONG_ARRAY: {
                        long[] array = (long[])object;
                        serializedObject = alloc(object, klass, array.length);
                        for (int i = 0 ; i < array.length ; i++) {
                            Unsafe.setLong(serializedObject, i, array[i]);
                        }
                        break;
                    }
                    case CID.DOUBLE_ARRAY: {
                        double[] array = (double[])object;
                        serializedObject = alloc(object, klass, array.length);
                        for (int i = 0 ; i < array.length ; i++) {
                            Unsafe.setLong(serializedObject, i, Double.doubleToLongBits(array[i]));
                        }
                        break;
                    }
                    case CID.GLOBAL_ARRAY:
                    case CID.LOCAL_ARRAY: {
                        Assert.shouldNotReachHere();
                        break;
                    }
                    default: {
                        Assert.that(Klass.OBJECT_ARRAY.isAssignableFrom(klass));
                        Object[] array = (Object[])object;
                        serializedObject = alloc(object, klass, array.length);
                        for (int i = 0 ; i < array.length ; i++) {
                            Object value = array[i];
                            Object serializedValue = Address.zero();
                            if (value != null) {
                                serializedValue = save(value);
                            }
                            Unsafe.setObject(serializedObject, i, serializedValue);
                        }
                        break;
                    }
                }
            } else {

                /*
                 * Allocate the object
                 */
                serializedObject = alloc(object, klass, -1);
                if (!(object instanceof MethodBody)) {
                    saveFields(object, serializedObject);
                }
            }
        }
        return serializedObject;
    }

    /**
     * Copies the fields of an object in the host memory to the fields of
     * the corresponding object in Squawk memory.<p>
     *
     * @param   object  the host object whose fields are to be copied
     * @param   serializedObject the object in Squawk memory corresponding to <code>object</code>
     */
    void saveFields(Object object, Object serializedObject) {
        boolean isSuite = object instanceof Suite;
        Klass klass = classToKlass(object.getClass());
        while (klass != Klass.OBJECT) {

            /* Save instance fields */
            saveDeclaredFields(object, serializedObject, isSuite, klass, false);

            /* Save static fields */
            //saveDeclaredFields(object, serializedObject, isSuite, klass, true);
            klass = klass.getSuperclass();
        }
    }

    /**
     * Copies the instance fields of a single class in an object's type hierarchy.
     *
     * @param object            the host object whose fields are to be copied
     * @param serializedObject  the object in Squawk memory corresponding to <code>object</code>
     * @param isSuite           true if the supplied object is an instance of <code>suite</code>
     * @param klass             the klass of the supplied object
     */
    private void saveDeclaredFields(Object object, Object serializedObject,
                                    boolean isSuite, Klass klass, boolean isStatic) {
        int count = klass.getFieldCount(isStatic);
        for (int i = 0 ; i < count ; i++) {
            Field field = klass.getField(i, isStatic);
            Klass type = field.getType();
            switch (type.getClassID()) {
                case CID.BOOLEAN:
                case CID.BYTE: {
                    int value = FieldReflector.getByte(object, field);
                    Unsafe.setByte(serializedObject, field.getOffset(), value);
                    break;
                }
                case CID.SHORT: {
                    int value = FieldReflector.getShort(object, field);
                    Unsafe.setShort(serializedObject, field.getOffset(), value);
                    break;
                }
                case CID.CHAR: {
                    int value = FieldReflector.getChar(object, field);
                    Unsafe.setChar(serializedObject, field.getOffset(), value);
                    break;
                }
                case CID.FLOAT:
                case CID.INT: {
                    int value = FieldReflector.getInt(object, field);
                    Unsafe.setInt(serializedObject, field.getOffset(), value);
                    break;
                }
                case CID.DOUBLE:
                case CID.LONG: {
                    long value = FieldReflector.getLong(object, field);
                    Unsafe.setLongAtWord(serializedObject, field.getOffset(), value);
                    break;
                }
                case CID.UWORD: {
                    UWord value = FieldReflector.getUWord(object, field);
                    Unsafe.setUWord(serializedObject, field.getOffset(), value);
                    break;
                }
                default: {
                    Object value = FieldReflector.getObject(object, field);
                    Object serializedValue = Address.zero();

                    // Prune the symbolic information in a suite
                    if (isSuite && value != null && field.getName().equals("metadatas")) {
                        value = KlassMetadata.prune((Suite)object, (KlassMetadata[])value, romizer.suiteType, romizer.retainLNTs, romizer.retainLVTs);
                    }

                    if (value != null) {
                        serializedValue = save(value);
                    }
                    Unsafe.setObject(serializedObject, field.getOffset(), serializedValue);
                    break;
                }
            }
        }
    }

    /*
     * Speeds up Class -> Klass conversion
     */
    private Map classKlassMap = new HashMap();

    /**
     * Get the Squawk class name corresponding to a standard Java Class name.
     *
     * @param  c  a Class instance
     * @return the name of <code>c</code> in Squawk format
     */
    private String getKlassName(Class c) {
        if (c.isArray()) {
            return "[" + getKlassName(c.getComponentType());
        } else {
            if (c == Klass.class) {
                return "java.lang.Class";
            }
            return c.getName();
        }
    }

    /**
     * Gets the Klass instance corresponding to a Class instance.
     *
     * @param   cls  the Class instance to convert
     * @return  the  Klass instance corresponding to <code>cls</code>
     */
    private Klass classToKlass(Object cls) {
        Class c = (Class)cls;
        Klass klass = (Klass)classKlassMap.get(c);
        if (klass == null) {
            String name = getKlassName(c);
            /*
             * Convert to Squawk name
             */
            Suite suite = VM.getCurrentIsolate().getOpenSuite();
            klass = suite.lookup(name);
            Assert.that(klass != null, "Lookup failure for class "+name);
            classKlassMap.put(c, klass);
        }
        return klass;
    }
}
