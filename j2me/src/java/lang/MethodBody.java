/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */
package java.lang;

import com.sun.squawk.vm.*;
import com.sun.squawk.util.Assert;

/**
 * An instance of <code>MethodBody</code> represents the Squawk bytecode for
 * a method as well as all the other information related to the bytecode
 * such as exception handler tables, oop map for the activation frame etc.
 *
 * @author  Doug Simon, Nik Shaylor
 */
public final class MethodBody {

    /**
     * Configuration option.
     * <p>
     * If set true then a long or double local variable will be
     * referenced as slot+0. If set false then it is addressed as slot+1.
     * <p>
     * Setting this false is will produce the correct offsets when the locals
     * are allocated at a negative offset from the frame pointer (which is common
     * for virtually all C ABIs).
     */
    public final static boolean LOCAL_LONG_ORDER_NORMAL = false;

    /**
     * The enclosing method.
     */
    private final Method definingMethod;

    /**
     * The index of this method's definition in the symbols table
     * of its defining class.
     */
    private final int index;

    /**
     * The maximum size (in words) of the operand stack during execution
     * of this method.
     */
    private final int maxStack;

    /**
     * The number of words required by the parameters.
     */
    private final int parametersCount;

    /**
     * The exception handler table.
     */
    private final ExceptionHandler[] exceptionTable;

    /**
     * The debug information for the method.
     */
    private final MethodMetadata metadata;

    /**
     * The type map of the parameters and locals.
     */
    private final Klass[] localTypes;

    /**
     * The Squawk bytecode.
     */
    private final byte[] code;

/*if[TYPEMAP]*/
    /**
     * The type map describing the type of the value (if any) written to memory by each instruction in 'code'.
     */
    private final byte[] typeMap;
/*end[TYPEMAP]*/

    /**
     * Creates a <code>MethodBody</code> representing the implementation details
     * of a method.
     *
     * @param definingMethod    the method in which the method body was defined
     * @param index             the index of the method in the symbols table
     * @param maxStack          the maximum size in words of the operand stack
     * @param locals            the types of the local variables (excludes parameters)
     * @param exceptionTable    the exception handler table
     * @param lnt               the table mapping instruction addresses to the
     *                          source line numbers that start at the addresses.
     *                          The table is encoded as an int array where the high
     *                          16-bits of each element is an instruction address and
     *                          the low 16-bits is the corresponding source line
     * @param lvt               the table describing the symbolic information for
     *                          the local variables in the method
     * @param code              the Squawk bytecode
     * @param typeMap           the type map describing the type of the value (if any) written
     *                          to memory by each instruction in 'code'
     * @param reverseParameters true if the parameters are pushed right-to-left
     */
    public MethodBody(
                       Method                definingMethod,
                       int                   index,
                       int                   maxStack,
                       Klass[]               locals,
                       ExceptionHandler[]    exceptionTable,
                       int[]                 lnt,
/*if[SCOPEDLOCALVARIABLES]*/
                       ScopedLocalVariable[] lvt,
/*end[SCOPEDLOCALVARIABLES]*/
                       byte[]                code,
/*if[TYPEMAP]*/
                       byte[]                typeMap,
/*end[TYPEMAP]*/
                       boolean               reverseParameters
                     ) {
        this.definingMethod  = definingMethod;
        this.index           = index;
        this.maxStack        = maxStack;
        this.exceptionTable  = exceptionTable;
        this.metadata        = new MethodMetadata(
                                                   definingMethod.getID(),
/*if[SCOPEDLOCALVARIABLES]*/
                                                   lvt,
/*end[SCOPEDLOCALVARIABLES]*/
                                                   lnt
                                                 );
        this.code = code;
/*if[TYPEMAP]*/
        this.typeMap = typeMap;
/*end[TYPEMAP]*/

        /*
         * Make an array of classes with both the parameter and local types.
         */
        Klass[] parms   = definingMethod.getRuntimeParameterTypes(reverseParameters);
        parametersCount = parms.length;

        localTypes = new Klass[parms.length+locals.length];

        int j = 0;
        for (int i = 0 ; i < parms.length ; i++, j++) {
            localTypes[j] = parms[i];
        }
        for (int i = 0 ; i < locals.length ; i++, j++) {
            localTypes[j] = locals[i];
        }

        Assert.that(parametersCount >= 0);
        Assert.that(maxStack >= 0);
    }

    /**
     * Produce String for debugging
     *
     * @return the string
     */
    public String toString() {
        return "[bytecode for "+definingMethod.getDefiningClass().getName()+"."+definingMethod.getName();
    }

    /**
     * Gets the index of this method's definition in the symbols table
     * of its defining class.
     *
     * @return  the index of this method's definition
     */
    public int getIndex() {
        return index;
    }

    /**
     * Gets the bytecode.
     *
     * @return the bytecode
     */
    public byte[] getCode() {
        return code;
    }

/*if[TYPEMAP]*/
    /**
     * Gets the type map describing the types in activation frame expected by each bytecode.
     *
     * @return the type map describing the types in activation frame expected by each bytecode
     */
    public byte[] getTypeMap() {
        return typeMap;
    }
/*end[TYPEMAP]*/

    /**
     * Get the type map.
     *
     * @return the type map
     */
    public Klass[] getTypes() {
        return localTypes;
    }

    /**
     * Gets the class that defined this method.
     *
     * @return the class that defined this method
     */
    public Method getDefiningMethod() {
        return definingMethod;
    }

    /**
     * Gets the class that defined this method.
     *
     * @return the class that defined this method
     */
    public Klass getDefiningClass() {
        return definingMethod.getDefiningClass();
    }

    /**
     * Get the number of parameters.
     *
     * @return the number
     */
    public int getParametersCount() {
        return parametersCount;
    }

    /**
     * Get the exception table.
     *
     * @return the number
     */
    public ExceptionHandler[] getExceptionTable() {
        return exceptionTable;
    }

    /**
     * Get the number of stack words needed.
     *
     * @return the number
     */
    public int getMaxStack() {
        return maxStack;
    }

    /**
     * Gets the debug information (if any) pertaining to this method body.
     *
     * @return  the debug information pertaining to this method body or null
     *          if there isn't any
     */
    public MethodMetadata getMetadata() {
        return metadata;
    }


    /*-----------------------------------------------------------------------*\
     *                                Encoding                               *
    \*-----------------------------------------------------------------------*/

    /**
     * Minfo format encodings.
     */
    private final static int FMT_LARGE   = 0x80,   // specifies a large minfo section
                             FMT_E       = 0x01,   // specifies that there is an exception table
                             FMT_R       = 0x02,   // specifies that there is a relocation table
                             FMT_T       = 0x04;   // specifies that there is an type table

    /**
     * Encode the method header. The format of the header is described by the
     * following pseudo-C structures:
     * <p><hr><blockquote><pre>
     *  header {
     *      {
     *           u4 type;                               // class id of a float, long, double, Address or UWord local variable
     *           u4 index;                              // index of the local variable
     *      } type_table[type_table_size];
     *      {                                           // TBD: not yet designed/used
     *      } relocation_table[relocation_table_size];
     *      {
     *          u4 start_pc;
     *          u4 end_pc;
     *          u4 handler_pc;
     *          u4 catch_type;
     *      } exception_table[exception_table_size];
     *      u1 oopMap[oopMap_size];
     *      union {
     *          {
     *              u1 lo;      //  lllsssss
     *              u1 hi;      //  0pppppll
     *          } small_minfo;  //  'lllll' is locals_count, 'sssss' is max_stack, 'ppppp' is parameters_count
     *          {
     *              minfo_size type_table_size;         // exists only if 'T' bit in 'fmt' is set
     *              minfo_size relocation_table_size;   // exists only if 'R' bit in 'fmt' is set
     *              minfo_size exception_table_size;    // exists only if 'E' bit in 'fmt' is set
     *              minfo_size parameters_count;
     *              minfo_size locals_count;
     *              minfo_size max_stack;
     *              u1 fmt;                             // 10000TRE
     *          } large_minfo;
     *      }
     *  }
     *
     * The minfo_size type is a u1 value if its high bit is 0, otherwise its a u2 value where
     * the high bit is masked off.
     *
     * </pre></blockquote><hr><p>
     *
     * The structures described above are actually stored in a byte array
     * encoded and decoded with a {@link ByteBufferEncoder} and
     * {@link ByteBufferDecoder} respectively.
     *
     * @param enc encoder
     */
    void encodeHeader(ByteBufferEncoder enc) {
        int start ;
        int localsCount = localTypes.length - parametersCount;

        /*
         * Encode the type table.
         */
        start = enc.getSize();
        for (int i = 0 ; i < localTypes.length ; i++) {
            Klass k = localTypes[i];
            if (k == Klass.FLOAT || k == Klass.DOUBLE || k == Klass.LONG || k.isSquawkPrimitive()) {
                enc.addUnsignedInt(k.getClassID());
                enc.addUnsignedInt(i);
            }
        }
        int typeTableSize = enc.getSize() - start;

        /*
         * Encode the relocation table.
         */
        start = enc.getSize();
        int relocTableSize = enc.getSize() - start;

        /*
         * Encode the exception table.
         */
        start = enc.getSize();
        if (exceptionTable != null) {
            for(int i = 0 ; i < exceptionTable.length ; i++) {
                ExceptionHandler handler = exceptionTable[i];
                enc.addUnsignedInt(handler.getStart());
                enc.addUnsignedInt(handler.getEnd());
                enc.addUnsignedInt(handler.getHandler());
                enc.addUnsignedInt(handler.getKlass().getClassID());
            }
        }
        int exceptionTableSize = enc.getSize() - start;

        /*
         * Encode the oopmap.
         */
        start = enc.getSize();
        int count = localTypes.length;
        int next = 0;
        while (count > 0) {
            int bite = 0;
            int n = (count < 8) ? count : 8;
            count -= n;
            for (int i = 0 ; i < n ; i++) {
                Klass k = localTypes[next++];
                if (k.isReferenceType()) {
                    bite |= (1<<i);
                }
            }
            enc.addUnsignedByte(bite);
        }
        int oopMapSize = enc.getSize() - start;

        Assert.that(oopMapSize == ((localsCount+parametersCount+7)/8));
        Assert.that(typeTableSize      < 32768);
        Assert.that(relocTableSize     < 32768);
        Assert.that(exceptionTableSize < 32768);
        Assert.that(localsCount        < 32768);
        Assert.that(parametersCount    < 32768);
        Assert.that(maxStack           < 32768);

        /*
         * Write the minfo area.
         *
         * The minfo is written in reverse. There are two formats, a compact one where there is no
         * type table, relocation table, exception table, and the number of words for local variables,
         * parameters, and stack are all less than 32 words, and there is a large format where the only
         * limits are that none of these values may exceed 32767.
         */
        if (
            localsCount        < 32 &&
            parametersCount    < 32 &&
            maxStack           < 32 &&
            typeTableSize      == 0 &&
            relocTableSize     == 0 &&
            exceptionTableSize == 0
           ) {
            /*
             * Small Minfo
             */
            enc.addUnencodedByte((localsCount<<5)     | (maxStack));         // byte 1 - lllsssss
            enc.addUnencodedByte((parametersCount<<2) | (localsCount>>3));   // byte 0 - 0pppppll
        } else {
            /*
             * Large Minfo
             */
            int fmt = FMT_LARGE;
            if (typeTableSize > 0) {
                writeMinfoSize(enc, typeTableSize);
                fmt |= FMT_T;
            }
            if (relocTableSize > 0) {
                writeMinfoSize(enc, relocTableSize);
                fmt |= FMT_R;
            }
            if (exceptionTableSize > 0) {
                writeMinfoSize(enc, exceptionTableSize);
                fmt |= FMT_E;
            }
            writeMinfoSize(enc, parametersCount);
            writeMinfoSize(enc, localsCount);
            writeMinfoSize(enc, maxStack);
            enc.addUnsignedByte(fmt);
        }
    }

    /**
     * Roundup the data in the ByteBufferEncoder so that it is modulo HDR.BYTES_PER_WORD in length
     * after some extra data is added.
     *
     * @param enc the encoder
     * @param extra the number of bytes that will be added
     */
    private void roundup(ByteBufferEncoder enc, int extra) {
        while ((enc.getSize()+extra) % HDR.BYTES_PER_WORD != 0) {
            enc.addUnsignedByte(0);
        }
    }

    /**
     * Write a length into the minfo
     *
     * @param enc the encoder
     * @param value the value
     */
    private void writeMinfoSize(ByteBufferEncoder enc, int value) {
        if (value < 128) {
            enc.addUnsignedByte(value);
        } else {
            Assert.that(value < 32768);
            enc.addUnsignedByte(value & 0xFF);
            enc.addUnsignedByte(0x80|(value>>8));
        }
    }

    /**
     * Return size of the method byte array.
     *
     * @return the size in bytes
     */
    int getCodeSize() {
        return code.length;
    }

    /**
     * Write the bytecodes to VM memory.
     *
     * @param oop address of the method object
     */
    void writeToVMMemory(Object oop) {
        for (int i = 0 ; i < code.length ; i++) {
            Unsafe.setByte(oop, i, code[i]);
        }
    }

/*if[TYPEMAP]*/
    /**
     * Write the type map for the bytecodes to VM memory.
     *
     * @param oop address of the method object
     */
    void writeTypeMapToVMMemory(Object oop) {
        Assert.always(VM.usingTypeMap());
        Address p = Address.fromObject(oop);
        for (int i = 0 ; i < typeMap.length ; i++) {
            Unsafe.setType(p, typeMap[i], 1);
            p = p.add(1);
        }
    }
/*end[TYPEMAP]*/

    /*-----------------------------------------------------------------------*\
     *                                Decoding                               *
    \*-----------------------------------------------------------------------*/

    /**
     * Decode the parameter count from the method header.
     *
     * @param oop the pointer to the method
     * @return the number of parameters
     */
    static int decodeParameterCount(Object oop) {
        int b0 = Unsafe.getByte(oop, HDR.methodInfoStart) & 0xFF;
        if (b0 < 128) {
            return b0 >> 2;
        } else {
            return minfoValue(oop, 3);
        }
    }

    /**
     * Decode the local variable count from the method header.
     *
     * @param oop the pointer to the method
     * @return the number of locals
     */
    static int decodeLocalCount(Object oop) {
        int b0 = Unsafe.getByte(oop, HDR.methodInfoStart) & 0xFF;
        if (b0 < 128) {
            int b1 = Unsafe.getByte(oop, HDR.methodInfoStart-1) & 0xFF;
            return (((b0 << 8) | b1) >> 5) & 0x1F;
        } else {
            return minfoValue(oop, 2);
        }
    }

    /**
     * Decode the stack count from the method header.
     *
     * @param oop the pointer to the method
     * @return the number of stack words
     */
    static int decodeStackCount(Object oop) {
        int b0 = Unsafe.getByte(oop, HDR.methodInfoStart) & 0xFF;
        if (b0 < 128) {
            int b1 = Unsafe.getByte(oop, HDR.methodInfoStart-1) & 0xFF;
            return b1 & 0x1F;
        } else {
            return minfoValue(oop, 1);
        }
    }

    /**
     * Decode the exception table size from the method header.
     *
     * @param oop the pointer to the method
     * @return the number of bytes
     */
    static int decodeExceptionTableSize(Object oop) {
        int b0 = Unsafe.getByte(oop, HDR.methodInfoStart) & 0xFF;
        if (b0 < 128 || ((b0 & FMT_E) == 0)) {
            return 0;
        }
        return minfoValue(oop, 4);
    }

    /**
     * Decode the relocation table size from the method header.
     *
     * @param oop the pointer to the method
     * @return the number of bytes
     */
    static int decodeRelocationTableSize(Object oop) {
        int b0 = Unsafe.getByte(oop, HDR.methodInfoStart) & 0xFF;
        if (b0 < 128 || ((b0 & FMT_R) == 0)) {
            return 0;
        }
        int offset = 4;
        if ((b0 & FMT_E) != 0) {
            offset++;
        }
        return minfoValue(oop, offset);
    }

    /**
     * Decode the type table size from the method header.
     *
     * @param oop the pointer to the method
     * @return the number of bytes
     */
    static int decodeTypeTableSize(Object oop) {
        int b0 = Unsafe.getByte(oop, HDR.methodInfoStart) & 0xFF;
        if (b0 < 128 || ((b0 & FMT_T) == 0)) {
            return 0;
        }
        int offset = 4;
        if ((b0 & FMT_E) != 0) {
            offset++;
        }
        if ((b0 & FMT_R) != 0) {
            offset++;
        }
        return minfoValue(oop, offset);
    }

    /**
     * Decode a counter from the minfo area
     *
     * @param oop the pointer to the method
     * @param offset the ordinal offset of the counter (e.g. 1st, 2nd, ...  etc.)
     * @return the value
     */
    private static int minfoValue(Object oop, int offset) {
        int p = HDR.methodInfoStart;
        int b = Unsafe.getByte(oop, p--) & 0xFF;
        int val = -1;
        Assert.that((b & FMT_LARGE) != 0);
        while(offset-- > 0) {
            val = Unsafe.getByte(oop, p--) & 0xFF;
            if (val > 127) {
                val = val & 0x7F;
                val = val << 8;
                val = val | Unsafe.getByte(oop, p--) & 0xFF;
            }
        }
        Assert.that(val >= 0);
        return val;
    }

    /**
     * Get the offset to the last byte of the Minfo area.
     *
     * @param oop the pointer to the method
     * @return the length in bytes
     */
    private static int getOffsetToLastMinfoByte(Object oop) {
        int p = HDR.methodInfoStart;
        int b0 = Unsafe.getByte(oop, p--) & 0xFF;
        if (b0 < 128) {
            p--;
        } else {
            int offset = 3;
            if ((b0 & FMT_E) != 0) {
                offset++;
            }
            if ((b0 & FMT_R) != 0) {
                offset++;
            }
            if ((b0 & FMT_T) != 0) {
                offset++;
            }
            while(offset-- > 0) {
                int val = Unsafe.getByte(oop, p--) & 0xFF;
                if (val > 127) {
                    p--;
                }
            }
        }
        return p + 1;
    }

    /**
     * Decode the offset from the method header to the start of the oop map.
     *
     * @param oop the pointer to the method
     * @return the offset in bytes
     */
    static int decodeOopmapOffset(Object oop) {
        int vars = decodeLocalCount(oop) + decodeParameterCount(oop);
        int oopmapLth = (vars+7)/8;
        return getOffsetToLastMinfoByte(oop) - oopmapLth;
    }

    /**
     * Decode the offset from the method header to the start of the exception table.
     *
     * @param oop the pointer to the method
     * @return the offset in bytes
     */
    static int decodeExceptionTableOffset(Object oop) {
        int vars = decodeLocalCount(oop) + decodeParameterCount(oop);
        int oopmapLth = (vars+7)/8;
        return getOffsetToLastMinfoByte(oop) - oopmapLth - decodeExceptionTableSize(oop);
    }

    /**
     * Decode the offset from the method header to the start of the relocation table.
     *
     * @param oop the pointer to the method
     * @return the offset in bytes
     */
    static int decodeRelocationTableOffset(Object oop) {
        int vars = decodeLocalCount(oop) + decodeParameterCount(oop);
        int oopmapLth = (vars+7)/8;
        return getOffsetToLastMinfoByte(oop) - oopmapLth - decodeExceptionTableSize(oop) - decodeRelocationTableSize(oop);
    }

    /**
     * Decode the offset from the method header to the start of the type table.
     *
     * @param oop the pointer to the method
     * @return the offset in bytes
     */
    static int decodeTypeTableOffset(Object oop) {
        int vars = decodeLocalCount(oop) + decodeParameterCount(oop);
        int oopmapLth = (vars+7)/8;
        return getOffsetToLastMinfoByte(oop) - oopmapLth - decodeExceptionTableSize(oop) - decodeRelocationTableSize(oop) - decodeTypeTableSize(oop);
    }

    /**
     * Decode the oopmap and type table into some simple class types.
     * <p>
     * This cannot be used by the garbage collector because
     * it allocates an object, but it is useful for the mapper and to
     * verify the bytecodes.
     * <p>
     * NOTE - It would be more logical for this routine to return an array
     * of Klasses, but this would cause class <code>Klass</code> to be
     * initialized and this cannot currently be done in the context of
     * the mapper program which uses this routine.
     * <p>
     * The return types are:
     * <pre>
     * 'I' = Klass.INT
     * 'J' = Klass.LONG
     * 'K' = Klass.LONG2
     * 'F' = Klass.FLOAT
     * 'D' = Klass.DOUBLE
     * 'E' = Klass.DOUBLE2
     * 'A' = Klass.ADDRESS
     * 'W' = Klass.WORD
     * 'R' = All reference types
     * </pre>
     *
     * @param oop the pointer to the method
     * @return an array of characters one for each type
     */
    static char[] decodeTypeMap(Object oop) {

        int localCount     = decodeLocalCount(oop);
        int parameterCount = decodeParameterCount(oop);

        char types[] = new char[parameterCount+localCount];

        /*
         * Decode the oopmap.
         */
        if (types.length > 0) {
            int offset = decodeOopmapOffset(oop);
            for (int i = 0 ; i < types.length ; i++) {
                int pos = i / 8;
                int bit = i % 8;
                int bite = Unsafe.getByte(oop, offset+pos) & 0xFF;
                boolean isRef = ((bite>>bit)&1) != 0;
                types[i] = (isRef) ? 'R' : 'I';
            }
        }

        /*
         * Decode the type table.
         */
        if (decodeTypeTableSize(oop) > 0) {
            int size   =  decodeTypeTableSize(oop);
            int offset =  decodeTypeTableOffset(oop);
            VMBufferDecoder dec = new VMBufferDecoder(oop, offset);
            int end = offset + size;
            while (dec.getOffset() < end) {
                int cid  = dec.readUnsignedInt();
                int slot = dec.readUnsignedInt();
                char k1 = 0;
                char k2 = 0;
                switch (cid) {
                    case CID.ADDRESS: k1 = 'A'; k2 = 0;      break;
                    case CID.UWORD:    k1 = 'W'; k2 = 0;      break;
                    case CID.LONG:    k1 = 'J'; k2 = 'K';    break;
                    case CID.FLOAT:   k1 = 'F'; k2 = 0;      break;
                    case CID.DOUBLE:  k1 = 'D'; k2 = 'E';    break;
//                    default: Assert.shouldNotReachHere();
default: k1 = (char)('0' + (cid/10)); k2 = (char)('0' + (cid % 10));
                }
                types[slot] = k1;
                if (k2 != 0) {
                    if (slot < parameterCount || LOCAL_LONG_ORDER_NORMAL) {
                        types[slot+1] = k2;
                    } else {
                        types[slot-1] = k2;
                    }
                }
            }
        }
        return types;
    }

    /**
     * Return the address of the first word of the object header.
     *
     * @param oop the pointer to the method
     * @return the VM address of the header
     */
    static Address oopToBlock(Object oop) {
        int offset = decodeTypeTableOffset(oop);
        while ((offset % HDR.BYTES_PER_WORD) != 0) {
            --offset;
        }
        offset -= HDR.BYTES_PER_WORD; // skip back the header length word
        return Address.fromObject(oop).add(offset);
    }


    /*-----------------------------------------------------------------------*\
     *                                Verifing                               *
    \*-----------------------------------------------------------------------*/

    /**
     * Verify a new method.
     *
     * @param oop the pointer to the encoded method
     */
    void verifyMethod(Object oop) {
/*if[J2ME.DEBUG]*/
        /*
         * Check the basic parameters.
         */
        int localCount = localTypes.length - parametersCount;
        Assert.that(decodeLocalCount(oop)     == localCount);
        Assert.that(decodeParameterCount(oop) == parametersCount);
        Assert.that(decodeStackCount(oop)     == maxStack);

        /*
         * Check the oopmap.
         */
        if (localTypes.length > 0) {
            int offset = decodeOopmapOffset(oop);
            for (int i = 0 ; i < localTypes.length ; i++) {
                Klass k = localTypes[i];
                int pos = i / 8;
                int bit = i % 8;
                int bite = Unsafe.getByte(oop, offset+pos) & 0xFF;
                boolean isOop = ((bite>>bit)&1) != 0;
                if (k.isReferenceType()) {
                    Assert.that(isOop == true);
                } else {
                    Assert.that(isOop == false);
                }
            }
        }

        /*
         * Check the exception table.
         */
        if (decodeExceptionTableSize(oop) == 0) {
            Assert.that(exceptionTable == null || exceptionTable.length == 0);
        } else {
            Assert.that(exceptionTable != null && exceptionTable.length > 0);
            int size   = decodeExceptionTableSize(oop);
            int offset = decodeExceptionTableOffset(oop);
            VMBufferDecoder dec = new VMBufferDecoder(oop, offset);
            for (int i = 0 ; i < exceptionTable.length ; i++) {
                ExceptionHandler handler = exceptionTable[i];
                Assert.that(dec.readUnsignedInt() == handler.getStart());
                Assert.that(dec.readUnsignedInt() == handler.getEnd());
                Assert.that(dec.readUnsignedInt() == handler.getHandler());
                Assert.that(dec.readUnsignedInt() == handler.getKlass().getClassID());
            }
            dec.checkOffset(offset + size);
        }

        /*
         * Check the type table.
         */
        if (decodeTypeTableSize(oop) == 0) {
            for (int i = 0 ; i < localTypes.length ; i++) {
                Klass k = localTypes[i];
                Assert.that(k != Klass.FLOAT && k != Klass.DOUBLE && k != Klass.LONG && !k.isSquawkPrimitive());
            }
        } else {
            int size   = decodeTypeTableSize(oop);
            int offset = decodeTypeTableOffset(oop);
            VMBufferDecoder dec = new VMBufferDecoder(oop, offset);
            for (int i = 0 ; i < localTypes.length ; i++) {
                Klass k = localTypes[i];
                if (k == Klass.FLOAT || k == Klass.DOUBLE || k == Klass.LONG || k.isSquawkPrimitive()) {
                    Assert.that(dec.readUnsignedInt() == k.getClassID());
                    Assert.that(dec.readUnsignedInt() == i);
                }
            }
            dec.checkOffset(offset + size);
        }

        /*
         * Check the relocation table.
         */
        Assert.that(decodeRelocationTableSize(oop) == 0);

        /*
         * Check the bytecodes.
         */
        Assert.that(GC.getArrayLengthNoCheck(oop) == code.length);
        if (!VM.usingTypeMap()) {
            for (int i = 0; i < code.length; i++) {
                Assert.that(Unsafe.getByte(oop, i) == code[i]);
            }
        }
/*end[J2ME.DEBUG]*/
    }

}
