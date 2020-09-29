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
import com.sun.squawk.util.*;
import com.sun.squawk.util.Hashtable;
import com.sun.squawk.util.Stack;
import com.sun.squawk.util.ArgsUtilities;
import com.sun.squawk.translator.Translator;

/**
 * This class provides base functionality for dumping one or more object memories, annotating the
 * dump with symbolic information.
 *
 * @author  Doug Simon
 */
public final class ClasspathMapper {

    /*---------------------------------------------------------------------------*\
     *                      Translator                                           *
    \*---------------------------------------------------------------------------*/

    private static final String DEFAULT_CLASSPATH = ArgsUtilities.toPlatformPath("j2me/j2meclasses:translator/j2meclasses:graphics/j2meclasses:samples/j2meclasses:tck/j2meclasses", true);

    private static final int BYTES_PER_WORD = HDR.BYTES_PER_WORD;

    private int bytecodes = 0;

    /**
     * Creates and initializes the translator.
     *
     * @param classPath   the class search path
     */
    private void initializeTranslator(String classPath) {
        SuiteManager.initialize(new Suite[0]);
        int sno = SuiteManager.allocateFreeSuiteNumber();
        Suite suite = new Suite("-open-", sno, null, classPath);
        SuiteManager.installSuite(suite);

        Isolate isolate = new Isolate(null, null, suite);
        VM.setCurrentIsolate(isolate);

        isolate.setTranslator(new Translator());
        VM.translator = (Translator)isolate.getTranslator();

//Tracer.enableFeature("loading");

        /*
         * Trigger the class initializer for java.lang.Klass. An error will have
         * occurred if it was triggered before this point.
         */
        Klass top = Klass.TOP;
    }

    /*---------------------------------------------------------------------------*\
     *                        Histogram                                          *
    \*---------------------------------------------------------------------------*/

    /**
     * Stores a histogram of how many instances of each type are present in the object memory
     * currently being dumped.
     */
    private HashMap histogram = new HashMap();

    /**
     * A HistogramEntry instance records the frequency and total size of instances of a single class.
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
            /**
             * Specifies if sorting is to be by the 'count' or 'size' field.
             */
            private final boolean sortByCount;

            /**
             * Create a sorter to sort HistogramEntrys by their 'count' or 'size' fields.
             *
             * @param sortByCount  specifies if sorting is to be by the 'count' or 'size' field
             */
            Sorter(boolean sortByCount) {
                this.sortByCount = sortByCount;
            }

            /**
             * Compares two HistogramEntrys.
             *
             * @param o1 Object
             * @param o2 Object
             * @return int
             */
            public int compare(Object o1, Object o2) {
                if (o1 == o2) {
                    return 0;
                }

                HistogramEntry e1 = (HistogramEntry)o1;
                HistogramEntry e2 = (HistogramEntry)o2;
                int result = sortByCount ? e1.count - e2.count : e1.size - e2.size;
                if (result == 0) {
                    return e1.klass.compareTo(e2.klass);
                } else {
                    return result;
                }
            }
        }
    }

    /**
     * Updates the histogram entry for a given class based on an instance being parsed by the caller.
     *
     * @param name  the name of the class
     * @param size  the size (in bytes) of the instance and its header
     */
    private void updateHistogramFor(String name, int size) {
        HistogramEntry entry = (HistogramEntry)histogram.get(name);
        if (entry == null) {
            entry = new HistogramEntry(name);
        }
        entry.count++;
        entry.size += size;
        histogram.put(name, entry);
    }

    /**
     * Dumps the histogram of instance frequencies or sizes for the current object memory.
     *
     * @param ofCounts  specifies if the histogram of counts or sizes is to be dumped
     */
    private void dumpHistogram(boolean ofCounts) {
        SortedSet sorted = new TreeSet(new HistogramEntry.Sorter(ofCounts));
        sorted.addAll(histogram.values());

        out.println();
        out.println("Histogram of instance " + (ofCounts ? "count" : "size") + "s:" );
        int total = 0;
        for (Iterator iterator = sorted.iterator(); iterator.hasNext(); ) {
            HistogramEntry entry = (HistogramEntry)iterator.next();
            int value = (ofCounts ? entry.count : entry.size);
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

    /*---------------------------------------------------------------------------*\
     *                                Relocation                                 *
    \*---------------------------------------------------------------------------*/

    /**
     * Map of object memories to the addresses at which they were relocated in an execution.
     */
    private Hashtable relocationTable;

    /**
     * The address to which the current object memory was relocated or null if it wasn't relocated.
     */
    private Offset currentRelocation;

    /**
     * Updates the {@link relocationTable relocation table} for a given object memory and its parents
     * based on a given properties object mapping object memory URLs to addresses.
     *
     * @param om          an object memory
     * @param properties  map of URLs to addresses
     * @return the address at which <code>om</code> was relocated or null if it wasn't
     */
    private Address setRelocationFor(ObjectMemory om, Properties properties) {
        ObjectMemory parent = om.getParent();
        Address parentRelocation = Address.zero();
        if (parent != null) {
            parentRelocation = setRelocationFor(parent, properties);
            if (parentRelocation == null) {
                return null;
            }
        }

        String value = properties.getProperty(om.getURL());
        Address relocatedTo = null;
        if (value == null) {
            if (parent != null) {
                relocatedTo = parentRelocation.add(parent.getSize());
                relocationTable.put(om, relocatedTo);
            }
        } else {
            relocatedTo = Address.zero().addOffset(Offset.fromPrimitive((int/*S64*/)Long.parseLong(value)));
            relocationTable.put(om, relocatedTo);
        }
        return relocatedTo;
    }

    /**
     * Parses a given file containing relocation information and updates the {@link relocationTable relocation table}
     * for a given object memory and its parents. Each line in a relocation file must be of the format '<url>=<address>'.
     *
     * @param file    the name of the file containing relocation information
     * @param om      an object memory
     * @throws IOException
     */
    private void parseRelocationFile(String file, ObjectMemory om) throws IOException {
        if (!new File(file).exists()) {
            if (!file.equals("squawk.reloc")) {
                throw new RuntimeException(file + " does not exist");
            }
        }

        Properties properties = new Properties();
        BufferedReader br = new BufferedReader(new FileReader(file));
        String line = br.readLine();
        int lno = 1;
        while (line != null) {
            StringTokenizer st = new StringTokenizer(line, "=");
            if (st.countTokens() != 2) {
                throw new RuntimeException(file + ":" + lno + ": does not match '<url>=<address>' pattern");
            }
            properties.setProperty(st.nextToken(), st.nextToken());
            line = br.readLine();
            lno++;
        }

        relocationTable = new Hashtable();
        if (setRelocationFor(om, properties) == null) {
            relocationTable = null;
        }
    }

    /**
     * Relocates an address that is within the range of the current object memory. No relocation
     * is performed if a relocation file was not provided.
     *
     * @param addr   the address to relocate
     * @return the relocated address
     */
    private Address relocate(Address addr) {
        if (relocationTable == null) {
            return addr;
        }
        return addr.addOffset(currentRelocation);
    }

    /**
     * Relocates an address that is within the range of any of the loaded object memories. No relocation
     * is performed if a relocation file was not provided.
     *
     * @param addr   the address to relocate
     * @return the relocated address
     */
    private Address relocatePointer(Address addr) {
        if (relocationTable == null || addr == Address.zero()) {
            return addr;
        }

        ObjectMemory[] memories = loadedObjectMemories;
        for (int i = 0; i != memories.length; ++i) {
            ObjectMemory memory = memories[i];
            Address start = memory.getStart();
            Address end = memory.getEnd();
            if (addr.hi(start) && addr.loeq(end)) {
                Offset delta = ((Address)relocationTable.get(memory)).diff(memory.getStart());
                return addr.addOffset(delta);
            }
        }
        return addr.addOffset(currentRelocation);
    }

    /*---------------------------------------------------------------------------*\
     *                        Command line parsing and main loop                 *
    \*---------------------------------------------------------------------------*/

    /**
     * Looks for any system property that ends with ".cpu.endian" to
     * determine the endianess of the image.
     *
     * @return  true if a system property ending with ".cpu.endian" was found
     *          and its value contained the string "big" otherwise false
     */
    private static boolean getEndianess() {
        Properties properties = System.getProperties();
        Enumeration names = properties.propertyNames();
        while (names.hasMoreElements()) {
            String name = (String)names.nextElement();
            if (name.endsWith(".cpu.endian")) {
                String value = properties.getProperty(name);
                boolean big = (value.toLowerCase().indexOf("big") != -1);
                return big;
            }
        }
        return false;
    }

    /**
     * Prints the usage message.
     *
     * @param  errMsg  an optional error message
     */
    private void usage(String errMsg) {
        boolean bigEndian = getEndianess();
        PrintStream out = System.out;
        if (errMsg != null) {
            out.println(errMsg);
        }
        out.println("Usage: mapper [-options] [object_memory_file]");
        out.println("where options include:");
        out.println();
        out.println("    -cp:<directories and jar/zip files separated by '"+File.pathSeparatorChar+"'>");
        out.println("                paths where classes can be found (default=" + DEFAULT_CLASSPATH + ")");
        out.println("    -cp/a:<directories and jar/zip files separated by '"+File.pathSeparatorChar+"'>");
        out.println("                append to end of class path");
        out.println("    -cp/p:<directories and jar/zip files separated by '"+File.pathSeparatorChar+"'>");
        out.println("                prepend in front of class path");
        out.println("    -o:<file>   dump to 'file' (default=squawk.cmap)");
        out.println("    -s:<n>      start dumping at the n'th object memory");
        out.println("    -r:<file>   uses file or relocation info (default=squawk.reloc)");
        out.println("    -little     specifies little endian object memories" + (!bigEndian ? " (default)" : ""));
        out.println("    -big        specifies big endian object memories" +    ( bigEndian ? " (default)" : ""));
        out.println("    -h          show this help message");
    }

    /**
     * The output dump stream.
     */
    private PrintStream out;

    /**
     * Set true when the next output line is the destination of an oop.
     */
    private boolean oopNext;

    /**
     * The remaining object memories to be dumped.
     */
    private Stack objectMemories;

    /**
     * The loaded object memories.
     */
    private ObjectMemory[] loadedObjectMemories;

    /**
     * The object memory currently being dumped.
     */
    private ObjectMemory currentObjectMemory;

    /**
     * The number of methods in the suite.
     */
    private int totalMethods;

    /**
     * The total number of bytes in the method headers.
     */
    private int totalMethodHeaderLength;

    /**
     * Display a warning message for a supplied command line option that is not
     * enabled in the system.
     *
     * @param option the disabled option
     */
    private void showDisabledOptionWarning(String option) {
        System.err.println("warning: '" + option + "' option is disabled in current build");
    }

    /**
     * Parses the command line arguments to configure an execution of the mapper.
     *
     * @param args   the command line arguments
     * @return boolean true if there were no errors in the arguments
     */
    private final boolean parseArgs(String[] args) throws IOException {
        String objectMemoryFile = "squawk.suite";
        String outFile = "squawk.cmap";
        int start = 0;
        String relocationFile = null;
        String classPath = DEFAULT_CLASSPATH;
        String prependClassPath = null;
        String appendClassPath = null;
        VM.bigEndian = getEndianess();

        int argc = 0;
        for (; argc != args.length; ++argc) {
            String arg = args[argc];
            if (arg.charAt(0) != '-') {
                break;
            }
            if (arg.startsWith("-s:")) {
                start = Integer.parseInt(arg.substring("-s:".length()));
            } else if (arg.startsWith("-cp:")) {
                classPath = arg.substring("-cp:".length());
            } else if (arg.startsWith("-cp/p:")) {
                prependClassPath = arg.substring("-cp/p:".length());
            } else if (arg.startsWith("-cp/a:")) {
                appendClassPath = arg.substring("-cp/a:".length());
            } else if (arg.startsWith("-r:")) {
                relocationFile = arg.substring("-r:".length());
            } else if (arg.startsWith("-o:")) {
                outFile = arg.substring("-o:".length());
            } else if (arg.startsWith("-trace")) {
                if (arg.startsWith("-tracefilter:")) {
                    String optArg = arg.substring("-tracefilter:".length());
                    if (Klass.DEBUG) {
                        Tracer.setFilter(optArg);
                    } else {
                        showDisabledOptionWarning(arg);
                    }
                } else {
                    if (Klass.DEBUG) {
                        Tracer.enableFeature(arg.substring("-trace".length()));
                        if (arg.equals("-traceconverting")) {
                            Tracer.enableFeature("loading"); // -traceconverting subsumes -traceloading
                        }
                    } else {
                        showDisabledOptionWarning(arg);
                    }
                }
            } else if (arg.equals("-big")) {
                VM.bigEndian = true;
            } else if (arg.equals("-little")) {
                VM.bigEndian = false;
            } else if (arg.equals("-h")) {
                usage(null);
                return false;
            } else {
                usage("unknown option: " + arg);
                return false;
            }
        }

        if (argc != args.length) {
            objectMemoryFile = args[argc];
        }

        if (prependClassPath != null) {
            classPath = prependClassPath + File.pathSeparatorChar + classPath;
        }
        if (appendClassPath != null) {
            classPath += File.pathSeparatorChar + appendClassPath;
        }
        initializeTranslator(classPath);
        out = new PrintStream(new BufferedOutputStream(new FileOutputStream(outFile), 10000));
        ObjectMemory leafObjectMemory = loadObjectMemories(objectMemoryFile, start);

        if (relocationFile != null) {
            parseRelocationFile(relocationFile, leafObjectMemory);
        }

        out.println("loaded object memories: " + objectMemories.size());
        for (int i = objectMemories.size() - 1; i >= 0; --i) {
            ObjectMemory objectMemory = (ObjectMemory)objectMemories.elementAt(i);
            showSummary(objectMemory);
        }

        return true;
    }

    /**
     * Execute the mapper and produce the dump.
     */
    private final void run() {
        try {
            while (!objectMemories.isEmpty()) {
                currentObjectMemory = (ObjectMemory)objectMemories.pop();
                if (relocationTable != null) {
                    Address relocatedTo = (Address)relocationTable.get(currentObjectMemory);
                    currentRelocation = relocatedTo.diff(currentObjectMemory.getCanonicalStart());
                }

                Address block = currentObjectMemory.getStart();
                Address end = block.add(currentObjectMemory.getSize());

                out.println("+++++++++++++++++++++  start of object memory in " + currentObjectMemory.getURL() + " +++++++++++++++++++++++");
                while (block.lo(end)) {
                    int tag = Unsafe.getUWord(block, 0).and(UWord.fromPrimitive(HDR.headerTagMask)).toInt();
                    switch ((int)tag) {
                        case (int) HDR.basicHeaderTag:   block = decodeInstance(block); break;
                        case (int) HDR.arrayHeaderTag:   block = decodeArray(block);    break;
                        case (int) HDR.methodHeaderTag:  block = decodeMethod(block);   break;
                        default: Assert.shouldNotReachHere("Invalid header word");
                            break;
                    }
                }
                out.flush();


                // Show the histogram of counts
                dumpHistogram(true);

                // Show the histogram of sizes
                dumpHistogram(false);
                out.println();

                // Show the number of methods
                out.println("Method count = " + totalMethods);
                out.println("Average method header = " + ((double)totalMethodHeaderLength)/totalMethods);
                out.println();


                // Show the number of bytecodes
                out.println("Bytecode count = " + bytecodes);
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
     * Initialize the contents of memory from one or more object memory files.
     *
     * @param file       the tail object memory file name
     * @return the object memory in <code>file</code>
     */
    private ObjectMemory loadObjectMemories(String file, int start) throws IOException {
        ObjectMemory leafObjectMemory = ObjectMemoryLoader.load("file://" + file, false);
        objectMemories = new Stack();
        ObjectMemory objectMemory = leafObjectMemory;
        Stack allObjectMemories = new Stack();

        while (objectMemory != null) {
            if (objectMemory.getParentCount() >= start) {
                objectMemories.push(objectMemory);
            }
            allObjectMemories.push(objectMemory);
            objectMemory = objectMemory.getParent();

        }

        // Register the classes and methods defined in the object memories
        loadedObjectMemories = new ObjectMemory[allObjectMemories.size()];
        int index = 0;
        while (!allObjectMemories.isEmpty()) {
            objectMemory = (ObjectMemory)allObjectMemories.pop();
            loadedObjectMemories[index++] = objectMemory;

            // Register the classes
            Address block = objectMemory.getStart();
            Address end = block.add(objectMemory.getSize());
            while (block.lo(end)) {
                Address object = GC.blockToOop(block);
                Klass klass = GC.getKlass(object);
                block = object.add(GC.getBodySize(klass, object));
            }

            // Register the methods
            int staticMethodsOffset = Klass.CLASS.lookupField("staticMethods", Klass.OBJECT_ARRAY, false).getOffset();
            int virtualMethodsOffset = Klass.CLASS.lookupField("virtualMethods", Klass.OBJECT_ARRAY, false).getOffset();
            block = objectMemory.getStart();
            while (block.lo(end)) {
                Address object = GC.blockToOop(block);
                if (GC.getKlass(object).isSubclassOf(Klass.CLASS)) {
                    Klass klass = VM.asKlass(object);
                    registerMethods(klass, Unsafe.getObject(object, virtualMethodsOffset), false);
                    registerMethods(klass, Unsafe.getObject(object, staticMethodsOffset), true);
                }
                block = object.add(GC.getBodySize(GC.getKlass(object), object));
            }
        }

        return leafObjectMemory;
    }

    /**
     * Show a summary of an object memory.
     *
     * @param memory   the object memory to summarize
     */
    private void showSummary(ObjectMemory memory) {
        if (memory != null) {
            int size = memory.getSize();
            out.println(memory.getURL() + " {");
            out.println("                size = " + size);
            out.println("                hash = " + memory.getHash());
            out.println("                root = " + Address.fromObject(memory.getRoot()).diff(memory.getStart()));
            out.println("     canonical start = " + memory.getCanonicalStart());
            out.println("       canonical end = " + memory.getCanonicalStart().add(size));
            if (relocationTable != null) {
                Address relocatedTo = (Address)relocationTable.get(memory);
                out.println("    relocation start = " + relocatedTo);
                out.println("      relocation end = " + relocatedTo.add(size));
            }
            out.println("}");
        }
    }

    /*---------------------------------------------------------------------------*\
     *                        Space padding                                      *
    \*---------------------------------------------------------------------------*/

    /**
     * An array of characters used to speed up padding of string buffers.
     */
    private static final char[] SPACES;
    static {
        SPACES = new char[1000];
        for (int i = 0; i != SPACES.length; ++i) {
            SPACES[i] = ' ';
        }
    }

    /**
     * Appends space characters to the end of a given string buffer until its length
     * is equal to a given width. If the given width is greater than or equal to the
     * string buffer's current length, then no padding is performed.
     *
     * @param buf   the StringBuffer to pad
     * @param width the length 'buf' should be after padding
     */
    private void pad(StringBuffer buf, int width) {
        int pad = width - buf.length();
        if (pad > 0) {
            buf.append(SPACES, 0, pad);
        }
    }

    /*---------------------------------------------------------------------------*\
     *                        Print line primitives                              *
    \*---------------------------------------------------------------------------*/

    /**
     * Print a line of the map file corresponding to one word.
     *
     * @param addr    the address of the word
     * @param text    the text to be written after the hex
     * @param isOop   true if the word at <code>addr</code> is a reference
     */
    private void printWordLine(Address addr, String text, boolean isOop) {
        printLine(addr, BYTES_PER_WORD, text, (isOop && Unsafe.getUWord(addr, 0).ne(UWord.zero())));
    }

    /**
     * Print a line of the map file corresponding to one or more bytes.
     *
     * @param addr    the address of the first byte
     * @param length  the number of bytes
     * @param text    the text to be written after the hex
     * @param isOop   true if the word at <code>addr</code> is a reference
     */
    private void printLine(Address addr, int length, String text, boolean isOop) {

        Assert.that(length <= 9);
        StringBuffer buf = new StringBuffer(120);

        // Denote a pointer/reference by prefixing the line with a '+'
        if (Unsafe.isReference(addr)) {
            buf.append('+');
        } else {
            Assert.that(!isOop, "oop map bit not set for pointer at " + addr);
            buf.append(' ');
        }

        // Pad the address with leading spaces
        String address = relocate(addr).toString();
        pad(buf, 11 - address.length());

        // Dump the address
        buf.append(address);


        // Denote the start of an object with '*'
        if (oopNext) {
            buf.append(" * ");
            oopNext = false;
        } else {
            buf.append(" : ");
        }

        // Only print up to 8 bytes per line
        int startBytes = buf.length();
        for (int i = 0 ; i != 8 ; i++) {
            if (i < length) {
                int b = Unsafe.getByte(addr, i) & 0xFF;
                if (b < 16) {
                    buf.append(" 0").append(Integer.toHexString(b));
                } else {
                    buf.append(" ").append(Integer.toHexString(b));
                }
/*if[TYPEMAP]*/
                byte type = Unsafe.getType(addr.add(i));
                buf.append(':').append(AddressType.getMnemonic(type));
/*end[TYPEMAP]*/
            }
        }

        // Add padding after the bytes on the assumption that each byte occupies 3 spaces
/*if[TYPEMAP]*/
        pad(buf, (startBytes + 8 * 5));
/*else[TYPEMAP]*/
//      pad(buf, (startBytes + 8 * 3));
/*end[TYPEMAP]*/


        // Pad after the bytes
        buf.append("   ");

        // Dump the line
        out.print(buf);
        if (length > 8) {
            out.println(text + " ...");
            printLine(addr.add(8), length - 8, "... " + text, false);
        } else {
            out.println(text);
        }
    }

    /**
     * Print a line of the map file corresponding to one or more bytes.
     *
     * @param addr    the address of the first byte
     * @param length  the number of bytes
     * @param text    the text to be written after the hex
     */
    private void printLine(Address addr, int length, String text) {
        printLine(addr, length, text, false);
    }

    /**
     * Formats a base type, optional field name or element index and value and appends
     * this to a given string buffer.
     *
     * @param buf   the StringBuffer to append to
     * @param type  the base type of the value
     * @param name  the field name or element index
     * @param value the value
     */
    private void appendTypeValueName(StringBuffer buf, String type, String name, String value) {
        buf.append("  ").append(type);
        pad(buf, 9);
        if (name != null) {
            buf.append(name).append(" = ");
        }
        buf.append(value).append(' ');
    }

    /**
     * Prints a line for a primitive value.
     *
     * @param address       the address of the value
     * @param size          the size (in byters) of the value
     * @param type          the type of the value
     * @param name          the name of the value or null
     * @param value         the value
     */
    private void printPrimitiveLine(Address address, int size, String type, String name, String value) {
        StringBuffer buf = new StringBuffer(40);
        appendTypeValueName(buf, type, name, value);
        printLine(address, size, buf.toString());
    }

    /**
     * Prints a line for a pointer value. Some extra annotation is also appended for pointers of certain types.
     *
     * @param pointerAddress      the address of the pointer
     * @param fieldName           the name of an instance field or index of an array
     * @param declaredType        the declared type of the object pointed to or null if this is a pointer to
     *                            within an object
     */
    private void printPointerLine(Address pointerAddress, String name, Klass declaredType) {
        Address ref = (Address)Unsafe.getObject(pointerAddress, 0);
        StringBuffer buf = new StringBuffer(80);
        boolean isInternalPointer = (declaredType == null);
        appendTypeValueName(buf, isInternalPointer ? "iref" : "ref", name, relocatePointer(ref).toString());

        if (!isInternalPointer) {
            pad(buf, 35);
            if (ref.isZero()) {
                buf.append(declaredType.getName());
            } else {
                Klass actualType = GC.getKlass(ref);
                buf.append(actualType.getName());
                int classID = actualType.getClassID();
                if (actualType.isSubclassOf(Klass.CLASS)) {
                    classID = CID.CLASS;
                }
                switch (classID) {
                    case CID.CLASS: {
                        pad(buf, 70);
                        buf.append("// ").append(VM.asKlass(ref).getName());
                        break;
                    }
                    case CID.BYTECODE_ARRAY: {
                        pad(buf, 70);
                        String signature = getMethodSignature(ref);
                        buf.append("// ").append(signature);
                        break;
                    }
                    case CID.STRING:
                    case CID.STRING_OF_BYTES: {
                        pad(buf, 70);
                        String value = VM.asString(ref);
                        buf.append("// \"");
                        for (int j = 0; j != value.length(); ++j) {
                            char ch = value.charAt(j);
                            if (ch < ' ' || ch > '~') {
                                ch = '~';
                            }
                            buf.append(ch);
                        }
                        buf.append('"');
                    }
                }
            }
        }
        printWordLine(pointerAddress, buf.toString(), true);
    }

    /*---------------------------------------------------------------------------*\
     *                        Header printing                                    *
    \*---------------------------------------------------------------------------*/

    /**
     * Prints the type of a non-array instance.
     *
     * @param oop the address of a non-array instance
     */
    private void printInstanceHeader(Address oop) {
        Klass klass = GC.getKlass(oop);
        printWordLine(oop.sub(BYTES_PER_WORD), "instance " + klass.getInternalName(), true);
        oopNext = true;
    }

    /**
     * Prints the type and length of an array.
     *
     * @param oop the address of an array
     * @return  the length of the array
     */
    private int printArrayHeader(Address oop) {
        Klass klass = GC.getKlass(oop);
        int length = GC.getArrayLengthNoCheck(oop);
        printWordLine(oop.sub(HDR.arrayHeaderSize), "[" + length + "]", false);
        printWordLine(oop.sub(HDR.basicHeaderSize), "array " + klass.getInternalName(), true);
        oopNext = true;
        return length;
    }

    /**
     * Prints the detail in a method header.
     *
     * @param block   the block containing a method
     * @param oop     the address of the method
     * @return int    the size of the method's header
     */
    private int printMethodHeader(Address block, Address oop) {
        int headerLength = oop.diff(block).toInt();
        totalMethodHeaderLength += headerLength;
        totalMethods++;
        printWordLine(block, "{"+headerLength+"}", false);
        String headerString = getMethodHeaderText(oop);

        Address definingClassAddress = oop.add(HDR.methodDefiningClass * BYTES_PER_WORD);
        for (Address i = block.add(BYTES_PER_WORD) ; i.lo(definingClassAddress); i = i.add(BYTES_PER_WORD)) {
            printWordLine(i, headerString, false);
            headerString = "";
        }
        Klass definingClass = VM.asKlass(Unsafe.getObject(oop, HDR.methodDefiningClass));
        printWordLine(definingClassAddress, "defined in "+definingClass.getName(), true);
        printWordLine(oop.sub(HDR.arrayHeaderSize),  "["+GC.getArrayLengthNoCheck(oop)+"]", false);
        printWordLine(oop.sub(HDR.basicHeaderSize), "method "+getMethodSignature(oop), true);
        oopNext = true;
        return headerLength;
    }


    /**
     * Get the text for the method header.
     *
     * @param oop method object pointer
     * @return the text
     */
    private String getMethodHeaderText(Object oop) {
        StringBuffer sb    = new StringBuffer();
        int localCount     = MethodBody.decodeLocalCount(oop);
        int parameterCount = MethodBody.decodeParameterCount(oop);
        int maxStack       = MethodBody.decodeStackCount(oop);

        sb.append("p="+parameterCount+" l="+localCount+" s="+maxStack);

        // Format the oopmap.
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

        // Format the type map.
        if (parameterCount+localCount > 0) {
            sb.append(" types=");
            sb.append(MethodBody.decodeTypeMap(oop));
        }


        // Format the exception table (if any).
        if (MethodBody.decodeExceptionTableSize(oop) != 0) {
            int size   = MethodBody.decodeExceptionTableSize(oop);
            int offset = MethodBody.decodeExceptionTableOffset(oop);
            VMBufferDecoder dec = new VMBufferDecoder(oop, offset);
            long end = offset + size;
            Suite suite = VM.getCurrentIsolate().getBootstrapSuite();
            while (dec.getOffset() < end) {
                sb.append(" ["+dec.readUnsignedInt()).
                    append(":"+dec.readUnsignedInt()).
                    append(":"+dec.readUnsignedInt()).
                    append(":"+VM.asKlass(dec.readUnsignedInt()).getName()).
                    append("]");
            }
        }
        return sb.toString();
    }


    /*---------------------------------------------------------------------------*\
     *                        Non-array object decoding                          *
    \*---------------------------------------------------------------------------*/

    /**
     * Decode an instance.
     *
     * @param block  the address of the header of the object
     * @return the address of the next object header
     */
    private Address decodeInstance(Address block) {
        Address oop = block.add(HDR.basicHeaderSize);
        printInstanceHeader(oop);
        Klass klass = GC.getKlass(oop);
        Address nextBlock = printAllInstanceFields(oop, klass);
        updateHistogramFor(klass.getName(), nextBlock.diff(block).toInt());
        return nextBlock;
    }

    /**
     * Print the fields of a non-array object.
     *
     * @param oop    the address of the object
     * @param klass  the class of the object
     * @return the address of the next object header
     */
    private Address printAllInstanceFields(Address oop, Klass klass) {
        Klass superClass = klass.getSuperclass();
        Address superEnd;
        if (superClass != null) {
            superEnd = printAllInstanceFields(oop, superClass);
        } else {
            superEnd = oop;
        }
        Address oopEnd = printFields(oop, superEnd, klass, false);
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
    private Address printFields(final Address oop, final Address firstField, Klass klass, boolean isStatic) {
        int size = (isStatic ? klass.getStaticFieldsSize() + CS.firstVariable : klass.getInstanceSize()) * BYTES_PER_WORD;
        Address nextField = firstField;

        // Dump the class pointer and next pointer
        if (isStatic) {

            printPointerLine(oop.add(CS.klass * BYTES_PER_WORD), "CS.klass", Klass.CLASS);
            printPointerLine(oop.add(CS.next * BYTES_PER_WORD), "CS.next", Klass.CLASS);
        }

        // Print the fields.
        int fieldCount = klass.getFieldCount(isStatic);
        for (int i = 0 ; i != fieldCount; i++) {
            Field field = klass.getField(i, isStatic);
            String name = field.getName();
            int offset = field.getOffset();
            Klass type = field.getType();

            int fsize = type.getDataSize();
            Address fieldAddress = oop.add(offset * fsize);

            if (isStatic) {
                fieldAddress = firstField.add(offset * BYTES_PER_WORD);
                fsize = Math.max(BYTES_PER_WORD, type.getDataSize());
                if (field.isFinal() && field.hasConstant()) {
                    continue;
                }
            }

            if (!Klass.SQUAWK_64 && (type.isDoubleWord())) {
                fieldAddress = oop.add(offset * 4);
            }

            switch (type.getClassID()) {
                case CID.BOOLEAN:   printPrimitiveLine(fieldAddress, fsize, "bool",   name, ""+Unsafe.getByte(fieldAddress, 0));  break;
                case CID.BYTE:      printPrimitiveLine(fieldAddress, fsize, "byte",   name, ""+Unsafe.getByte(fieldAddress, 0));  break;
                case CID.CHAR:      printPrimitiveLine(fieldAddress, fsize, "char",   name, ""+Unsafe.getChar(fieldAddress, 0));  break;
                case CID.SHORT:     printPrimitiveLine(fieldAddress, fsize, "short",  name, ""+Unsafe.getShort(fieldAddress, 0)); break;
                case CID.INT:       printPrimitiveLine(fieldAddress, fsize, "int",    name, ""+Unsafe.getInt(fieldAddress, 0));   break;
                case CID.FLOAT:     printPrimitiveLine(fieldAddress, fsize, "float",  name, ""+Float.intBitsToFloat(Unsafe.getInt(fieldAddress, 0)));   break;
                case CID.LONG:      printPrimitiveLine(fieldAddress, fsize, "long",   name, ""+Unsafe.getLong(fieldAddress, 0));  break;
                case CID.DOUBLE:    printPrimitiveLine(fieldAddress, fsize, "double", name, ""+Double.longBitsToDouble(Unsafe.getLong(fieldAddress, 0)));  break;
                case CID.UWORD:     printPrimitiveLine(fieldAddress, fsize, "uword",  name, ""+Unsafe.getUWord(fieldAddress, 0));  break;
                case CID.OFFSET:    printPrimitiveLine(fieldAddress, fsize, "offset", name, ""+Unsafe.getUWord(fieldAddress, 0));  break;
                default: {
                    printPointerLine(fieldAddress, name, type);
                    break;
                }
            }
            nextField = fieldAddress.add(fsize);
        }

        nextField = nextField.roundUpToWord();
        Assert.that(oop.add(size).roundUpToWord() == nextField);
        return nextField;
    }

    /*---------------------------------------------------------------------------*\
     *                        Array object decoding                              *
    \*---------------------------------------------------------------------------*/

    /**
     * Decodes an array object.
     *
     * @param block   the address of the header of the array object
     * @return the address of the next object header
     */
    private Address decodeArray(Address block) {
        Address oop = block.add(HDR.arrayHeaderSize);
        Klass klass = GC.getKlass(oop);
        Klass componentType = klass.getComponentType();
        int componentSize = klass.getSquawkArrayComponentDataSize();

        int length = printArrayHeader(oop);
        if (length == 0) {
            return oop;
        }

        if (klass == Klass.LOCAL_ARRAY) {
            decodeChunk(oop, length);
        } else if (klass == Klass.GLOBAL_ARRAY) {
            klass = VM.asKlass(Unsafe.getObject(oop, CS.klass));
            printFields(oop, oop.add(CS.firstVariable * BYTES_PER_WORD), klass, true);
        } else if (klass == Klass.STRING_OF_BYTES || klass == Klass.STRING) {
            String value = VM.asString(oop);
            int charsPerLine = 8 / componentSize;
            Address addr = oop;

            while (value.length() != 0) {
                int charsThisLine = (int)Math.min(charsPerLine, value.length());
                int bytesThisLine = charsThisLine * componentSize;
                printLine(addr, bytesThisLine, '"' + value.substring(0, charsThisLine) + '"');

                if (value.length() >= charsThisLine) {
                    value = value.substring(charsThisLine);
                    addr = addr.add(bytesThisLine);
                }
            }
        } else {
            for (int i = 0 ; i < length ; ++i) {
                Address componentAddress = oop.add(i * componentSize);
                String index = ""+i;
                switch (klass.getClassID()) {
                    case CID.BOOLEAN_ARRAY:   printPrimitiveLine(componentAddress, componentSize, "bool",   index, ""+Unsafe.getByte(componentAddress, 0));  break;
                    case CID.CHAR_ARRAY:      printPrimitiveLine(componentAddress, componentSize, "char",   index, ""+Unsafe.getChar(componentAddress, 0));  break;
                    case CID.SHORT_ARRAY:     printPrimitiveLine(componentAddress, componentSize, "short",  index, ""+Unsafe.getShort(componentAddress, 0)); break;
                    case CID.INT_ARRAY:       printPrimitiveLine(componentAddress, componentSize, "int",    index, ""+Unsafe.getInt(componentAddress, 0));   break;
                    case CID.FLOAT_ARRAY:     printPrimitiveLine(componentAddress, componentSize, "float",  index, ""+Float.intBitsToFloat(Unsafe.getInt(componentAddress, 0)));   break;
                    case CID.LONG_ARRAY:      printPrimitiveLine(componentAddress, componentSize, "long",   index, ""+Unsafe.getLong(componentAddress, 0));  break;
                    case CID.DOUBLE_ARRAY:    printPrimitiveLine(componentAddress, componentSize, "double", index, ""+Double.longBitsToDouble(Unsafe.getLong(componentAddress, 0)));  break;
                    case CID.UWORD_ARRAY:     printPrimitiveLine(componentAddress, componentSize, "word",   index, ""+Unsafe.getUWord(componentAddress, 0));   break;

                    default: {
                        printPointerLine(componentAddress, index, componentType);
                        break;
                    }

                    case CID.BYTE_ARRAY: {
                        int b  = Unsafe.getByte(componentAddress, 0);
                        StringBuffer s = new StringBuffer(80).
                            append("    byte   ").
                            append(b);
                        if (b < 0) {
                            s.append(" (").append(b&0xFF).append(')');
                        }
                        char ch = (char)(b & 0xFF);
                        if (ch >= ' ' && ch <= '~') {
                            pad(s, 30);
                            s.append(ch);
                        }
                        printLine(componentAddress, 1, s.toString());
                        break;
                    }
                }
            }
        }
        Address nextBlock = oop.add(componentSize * length).roundUpToWord();
        updateHistogramFor(klass.getName(), nextBlock.diff(block).toInt());
        return nextBlock;

    }

    /*---------------------------------------------------------------------------*\
     *                        Stack chunk decoding                               *
    \*---------------------------------------------------------------------------*/

    /**
     * Decodes and dumps a stack chunk.
     *
     * @param chunk  the pointer to the stack chunk
     * @param size   the size of a word
     * @param length the number of words
     */
    private void decodeChunk(Address chunk, int length) {
        printPointerLine(chunk.add(SC.next * BYTES_PER_WORD), "next sc", Klass.LOCAL_ARRAY);
        printPointerLine(chunk.add(SC.owner * BYTES_PER_WORD), "owner", Klass.OBJECT);
        printPointerLine(chunk.add(SC.lastFP * BYTES_PER_WORD), "last fp", null);

        Address fp = Address.fromObject(Unsafe.getObject(chunk, SC.lastFP));
        Address bogusEnd;

        // Calculate where the last bogus slot is
        if (fp.isZero()) {
            // The whole chunk is bogus
            bogusEnd = chunk.add(length * BYTES_PER_WORD);
        } else {
            // The last bogus slot is the local slot 1 as local slot 0 contains the
            // method pointer which should be valid
            bogusEnd = fp.sub(BYTES_PER_WORD);
        }

        // Dump the bogus/unused slots
        for (Address slot = chunk.add(SC.limit * BYTES_PER_WORD); slot.loeq(bogusEnd); slot = slot.add(BYTES_PER_WORD)) {
            printPrimitiveLine(slot, BYTES_PER_WORD, "bogus", null, "" + Unsafe.getUWord(slot, 0));
        }

        boolean isInnerMostActivation = true;
        while (!fp.isZero()) {
            decodeActivation(fp, isInnerMostActivation);
            fp = Address.fromObject(Unsafe.getObject(fp, FP.returnFP));
            isInnerMostActivation = false;
        }
    }

    /**
     * Prints a line for a stack chunk slot corresponding to a local variable or parameter.
     *
     * @param lp     the address of the local variable or parameter
     * @param type   the type of the local variable or parameter as specified by {@link MethodBody.decodeTypeMap(Object)}
     * @param name   the name of the local variable or parameter
     */
    private void printLocalOrParameterLine(Address lp, char type, String name) {
        switch (type) {
            case 'I': printPrimitiveLine(lp, BYTES_PER_WORD, "int",     name, "" + Unsafe.getInt(lp, 0)); break;
            case 'J': printPrimitiveLine(lp, BYTES_PER_WORD, "long",    name, "" + Unsafe.getUWord(lp, 0) + "  {" + Unsafe.getLong(lp, 0) + "L}"); break;
            case 'K': printPrimitiveLine(lp, BYTES_PER_WORD, "long2",   name, "" + Unsafe.getInt(lp, 0)); break;
            case 'F': printPrimitiveLine(lp, BYTES_PER_WORD, "float",   name, "" + Unsafe.getInt(lp, 0) + "  {" + Float.intBitsToFloat(Unsafe.getInt(lp, 0)) + "F}"); break;
            case 'D': printPrimitiveLine(lp, BYTES_PER_WORD, "double",  name, "" + Unsafe.getUWord(lp, 0) + "  {" + Double.longBitsToDouble(Unsafe.getLong(lp, 0)) + "D}"); break;
            case 'E': printPrimitiveLine(lp, BYTES_PER_WORD, "double2", name, "" + Unsafe.getInt(lp, 0)); break;
            case 'R': printPointerLine(lp, name, Klass.OBJECT); break;
        }
    }

    /**
     * Decodes a single activation frame.
     *
     * @param fp                     the frame pointer
     * @param isInnerMostActivation  specifies if this is the inner most activation frame on the chunk
     *                               in which case it's local variables are bogus
     */
    private void decodeActivation(Address fp, boolean isInnerMostActivation) {

        Address mp = Address.fromObject(Unsafe.getObject(fp, FP.method));

        StringBuffer buf = new StringBuffer(120);
        pad(buf, 43);
        buf.append("---------- ").
            append(getMethodSignature(mp)).
            append(" ----------");
        out.println(buf);

        int localCount     = isInnerMostActivation ? 1 : MethodBody.decodeLocalCount(mp);
        int parameterCount = MethodBody.decodeParameterCount(mp);
        char[] typeMap     = MethodBody.decodeTypeMap(mp);
        int typeIndex = typeMap.length;


        // Print the locals
        while (localCount-- > 0) {
            Address lp = fp.sub(localCount * BYTES_PER_WORD);
            String name = (lp == fp ? "MP/local0" : "local" + localCount);
            printLocalOrParameterLine(lp, typeMap[--typeIndex], name);
        }

        // Print the return FP and return IP
        printPointerLine(fp.add(FP.returnFP * BYTES_PER_WORD), "returnFP", null);
        printPointerLine(fp.add(FP.returnIP * BYTES_PER_WORD), "returnIP", null);

        // Print the parameters
        int offset = FP.parm0;
        typeIndex = 0;
        while (parameterCount-- > 0) {
            Address lp = fp.add(offset * BYTES_PER_WORD);
            printLocalOrParameterLine(lp, typeMap[typeIndex], "parm" + typeIndex);
            offset++;
            typeIndex++;
        }
    }

    /*---------------------------------------------------------------------------*\
     *                        Method body decoding                               *
    \*---------------------------------------------------------------------------*/

    /**
     * Decode a method.
     *
     * @param block the address before the header of the object
     * @return the address of the next object header
     */
    private Address decodeMethod(Address block) {
        int headerLength = (int)GC.decodeLengthWord(Unsafe.getUWord(block, 0));
        Address oop = block.add(headerLength * BYTES_PER_WORD);
        Klass klass = GC.getKlass(oop);
        int length = GC.getArrayLengthNoCheck(oop);
        printMethodHeader(block, oop);
        MapBytecodeTracer tracer = new MapBytecodeTracer(oop, length);
        for (int i = 0 ; i < length ;) {
            BytecodeTracerEntry entry = tracer.trace();
            String text = entry.getText();
            printLine(entry.getAddress(), entry.getSize(), text);
            i += entry.getSize();
            bytecodes++;
        }

        Address nextBlock = oop.add(length).roundUpToWord();
        updateHistogramFor(klass.getName(), nextBlock.diff(block).toInt());
        return nextBlock;
    }

    /*-----------------------------------------------------------------------*\
     *                           Methods                                     *
    \*-----------------------------------------------------------------------*/

    /**
     * The map from method addresses to method signatures.
     */
    private HashMap methodMap = new HashMap();

    /**
     * Registers the signature of all the methods defined by a given class.
     *
     * @param klass Klass
     * @param methods Object
     * @param isStatic boolean
     */
    private void registerMethods(Klass klass, Object methods, boolean isStatic) {
        if (methods != Address.zero()) {
            int length = GC.getArrayLengthNoCheck(methods);
            int count = klass.getMethodCount(isStatic);
            for (int i = 0; i != count; ++i) {
                Method method = klass.getMethod(i, isStatic);
                int offset = method.getOffset();
                String signature = method.toString();
                Assert.that(offset < length);
                Object methodBody = Unsafe.getObject(methods, offset);
                if (methodBody != Address.zero()) {
                    this.methodMap.put(methodBody, signature);
                }
            }
        }
    }

    /**
     * Gets the signature of a method based on a given address.
     *
     * @param addr   an address
     * @return the signature of the method at <code>addr</code> or null if there is no method there
     */
    private String getMethodSignature(Address addr) {
        return (String)methodMap.get(addr);
    }

    /*---------------------------------------------------------------------------*\
     *                                  main                                     *
    \*---------------------------------------------------------------------------*/

    public static void main(String[] args) throws IOException {
        ClasspathMapper mapper = new ClasspathMapper();
        if (mapper.parseArgs(args)) {
            mapper.run();
        }
    }
}
