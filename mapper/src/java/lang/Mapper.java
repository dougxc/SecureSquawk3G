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
import java.util.*;
import com.sun.squawk.vm.*;
import com.sun.squawk.util.Assert;
import com.sun.squawk.util.Vector;
import com.sun.squawk.translator.Translator;
import com.sun.squawk.util.IntHashtable;

/**
 * The rom dump program.
 *
 * @author Nik Shaylor
 */
public class Mapper {

    /**
     * Offset to the superType field in Klass.
     */
    private static final int Class_superType = (int)FieldOffsets.java_lang_Klass$superType;

    /**
     * Offset to the classID field in Klass.
     */
    private static final int Class_classID = (int)FieldOffsets.java_lang_Klass$classID;

    /**
     * Offset to the componentType field in Klass.
     */
    private static final int Class_componentType = (int)FieldOffsets.java_lang_Klass$componentType;

    /**
     * Offset to the instanceSize field in Klass.
     */
    private static final int Class_instanceSize = (int)FieldOffsets.java_lang_Klass$instanceSize;

    /**
     * Offset to the instanceSize field in Klass.
     */
    private static final int Class_name = (int)FieldOffsets.java_lang_Klass$name;

    /**
     * Properties from "squawk.sym".
     */
    private static Properties map = new Properties();

    /**
     * The output map stream.
     */
    private static PrintStream out;

    /**
     * Set true when the next output line is the destination of an oop.
     */
    private static boolean oopNext;

    /**
     * Image relocation address.
     */
    private static Offset relocation = Offset.zero();

    /**
     * Print and an error and stop.
     *
     * @param msg the error message
     */
    private static void fatal(String msg) {
        System.err.println(msg);
        System.exit(-1);
    }

    private static Translator translator;

    private static void initializeTranslator(String classPath) {
        SuiteManager.initialize(new Suite[0]);
        int sno = SuiteManager.allocateFreeSuiteNumber();
        Suite suite = new Suite("-open-", sno, null, classPath);
        SuiteManager.installSuite(suite);

        Isolate isolate = new Isolate(null, null, suite);
        VM.setCurrentIsolate(isolate);

        isolate.setTranslator(new Translator());
        translator = (Translator)isolate.getTranslator();

        /*
         * Trigger the class initializer for java.lang.Klass. An error will have
         * occurred if it was triggered before this point.
         */
        Klass top = Klass.TOP;
    }


    /**
     * Stores a histogram of how many instances of each type are present in each object memory.
     */
    private static HashMap histogram = new HashMap();

    /**
     * An HistogramEntry instance records the frequency and total size of instances of a given class.
     */
    static class HistogramEntry {
        /**
         * Name of the class.
         */
        final String klass;

        /**
         * Frequency count of instances of the class.
         */
        int count;

        /**
         * Total size of all the instances of the class which includes the size of the instance headers.
         */
        int size;
        HistogramEntry(String klass) {
            this.klass = klass;
        }

        /**
         * A Comparator to sort histogram entries by their 'count' fields.
         */
        static class Sorter implements Comparator {
            static final int KLASS = 0;
            static final int COUNT = 1;
            static final int SIZE = 2;
            private final int key;
            Sorter(int key) {
                this.key = key;
            }
            public int compare(Object o1, Object o2) {
                if (o1 == o2) {
                    return 0;
                }

                HistogramEntry e1 = (HistogramEntry)o1;
                HistogramEntry e2 = (HistogramEntry)o2;

                if (key == COUNT) {
                    return e1.count - e2.count;
                } else if (key == SIZE) {
                    return e1.size - e2.size;
                } else {
                    Assert.that(key == KLASS);
                    return e1.klass.compareTo(e2.klass);
                }
            }
        }
    }

    /**
     * Gets the argument to a command line option. If the argument is not
     * provided, then a usage message is printed and RuntimeException is
     * thrown.
     *
     * @param  args   the command line arguments
     * @param  index  the index at which the option's argument is located
     * @param  opt    the name of the option
     * @return the value of the option's argument
     * @throws RuntimeException if the required argument is missing
     */
    static private String getOptArg(String[] args, int index, String opt) {
        if (index >= args.length) {
            usage("The " + opt + " option requires an argument.");
            throw new RuntimeException();
        }
        return args[index];
    }

    static void showHistogram(int key) {
        // Show the histogram of counts
        SortedSet sorted = new TreeSet(new HistogramEntry.Sorter(key));
        sorted.addAll(histogram.values());
        boolean isCount = key == HistogramEntry.Sorter.COUNT;

        out.println();
        out.println("Histogram of instance " + (isCount ? "count" : "size") + "s:" );
        int total = 0;
        for (Iterator iterator = sorted.iterator(); iterator.hasNext(); ) {
            HistogramEntry entry = (HistogramEntry)iterator.next();
            int value = (isCount ? entry.count : entry.size);
            total += value;
            String sValue = "" + value;
            out.print(sValue);
            for (int i = sValue.length(); i < 10; ++i) {
                out.print(' ');
            }
            out.println(entry.klass);
        }
        out.println("Total: " + total);
    }

    /**
     * Prints the usage message.
     *
     * @param  errMsg  an optional error message
     */
    static private void usage(String errMsg) {
        PrintStream out = System.out;
        if (errMsg != null) {
            out.println(errMsg);
        }
        out.println("Usage: mapper [-options] [object_memory_file]");
        out.println("where options include:");
        out.println();
        out.println("    -o <file>   dump to 'file' (default=squawk.map)");
        out.println("    -cp <directories and jar/zip files separated by '"+File.pathSeparatorChar+"'>");
        out.println("                paths where classes and sources can be found");
        out.println("    -start <n>  only start dumping at the n'th object memory");
        out.println("    -trace      trace loading of the object memory files");
        out.println("    -r <addr>   specifies a relocation address for the");
        out.println("                first object memory (i.e. ROM)");
        out.println("    -h          show this help message");
    }

    /**
     * Main routine.
     *
     * @param args
     */
    public static void main(String args[]) throws IOException {
        String file = "squawk.suite";
        String outFile = "squawk.map";
        int start = 0;

        int argc = 0;
        for (; argc != args.length; ++argc) {
            String arg = args[argc];
            if (arg.charAt(0) != '-') {
                break;
            }
            if (arg.equals("-start")) {
                start = Integer.parseInt(getOptArg(args, ++argc, arg));
            } else if (arg.equals("-trace")) {
                com.sun.squawk.util.Tracer.enableFeature("oms");
                com.sun.squawk.util.Tracer.setPrintStream(VM.out);
            } else if (arg.equals("-r")) {
                relocation = Offset.fromPrimitive((int/*S64*/)Long.parseLong(getOptArg(args, ++argc, arg)));
            } else if (arg.equals("-cp")) {
                initializeTranslator(getOptArg(args, ++argc, arg));
            } else if (arg.equals("-o")) {
                outFile = getOptArg(args, ++argc, arg);
            } else if (arg.equals("-h")) {
                usage(null);
                return;
            } else {
                usage("unknown option: " + arg);
                return;
            }
        }

        if (argc != args.length) {
            file = args[argc];
        }

        out = new PrintStream(new BufferedOutputStream(new FileOutputStream(outFile), 10000));
        loadMap("squawk.sym");
        boolean bigEndian = getIntProperty("PMR.BIG_ENDIAN") == 1;

        final ObjectMemory lastMemory = loadObjectMemory(file, bigEndian);

        out.println("   bigEndian       = " + bigEndian);
        out.println("   relocation      = " + relocation);
        out.println("   word size       = " + HDR.BYTES_PER_WORD);
        out.println();

        out.println("loaded object memorys:");
        showSummary(lastMemory);
        out.println();

        Stack memories = new Stack();
        ObjectMemory memory = lastMemory;
        while (memory != null && memory.getParentCount() >= start) {
            memories.push(memory);
            memory = memory.getParent();
        }

        try {
            while (!memories.isEmpty()) {
                memory = (ObjectMemory) memories.pop();
                Address block = memory.getStart();
                Address end = block.add(memory.getSize());

                out.println("+++++++++++++++++++++  start of object memory in " + memory.getURL() + " +++++++++++++++++++++++");

                while (block.lo(end)) {
                    int tag = Unsafe.getAsUWord(block, 0).and(UWord.fromPrimitive(HDR.headerTagMask)).toInt();
                    switch ( (int) tag) {
                        case (int) HDR.basicHeaderTag:
                            block = decodeInstance(block);
                            break;
                        case (int) HDR.arrayHeaderTag:
                            block = decodeArray(block);
                            break;
                        case (int) HDR.methodHeaderTag:
                            block = decodeMethod(block);
                            break;
                        default:
                            fatal("Invalid header word");
                            break;
                    }
                }
                out.flush();


                // Show the histogram of counts
                showHistogram(HistogramEntry.Sorter.COUNT);

                // Show the histogram of sizes
                showHistogram(HistogramEntry.Sorter.SIZE);
                out.println();

                // Reset the histogram for the next object memory
                histogram = new HashMap();
            }
        } finally {
            out.flush();
            out.close();
        }
    }


    /**
     * Initialize the contents of memory from an object memory file.
     *
     * @param name       the object memory file name
     * @param bigEndian  the endianess
     */
    static ObjectMemory loadObjectMemory(String name, boolean bigEndian) throws IOException {
        ObjectMemory om = ObjectMemoryLoader.load("file://" + name, false);

        // Set the endianess
        VM.bigEndian  = bigEndian;

        return om;
    }

    /**
     * Show a summary of an object memory.
     *
     * @param memory   the object memory to summarize
     */
    private static void showSummary(ObjectMemory memory) {
        if (memory != null) {
            showSummary(memory.getParent());
            int size = memory.getSize();
            out.print("  " + memory.getURL() + " {");
            out.print(" canonical start = " + memory.getCanonicalStart());
            out.print(" size = " + size);
            out.print(" root = " + Address.fromObject(memory.getRoot()).diff(memory.getStart()));
            out.println("}");
        }
    }

    /**
     * Get the relocated address.
     *
     * @param addr the address
     * @return the relocated address
     */
    static Address relocate(Address addr) {

        if (addr != Address.zero()) {
            return addr.addOffset(relocation);
        } else {
            return Address.zero();
        }
    }

    /**
     * Print a line of the map file corresponding to one word.
     *
     * @param addr    the address of the first byte
     * @param text    the text to be written after the hex
     */
    private static void printWordLine(Address addr, String text, boolean isOop) {
        printLine(addr, HDR.BYTES_PER_WORD, text, (isOop && Unsafe.getUWord(addr, 0).ne(UWord.zero())));
    }

    /**
     * Print a line of the map file.
     *
     * @param addr the address of the first byte
     * @param length the number of bytes
     * @param text the text to be written after the hex
     */
    private static void printLine(Address addr, int length, String text) {
        printLine(addr, length, text, false);
    }
    private static void printLine(Address addr, int length, String text, boolean isOop) {

        Assert.that(length <= 9);
        String s = ""+relocate(addr);
        while (s.length() < 10) {
            s = " "+s;
        }

        if (Unsafe.isReference(addr)) {
            s = "+" + s;
        } else {
            Assert.that(!isOop);
            s = " " + s;
        }


        if (oopNext) {
            s = s + " * ";
            oopNext = false;
        } else {
            s = s + " : ";
        }
        for (int i = 0 ; i < 9 ; i++) {
            if (i < length) {
                int b = Unsafe.getByte(addr, i) & 0xFF;
                if (b < 16) {
                    s = s + " 0"+Integer.toHexString(b);
                } else {
                    s = s +  " "+Integer.toHexString(b);
                }
            } else {
                s = s + "   ";
            }
        }
        out.print(s);
        out.println(text);
    }

    private static Object getKlass(Address oop) {
        Object something = Unsafe.getObject(oop, HDR.klass);
        Object klass = Unsafe.getObject(something, (int)FieldOffsets.java_lang_Klass$self);
        return klass;
    }

    /**
     * Updates the histogram entry for a given class based on an instance being parsed by the caller.
     *
     * @param name  the name of the class
     * @param size  the size (in bytes) of the instance and its header
     */
    private static void updateHistogramFor(String name, int size) {
        HistogramEntry entry = (HistogramEntry)histogram.get(name);
        if (entry == null) {
            entry = new HistogramEntry(name);
        }
        entry.count++;
        entry.size += size;
        histogram.put(name, entry);
    }

    /**
     * Print a class name of an instance.
     *
     * @param oop the oop of an instance
     */
    private static void printInstanceHeader(Address oop) {
        Object klass = getKlass(oop);
        String name = getClassName(klass);
        printWordLine(oop.sub(HDR.BYTES_PER_WORD), "instance "+name, true);
        oopNext = true;
    }

    /**
     * Print a class name and length of an array.
     *
     * @param oop the oop of an array
     */
    private static void printArrayHeader(Address oop) {
        Object klass = getKlass(oop);
        String name = getClassName(klass);
        int length = decodeLengthWord(oop, HDR.length);
        printWordLine(oop.sub(HDR.arrayHeaderSize), "["+length+"]", false);
        printWordLine(oop.sub(HDR.basicHeaderSize), "array " + name, true);
        oopNext = true;
    }

    /**
     * Print a class name and length of an array.
     *
     * @param oop the oop of an array
     */
    private static void printMethodHeader(Address block, Address oop) {
        int hdrlength = decodeLengthWord(block, 0);
        int length = decodeLengthWord(oop, HDR.length);
        printWordLine(block, "{"+hdrlength+"}", false);
        String headerString = getMethodHeaderText(oop);

        Address dklassAddress = oop.add(HDR.methodDefiningClass * HDR.BYTES_PER_WORD);
        for (Address i = block.add(HDR.BYTES_PER_WORD) ; i.lo(dklassAddress); i = i.add(HDR.BYTES_PER_WORD)) {
            printWordLine(i, headerString, false);
            headerString = "";
        }
        Object dklass = Unsafe.getObject(oop, HDR.methodDefiningClass);
        printWordLine(dklassAddress, "defined in "+getClassName(dklass), true);
        printWordLine(oop.sub(HDR.arrayHeaderSize),  "["+length+"]", false);
        printWordLine(oop.sub(HDR.basicHeaderSize), "method "+getMethodName(oop), true);
        oopNext = true;
    }


    /**
     * Get the text for the method header.
     *
     * @param oop method object pointer
     * @return the text
     */
    private static String getMethodHeaderText(Object oop) {
        StringBuffer sb    = new StringBuffer();
        int localCount     = MethodBody.decodeLocalCount(oop);
        int parameterCount = MethodBody.decodeParameterCount(oop);
        int maxStack       = MethodBody.decodeStackCount(oop);

        sb.append("p="+parameterCount+" l="+localCount+" s="+maxStack);

        /*
         * Format the oopmap.
         */
        if (parameterCount+localCount > 0) {
            sb.append(" map=");
            int offset = MethodBody.decodeOopmapOffset(oop);
            for (int i = 0 ; i < parameterCount+localCount ; i++) {
                int pos = i / 8;
                int bit = i % 8;
                int bite = Unsafe.getByte(oop, offset+pos) & 0xFF;
                boolean isOop = ((bite>>bit)&1) != 0;
                sb.append(isOop?"1":"0");
            }
        }

        /*
         * Format the type map.
         */
        if (parameterCount+localCount > 0) {
            sb.append(" types=");
            sb.append(MethodBody.decodeTypeMap(oop));
        }


        /*
         * Check the exception table.
         */
        if (MethodBody.decodeExceptionTableSize(oop) != 0) {
            int size   = MethodBody.decodeExceptionTableSize(oop);
            int offset = MethodBody.decodeExceptionTableOffset(oop);
            VMBufferDecoder dec = new VMBufferDecoder(oop, offset);
            long end = offset + size;
            while (dec.getOffset() < end) {
                sb.append(" ["+dec.readUnsignedInt());
                sb.append(":"+dec.readUnsignedInt());
                sb.append(":"+dec.readUnsignedInt());
                sb.append(":"+getClassName(dec.readUnsignedInt()));
                sb.append("]");
            }
        }
        return sb.toString();
    }


    /**
     * Decode an instance.
     *
     * @param block the address before the header of the object
     * @return the address of the next object header
     */
    private static Address decodeInstance(Address block) {
        Address oop = block.add(HDR.basicHeaderSize);
        printInstanceHeader(oop);
        Object klass = getKlass(oop);
        Address nextBlock = printAllInstanceFields(oop, klass);
        updateHistogramFor(getClassName(klass), nextBlock.diff(block).toInt());
        return nextBlock;
    }

    /**
     * Print the fields of an instance.
     *
     * @param block the address before the header of the object
     * @return the address of the next object header
     */
    private static Address printAllInstanceFields(Address oop, Object klass) {
        Object supr = getSuperType(klass);
        Address superEnd;
        if (supr != null) {
            superEnd = printAllInstanceFields(oop, supr);
        } else {
            superEnd = oop;
        }

        Address oopEnd = printInstanceFields(oop, superEnd, klass);
        return oopEnd;
    }

    /**
     * Print the fields of an instance for a given class in the hierarchy of the instance.
     *
     * @param oop         the pointer to the instance
     * @param firstField  the address of the first field in the instance defined by <code>klass</code>
     * @param klass       the class whose fields are to be printed
     * @return the address at which the first field of the next sub-class in the instance's hierachy (if any)
     *                    will be located
     */
    private static Address printInstanceFields(final Address oop, final Address firstField, Object klass) {
        int size = getInstanceSize(klass) * HDR.BYTES_PER_WORD;
        Address nextField = firstField;

        // Print the fields.
        for (int i = 0 ;; i++) {
            FieldInfo field = getField(klass, i);
            if (field == null) {
                break;
            }
            String name = field.name;
            int offset = field.offset;
            int typeID   = field.typeID;
            int fsize  = getDataSize(typeID);

            Address addr = oop.add(offset * fsize);
            if (!Klass.SQUAWK_64 && (typeID == CID.LONG || typeID == CID.DOUBLE)) {
                addr = oop.add(offset * 4);
            }

            switch (typeID) {
                case CID.BOOLEAN:   printLine(addr, fsize, "    bool   "+name+" = "+Unsafe.getByte(addr, 0));  break;
                case CID.BYTE:      printLine(addr, fsize, "    byte   "+name+" = "+Unsafe.getByte(addr, 0));  break;
                case CID.CHAR:      printLine(addr, fsize, "    char   "+name+" = "+Unsafe.getChar(addr, 0));  break;
                case CID.SHORT:     printLine(addr, fsize, "    short  "+name+" = "+Unsafe.getShort(addr, 0)); break;
                case CID.INT:       printLine(addr, fsize, "    int    "+name+" = "+Unsafe.getInt(addr, 0));   break;
                case CID.FLOAT:     printLine(addr, fsize, "    float  "+name+" = "+Unsafe.getInt(addr, 0));   break;
                case CID.LONG:      printLine(addr, fsize, "    long   "+name+" = "+Unsafe.getLong(addr, 0));  break;
                case CID.DOUBLE:    printLine(addr, fsize, "    double "+name+" = "+Unsafe.getLong(addr, 0));  break;
                default: {
                    String s = "    ref    "+name+" = " + relocate((Address)Unsafe.getObject(addr, 0));
                    while (s.length() < 40) {
                        s = s + " ";
                    }
                    s += " "+ getClassName(typeID);
                    printWordLine(addr, s, true);
                    break;
                }
                case CID.STRING:
                case CID.STRING_OF_BYTES: {
                    Address soop  = Address.fromObject(Unsafe.getObject(addr, 0));
                    String s  = "    string "+name+" = "+relocate(soop);
                    if (soop.isZero()) {
                        printLine(addr, fsize, s);
                    } else {
                        while (s.length() < 40) {
                            s = s + " ";
                        }
                        s += " \"";
                        s += getString(soop);
                        printLine(addr, fsize, s+"\"");
                    }
                    break;
                }
            }
            nextField = addr.add(fsize);
        }

        Address end = oop.add(size).roundUpToWord();
        nextField = nextField.roundUpToWord();

        if (end != nextField) {
            while (nextField != end) {
                int diff = end.diff(nextField).toInt();
                int modWord = diff % HDR.BYTES_PER_WORD;
                int fsize = HDR.BYTES_PER_WORD - modWord;
                printLine(nextField, fsize, "    {unknown field}", false);
                nextField = nextField.add(fsize);
            }
        }

        return end;
    }

    /**
     * Extracts a string from a string address in memory.
     *
     * @param soop   pointer to a String instance in the memory
     * @return the String instance corresponding to soop
     */
    private static String getString(Address soop) {

        if (soop.isZero()) {
            return "";
        }
        int slth  = decodeLengthWord(soop, HDR.length);
        int ssize = getComponentSize(getKlass(soop));
        StringBuffer buf = new StringBuffer(slth);
        for (int j = 0 ; j < slth ; j++) {
            char ch;
            if (ssize == 1) {
                ch = (char)(Unsafe.getByte(soop, j) & 0xFF);
            } else {
                ch = (char) Unsafe.getChar(soop, j);
            }
            if (ch < ' ' || ch > '~') {
                ch = '~';
            }
            buf.append(ch);
        }
        return buf.toString();
    }

    /**
     * Decode an array.
     *
     * @param block the address before the header of the object
     * @return the address of the next object header
     */
    private static Address decodeArray(Address block) {
        Address oop = block.add(HDR.arrayHeaderSize);
        Object klass = getKlass(oop);
        int kcid = getClassID(klass);
        int size     = getComponentSize(klass);
        int length   = decodeLengthWord(oop, HDR.length);

        printArrayHeader(oop);

        if (kcid == CID.LOCAL_ARRAY) {
            decodeChunk(oop, size, length);
        } else if (kcid == CID.STRING_OF_BYTES) {
            int lth = length;
            Address addr = oop;
            while (lth > 0) {
                int chunk = lth;
                if (chunk > 8) {
                    chunk = 8;
                }
                String s = "    \"";
                for (int i = 0 ; i < chunk ; i++) {
                    char ch = (char)(Unsafe.getByte(addr, i) & 0xFF);
                    if (ch < ' ' || ch > '~') {
                        ch = '~';
                    }
                    s += ch;
                }
                printLine(addr, chunk, s+"\"");
                addr = addr.add(chunk);
                lth -= chunk;
            }
        } else {
            for (int i = 0 ; i < length*size ; i+=size) {
                Address addr = oop.add(i);
                switch (kcid) {
                    case CID.BOOLEAN_ARRAY:   printLine(addr, size, "    bool   "+Unsafe.getByte(addr, 0));  break;
                    case CID.CHAR_ARRAY:      printLine(addr, size, "    char   "+Unsafe.getChar(addr, 0));  break;
                    case CID.SHORT_ARRAY:     printLine(addr, size, "    short  "+Unsafe.getShort(addr, 0)); break;
                    case CID.INT_ARRAY:       printLine(addr, size, "    int    "+Unsafe.getInt(addr, 0));   break;
                    case CID.FLOAT_ARRAY:     printLine(addr, size, "    float  "+Unsafe.getInt(addr, 0));   break;
                    case CID.LONG_ARRAY:      printLine(addr, size, "    long   "+Unsafe.getLong(addr, 0));  break;
                    case CID.DOUBLE_ARRAY:    printLine(addr, size, "    double "+Unsafe.getLong(addr, 0));  break;

                    default: {
                        Address ref = Address.fromObject(Unsafe.getObject(addr, 0));
                        String s = getMethodName(ref);
                        if (s == null) {
                            s = "";
                        } else {
                            s = "    "+s;
                        }
                        printLine(addr, size, "    ref    "+relocate(ref)+s);
                        break;
                    }

                    case CID.BYTE_ARRAY: {
                        int b  = Unsafe.getByte(addr, 0);
                        String s  = "    byte   "+b;
                        if (b < 0) {
                            s += " ("+(b&0xFF)+")";
                        }
                        char ch = (char)(b & 0xFF);
                        if (ch >= ' ' && ch <= '~') {
                            while (s.length() < 30) {
                                s = s + " ";
                            }
                            s = s + ch;
                        }
                        printLine(addr, size, s);
                        break;
                    }
                }
            }
        }
        Address nextBlock = oop.add(size * length).roundUpToWord();
        updateHistogramFor(getClassName(klass), nextBlock.diff(block).toInt());
        return nextBlock;

    }

    /**
     * Update the frame pointers within a stack chunk.
     *
     * @param chunk  the pointer to the stack chunk.
     * @param size the size of a word.
     * @param length the number of words
     */
    private static void decodeChunk(Address chunk, int size, int length) {
        Hashtable annotations = new Hashtable();

        /*
         * Skip through the activations building up the annotations.
         */
        annotations.put(chunk, "root fp");
        Address fp = Address.fromObject(Unsafe.getObject(chunk, 0));
        boolean hasLocals = false;
        while (!fp.isZero()) {
            //int delta = fp.diff(chunk);
            annotateActivation(fp, hasLocals, annotations);
            hasLocals = true;
            //int offsetToPointer = delta + (FP.returnFP * HDR.BYTES_PER_WORD);
            //fp = Address.fromObject(Unsafe.getObject(chunk.add(offsetToPointer), 0));
            fp = getPreviousFP(fp);
        }

        /*
         * Dunp the words adding the annotations.
         */
        for (int i = 0 ; i < length * size ; i += size) {
            Address addr = chunk.add(i);
            Address ref = Address.fromObject(Unsafe.getObject(addr, 0));
            String ann  = (String)annotations.get(addr);
            if (ann == null) {
                ann = "";
            } else {
                ann = "    "+ann;
            }
            String s = getMethodName(ref);
            if (s == null) {
                s = "";
            } else {
                s = "    {"+s+"}";
            }
            printLine(addr, size, "    ref    "+relocate(ref)+ann+s);
        }
    }

    private static Address getPreviousFP(Address fp) {
        return Address.fromObject(Unsafe.getObject(fp, FP.returnFP));
    }

    private static Address getPreviousIP(Address fp) {
        return Address.fromObject(Unsafe.getObject(fp, FP.returnIP));
    }

    private static Address getCurrentMP(Address fp) {
        return Address.fromObject(Unsafe.getObject(fp, FP.method));
    }

    /**
     * Add annotations for an activation record.
     *
     * @param fp  the frame pointer
     * @param hasLocals true if the activation has locals
     * @param annotations the hash table in which to place the annotations
     */
    private static void annotateActivation(Address fp, boolean hasLocals, Hashtable annotations) {

        annotations.put(fp, "<- fp");
        annotations.put(fp.add(FP.returnFP * HDR.BYTES_PER_WORD), "r-fp");
        annotations.put(fp.add(FP.returnIP * HDR.BYTES_PER_WORD), "r-ip");

        Address previousIP = getPreviousIP(fp);

        if (!previousIP.isZero()) {
            Address previousFP = getPreviousFP(fp);
            Address previousMP = getCurrentMP(previousFP);
            int diff = previousIP.diff(previousMP).toInt();
            int mlength = decodeLengthWord(previousMP, HDR.length);
            if (diff >= mlength) {
                System.out.println("ip "+diff+" appears to overflow method "+previousMP);
            }
        }
    }


    /**
     * Decode a header word.
     *
     * @param oop the object pointer
     * @param offset the the offset in words to the length word
     * @return the decoded length word
     */
    private static int decodeLengthWord(Address oop, int offset) {
        int word = Unsafe.getUWord(oop, offset).toInt();
        int length = word >>> HDR.headerTagBits;
        return length;
    }

    /**
     * Decode an method.
     *
     * @param block the address before the header of the object
     * @return the address of the next object header
     */
    private static Address decodeMethod(Address block) {
        int headerLength = decodeLengthWord(block, 0);
        Address oop = block.add(headerLength * HDR.BYTES_PER_WORD);
        Object klass = getKlass(oop);
        int length = decodeLengthWord(oop, HDR.length);
        //Object definingKlass = Unsafe.getObject(oop, HDR.methodDefiningClass);
        printMethodHeader(block, oop);
        MapBytecodeTracer tracer = new MapBytecodeTracer(oop, length);
        for (int i = 0 ; i < length ;) {
            BytecodeTracerEntry entry = tracer.trace();
            printLine(entry.getAddress(), entry.getSize(), entry.getText());
            i += entry.getSize();
        }

        Address nextBlock = oop.add(length).roundUpToWord();
        updateHistogramFor(getClassName(klass), nextBlock.diff(block).toInt());
        return nextBlock;
    }

    /**
     * Get super class of a class
     *
     * @param klass the klass
     * @return the super class or null of there is none
     */
    private static Object getSuperType(Object klass) {
        if (getClassID(klass) == CID.OBJECT) {
            return null;
        } else {
            return Unsafe.getObject(klass, Class_superType);
        }
    }

    /**
     * Get the class ID from a klass
     *
     * @param klass the klass
     * @return the class id
     */
    private static int getClassID(Object klass) {
        return Unsafe.getInt(klass, Class_classID);
    }

    /**
     * Get the instance size of a klass
     *
     * @param klass the klass
     * @return the instance size
     */
    private static int getInstanceSize(Object klass) {
        return Unsafe.getShort(klass, Class_instanceSize);
    }

    /**
     * Get the compent size of a klass
     *
     * @param klass the klass
     * @return the component size in bytes
     */
    private static int getComponentSize(Object klass) {
        int cid  = getClassID(klass);
        if (cid == CID.STRING) {
            return 2;
        }
        if (cid == CID.STRING_OF_BYTES) {
            return 1;
        }
        return getKlassDataSize(Unsafe.getObject(klass, Class_componentType));
    }

    /**
     * Get the data size (in bytes) of the type represented by this class.
     *
     * @return the data size of a value of the type represented by this class
     */
    private static int getDataSize(int cid) {
        switch (cid) {
            case CID.BOOLEAN:
            case CID.BYTE: {
                return 1;
            }
            case CID.CHAR:
            case CID.SHORT: {
                return 2;
            }
            case CID.DOUBLE:
            case CID.LONG: {
                return 8;
            }
            case CID.FLOAT:
            case CID.INT: {
                return 4;
            }
            default: {
                return HDR.BYTES_PER_WORD;
            }
        }
    }

    /**
     * Get the data size (in bytes) of the type represented by this class.
     *
     * @return the data size of a value of the type represented by this class
     */
    private static int getKlassDataSize(Object klass) {
        return getDataSize(getClassID(klass));
    }

    /**
     * Load a map file
     *
     * @param file the name of the file
     */
    private static void loadMap(String file) throws IOException {
        map.load(new FileInputStream(file));

        /*
         * Find various fields in java.lang.Klass.
         */
/*
        Assert.that(getStringProperty("CLASS."+CID.CLASS+".NAME").equals("java.lang.Class"));

        for (int i = 0 ;; i++) {
            String name = getStringProperty("FIELD."+CID.CLASS+"."+i+".NAME");
            if (name == null) {
                break;
            }
            if (name.equals("superType")) {
                Class_superType = getIntProperty("FIELD."+CID.CLASS+"."+i+".OFFSET");
                Assert.that(getIntProperty("FIELD."+CID.CLASS+"."+i+".CLASS") == CID.CLASS);
            }
            if (name.equals("classID")) {
                Class_classID = getIntProperty("FIELD."+CID.CLASS+"."+i+".OFFSET");
                Assert.that(getIntProperty("FIELD."+CID.CLASS+"."+i+".CLASS") == CID.INT);
            }
            if (name.equals("name")) {
                Class_name = getIntProperty("FIELD."+CID.CLASS+"."+i+".OFFSET");
                Assert.that(getIntProperty("FIELD."+CID.CLASS+"."+i+".CLASS") == CID.STRING);
            }
            if (name.equals("instanceSize")) {
                Class_instanceSize = getIntProperty("FIELD."+CID.CLASS+"."+i+".OFFSET");
                Assert.that(getIntProperty("FIELD."+CID.CLASS+"."+i+".CLASS") == CID.SHORT);
            }
            if (name.equals("componentType")) {
                Class_componentType = getIntProperty("FIELD."+CID.CLASS+"."+i+".OFFSET");
                Assert.that(getIntProperty("FIELD."+CID.CLASS+"."+i+".CLASS") == CID.CLASS);
            }
        }

        Assert.that(Class_superType >= 0);
        Assert.that(Class_classID >= 0);
        Assert.that(Class_instanceSize >= 0);
        Assert.that(Class_componentType >= 0);
 */
    }

    /**
     * Get a string property
     *
     * @param name the property name
     * @return the property value
     */
    private static String getStringProperty(String name) {
        return map.getProperty(name);
    }

    /**
     * Get an int property
     *
     * @param name the property name
     * @return the property value
     */
    private static int getIntProperty(String name) {
        try {
            return Integer.parseInt(getStringProperty(name));
        } catch(NumberFormatException ex) {
            throw new RuntimeException("in getIntProperty()");
        }
    }

    /**
     * Get class name
     *
     * @param cid the class id
     * @return the name
     */
    private static String getClassName(int cid) {
        return getStringProperty("CLASS."+cid+".NAME");
    }

    /**
     * Get class name
     *
     * @param cid the class id
     * @return the name
     */
    private static String getClassName(Object klass) {
        Address soop = Address.fromObject(Unsafe.getObject(klass, Class_name));
        return getString(soop);
    }

    /*-----------------------------------------------------------------------*\
     *                           Methods                                     *
    \*-----------------------------------------------------------------------*/

    /**
     * Get method name
     *
     * @param addr the method oop
     * @return the name
     */
    private static String getMethodName(Address addr) {
        return getStringProperty("METHOD."+addr+".NAME");
    }

    /*-----------------------------------------------------------------------*\
     *                           Fields                                      *
    \*-----------------------------------------------------------------------*/

    static class FieldInfo {
        final String name;
        final int offset;
        final int typeID;
        FieldInfo(String name, int offset, int typeID) {
            this.name = name;
            this.offset = offset;
            this.typeID = typeID;
        }
    }

    /**
     * Cache of classes loaded via the translator.
     */
    private static IntHashtable klasses = new IntHashtable();

    /**
     * Tries to find a class's definition via the translator (if any).
     *
     * @param klass   the address of the class in Squawk memory
     * @return Klass  the Klass instance corresponf to <code>klass</code> or nnull if it is not available
     */
    private static Klass lookupKlass(Object klass) {
        if (translator == null) {
            return null;
        }
        int cid = getClassID(klass);
        Klass k = (Klass)klasses.get(cid);
        if (k == null) {
            String name = getClassName(klass);
            k = translator.findClass(name, -1, false);
            klasses.put(cid, k);
            try {
                translator.load(k);
            } catch (NoClassDefFoundError e) {
System.err.println("Couldn't find definition for class " + name);
                return null;
            }
        }
        return k;

    }

    private static FieldInfo getField(Object klass, int index) {
        int cid = getClassID(klass);
        int fieldID = cid << 16 | index;

        // Look for a class definition in the symbols file first
        if (getStringProperty("CLASS." + cid + ".NAME") != null) {
            String name = getStringProperty("FIELD." + cid + "." + index + ".NAME");
            if (name != null) {
                int offset = getIntProperty("FIELD." + cid + "." + index + ".OFFSET");
                int typeID = getIntProperty("FIELD." + cid + "." + index + ".CLASS");
                return new FieldInfo(name, offset, typeID);
            }
            return null;
        }

        // Now look for a class definition via the translator (if any)
        Klass k = lookupKlass(klass);
        if (k == null) {
            return null;
        }

        if (index < k.getFieldCount(false)) {
            Field f = k.getField(index, false);
            return new FieldInfo(f.getName(), f.getOffset(), f.getType().getClassID());
        }

        return null;
    }

}

/**
 * MapBytecodeTracer
 */
class MapBytecodeTracer extends BytecodeTracer {
    Address addr;
    int length;
    int currentPosition;
    int lastPosition;
    Vector queue = new Vector();

    /**
     * Constructor.
     */
    MapBytecodeTracer(Address addr, int length) {
        this.addr   = addr;
        this.length = length;
    }

    /**
     * Get the current bytecode offset.
     *
     * @return the value
     */
    protected int getCurrentPosition() {
        return currentPosition;
    }

    /**
     * Print a string.
     *
     * @param str the string
     */
    protected void print(String str) {
        int size = currentPosition - lastPosition;
        String s = ""+lastPosition+":";
        while (s.length() < 5) {
            s += " ";
        }
        s += " ";
        BytecodeTracerEntry entry = new BytecodeTracerEntry(addr.add(lastPosition), size, "    "+s+str);
        queue.addElement(entry);
        lastPosition = currentPosition;
    }

    /**
     * Get the next signed byte from the method.
     *
     * @return the value
     */
    protected int getByte() {
        return Unsafe.getByte(addr, currentPosition++);
    }

    /**
     * Get one trace entry.
     *
     * @return the entry
     */
    BytecodeTracerEntry trace() {
        if (queue.isEmpty()) {
            traceByteCode();
        }
        BytecodeTracerEntry entry = (BytecodeTracerEntry)queue.firstElement();
        queue.removeElementAt(0);
        return entry;
    }

}

/**
 * BytecodeTracerEntry
 */
class BytecodeTracerEntry {

    Address addr;
    int size;
    String text;

    BytecodeTracerEntry(Address addr, int size, String text) {
        this.addr = addr;
        this.size = size;
        this.text = text;
    }

    Address getAddress() {
        return addr;
    }

    int getSize() {
        return size;
    }

    String getText() {
        return text;
    }
}
