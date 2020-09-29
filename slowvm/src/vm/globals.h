/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */

#define MAX_STREAMS 4

/**
 * This struct encapsulates all the globals in the Squawk VM. This simplifies
 * (re)initialization of this state when the Squawk VM is being used as a
 * shared library from a Java based launcher.
 */
struct {
    Address     memory;                     /* The buffer containing ROM, NVM, RAM and serviceChunk */
    Address     memoryEnd;                  /* The end of the memory buffer. */
    UWord       memorySize;                 /* The size (in bytes) of the memory buffer. */

#ifndef MACROIZE
    int          iparm;                     /* The immediate operand value of the current bytecode. */
    ByteAddress  ip;                        /* The instruction pointer. */
    UWordAddress fp;                        /* The frame pointer. */
    UWordAddress sp;                        /* The stack pointer. */
#else
#ifdef TRACE
    ByteAddress  lastIP;
    UWordAddress lastFP;
#endif
#endif

    UWordAddress sl;                        /* The stack limit. */
    UWordAddress ss;                        /* The stack start. */
    int          bc;                        /* The branch counter. */

    int         Ints[GLOBAL_INT_COUNT];     /* Storage for the primitive typed Java globals. */
    Address     Addrs[GLOBAL_ADDR_COUNT];   /* Storage for the primitive typed Java globals. */
    Address     Oops[GLOBAL_OOP_COUNT];     /* Storage for the reference typed Java globals. */
    Address     Buffers[MAX_BUFFERS];       /* Buffers that are allocated by native code. */
    int         BufferCount;                /* Number of buffers that are currently allocated by native code. */
    JNIEnv     *JNI_env;                    /* The pointer to the table of JNI function pointers. */
    boolean     isCalledFromJava;           /* Flags whether or not Squawk was launched via a call from Java. */
    jmp_buf     vmStartScope;               /* The frame in which the Squawk VM was started from Java. */
    JavaVM     *jvm;                        /* Handle to the JVM created via the Invocation API. This will be null if Squawk was called from Java code. */
    FILE       *streams[MAX_STREAMS];       /* The file streams to which the VM printing directives sent. */
    int         currentStream;              /* The currently selected stream */
    ujlong      lastTime;                   /* Time for INTERNAL_GETTIME_LOW */
    jclass      channelIO_clazz;            /* JNI handle to com.sun.squawk.vm.ChannelIO. */
    jmethodID   channelIO_execute;          /* JNI handle to com.sun.squawk.vm.ChannelIO.execute(...) */

#ifdef IOPORT
    char       *ioport;                     /* The [host and] port number of the optional I/O server. */
    int         iosocket;                   /* The socket number of the optional I/O server. */
    int         result_low;                 /* The low 32 bits of the last result */
    int         result_high;                /* The high 32 bits of the last result */
    jlong       io_ops_time;
    int         io_ops_count;
#endif

    FILE       *traceFile;                  /* The trace file name */
    boolean     traceFileOpen;              /* Specifies if the trace file has been opened. */
    int         traceLastThreadID;          /* Specifies the thread ID at the last call to trace() */

#ifdef PROFILING
    int         sampleFrequency;            /* The profile sample frequency */
    jlong       instructionCount;
#endif

#ifdef TRACE
    int         totol_extends;              /* Total number of extends */
    int         totol_slots;                /* Total number of slots cleared */
#endif

    int         statsFrequency;             /* The statistics output frequency */

    Address     cachedClassState[CLASS_CACHE_SIZE > 0 ? CLASS_CACHE_SIZE : 1];
    Address     cachedClass     [CLASS_CACHE_SIZE > 0 ? CLASS_CACHE_SIZE : 1];
    int         cachedClassAccesses;
    int         cachedClassHits;

    Address    *pendingMonitors;
    int         pendingMonitorStackPointer;
    int         pendingMonitorAccesses;
    int         pendingMonitorHits;

    jlong       lastStatCount;
    boolean     notrap;

} Globals;

#define memory                              Globals.memory
#define memoryEnd                           Globals.memoryEnd
#define memorySize                          Globals.memorySize

#ifndef MACROIZE
#define iparm                               Globals.iparm
#define ip                                  Globals.ip
#define fp                                  Globals.fp
#define sp                                  Globals.sp
#else
#ifdef TRACE
#define lastIP                              Globals.lastIP
#define lastFP                              Globals.lastFP
#endif /* TRACE */
#endif /* MACROIZE */

#define sl                                  Globals.sl
#define ss                                  Globals.ss
#define bc                                  Globals.bc
#define lastTime                            Globals.lastTime
#define Ints                                Globals.Ints
#define Addrs                               Globals.Addrs
#define Oops                                Globals.Oops
#define Buffers                             Globals.Buffers
#define BufferCount                         Globals.BufferCount
#define JNI_env                             Globals.JNI_env
#define isCalledFromJava                    Globals.isCalledFromJava
#define vmStartScope                        Globals.vmStartScope
#define traceFile                           Globals.traceFile
#define traceFileOpen                       Globals.traceFileOpen
#define traceLastThreadID                   Globals.traceLastThreadID
#define jvm                                 Globals.jvm

#ifdef IOPORT
#define ioport                              Globals.ioport
#define iosocket                            Globals.iosocket
#define result_low                          Globals.result_low
#define result_high                         Globals.result_high
#define io_ops_time                         Globals.io_ops_time
#define io_ops_count                        Globals.io_ops_count
#endif

#define cachedClassState                    Globals.cachedClassState
#define cachedClass                         Globals.cachedClass
#define cachedClassAccesses                 Globals.cachedClassAccesses
#define cachedClassHits                     Globals.cachedClassHits

#define pendingMonitors                     Globals.pendingMonitors
#define pendingMonitorStackPointer          Globals.pendingMonitorStackPointer
#define pendingMonitorAccesses              Globals.pendingMonitorAccesses
#define pendingMonitorHits                  Globals.pendingMonitorHits

#define streams                             Globals.streams
#define currentStream                       Globals.currentStream

#define channelIO_clazz                     Globals.channelIO_clazz
#define channelIO_execute                   Globals.channelIO_execute

#define STREAM_COUNT                        (sizeof(Streams) / sizeof(FILE*))

#ifdef TRACE
#define setLongCounter(high, low, x)        { high = (int)(x >> 32); low = (int)(x);}
#define getLongCounter(high, low)           ((((ujlong)(unsigned)high) << 32) | ((unsigned)low))
#define getBranchCount()                    getLongCounter(branchCountHigh, branchCountLow)
#define getTraceStart()                     getLongCounter(traceStartHigh, traceStartLow)
#define getTraceEnd()                       getLongCounter(traceEndHigh, traceEndLow)
#define setTraceStart(x)                    setLongCounter(traceStartHigh, traceStartLow, (x)); if ((x) == 0) { tracing = true; }
#define setTraceEnd(x)                      setLongCounter(traceEndHigh, traceEndLow, (x))
#define statsFrequency                      Globals.statsFrequency
#else
#define getBranchCount()                    ((jlong)-1L)
#endif

#ifdef PROFILING
#define sampleFrequency                     Globals.sampleFrequency
#define instructionCount                    Globals.instructionCount
#endif

#ifdef TRACE
#define totol_extends                       Globals.totol_extends
#define totol_slots                         Globals.totol_slots
#endif

#define lastStatCount                       Globals.lastStatCount
#define notrap                              Globals.notrap



/**
 * Initialize/re-initialize the globals to their defaults.
 */
int initializeGlobals() {
    memset(&Globals, 0, sizeof(Globals));

    /*
     * Initialize the variables that have non-zero defaults.
     */
    java_lang_VM_romFileName = "squawk.suite";
    java_lang_VM_extendsEnabled = true;
    runningOnServiceThread = true;
    pendingMonitors = &Oops[ROM_GLOBAL_OOP_COUNT];

    streams[java_lang_VM_STREAM_STDOUT] = stdout;
    streams[java_lang_VM_STREAM_STDERR] = stderr;
    currentStream = java_lang_VM_STREAM_STDERR;

#ifdef TRACE
    setTraceStart(TRACESTART);
    setTraceEnd(TRACESTART);
    traceLastThreadID = -2;
#endif /* TRACE */

#ifdef IOPORT
    ioport = null;
    iosocket = -1;
#endif

    return 0;
}

/**
 * Prints the name and current value of all the globals.
 */
void printGlobals() {
    FILE *vmOut = streams[currentStream];
#ifdef TRACE
    int i;

    // Print the global integers
    fprintf(vmOut, "Global ints:\n");
    for (i = 0; i != GLOBAL_INT_COUNT; ++i) {
        fprintf(vmOut, "  %s = %d\n", getGlobalIntName(i), Ints[i]);
    }

    // Print the global oops
    fprintf(vmOut, "Global oops:\n");
    for (i = 0; i != GLOBAL_OOP_COUNT; ++i) {
        fprintf(vmOut, format("  %s = %A\n"), getGlobalOopName(i), Oops[i]);
    }

    // Print the global addresses
    fprintf(vmOut, "Global addresses:\n");
    for (i = 0; i != GLOBAL_ADDR_COUNT; ++i) {
        fprintf(vmOut, format("  %s = %A\n"), getGlobalAddrName(i), Addrs[i]);
    }
#else
    fprintf(vmOut, "printGlobals() requires tracing\n");
#endif
}

/**
 * Closes all the open files used for VM printing.
 */
void finalizeStreams() {
    int i;
    for (i = 0 ; i < MAX_STREAMS ; i++) {
        FILE *file = streams[i];
        if (file != null) {
            fflush(file);
            if (file != stdout && file != stderr) {
                fclose(file);
            }
        }
    }
}

//#define BAD_VALUE 1234

/**
 * Check to see if a specific address was written to and print it if it was.
 *
 * @param ea    the address of the last write to memory
 * @param size  the number of bytes written
 * @param addr  the address to check for
 */
void checkOneAddress(Address ea, int size, Address addr) {
    ByteAddress start  = (ByteAddress)ea;
    ByteAddress end    = start + size;
    ByteAddress target = (ByteAddress)addr;
    if (target >= start && target < end) {
        UWord value = ((UWord *)target)[0];
        fprintf(stderr, format("*******************  [%A] = %W\n"), target, value);
#ifdef BAD_VALUE
        if (value == BAD_VALUE) {
            fprintf(stderr, format("Stopping because bad value %W written in the range [%A .. %A)\n"), value, start, end);
            stopVM(-1);
        }
#endif
    }
}

///**
// * Checks an address
// *
// * @param ea   the address of the last write to memory
// */
//#define checkAddress(ea) {                                  \
//    assume(java_lang_GC_collecting ||                       \
//           cheneyStartMemoryProtect == 0 ||                 \
//           lo(ea, cheneyStartMemoryProtect) ||              \
//           hieq(ea, cheneyEndMemoryProtect));               \
//}

/**
 * Performs a number of checks on a given part of memory immediately after
 * it was written to.
 *
 * @param ea   the address of the last write to memory
 * @param size the number of bytes written
 */
#define checkPostWrite(ea, size) {                          \
    /* checkOneAddress(ea, size, (Address)417661756);*/     \
    assume(cheneyStartMemoryProtect == 0 ||                 \
           lo(ea, cheneyStartMemoryProtect) ||              \
           hieq(ea, cheneyEndMemoryProtect));               \
}

