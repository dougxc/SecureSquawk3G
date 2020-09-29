/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM translator.
 */
package com.sun.squawk.translator.ci;

import java.io.*;
import java.util.*;

import com.sun.squawk.translator.*;
import com.sun.squawk.translator.ir.*;
import com.sun.squawk.translator.ir.instr.*;
import com.sun.squawk.util.*;
import com.sun.squawk.util.Arrays;
import com.sun.squawk.util.Vector;    // Version without synchronization
import com.sun.squawk.util.Hashtable; // Version without synchronization


/**
 * An instance of <code>CodeParser</code> is used to decode the "Code" attribute
 * of a method.
 *
 * @author  Doug Simon
 */
public final class CodeParser implements Context {

    private static final ExceptionHandler[] NO_EXCEPTIONHANDLERS = {};

    /**
     * The method being processed by this parser.
     */
    private final Method method;

    /**
     * The constant pool used by elements of the "Code" attributes to
     * refer to other classes and constants.
     */
    private final ConstantPool constantPool;

    /**
     * The maximum depth of the operand stack at any point during
     * execution of the method.
     */
    private final int maxStack;

    /**
     * The number of local variables allocated upon invocation of the method.
     */
    private final int maxLocals;

    /**
     * The exception handlers for the method.
     */
    private final ExceptionHandler[] exceptionHandlers;

    /**
     * The reader used to read the parts of the code attribute including the
     * bytecode array.
     */
    private ClassFileReader cfr;

    /**
     * The input stream used to read the bytecode array that supports querying
     * the current read position.
     */
    private final IndexedInputStream bcin;

    /**
     * Vector of pseudo opcodes used in getLastPseudoOpcodes().
     */
    private Vector pseudoOpcodes = new Vector();

    /**
     * Creates a <code>CodeReader</code> instance to read and decode
     * the bytecode of a given method.
     *
     * @param  method       the method to be processed
     * @param  code         the data of the "Code" attribute for <code>method</code>
     * @param  constantPool the constant pool used by the elements of the
     *                      "Code" attribute
     */
    public CodeParser(Method method, byte[] code, ConstantPool constantPool) {
        this.method = method;
        this.constantPool = constantPool;

        Klass klass = method.getDefiningClass();
        String filePath =  ClassFileLoader.getClassFilePath(klass);
        cfr = new ClassFileReader(new ByteArrayInputStream(code), filePath);

        maxStack = cfr.readUnsignedShort("cod-maxStack");
        maxLocals = cfr.readUnsignedShort("cod-maxLocals");
        int codeLength = cfr.readInt("cod-length");

        if (codeLength <= 0) {
            throw cfr.formatError("the value of code_length must be greater than 0");
        } else if (codeLength >= 0xFFFF) {
            throw cfr.formatError("method code longer than 64 KB");
        }

        /*
         * Mark the position at which the bytecodes begin
         */
        bcin = new IndexedInputStream(code, 8, codeLength);
        cfr.skip(codeLength, null);

        /*
         * Read in the exception handlers
         */
        int ehCount = cfr.readUnsignedShort("hnd-handlers");
        if (ehCount != 0) {
            exceptionHandlers = new ExceptionHandler[ehCount];
            for (int i = 0; i < ehCount; i++) {
                int startPC = cfr.readUnsignedShort("hnd-startPC");
                int endPC = cfr.readUnsignedShort("hnd-endPC");
                int handlerPC = cfr.readUnsignedShort("hnd-handlerPC");
                int catchIndex = cfr.readUnsignedShort("hnd-catchIndex");

                /*
                 * Verify that all the addresses are valid
                 */
                if (startPC >= codeLength || endPC > codeLength || startPC >= endPC || handlerPC >= codeLength) {
                    throw cfr.formatError( "invalid exception handler code range");
                }

                Klass catchType = null;
                if (catchIndex != 0) {
                    catchType = constantPool.getResolvedClass(catchIndex, this);
                    if (!Klass.THROWABLE.isAssignableFrom(catchType)) {
                        throw verifyError("invalid exception handler type");
                    }
                }
                exceptionHandlers[i] = new ExceptionHandler(startPC, endPC, handlerPC, catchType);
            }
        } else {
            exceptionHandlers = NO_EXCEPTIONHANDLERS;
        }

        /*
         * Read in the code attributes
         */
        Vector lvt = null;
        Vector lnt = null;
        IntHashtable sm = null;
        int attributesCount = cfr.readUnsignedShort("cod-attributesCount");
        for (int i = 0; i < attributesCount; i++) {
            int attributeNameIndex = cfr.readUnsignedShort("cod-attributeNameIndex");
            int attributeLength    = cfr.readInt("cod-attributeLength");
            String attributeName   = constantPool.getUtf8(attributeNameIndex);
            if (attributeName.equals("StackMap")) {
                sm = StackMap.loadStackMap(this, cfr, constantPool);
            } else if (attributeName.equals("LineNumberTable")) {
                lnt = loadLineNumberTable(codeLength);
/*if[SCOPEDLOCALVARIABLES]*/
            } else if (attributeName.equals("LocalVariableTable")) {
                lvt = loadLocalVariableTable(codeLength);
/*end[SCOPEDLOCALVARIABLES]*/
            } else {
                cfr.skip(attributeLength, attributeName);
            }
        }
/*if[SCOPEDLOCALVARIABLES]*/
        localVariableTable = lvt;
/*end[SCOPEDLOCALVARIABLES]*/
        lineNumberTable = lnt;
        targets = sm;

        /*
         * Reset the class file reader to now read the bytecode array
         */
        cfr = new ClassFileReader(bcin, filePath);
    }

    /**
     * Gets another parser for the "Code" attribute being parsed by this
     * parser.
     *
     * @return a copy of this parser
     */
    public CodeParser reset() {
        return new CodeParser(method, bcin.getBuffer(), constantPool);
    }


    /*---------------------------------------------------------------------------*\
     *                  Temporary types for uninitialized instances              *
    \*---------------------------------------------------------------------------*/

    /**
     * The table of interned types representing uninitialised objects created
     * by <i>new</i> bytecodes.
     */
    private Hashtable UninitializedObjectClasses;

    /**
     * Gets an <code>UninitializedObjectClass</code> instance representing the
     * type pushed to the stack by a <i>new</i> bytecode.
     *
     * @param   address        the address of the <i>new</i> bytecode
     * @param   uninitializedType  the class specified by the operand of the
     *                         <i>new</i> bytecode
     * @return a type representing the uninitialized object of type
     *                         <code>uninitializedType</code> created by a
     *                         <i>new</i> bytecode at address
     *                         <code>address</code>
     */
    public UninitializedObjectClass getUninitializedObjectClass(int address, Klass uninitializedType) {
        if (UninitializedObjectClasses == null) {
            UninitializedObjectClasses = new Hashtable();
        }
        String name = "new@" + address;
        UninitializedObjectClass klass = (UninitializedObjectClass)UninitializedObjectClasses.get(name);
        if (klass == null) {
            klass = new UninitializedObjectClass(name, uninitializedType);
            UninitializedObjectClasses.put(name, klass);
        } else if (!klass.hasInitializedTypeBeenSet()) {
            klass.setInitializedType(uninitializedType);
        }
        return klass;
    }


    /*---------------------------------------------------------------------------*\
     *             Positions for LocalVariableTable and LineNumberTable          *
    \*---------------------------------------------------------------------------*/

    /**
     * The table of positions within the bytecodes that are referenced by the
     * "LocalVariableTable" and "LineNumberTable" attributes.
     */
    private IntHashtable positions;

    /**
     * Gets a <code>Position</code> instance representing a logical
     * instruction position whose physical address may change as the
     * bytecodes are transformed from JVM bytecodes to Squawk bytecodes.
     *
     * @param   address  the address of the position in the JVM bytecode
     * @return  the relocatable position denoted by <code>address</code>
     */
    private Position getPosition(int address) {
        if (positions == null) {
            positions = new IntHashtable();
        }
        Position position = (Position)positions.get(address);
        if (position == null) {
            position = new Position(address);
            positions.put(address, position);
        }
        return position;
    }

    /**
     * Gets the bytecode positions in a sorted array.
     *
     * @return the bytecode positions in a sorted array
     */
    private Position[] sortPositions() {
        Assert.that(positions != null);
        Position[] sorted = new Position[positions.size()];
        Enumeration e = positions.elements();
        for (int i = 0; i != sorted.length; ++i) {
            sorted[i] = (Position)e.nextElement();
        }
        Arrays.sort(sorted, new Comparer() {
            public int compare(Object o1, Object o2) {
                return ((Position)o1).getBytecodeOffset() - ((Position)o2).getBytecodeOffset();
            }
        });
        return sorted;
    }


    /*---------------------------------------------------------------------------*\
     *                            LocalVariableTable                             *
    \*---------------------------------------------------------------------------*/

/*if[SCOPEDLOCALVARIABLES]*/

    /**
     * The (optional) local variable table for the method.
     */
    private final Vector localVariableTable;

    /**
     * Load the "LocalVariableTable" attribute the code parser is
     * currently positioned at.
     *
     * @param  codeLength    the length of the bytecode array for the
     *                       enclosing method
     * @return the table as a vector of <code>LocalVariableTableEntry</code>
     */
    private Vector loadLocalVariableTable(int codeLength) {
        int count = cfr.readUnsignedShort("lvt-localVariableTableLength");
        Vector table = new Vector(count);
        for (int i = 0; i != count; ++i) {
            int start_pc = cfr.readUnsignedShort("lvt-startPC");
            if (start_pc >= codeLength) {
                throw cfr.formatError("start_pc of LocalVariableTable is out of range");
            }
            int length = cfr.readUnsignedShort("lvt-length");
            if (start_pc+length > codeLength) {
                throw cfr.formatError("start_pc+length of LocalVariableTable is out of range");
            }
            String name = constantPool.getUtf8(cfr.readUnsignedShort("lvt-nameIndex"));
            String desc = constantPool.getUtf8(cfr.readUnsignedShort("lvt-descriptorIndex"));
            Klass type = Translator.getClass(desc, true);
            int index = cfr.readUnsignedShort("lvt-index");
            Position start = getPosition(start_pc);
            Position end = getPosition(start_pc + length);
            table.addElement(new LocalVariableTableEntry(start, end, name, type, index));
        }
        return table;
    }

    /**
     * Handles the allocation of a local variable by updating the local variable
     * table (if it exists) to record the correlation between the relevant
     * local variable table entry and the allocated local.
     *
     * @param address  the bytecode address at which the local variable was allocated
     * @param local    the allocated local variable
     */
    public void localVariableAllocated(int address, Local local) {
        if (localVariableTable != null) {
            Vector table = localVariableTable;
            for (Enumeration e = table.elements(); e.hasMoreElements();) {
                LocalVariableTableEntry lve = (LocalVariableTableEntry)e.nextElement();
                if (lve.matches(local.getJavacIndex(), address)) {
                    lve.setLocal(local);
                }
            }
        }
    }

    /**
     * Returns an array of scoped local variables.
     *
     * @return the array or null if the local variable information is unavailable
     */
    public ScopedLocalVariable[] getLocalVariableTable() {
        ScopedLocalVariable[] table = null;
        if (localVariableTable != null) {
            int length = localVariableTable.size();
            table = new ScopedLocalVariable[length];
            int i = 0;
            for (Enumeration e = localVariableTable.elements(); e.hasMoreElements();) {
                LocalVariableTableEntry entry = (LocalVariableTableEntry)e.nextElement();
                int start = entry.getStart().getBytecodeOffset();
                int lth   = entry.getEnd().getBytecodeOffset() - start;
                ScopedLocalVariable local = new ScopedLocalVariable(
                                                                     entry.getName(),
                                                                     entry.getType(),
                                                                     entry.getIndex(),
                                                                     start,
                                                                     lth
                                                                   );
                table[i++] = local;
            }
        }
        return table;
    }

    /**
     * Gets an enumeration over all the entries in the local variable debug table.
     *
     * @return  an enumeration of <code>LocalVariableTableEntry</code>s or null if there are none
     */
    public Enumeration getLocalVariableTableEntries() {
        return localVariableTable == null ? null : localVariableTable.elements();
    }

/*end[SCOPEDLOCALVARIABLES]*/


    /*---------------------------------------------------------------------------*\
     *                              LineNumberTable                              *
    \*---------------------------------------------------------------------------*/

    /**
     * The (optional) line number table for the method.
     */
    private final Vector lineNumberTable;

    /**
     * Load the "LineNumberTable" attribute the code parser is
     * currently positioned at.
     *
     * @param  codeLength    the length of the bytecode array for the enclosing method
     * @return the line number table encoded as a vector (<code>Position</code>, <code>Integer</code>)
     *                       pairs representing the IP and source line number respectively
     */
    private Vector loadLineNumberTable(int codeLength) {
        int length = cfr.readUnsignedShort("lin-lineNumberTableLength");
        Vector table = new Vector(length * 2);
        for (int i = 0; i < length; ++i) {
            int pc = cfr.readUnsignedShort("lnt-startPC");
            if (pc >= codeLength) {
                throw cfr.formatError(method + ": " + "start_pc of LineNumberTable is out of range");
            }
            int sourceLine = cfr.readUnsignedShort("lnt-lineNumber");
            Position position = getPosition(pc);
            table.addElement(position);
            table.addElement(new Integer(sourceLine));
        }
        return table;
    }

    /**
     * Gets the number of the source line whose implementation includes a given opcode address.
     *
     * @param  address  the opcode address
     * @return the number of the source line whose implementation includes <code>address</code>
     */
    public int getSourceLineNumber(int address) {
        int lno = -1;
        if (lineNumberTable != null) {
            Enumeration e = lineNumberTable.elements();
            while (e.hasMoreElements()) {
                Position position = (Position)e.nextElement();
                if (position.getBytecodeOffset() > address) {
                    break;
                }
                lno = ((Integer)e.nextElement()).intValue();
            }
        }
        return lno;
    }

    /**
     * Gets the line number table for this method encoded as an integer array.
     * The high 16-bits of each entry in the array is an address in the
     * bytecodes and the low 16-bits is the number of the source line whose
     * implementation starts at that address.
     *
     * @return  the encoded line number table or null if there is no such
     *          table for this method
     */
    public int[] getLineNumberTable() {
        int[] table = null;
        if (lineNumberTable != null) {
            table = new int[lineNumberTable.size() / 2];
            Enumeration e = lineNumberTable.elements();
            int i = 0;
            while (e.hasMoreElements()) {
                int address = ((Position)e.nextElement()).getBytecodeOffset();
                int lineNo = ((Integer)e.nextElement()).intValue();
                Assert.that((address & 0xFFFF) == address, "address overflow");
                Assert.that((lineNo & 0xFFFF) == lineNo, "line number overflow");
                /*
                 * If this is the first entry then set the address to zero so that
                 * instructions inserted by the translator that are before the first
                 * Java bytecode get a position in the map.
                 */
                if (i == 0) {
                    address = 0;
                    //System.err.println("addr="+address+" m="+method);
                }
                table[i++] = (address << 16) | lineNo;
            }
            Assert.that(i == table.length);
        }
        return table;
    }


    /*---------------------------------------------------------------------------*\
     *                    Stack map and target entry lookup                      *
    \*---------------------------------------------------------------------------*/

    /**
     * Interned targets.
     */
    private final IntHashtable targets;

    /**
     * Gets the <code>Target</code> instance encapsulating the stack map entry
     * at a bytecode address which must have a stack map entry.
     *
     * @param   address  a bytecode address
     * @return  the object encapsulating the stack map entry at
     *                   <code>address</code>
     * @throws  VerifyError if there is no target at <code>address</code>
     */
    public Target getTarget(int address) {
        if (targets != null) {
            Target target = (Target)targets.get(address);
            if (target != null) {
                return target;
            }
        }
        throw verifyError("missing stack map entry for address");
    }

    /**
     * Gets the numbers of targets (i.e. stack maps) in the code.
     *
     * @return the numbers of targets (i.e. stack maps) in the code
     */
    public int getTargetCount() {
        return targets == null ? 0 : targets.size();
    }

    /**
     * Gets an enumeration over all the targets representing the stack map
     * entries in the code.
     *
     * @return  an enumeration of <code>Target</code>s or null if there are none
     */
    public Enumeration getTargets() {
        return targets == null ? null : targets.elements();
    }


    /*---------------------------------------------------------------------------*\
     *                            Pseudo opcodes                                 *
    \*---------------------------------------------------------------------------*/

    /**
     * An instance of <code>PseudoOpcode</code> represents represents a point in
     * the bytecode stream delimiting a range of code protected by an exception
     * handler, the entry point for an exception handler or an explicit target
     * of a control flow instruction.
     */
    public final static class PseudoOpcode {

        /**
         * A pseudo opcode tag constant denoting a point in the bytecode stream
         * where an exception handler becomes active.
         */
        public static final int TRYEND = 0;

        /**
         * A pseudo opcode tag constant denoting a point in the bytecode stream
         * where an exception handler becomes deactive.
         */
        public static final int TRY = 1;

        /**
         * A pseudo opcode tag constant denoting a point in the bytecode stream
         * that is the entry to an exception handler.
         */
        public static final int CATCH = 2;

        /**
         * A pseudo opcode tag constant denoting a point in the bytecode stream
         * that is an explicit target of a control flow instruction.
         */
        public static final int TARGET = 3;

        /**
         * A pseudo opcode tag constant denoting a point in the bytecode stream
         * that is referenced by a LocalVariableTable or LineNumberTable
         * attribute.
         */
        public static final int POSITION = 4;

        /**
         * A tag denoting the semantics of this pseudo opcode.
         */
        private final int tag;

        /**
         * If this is a TRY or TRYEND pseudo opcode, then this is the index
         * of the correpsonding ExceptionHandler in the exception handler
         * table. This index is used to preserve the ordering of the exception
         * handlers.
         */
        private final int index;

        /**
         * If <code>tag</code> is <code>TARGET</code> or <code>CATCH</code>,
         * then this is an instance of {@link Target} otherwise it's an
         * instance of {@link ExceptionHandler} unless <code>tag</code>
         * is <code>POSITION</code> in which case it is an instance of
         * {@link Position}.
         */
        private final Object context;

        PseudoOpcode(int tag, Object context, int index) {
            this.tag     = tag;
            this.context = context;
            this.index   = index;
        }

        /**
         * Gets the constant denoting the semantics of this pseudo opcode.
         *
         * @return {@link #TRY}, {@link #TRYEND}, {@link #CATCH},
         *         {@link #TARGET} or {@link #POSITION}
         */
        public int getTag() {
            return tag;
        }

        /**
         * Gets the string representation of the pseudo opcode
         *
         * @return a string
         */
        public String toString() {
            if (Assert.ASSERTS_ENABLED) {
                String str = null;
                switch (tag) {
                    case TRY:      str = "try";         break;
                    case TRYEND:   str = "tryend";      break;
                    case CATCH:    str = "catch";       break;
                    case TARGET:   str = "target";      break;
                    case POSITION: str = "position";    break;
                    default: Assert.shouldNotReachHere();
                }
                return "["+str +"] index = "+index+" context = "+context;
            } else {
               return super.toString();
            }
        }

        /**
         * Gets an object describing extra information about the point in the
         * bytecode stream corresponding to the object.
         *
         * @return an instance of {@link Target}, {@link ExceptionHandler}
         *         or {@link Position}
         */
        public Object getContext() {
            return context;
        }

        /**
         * This Comparer sorts pseudo instructions at a given address in the
         * following ascending order: TRYEND, TRY, TARGET, CATCH, POSITION. Note
         * that it is impossible to have both a TARGET and a CATCH at the same
         * address. Mutilple TRY pseudo instructions are sorted so that those
         * who's exception table entry is at a higher index in the table come
         * before those whose index in the table is lower. The reverse is
         * true for mutliple TRYEND instructions. This preserves the ordering
         * of the exception handlers expressed in the class file.
         */
        static Comparer COMPARER = new Comparer() {
            public int compare(Object o1, Object o2) {
                if (o1 == o2) {
                    return 0;
                }
                PseudoOpcode po1 = (PseudoOpcode)o1;
                PseudoOpcode po2 = (PseudoOpcode)o2;
                int tag1 = po1.tag;
                int tag2 = po2.tag;
                if (tag1 < tag2) {
                    return -1;
                } else if (tag1 > tag2) {
                    return 1;
                } else {
                    Assert.that(tag1 == TRY || tag1 == TRYEND, "multiple incompatible pseudo opcodes at address");
                    if (tag1 == TRY) {
                        return po2.index - po1.index;
                    } else {
                        return po1.index - po2.index;
                    }
                }
            }
        };
    }

    /**
     * Gets the pseudo opcodes representing exception handler points or
     * branch targets at the address of the last parsed opcode.
     *
     * @return  the pseudo opcodes at this address or null if there are none
     */
    public PseudoOpcode[] getLastPseudoOpcodes() {

        /*
         * Quick test for nothing to do.
         */
        if (exceptionHandlers.length == 0 && targets == null && positions == null) {
            return null;
        }

        int address = lastOpcodeAddress;
        boolean isCatchAddress = false;

        /*
         * Create the pseduo opcodes for the exception handler points (if any)
         */
        if (exceptionHandlers.length != 0) {
            for (int i = 0; i != exceptionHandlers.length; ++i) {
                ExceptionHandler exceptionHandler = exceptionHandlers[i];
                if (exceptionHandler.getEnd() == address) {
                    PseudoOpcode pseudoOpcode = new PseudoOpcode(PseudoOpcode.TRYEND, exceptionHandler, i);
                    pseudoOpcodes.addElement(pseudoOpcode);
                }
                if (exceptionHandler.getStart() == address) {
                    PseudoOpcode pseudoOpcode = new PseudoOpcode(PseudoOpcode.TRY, exceptionHandler, i);
                    pseudoOpcodes.addElement(pseudoOpcode);
                }
                if (exceptionHandler.getHandler() == address) {
                    if (!isCatchAddress) {
                        isCatchAddress = true;
                        PseudoOpcode pseudoOpcode = new PseudoOpcode(PseudoOpcode.CATCH, getTarget(address), -1);
                        pseudoOpcodes.addElement(pseudoOpcode);
                    }
                }
            }
        }

        /*
         * Get the pseudo opcode for the control flow target (if any).
         */
        if (!isCatchAddress && targets != null) {
            Target target = (Target)targets.get(address);
            if (target != null) {
                PseudoOpcode pseudoOpcode = new PseudoOpcode(PseudoOpcode.TARGET, target, -1);
                pseudoOpcodes.addElement(pseudoOpcode);
            }
        }

        /*
         * Get the pseudo opcode for a position (if any).
         */
        if (positions != null) {
            Position position = (Position)positions.get(address);
            if (position != null) {
                PseudoOpcode pseudoOpcode = new PseudoOpcode(PseudoOpcode.POSITION, position, -1);
                pseudoOpcodes.addElement(pseudoOpcode);
            }
        }

        /*
         * Optomize the way the result is returned.
         */
        switch (pseudoOpcodes.size()) {
            case 0: {
                return null;
            }
            case 1: {
                PseudoOpcode res = (PseudoOpcode)pseudoOpcodes.elementAt(0);
                pseudoOpcodes.removeAllElements();
                return new PseudoOpcode[] { res };
            }
            default: {
                PseudoOpcode[] opcodes = new PseudoOpcode[pseudoOpcodes.size()];
                pseudoOpcodes.copyInto(opcodes);

                /*
                 * Sort the pseudo opcodes
                 */
                if (opcodes.length > 1) {
                    Arrays.sort(opcodes, PseudoOpcode.COMPARER);
                }
                pseudoOpcodes.removeAllElements();
                return opcodes;
            }
        }
    }


    /*---------------------------------------------------------------------------*\
     *                            Max stack & locals                             *
    \*---------------------------------------------------------------------------*/

    /**
     * Gets the value of the <code>max_locals</code> item in the class file
     * for this method.
     *
     * @return the value of the <code>max_locals</code> item in the class file
     *         for this method
     */
    public int getMaxLocals() {
        return maxLocals;
    }

    /**
     * Gets the value of the <code>max_stack</code> item in the class file
     * for this method.
     *
     * @return the value of the <code>max_stack</code> item in the class file
     *         for this method
     */
    public int getMaxStack() {
        return maxStack;
    }

    /**
     * Gets the class file method being parsed by this parser.
     *
     * @return the method being parsed by this parser
     */
    public Method getMethod() {
        return method;
    }


    /*---------------------------------------------------------------------------*\
     *                            Bytecode parsing                               *
    \*---------------------------------------------------------------------------*/

    /**
     * Constants denoting array element type for <i>newarray</i>.
     */
    public static final int T_BOOLEAN   = 0x00000004;
    public static final int T_CHAR      = 0x00000005;
    public static final int T_FLOAT     = 0x00000006;
    public static final int T_DOUBLE    = 0x00000007;
    public static final int T_BYTE      = 0x00000008;
    public static final int T_SHORT     = 0x00000009;
    public static final int T_INT       = 0x0000000a;
    public static final int T_LONG      = 0x0000000b;

    /**
     * {@inheritDoc}
     */
    public String prefix(String msg) {
        if (lastOpcodeAddress != -1) {
            int lno = getSourceLineNumber(lastOpcodeAddress);
            if (lno != -1) {
                String sourceFile = method.getDefiningClass().getSourceFileName();
                msg = "@" + lastOpcodeAddress + "(" + sourceFile + ":" + lno + "): " + msg;
            } else {
                msg = "@" + lastOpcodeAddress + ": " + msg;
            }
        } else {
            msg = ": " + msg;
        }
        return method + msg;
    }

    /**
     * Throws a LinkageError instance to indicate there was a verification
     * error while processing the bytecode for a method.
     *
     * @param   msg  the cause of the error
     * @return  the LinkageError raised
     */
    public LinkageError verifyError(String msg) {
        Translator.throwVerifyError(prefix(msg));
        return null;
    }

    /**
     * The index in the bytecode array of the last opcode returned by
     * {@link #parseOpcode()}.
     */
    private int lastOpcodeAddress = -1;

    /**
     * Gets the address of the last opcode returned by {@link #parseOpcode()}.
     *
     * @return  the address of the last opcode read
     */
    public int getLastOpcodeAddress() {
        return lastOpcodeAddress;
    }

    /**
     * Gets the current bytecode offset.
     *
     * @return the current bytecode offset
     */
    public int getCurrentIP() {
        return bcin.getCurrentIndex();
    }

    /**
     * Determines whether or not this parser is at the end of the
     * instruction stream.
     *
     * @return true if this parser is at the end of the bytecode stream
     */
    public boolean atEof() {
        if (bcin.available() == 0) {
            lastOpcodeAddress = bcin.getCurrentIndex();
            return true;
        } else {
            return false;
        }
    }

    /**
     * Read an opcode from the bytecode stream. The returned value will be
     * one of the <code>opc_...</code> defined in {@link Opcode}.
     *
     * @return  the opcode read
     */
    public int parseOpcode() {
        lastOpcodeAddress = bcin.getCurrentIndex();
        int opcode = cfr.readUnsignedByte(null);
        if (Klass.DEBUG && Tracer.isTracing("classfile", method.toString())) {
            Tracer.traceln("["+lastOpcodeAddress+"]:opcode:"+Opcode.mnemonics[opcode]);
        }
        return opcode;
    }

    /**
     * Parses a unsigned 8-bit operand.
     *
     * @return the parsed byte value
     */
    public int parseUnsignedByteOperand() {
        return cfr.readUnsignedByte("operand");
    }

    /**
     * Parses a signed 8-bit operand.
     *
     * @return the parsed byte value
     */
    public int parseByteOperand() {
        return cfr.readByte("operand");
    }

    /**
     * Parses a signed 16-bit operand.
     *
     * @return the parsed short value
     */
    public int parseShortOperand() {
        return cfr.readShort("operand");
    }

    /**
     * Parses a signed 32-bit operand.
     *
     * @return the parsed integer value
     */
    public int parseIntOperand() {
        return cfr.readInt("operand");
    }

    /**
     * Parses the operand of an instruction that is an index into the
     * constant pool and returns the object at that index.
     *
     * @param   wide         specifies an 8-bit index if false, 16-bit otherwise
     * @param   longOrDouble specifies if the entry is a <code>long</code> or
     *                       <code>double</code> value
     * @return  the object in the constant pool at the parsed index
     */
    public Object parseConstantPoolOperand(boolean wide, boolean longOrDouble) {
        int index = wide ? cfr.readUnsignedShort("operand") : cfr.readUnsignedByte("operand");
        int tag = constantPool.getTag(index);
        if (longOrDouble) {
            if (tag != ConstantPool.CONSTANT_Long && tag != ConstantPool.CONSTANT_Double) {
                throw verifyError("expected long or double constant");
            }
        } else {
            if (tag != ConstantPool.CONSTANT_Integer && tag != ConstantPool.CONSTANT_Float && tag != ConstantPool.CONSTANT_String) {
                throw verifyError("expected int, float or string constant");
            }
        }
        return constantPool.getEntry(index, tag);
    }

    /**
     * Parses the operand of an instruction that is a field reference (via the
     * constant pool) and returns the referenced field, resolving it first
     * if it has not already been resolved.
     *
     * @param   isStatic  specifies whether the field is static or not
     * @return  the resolved field reference
     */
    public Field parseFieldOperand(boolean isStatic, int opcode) {
        int index = cfr.readUnsignedShort("operand");
        int tag = constantPool.getTag(index);
        if (tag !=  ConstantPool.CONSTANT_Fieldref) {
            throw verifyError("invalid field reference");
        }
        Field field = constantPool.getResolvedField(index, isStatic, this);

        /*
         * Check for assignment to final field from outside class that defines the field
         */
        if (field.isFinal() && (opcode == Opcode.opc_putstatic || opcode == Opcode.opc_putfield) && field.getDefiningClass() != method.getDefiningClass()) {
            Translator.throwIllegalAccessError(prefix("invalid assignment to final field"));
        }
        return field;
    }

    /**
     * Parses the operand of an instruction that is a method reference (via the
     * constant pool) and returns the referenced method, resolving it first
     * if it has not already been resolved.
     *
     * @param   isStatic     specifies whether the method is static or not
     * @param   invokeInterface  specifies whether or not the instruction is
     *                       <i>invokeinterface</i>
     * @return  the resolved method reference
     */
    public Method parseMethodOperand(boolean isStatic, boolean invokeInterface) {
        int index = cfr.readUnsignedShort("operand");
        int tag = constantPool.getTag(index);
        if (tag != ConstantPool.CONSTANT_Methodref && tag != ConstantPool.CONSTANT_InterfaceMethodref) {
            throw verifyError("invalid method reference");
        }
        return constantPool.getResolvedMethod(index, isStatic, invokeInterface, this);
    }

    /**
     * Parses the operand of a <i>newarray</i> instruction and returns the
     * the type it denotes.
     *
     * @return  the array type denoted by the operand to <i>newarray</i>
     */
    public Klass parseNewArrayOperand() {
        int tag = cfr.readUnsignedByte("operand");
        switch (tag) {
            case T_BOOLEAN: return Klass.BOOLEAN_ARRAY;
            case T_BYTE:    return Klass.BYTE_ARRAY;
            case T_CHAR:    return Klass.CHAR_ARRAY;
            case T_SHORT:   return Klass.SHORT_ARRAY;
            case T_INT:     return Klass.INT_ARRAY;
            case T_LONG:    return Klass.LONG_ARRAY;
            case T_FLOAT:   return Klass.FLOAT_ARRAY;
            case T_DOUBLE:  return Klass.DOUBLE_ARRAY;
            default:        throw verifyError("invalid array type");
        }
    }

    /**
     * Parses the operand of a <i>new</i> instruction and returns an
     * instance of <code>UninitializedObjectClass</code> that denotes
     * the type created as well as the address of the instruction.
     *
     * @return  the type denoted by the operand to <i>new</i>
     */
    public UninitializedObjectClass parseNewOperand() {
        int index = cfr.readUnsignedShort("operand");
        Klass type = constantPool.getResolvedClass(index, this);
        if (type.isArray()) {
            throw verifyError("can't create array with new");
        }
        if (type.isInterface()) {
            throw verifyError("can't create interface with new");
        }
        return getUninitializedObjectClass(lastOpcodeAddress, type);
    }

    /**
     * Parses the operand of an instruction that is a class reference (via the
     * constant pool) and returns the referenced class, resolving it first
     * if it has not already been resolved.
     *
     * @return  the class denoted by the operand to <i>new</i>
     */
    public Klass parseKlassOperand() {
        int index = cfr.readUnsignedShort("operand");
        return constantPool.getResolvedClass(index, this);
    }

    /**
     * Parses the operand to a local variable instruction.
     *
     * @param   wide         specifies an 8-bit index if false, 16-bit otherwise
     * @param   longOrDouble specifies if the type of the local variable is
     *                       <code>long</code> or <code>double</code>
     * @return  the index of the local variable denoted by the operand
     */
    public int parseLocalVariableOperand(boolean wide, boolean longOrDouble) {
        int index = wide ? cfr.readUnsignedShort("operand") : cfr.readUnsignedByte("operand");
        int adjust = longOrDouble ? 1 : 0;
        if (index+adjust >= maxLocals) {
            throw verifyError("invalid local variable index");
        }
        return index;
    }

    /**
     * Parses the offset operand to a control flow instruction.
     *
     * @param   wide   specifies a 16-bit index if false, 32-bit otherwise
     * @return the parsed offset
     */
    public Target parseBranchOperand(boolean wide) {
        int offset = wide ? cfr.readInt("operand") : cfr.readShort("operand");
        int address = lastOpcodeAddress + offset;
        return getTarget(address);
     }

     /**
      * Parse the 0-3 zero padded bytes after a <i>tableswitch</i> or
      * <i>lookupswitch</i> instruction.
      */
     public void parseSwitchPadding() {
         while (bcin.getCurrentIndex() % 4 != 0) {
             int ch = cfr.readUnsignedByte("operand");
             if (ch != 0) {
                 throw verifyError("tableswitch/lookupswitch instruction not padded with 0's");
             }
         }
     }
}
