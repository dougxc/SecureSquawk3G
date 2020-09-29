/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */
package java.lang;

import java.io.*;
import com.sun.squawk.util.Tracer;
import com.sun.squawk.util.Assert;

/**
 * The KlassMetadata class is a container for all the meta-information
 * pertaining to a class where this information is not necessarily
 * required by the runtime system.
 *
 * For example, the names and signatures of a class's fields and methods
 * are contained in this class. These types of information are only
 * required if the runtime system includes support for features such
 * as dynamic class loading, stack traces etc.
 *
 * @author  Doug Simon, Nik Shaylor
 * @version 1.0
 * @see     java.lang.Klass
 */
final class KlassMetadata {

    /**
     * The Klass instance to which the meta-information in this object pertains.
     */
    private final Klass definedClass;

    /**
     * The symbolic information for the class. This includes the signatures
     * of the fields and methods of the class. The structure encoded in this
     * array of bytes is accessed by a SymbolsParser instance.
     */
    private final byte[] symbols;

    /**
     * The debug information for the virtual methods described in
     * <code>symbols</code>.
     */
    private MethodMetadata[] virtualMethodsMetadata;

    /**
     * The debug information for the static methods described in
     * <code>symbols</code>.
     */
    private MethodMetadata[] staticMethodsMetadata;

    /**
     * The source file from which a class was compiled. This field's value
     * may be null if the corresponding class had no SourceFile attribute.
     */
    private final String sourceFile;

    /**
     * Create a new <code>KlassMetadata</code> for a <code>Klass</code> instance.
     *
     * @param definedClass   the class to which this metadata pertains
     * @param virtualMethods the virtual methods of the class
     * @param staticMethods  the static methods of the class
     * @param instanceFields the instance fields of the class
     * @param staticFields   the static fields of the class
     * @param sourceFile     the source file from which the class was compiled
     */
    KlassMetadata(
                    Klass             definedClass,
                    ClassFileMethod[] virtualMethods,
                    ClassFileMethod[] staticMethods,
                    ClassFileField[]  instanceFields,
                    ClassFileField[]  staticFields,
                    String            sourceFile,
                    int               vtableSize,
                    int               stableSize
                 ) {
        this.definedClass = definedClass;
        this.symbols      = SymbolParser.createSymbols(virtualMethods, staticMethods, instanceFields, staticFields);
        this.sourceFile   = sourceFile;

        this.virtualMethodsMetadata = new MethodMetadata[vtableSize];
        this.staticMethodsMetadata  = new MethodMetadata[stableSize];
    }

    /**
     * Set the debug information for a method body to the collection for the defining class.
     *
     * @param isStatic  true if the method is static
     * @param index     the index for a method body
     * @param metadata  the debug information for the method
     */
    void setMethodMetadata(boolean isStatic, int index, MethodMetadata metadata) {
        if (isStatic) {
            Assert.that(staticMethodsMetadata[index] == null);
            staticMethodsMetadata[index] = metadata;
        } else {
            Assert.that(virtualMethodsMetadata[index] == null);
            virtualMethodsMetadata[index] = metadata;
        }
    }

    /**
     * Get the debug information for a method body.
     *
     * @param isStatic  true if the method is static
     * @param index     the index for a method body
     * @return the debug information for the method
     */
    MethodMetadata getMethodMetadata(boolean isStatic, int index) {
        MethodMetadata[] methods = isStatic ? staticMethodsMetadata : virtualMethodsMetadata;
        if (methods == null) {
            return null;
        }
        return methods[index];
    }

    /**
     * Get the class to which this metadata pertains.
     *
     * @return the class to which this metadata pertains
     */
    Klass getDefinedClass() {
        return definedClass;
    }

    /**
     * Get the source file from which the class was compiled.
     *
     * @return the file name
     */
    String getSourceFileName() {
        return sourceFile;
    }

    /**
     * Get a parser for the symbolic information for the class.
     *
     * @return a parser for the symbolic information for the fields and
     *         methods of the class
     */
    SymbolParser getSymbolParser() {
        return SymbolParser.create(symbols);
    }

    /**
     * Get a parser for the symbolic information of a member of the class.
     *
     * @param   memberID  the index of the member to parse
     * @return  a parser for the member indicated by <code>memberID</code>
     */
    SymbolParser getSymbolParser(int memberID) {
        return SymbolParser.create(symbols, memberID);
    }

    int getSize() {
        return symbols.length;
    }

    /**
     * Flush the cached section parser objects.
     */
    static void flush() {
        SymbolParser.flush();
    }

    /*---------------------------------------------------------------------------*\
     *                                Pruning                                    *
    \*---------------------------------------------------------------------------*/

    /**
     * Constructor for pruning.
     *
     * @param symbols   the pruned symbols
     * @param original  the original symbolic information before pruning
     * @param retainLNTs if true the line number tables (if any) are to be preserved
     * @param retainLVTs if true the local variable tables (if any) are to be preserved
     */
    private KlassMetadata(byte[] symbols, KlassMetadata original, boolean retainLNTs, boolean retainLVTs) {
        this.symbols = symbols;
        this.sourceFile = original.sourceFile;
        this.definedClass = original.definedClass;
        if (retainLNTs || retainLVTs) {
            staticMethodsMetadata = pruneMethodMetadata(original.staticMethodsMetadata, retainLNTs, retainLVTs);
            virtualMethodsMetadata = pruneMethodMetadata(original.virtualMethodsMetadata, retainLNTs, retainLVTs);
        }

        if (Klass.DEBUG && Tracer.isTracing("pruning")) {
            if (symbols == null) {
                Tracer.traceln("  discarded all symbols for " + definedClass);
            }
            if (!retainLNTs) {
                Tracer.traceln("  discarded line number tables for " + definedClass);
            }
            if (!retainLVTs) {
                Tracer.traceln("  discarded local variable tables for " + definedClass);
            }
        }


    }

    /**
     * Prune the symbolic/debug information for the methods.
     *
     * @param methods     an array of method metadatas to prune
     * @param retainLNTs  if true the line number tables (if any) are to be preserved
     * @param retainLVTs  if true the local variable tables (if any) are to be preserved
     * @return the pruned method metadatas
     */
    private MethodMetadata[] pruneMethodMetadata(MethodMetadata[] methods, boolean retainLNTs, boolean retainLVTs) {
        if (methods == null) {
            return null;
        }
        MethodMetadata[] newMethods = new MethodMetadata[methods.length];
        for (int i = 0; i != methods.length; ++i) {
            MethodMetadata original = methods[i];
            if (original != null) {
                int[] lnt = retainLNTs ? original.getLineNumberTable() : null;
                newMethods[i] = new MethodMetadata(
                                                    original.getID(),
/*if[SCOPEDLOCALVARIABLES]*/
                                                    retainLVTs ? original.getLocalVariableTable() : null,
/*end[SCOPEDLOCALVARIABLES]*/
                                                    lnt
                                                   );
            }
        }
        return newMethods;
    }

    /**
     * Prunes the symbolic information for a given suite.
     *
     * @param suite      the enclosing suite
     * @param metadatas  the symbolic information for the classes in <code>suite</code>
     * @param type       the pruning policy
     * @param retainLNTs if true the line number tables (if any) are to be preserved
     * @param retainLVTs if true the local variable tables (if any) are to be preserved
     * @return the pruned symbolic information which may be null
     */
    static KlassMetadata[] prune(Suite suite, KlassMetadata[] metadatas, int type, boolean retainLNTs, boolean retainLVTs) {
        if (metadatas == null) {
            return null;
        }

        if (type == Suite.APPLICATION && !retainLNTs && !retainLVTs) {
            if (Klass.DEBUG && Tracer.isTracing("pruning")) {
                Tracer.traceln("Discarded all metadata for " + suite);
            }
            return null;
        }

        KlassMetadata[] newMetadatas = new KlassMetadata[metadatas.length];
        for (int i = 0; i != metadatas.length; ++i) {
            KlassMetadata metadata = metadatas[i];
            if (metadata != null) {
                switch (type) {
                    case Suite.DEBUG:
                        newMetadatas[i] = new KlassMetadata(metadata.symbols, metadata, retainLNTs, retainLVTs);
                        break;
                    case Suite.APPLICATION:
                        newMetadatas[i] = new KlassMetadata(null, metadata, retainLNTs, retainLVTs);
                        break;
                    case Suite.LIBRARY:
                    case Suite.EXTENDABLE_LIBRARY:
                        newMetadatas[i] = metadata.prune(type, retainLNTs, retainLVTs);
                        break;
                    default:
                        VM.fatalVMError();
                }
            }
        }
        return newMetadatas;
    }

    /**
     * Prunes the symbols based on a given suite type.
     *
     * @param type  specifies a closed suite type. Must be {@link Suite#LIBRARY} or {@link Suite#EXTENDABLE_LIBRARY}.
     * @param retainLNTs if true the line number tables (if any) are to be preserved
     * @param retainLVTs if true the local variable tables (if any) are to be preserved
     */
    private KlassMetadata prune(int type, boolean retainLNTs, boolean retainLVTs) {
        if (Klass.DEBUG && Tracer.isTracing("pruning")) {
            Tracer.traceln("Discarding metadata for " + definedClass);
        }

        if (VM.isHosted() && type == Suite.LIBRARY) {
            if (!definedClass.isPublic()) {
                return null;
            }
            if (
                 definedClass.getName().startsWith("com.sun.") &&
                !definedClass.getName().endsWith(".Hashtable") &&
                !definedClass.getName().endsWith(".Vector")    &&
                !definedClass.getName().endsWith(".Stack")
               ) {
                return null;
            }
        }

        byte[] symbols = getSymbolParser().prune(type);
        return new KlassMetadata(symbols, this, retainLNTs, retainLVTs);
    }

    /*---------------------------------------------------------------------------*\
     *                                 Debug                                     *
    \*---------------------------------------------------------------------------*/

    static class Debug {
        static int creates;
        static int createMisses;
        static int parses;
        static int parseMisses;
        static int selects;
        static int selectMisses;

        static void printStats(PrintStream out) {
            out.println("-- SymbolParser stats --");
            out.println();
            out.println("Creates "+creates);
            out.println("Misses  "+createMisses+"("+((createMisses*100)/creates)+"%)");
            out.println();
            out.println("Parses  "+parses);
            out.println("Misses  "+parseMisses+"("+((parseMisses*100)/parses)+"%)");
            out.println();
            out.println("Selects "+selects);
            out.println("Misses  "+selectMisses+"("+((selectMisses*100)/selects)+"%)");
        }

        static void dump(byte[] symbols) {
            PrintStream out = System.out;
            out.println("symbols:");
            for (int i = 0; i != symbols.length; ++i) {
                int b = symbols[i] & 0xFF;
                String s = "    "+i;
                while (s.length() < 15) s += " ";
                s += ""+b;
                if (b > ' ' && b < '~') {
                    while (s.length() < 25) s += " ";
                    s += ""+(char)b;
                }
                System.out.println(s);
            }
        }
    }
}


/*---------------------------------------------------------------------------*\
 *                               SymbolParser                                *
\*---------------------------------------------------------------------------*/

/**
 * This is a utility class for parsing and reifying the components in
 * the symbols of a class.<p>
 *
 * The format of the symbol data is described by the following pseudo-C
 * structures:
 * <p><hr><blockquote><pre>
 *  symbols {
 *      u1      flags;        // indicates which sections exist. A
 *                            // section exists if bit at position 'n'
 *                            // is set where 'n' is the constant
 *                            // denoting the section
 *      section sections[n];  // invariant: n >= 0
 *  }
 *
 *  section {
 *      u1     category;     // invariant: INSTANCE_FIELDS <= category <= STATIC_METHODS
 *      member members[n];   // invariant: n > 0
 *  }
 *
 *  member {
 *      u2   length;       // length of member after 'length' item [invariant: length > STATIC_METHODS]
 *      u2   access_flags;
 *      u2   offset;
 *      utf8 name;
 *      u4   type;          // returnType for method/type for field
 *      union {
 *          u4   parameters[];  // method parameters: occupies remainder of struct
 *          u1   value[n];      // constant value
 *      }
 *  }
 * </pre></blockquote><hr><p>
 *
 * The structures described above are actually stored in a byte array
 * encoded and decoded with a {@link ByteBufferEncoder} and
 * {@link ByteBufferDecoder} respectively.
 *
 */

final class SymbolParser extends ByteBufferDecoder {

    private SymbolParser(byte[] symbols) {
        super(symbols, 0);
    }


    /*---------------------------------------------------------------------------*\
     *                         Static fields and methods                         *
    \*---------------------------------------------------------------------------*/

    /**
     * A constant denoting the instance fields member category.
     */
    final static int INSTANCE_FIELDS = 0;

    /**
     * A constant denoting the static fields member category.
     */
    final static int STATIC_FIELDS = 1;

    /**
     * A constant denoting the virtual methods member category.
     */
    final static int VIRTUAL_METHODS = 2;

    /**
     * A constant denoting the static methods member category.
     */
    final static int STATIC_METHODS  = 3;

    /**
     * The first cached parser.
     */
    private static SymbolParser p1;

    /**
     * The first cached parser.
     */
    private static SymbolParser p2;

    /**
     * Create a symbols parser.
     *
     * @param  symbols  the symbols to parse
     * @return the created parser
     */
    static SymbolParser create(byte[] symbols) {
        if (Klass.DEBUG) {
            KlassMetadata.Debug.creates++;
        }
        if (p1 != null && p1.buf == symbols) {
            return p1;
        }
        if (p2 == null) {
            p2 = new SymbolParser(symbols);
        }
        if (p2.buf != symbols) {
            if (Klass.DEBUG) {
                KlassMetadata.Debug.createMisses++;
            }
            p2.setup(symbols);
        }

        /*
         * Swap p1 and p2 so that the other one is replaced next time
         */
        SymbolParser temp = p2;
        p2 = p1;
        p1 = temp;
        return p1;
    }

    /**
     * Create a symbols parser. The returned parser is positioned at the
     * method or field identified by <code>id</code>.
     *
     * @param  symbols  the symbols to parse
     * @param  id       the identifier of the method or field to select
     * @return the created parser
     */
    static SymbolParser create(byte[] symbols, int id) {
        SymbolParser parser = create(symbols);
        parser.select(id);
        return parser;
    }

    /**
     * Flush the cached section parser objects
     */
    static void flush() {
        p1 = p2 = null;
        symbolsBuffer = null;
    }

    /**
     * Creates a serialized representation of all the symbolic information
     * pertaining to the fields and methods of a class.
     *
     * @param  virtualMethods the virtual methods of the class
     * @param  staticMethods  the static methods of the class
     * @param  instanceFields the instance fields of the class
     * @param  staticFields   the static fields of the class
     * @return a serialized representation of all the symbolic in the
     *                        given fields and methods
     */
    synchronized static byte[] createSymbols(
                                              ClassFileMethod[] virtualMethods,
                                              ClassFileMethod[] staticMethods,
                                              ClassFileField[]  instanceFields,
                                              ClassFileField[]  staticFields
                                            ) {
        if (symbolsBuffer == null) {
            symbolsBuffer = new ByteBufferEncoder();
            membersBuffer = new ByteBufferEncoder();
        }
        symbolsBuffer.reset();

        // add place holder for flags
        int flags = 0;
        if (instanceFields.length != 0) {
            flags |= 1 << INSTANCE_FIELDS;
        }
        if (staticFields.length != 0) {
            flags |= 1 << STATIC_FIELDS;
        }
        if (virtualMethods.length != 0) {
            flags |= 1 << VIRTUAL_METHODS;
        }
        if (staticMethods.length != 0) {
            flags |= 1 << STATIC_METHODS;
        }
        symbolsBuffer.addUnsignedByte(flags);

        serializeFields(INSTANCE_FIELDS, instanceFields);
        serializeFields(STATIC_FIELDS,   staticFields);
        serializeMethods(VIRTUAL_METHODS, virtualMethods);
        serializeMethods(STATIC_METHODS,  staticMethods);

        return symbolsBuffer.toByteArray();
    }

    /**
     * Serialize the symbolic information for a set of fields into a
     * byte buffer.
     *
     * @param category the category of the fields (must be INSTANCE_FIELDS
     *                 or STATIC_FIELDS).
     * @param fields   the set of fields to serialize
     */
    private static void serializeFields(int category, ClassFileField[] fields) {
        Assert.that(category == INSTANCE_FIELDS || category == STATIC_FIELDS);
        if (fields.length != 0) {
            symbolsBuffer.addUnsignedByte(category);
            for (int i = 0; i != fields.length; ++i) {
                ClassFileField field = fields[i];
                int modifiers = field.getModifiers();
                Klass type = field.getType();
                membersBuffer.reset();
                membersBuffer.addUnsignedShort(modifiers);
                membersBuffer.addUnsignedShort(field.getOffset());
                membersBuffer.addUtf8(field.getName());
                membersBuffer.addUnsignedInt(type.getClassID());
                if (Modifier.hasConstant(modifiers)) {
                    long value = ((ClassFileConstantField)field).getConstantValue();
                    int dataSize = field.getType().getDataSize();
                    for (int bite = 0; bite != dataSize; ++bite) {
                        membersBuffer.addUnencodedByte((byte)value);
                        value = value >> 8;
                    }
                }
                symbolsBuffer.add(membersBuffer);
            }
        }
    }

    /**
     * Serialize the symbolic information for a set of methods into a
     * byte buffer.
     *
     * @param category the category of the methods (must be VIRTUAL_METHODS
     *                 or STATIC_METHODS).
     * @param methods  the set of methods to serialize
     */
    private static void serializeMethods(int category, ClassFileMethod[] methods) {
        Assert.that(category == VIRTUAL_METHODS || category == STATIC_METHODS);
        if (methods.length != 0) {
            symbolsBuffer.addUnsignedByte(category);
            for (int i = 0; i != methods.length; ++i) {
                ClassFileMethod method = methods[i];
                membersBuffer.reset();
                membersBuffer.addUnsignedShort(method.getModifiers());
                membersBuffer.addUnsignedShort(method.getOffset());
                membersBuffer.addUtf8(method.getName());
                membersBuffer.addUnsignedInt(method.getReturnType().getClassID());
                Klass[] parameterTypes = method.getParameterTypes();
                for (int j = 0; j != parameterTypes.length; ++j) {
                    membersBuffer.addUnsignedInt(parameterTypes[j].getClassID());
                }
                symbolsBuffer.add(membersBuffer);
            }
        }
    }

    /**
     * The buffer used to build the serialized symbols array for a class
     */
    private static ByteBufferEncoder symbolsBuffer;

    /**
     * The buffer used to build the serialized symbols array for a set
     * of methods or fields.
     */
    private static ByteBufferEncoder membersBuffer;

    /*---------------------------------------------------------------------------*\
     *                                   Pruning                                 *
    \*---------------------------------------------------------------------------*/

    /**
     * Determines if the symbolic information for a given field or method should be retained.
     *
     * @param type       specifies a closed suite type. Must be {@link Suite#LIBRARY} or {@link Suite#EXTENDABLE_LIBRARY}.
     * @param  modifiers the modifiers of the field or method in question
     * @param  fieldType the type of the field or null if this is a method
     * @return true if the symbolic information for <code>member</code> should be retained
     */
    private static boolean retainMember(int type, int modifiers, Klass fieldType) {
        // Discard primitive constants
        if (Modifier.hasConstant(modifiers) && fieldType.isPrimitive() && Modifier.isFinal(modifiers)) {
            return false;
        }
        if (type == Suite.LIBRARY) {
            return Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers);
        } else {
            Assert.that(type == Suite.EXTENDABLE_LIBRARY);
            return Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers) || !Modifier.isPrivate(modifiers);
        }
    }

    /**
     * Synchronization lock for {@link #prune(int)}.
     */
    private static final Object PRUNE_LOCK = new Object();

    /**
     * Prunes the symbols based on a given suite type.
     *
     * @param type  specifies a closed suite type. Must be {@link Suite#LIBRARY} or {@link Suite#EXTENDABLE_LIBRARY}.
     * @return the pruned symbols
     */
    byte[] prune(int type) {
        synchronized(PRUNE_LOCK) {
            if (symbolsBuffer == null) {
                symbolsBuffer = new ByteBufferEncoder();
                membersBuffer = new ByteBufferEncoder();
            }
            symbolsBuffer.reset();

            // Place holder for flags
            int flagsPos = symbolsBuffer.count;
            symbolsBuffer.addUnsignedByte(0);

            int flags = 0;
            flags |= pruneFields(type, INSTANCE_FIELDS);
            flags |= pruneFields(type, STATIC_FIELDS);
            flags |= pruneMethods(type, VIRTUAL_METHODS);
            flags |= pruneMethods(type, STATIC_METHODS);

            symbolsBuffer.buffer[flagsPos] = (byte)flags;

            return symbolsBuffer.toByteArray();
        }
    }

    /**
     * Prunes the fields based on a given suite type.
     *
     * @param type      specifies a closed suite type. Must be {@link Suite#LIBRARY} or {@link Suite#EXTENDABLE_LIBRARY}.
     * @param category  specifies instance or static fields
     * @return an integer with the only the bit in position 'category' set if at least one field was not pruned othwerwise 0
     */
    private int pruneFields(int type, int category) {
        Assert.that(category == INSTANCE_FIELDS || category == STATIC_FIELDS);
        int count = getMemberCount(category);
        boolean keptAtLeastOne = false;
        if (count != 0) {
            for (int i = 0; i != count; ++i) {
                select(category, i);
                int modifiers = getModifiers();
                Klass fieldType = SuiteManager.getKlass(getSignatureAt(0));
                if (retainMember(type, modifiers, fieldType)) {
                    if (!keptAtLeastOne) {
                        symbolsBuffer.addUnsignedByte(category);
                        keptAtLeastOne = true;
                    }
                    membersBuffer.reset();
                    membersBuffer.addUnsignedShort(modifiers);
                    membersBuffer.addUnsignedShort(getOffset());
                    membersBuffer.addUtf8(getName());
                    membersBuffer.addUnsignedInt(fieldType.getClassID());
                    symbolsBuffer.add(membersBuffer);
                } else if (Klass.DEBUG && Tracer.isTracing("pruning")) {
                    Tracer.trace("  discarded metadata for field: " + SuiteManager.getKlass(getSignatureAt(0)).getInternalName() + " " + getName());
                    if (Modifier.hasConstant(modifiers)) {
                        Tracer.trace(" [constantValue=" + getConstantValue() + "]");
                    }
                    Tracer.traceln("");
                }

            }
        }
        return keptAtLeastOne ? 1 << category : 0;
    }

    /**
     * Prunes the methods based on a given suite type.
     *
     * @param type      specifies a closed suite type. Must be {@link Suite#LIBRARY} or {@link Suite#EXTENDABLE_LIBRARY}.
     * @param category  specifies virtual or static methods
     * @return an integer with the only the bit in position 'category' set if at least one method was not pruned othwerwise 0
     */
    private int pruneMethods(int type, int category) {
        Assert.that(category == VIRTUAL_METHODS || category == STATIC_METHODS);
        int count = getMemberCount(category);
        boolean keptAtLeastOne = false;
        if (count != 0) {
            for (int i = 0; i != count; ++i) {
                select(category, i);
                int modifiers = getModifiers();
                String name = getName();
                if (retainMember(type, modifiers, null) || (VM.isHosted() && (name.startsWith("_SQUAWK_INTERNAL") || name.startsWith("do_")))) {
                    if (!keptAtLeastOne) {
                        symbolsBuffer.addUnsignedByte(category);
                        keptAtLeastOne = true;
                    }
                    membersBuffer.reset();
                    membersBuffer.addUnsignedShort(modifiers);
                    membersBuffer.addUnsignedShort(getOffset());
                    membersBuffer.addUtf8(name);
                    int sigCount = getSignatureCount();
                    for (int j = 0; j != sigCount; ++j) {
                        membersBuffer.addUnsignedInt(getSignatureAt(j));
                    }
                    symbolsBuffer.add(membersBuffer);
                } else if (Klass.DEBUG && Tracer.isTracing("pruning")) {

                    String signature = getName();
                    int parameterCount = getSignatureCount() - 1;
                    if (parameterCount == 0) {
                        signature += "()";
                    } else {
                        StringBuffer buf = new StringBuffer(15);
                        buf.append('(');
                        for (int j = 0 ; j < parameterCount ; j++) {
                            Klass parameterType = SuiteManager.getKlass(getSignatureAt(j + 1));
                            buf.append(parameterType.getInternalName());
                            if (j != parameterCount - 1) {
                                buf.append(',');
                            }
                        }
                        buf.append(')');
                        signature += buf.toString();
                    }
                    signature = SuiteManager.getKlass(getSignatureAt(0)).getInternalName() + " " + signature;
                    Tracer.traceln("  discarded metadata for method: " + signature);
                }
            }
        }
        return keptAtLeastOne ? 1 << category : 0;
    }

    /*---------------------------------------------------------------------------*\
     *                                 Fields                                    *
    \*---------------------------------------------------------------------------*/

    /**
     * The offsets to the method and field sections.
     */
    private short[] sectionStart = new short[STATIC_METHODS + 1];

    /**
     * The number of entries in each method and field section.
     */
    private short[] sectionCount = new short[STATIC_METHODS + 1];

    /**
     * The identifier of the currently selected method or field.
     */
    private short selection;

    /**
     * Flags whether or not the sections have been parsed.
     */
    private boolean sectionsParsed;

    /**
     * The access flags of the currently selected method or field.
     */
    private int modifiers;

    /**
     * The offset of the currently selected method or field.
     */
    private int offset;

    /**
     * The index of the name of the currently selected method or field.
     */
    private short nameStart;

    /**
     * The number of characters in the name (which may differ from its
     * UTF8 encoded length).
     */
    private int nameLength;

    /**
     * The index of the type of the currently selected field or
     * return type for the currently selected method.
     */
    private short signatureStart;

    /**
     * The number of components in the signature of the currently
     * selected field or method. If a method is currently selected, then
     * the first component of the signature is the return type and the
     * remainder of the signature is the parameter types. If a field
     * is currently selected, then there is only one component in the
     * signature which is the field's declared type. If the field has a constant
     * value then 'signatureCount - 1' is the data size (in bytes) of the value.
     */
    private short signatureCount;

    /**
     * Reads an encoded class identifier from the symbols.
     *
     * @return  the decoded class identifier
     */
    private int readClassID() {
        return readUnsignedInt();
    }


    /*---------------------------------------------------------------------------*\
     *                      Section selection and parsing                        *
    \*---------------------------------------------------------------------------*/

    /**
     * Reconfigures the parser based on a new symbols array.
     *
     * @param  symbols  the new symbols that will be parsed
     */
    private void setup(byte[] symbols) {
        this.buf = symbols;
        this.pos = 0;
        sectionsParsed = false;
        selection = -1;
    }

    /**
     * Parses a single member section. The current parse position must be
     * at the section identifier byte.
     */
    private int parseSection() {
        int section = buf[pos];
        Assert.that(section >= INSTANCE_FIELDS && section <= STATIC_METHODS);
        pos++;
        sectionStart[section] = (short)pos;
        sectionCount[section] = 0;
        while (pos < buf.length) {
            int lengthOrSection = buf[pos];
            /*
             * Is the length actually a new section header?
             */
            if (lengthOrSection >= 0 &&
                lengthOrSection <= STATIC_METHODS) {
                break;
            }
            lengthOrSection = readUnsignedShort();
            pos += lengthOrSection;
            sectionCount[section]++;
        }
        return section;
    }

    /**
     * Parses the member sections.
     */
    private void parseSections() {
        if (Klass.DEBUG) {
            KlassMetadata.Debug.parses++;
        }
        if (!sectionsParsed) {
            if (Klass.DEBUG) {
                KlassMetadata.Debug.parseMisses++;
            }
            sectionStart[STATIC_FIELDS]   = 0;
            sectionCount[STATIC_FIELDS]   = 0;
            sectionStart[INSTANCE_FIELDS] = 0;
            sectionCount[INSTANCE_FIELDS] = 0;
            sectionStart[STATIC_METHODS]  = 0;
            sectionCount[STATIC_METHODS]  = 0;
            sectionStart[VIRTUAL_METHODS] = 0;
            sectionCount[VIRTUAL_METHODS] = 0;
            pos = 1;
            while (pos < buf.length) {
                parseSection();
            }
            sectionsParsed = true;
        }
    }

    /**
     * Determines if a given member section is empty.
     *
     * @param   category  the section of class members to test
     * @return  true if section <code>category</code> is empty
     */
    private boolean isSectionEmpty(int category) {
        return (buf[0] & (1 << category)) == 0;
    }

    /**
     * Gets the number entries in a section of fields or methods.
     *
     * @param   category  the section of class members to count
     * @return  the number of fields or methods counted
     */
    int getMemberCount(int category) {
        if (buf == null ||isSectionEmpty(category)) {
            return 0;
        }
        parseSections();
        return sectionCount[category];
    }

    /**
     * Gets the identifier for a field or method.
     *
     * @param   category  the section of class members to search
     * @param   index  the index of the required field or method
     * @return  the identifer of the indexed field or method
     */
    int getMemberID(int category, int index) {
        Assert.that(!isSectionEmpty(category));
        parseSections();
        Assert.that(sectionCount[category] > index);
        pos = sectionStart[category];
        while (index-- > 0) {
            int length = readUnsignedShort();
            pos += length;
        }
        return pos;
    }

    /*---------------------------------------------------------------------------*\
     *                    Field or method component selection                    *
    \*---------------------------------------------------------------------------*/

    /**
     * Select a field or method.
     *
     * @param   category  the section of class members to search
     * @param   index     the index of the field or method to select
     */
    private void select(int category, int index) {
        int id = getMemberID(category, index);
        Assert.that(id > 0);
        select(id);
    }

    /**
     * Select a field or method.
     *
     * @param   id  the identifier of the fields or method to select
     * @return  the identifier of the next field or method (this value is only valid
     *              if the category has more members)
     */
    private int select(int id) {
        if (Klass.DEBUG) {
            KlassMetadata.Debug.selects++;
        }
        int next = -1;
        if (selection != id) {
            if (Klass.DEBUG) {
                KlassMetadata.Debug.selectMisses++;
            }
            selection  = (short)id;
            pos        = (short)id;
            int length = readUnsignedShort();
            next       = pos + length;
            modifiers  = readUnsignedShort();
            offset     = readUnsignedShort();
            nameLength = readUnsignedShort();
            nameStart  = (short)pos;
            for (int i = 0 ; i < nameLength ; i++) {
                readChar();
            }
            signatureStart = (short)pos;

            if (Modifier.hasConstant(modifiers)) {
                readClassID();
                signatureCount = (short)(1 + (next - pos));
            } else {
                signatureCount = 0;
                while (pos < next) {
                    readClassID();
                    signatureCount++;
                    Assert.that(pos <= next);
                }
            }
            pos = nameStart;
            //int ch = buf[pos]; // First character of name
        } else {
            pos = (short)id;
            int length = readUnsignedShort();
            next = pos + length;
        }
        return next;
    }


    /**
     * Parses the method or field signature the parser is currently
     * positioned at. The parsing is cut short if the signature does not
     * match the given signature components.
     *
     * @param   name               the name component of a signature
     * @param   parameterTypes     the parameter types of a method
     *                             signature. This will be a zero length
     *                             array when parsing a field signature
     * @param   returnOrFieldType  the return type of a method signature or
     *                             the type of a field signature
     * @param   end                the index in symbols of the byte
     *                             immediately the signature
     * @return  true if the parsed signature matches the given signature
     *                             components
     */
    private boolean matchSignature(String name, Klass[] parameterTypes, Klass returnOrFieldType, int end) {
        if (nameLength != name.length()) {
            return false;
        }
        nameStart = (short)pos;
        for (int i = 0 ; i < nameLength ; i++) {
            int c = readChar();
            if (c != name.charAt(i)) {
                return false;
            }
        }
        signatureStart = (short)pos;
        signatureCount = 1;
        if (readClassID() == returnOrFieldType.getClassID() ||
            (name.equals("<init>") && returnOrFieldType == Klass.VOID)) {

            if (Modifier.hasConstant(modifiers)) {
                signatureCount += (short)(end - pos);
                return true;
            }
            for (int i = 0 ; i < parameterTypes.length && pos < end ; i++) {
                if (readClassID() != parameterTypes[i].getClassID()) {
                    return false;
                }
                signatureCount++;
            }
            return pos == end && signatureCount == (parameterTypes.length + 1);
        } else {
            return false;
        }
    }

    /**
     * Searches for a method or field based on a signature and returns the
     * identifier of the method or field if it was found or -1 otherwise.
     *
     * @param   category           the section of class members to search
     * @param   name               the name component of a signature
     * @param   parameterTypes     the parameter types of a method
     *                             signature. This will be a zero length
     *                             array when parsing a field signature
     * @param   returnOrFieldType  the return type of a method signature or
     *                             the type of a field signature
     * @return  the identifier of the method or field found that matches
     *          the given signature or -1 if none was found
     */
    int lookupMember(int category, String name, Klass[] parameterTypes, Klass returnOrFieldType) {
        if (isSectionEmpty(category)) {
            return -1;
        }
        parseSections();
        int count = this.sectionCount[category];
        pos = sectionStart[category];
        for (int index = 0; index != count; ++index) {
            selection  = (short)pos;
            int length = readUnsignedShort();
            int end    = pos + length;
            modifiers  = readUnsignedShort();
            offset     = readUnsignedShort();
            nameLength = readUnsignedShort();
            if (matchSignature(name, parameterTypes, returnOrFieldType, end)) {
                return selection;
            }
            pos = (short)end;
        }
        this.selection = -1;
        return -1;
    }

    /**
     * Searches for a method or field based on an offset and type and returns the
     * identifier of the method or field if it was found or -1 otherwise.
     *
     * @param   category           the section of class members to search
     * @param   offset             the offset to match
     * @param   typeID             the class ID of the type to match if looking for a field (offsets are unique for methods)
     * @return  the identifier of the method or field found that matches
     *          the given offset and type or -1 if none was found
     */
    int lookupMember(int category, int offset, int typeID) {
        if (isSectionEmpty(category)) {
            return -1;
        }
        parseSections();
        int count = this.sectionCount[category];
        pos = sectionStart[category];
        int id = pos;
        for (int index = 0; index != count; ++index) {
            int nextID = select(id);
            if (this.offset == offset) {
                if (category == STATIC_METHODS || category == VIRTUAL_METHODS) {
                    return id;
                }
                if (getSignatureAt(0) == typeID) {
                    return id;
                }
            }
            id = nextID;
        }
        this.selection = -1;
        return -1;
    }

    /*---------------------------------------------------------------------------*\
     *                   Field or method component accessors                     *
    \*---------------------------------------------------------------------------*/

    /**
     * Gets the access flags for the currently selected field or method.
     *
     * @return a mask of the constants defined in {@link Modifier}
     */
    int getModifiers() {
        Assert.that(selection != -1);
        return modifiers;
    }

    /**
     * Gets the offset of the currently selected field or method.
     *
     * @return the offset of the currently selected field or method
     */
    int getOffset() {
        Assert.that(selection != -1);
        return offset & 0xFFFF;
    }

    /**
     * Gets the name of the currently selected field or method.
     *
     * @return  the name of the currently selected field or method
     */
    String getName() {
        Assert.that(selection != -1);
        char[] chars = new char[nameLength];
        pos = nameStart;
        for (int i = 0; i != chars.length; ++i) {
            chars[i] = readChar();
        }
        return new String(chars);
    }

    /**
     * Gets the number of types in the signature of the currently selected
     * field or method.
     *
     * @return the number of types in the signature of the currently
     *         selected field or method
     */
    int getSignatureCount() {
        Assert.that(selection != -1);
        return signatureCount;
    }

    /**
     * Gets a type from the signature of the currently selected field
     * or method.
     *
     * @param  index  the index of the type to retrieve
     * @return the class ID of the type at index <code>index</code> in the
     *                signature of the currently selected field or method
     */
    int getSignatureAt(int index) {
        Assert.that(selection != -1);
        Assert.that(index >= 0);
        Assert.that(index < signatureCount);
        pos = signatureStart;
        int res = -1;
        while (index-- >= 0) {
            res = readClassID();
        }
        return res;
    }

    /**
     * Gets the constant value of a field.
     *
     * @return the primitive constant value of this field encoded in a long
     */
    public long getConstantValue() {
        Assert.that(Modifier.hasConstant(modifiers));
        pos = signatureStart;
        readClassID();
        int dataSize = (signatureCount - 1);
        Assert.that(dataSize > 0);
        long value = 0;
        for (int bite = 0; bite != dataSize; ++bite) {
            int shift = bite * 8;
            long b = nextByte() & 0xFF;
            value |= (b << shift);
        }
        return value;
    }

}



