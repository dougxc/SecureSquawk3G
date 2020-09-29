/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */

// Size of class to class state cache.
#define CLASS_CACHE_SIZE 6

// The number of pending monitors.
#define MONITOR_CACHE_SIZE 6

#include "platform.h"
#include "buildflags.h"
#include "os.c"

#ifdef PROFILING
#ifndef TRACE
#define TRACE true
#endif
#endif

#ifndef IODOTC
#define IODOTC "io.c"
#endif

#if defined(TYPEMAP) && TYPEMAP != 0
#undef TYPEMAP
#define TYPEMAP true
#else
#define TYPEMAP false
#endif

/*
 * Include the type definitions and operations on machine word sized quantities
 */
#include "address.c"

/*
 * Forward refs.
 */
void printCacheStats();
#ifdef TRACE
boolean openTraceFile();
void printStackTrace(const char* msg);
void printStackTraceOpcode(int code);
void printProfileStackTrace(ByteAddress traceIP, UWordAddress traceFP, int lastOpcode);
void printStackTracePrim(int opcode, ByteAddress traceIP, UWordAddress traceFP, const char* msg, const char* mnemonic);
#else
#define openTraceFile() true
#define printStackTrace(msg)
#define printStackTraceOpcode(code)
#define printProfileStackTrace( traceIP, traceFP, lastOpcode)
#define printStackTracePrim(opcode, traceIP, traceFP, msg, mnemonic)
#endif /* TRACE */

/*
    This is the 'slow' Squawk VM that is implemented in C. The techniques used in
    this VM are such that it should be expected that it will be several times slower
    than other VMs implemented in C. Its purpose is only to have an initial Squawk3G
    implementation running quickly, and to provide a reference implementation
    to test the final system against.

    VM has five virtual machine registers.

      ip - the instruction pointer.
      fp - the frame pointer
      sp - the stack pointer
      sl - the stack limit
      bc - the branch counter

    The stack goes downwards, and activation records have the following format:

    High:
             -------------------------------
            |              P3               |
             -------------------------------
            |              P2               |
             -------------------------------
            |              P1               |
             -------------------------------
            |              P0               |
             -------------------------------
            |           Return IP           |
             -------------------------------
            |           Return FP           |
             -------------------------------
    fp ->   |         Current method        |
             -------------------------------
            |              L0               |
             -------------------------------
            |              L1               |
             -------------------------------
            |              ...              |
             -------------------------------
    sp ->   |              LN               |
             -------------------------------
            |                               |
            :        Evaluation stack       :
            :                               :
            |                               |
             -------------------------------
    Low:

*/

/*
 * Include the romized image.
 */
#include "rom.h"

/*
 * Add the monitor cache size to the global oop count
 */
#define GLOBAL_OOP_COUNT (ROM_GLOBAL_OOP_COUNT + MONITOR_CACHE_SIZE)
#define GLOBAL_INT_COUNT  ROM_GLOBAL_INT_COUNT
#define GLOBAL_ADDR_COUNT ROM_GLOBAL_ADDR_COUNT

/**
 * The default GC chunk, NVM and RAM sizes.
 */
#ifndef SERVICE_CHUNK_SIZE
#ifdef java_lang_GC_VIRTUAL_STACK_SIZE
#define SERVICE_CHUNK_SIZE java_lang_GC_VIRTUAL_STACK_SIZE
#else
#define SERVICE_CHUNK_SIZE (24*1024)
#endif
#endif
#define TWOWORDS (HDR_BYTES_PER_WORD * 2)
#define SERVICE_CHUNK_SIZE_MINUS2WORDS (SERVICE_CHUNK_SIZE - TWOWORDS)
#define DEFAULT_RAM_SIZE   (8*1024*1024)
#define DEFAULT_NVM_SIZE   (8*1024*1024)
#define TIMEQUANTA 1000
#define MAX_BUFFERS 10
#define MAX_JVM_ARGS 20

/**
 * The tracing limits.
 */
const jlong MAX_UJLONG = (jlong)0x7fffffff << 32 | 0xffffffff;
#ifndef TRACESTART
#define TRACESTART MAX_UJLONG
#endif

/**
 * Forward definition of printf/fprintf helper function.
 */
const char *format(const char* fmt);

/*
 * Forward declaration of VM exit routine.
 */
void stopVM(int);

/*
 * Include all the globals.
 */
#include "globals.h"

/*
 * Setup ss and sl.
 */
#define setStack(newss) {                                \
    ss = (UWordAddress)newss;                            \
    sl = ss + SC_limit;                                  \
}                                                        \

/*
 * Test to see if the interpreter is running on the GC stack
 */
#define usingServiceStack() (ss == (UWordAddress)java_lang_Thread_serviceStack)

/*
 * Include utility functions.
 */
#include "util.h"

/*
 * Size of buffer used for pre-formatting printf/fprintf format specifications.
 */
#define FORMAT_BUF_LEN 1000

#ifdef _MSC_VER
#    define FORMAT_64  buf[bufPos++] = 'I'; buf[bufPos++] = '6'; buf[bufPos++] = '4';
#else
#    define FORMAT_64  buf[bufPos++] = 'l'; buf[bufPos++] = 'l';
#endif /* _MSC_VER */

/**
 * Preformats a format specification string given to 'printf' or 'fprintf'
 * so that the platform dependent part of printing of Offset/Word/Address/long
 * values is encapsulated here.
 *
 * This preformatter transforms the following format specifications:
 *
 *  '%A'  - formats the corresponding argument as an unsigned 32 or 64 bit value
 *  '%W'  - formats the corresponding argument as a signed 32 or 64 bit value
 *  '%L'  - formats the corresponding argument as a signed 64 bit value
 *  '%U'  - formats the corresponding argument as an unsigned 64 bit value
 *  '%D'  - formats the corresponding argument as a 64 bit floating point value
 *
 * @param  fmt  the format specification to be pre-formatted
 * @return the transformed version of 'fmt'
 */
const char *format(const char* fmt) {
    static char buf[FORMAT_BUF_LEN];
    int fmtPos = 0;
    int bufPos = 0;
    int fmtLen = strlen(fmt);

    while (fmtPos < fmtLen) {
        assume(bufPos < 1000);

        if (fmt[fmtPos] != '%') {
            buf[bufPos++] = fmt[fmtPos++];
        } else {
            fmtPos++;
            buf[bufPos++] = '%';
            switch (fmt[fmtPos++]) {
                case 'A': {
#if SQUAWK_64
                    FORMAT_64
#endif /* SQUAWK_64 */
                    buf[bufPos++] = 'u';
                    break;
                }
                case 'W': {
#if SQUAWK_64
                    FORMAT_64
#endif /* SQUAWK_64 */
                    buf[bufPos++] = 'd';
                    break;
                }
                case 'L': {
                    FORMAT_64
                    buf[bufPos++] = 'd';
                    break;
                }
                case 'U': {
                    FORMAT_64
                    buf[bufPos++] = 'u';
                    break;
                }
                case 'D': {
                    FORMAT_64
                    buf[bufPos++] = 'f';
                    break;
                }
                default: {
                    buf[bufPos++] = fmt[fmtPos - 1];
                }
            }
        }

    }
    buf[bufPos++] = 0;
    return buf;
}

/*
 * Define a few extra things.
 */
enum {EQ, NE, LT, GT, LE, GE};
#define USHORT CHAR
#define OOP java_lang_Object
typedef int Type;

/**
 * Allocate a word aligned byte buffer from the C memory and zero its contents.
 * The call will exit the system if it could not allocate the buffer.
 *
 * NOTE; The current implementation actually allocates on a page boundary and buffer
 *       the size of the buffer allocated in a multiple of the system page size. This
 *       enables these buffers to be set as read-only using the system specific
 *       memory protection mechanism.
 *
 * @param size        the size (in bytes) to allocate.
 * @param desc        a description of what the buffer is for
 * @param fatalIfFail if true and the buffer cannot be allocated then cause a VM error
 * @return a word aligned buffer of the given size
 */
void *newBuffer(UWord size, const char *desc, boolean fatalIfFail) {

    void *buffer;

    /*
     * Adjust size so that it is a multiple of the page size of the machine
     */
    int pageSize = getSystemPageSize();
    int actualSize = (size + (pageSize - 1)) & ~(pageSize - 1);

#ifdef _MSC_VER
    buffer = VirtualAlloc(0, actualSize, MEM_RESERVE|MEM_COMMIT, PAGE_READWRITE);
#else
#ifdef sun
    buffer = malloc(actualSize);
#endif
    buffer = valloc(actualSize);
#endif /* _MSC_VER */
//fprintf(stderr, format("newBuffer: desc=%s, size=%W, actualSize=%W buffer=%A\n"), desc, size, actualSize, buffer);
    /*
     * Ensure that the resulting buffer is word aligned (which is surely guaranteed
     * if it is page aligned!)
     */
    assume(isWordAligned((UWord)buffer));


    if (buffer == null) {
        if (fatalIfFail) {
            printf(format("Failed to allocate buffer of %W bytes for %s\n"), size, desc);
            stopVM(1);
        } else {
            return null;
        }
    }

    /*
     * Zero the bytes in the buffer.
     */
    memset(buffer, 0, (int)size);

    /*
     * Register the buffer.
     */
    if (BufferCount >= MAX_BUFFERS) {
        fatalVMError("exceeded MAX_BUFFERS allocations");
    }
    Buffers[BufferCount++] = buffer;

    return buffer;
}

/**
 * Free a given buffer that was allocated by 'newBuffer' and remove
 * it from the table of allocated buffers.
 */
void freeBuffer(Address buffer) {
    int i = 0;
    int insert = 0;

    while (i != BufferCount) {
        if (Buffers[i] == buffer) {
//fprintf(stderr, format("freeBuffer: buffer=%A\n"), buffer);
#ifdef _MSC_VER
            VirtualFree(buffer, 0, MEM_RELEASE);
#else
            free(buffer);
#endif /* _MSC_VER */
        } else {
            Buffers[insert] = Buffers[i];
            insert++;
        }
        i++;
    }

    if (BufferCount != insert + 1) {
        fatalVMError("buffer not in Buffers exactly once");
    }
    BufferCount = insert;
}

/**
 * Free all the buffers that were allocated by 'newBuffer'.
 */
void freeBuffers() {
    while (BufferCount != 0) {
        freeBuffer(Buffers[0]);
    }
}

/*
 * Include the memory interface.
 */
#include "memory.c"

/*
 * Include the bitmap used by the Lisp2 collector.
 */
#ifdef WRITE_BARRIER
#include "lisp2.c"
#endif /* WRITE_BARRIER */

/*
 * Include the switch and bytecode routines.
 */
#include "bytecodes.c"

/*
 * Include the I/O system
 */
#include "cio.c"

/*
 * Include the instruction trace routine.
 */
#include "trace.c"

#ifdef DB_DEBUG
/*
 * Include support for low-level interactive debug
 */
#include "debug.c"
#endif

/**
 * Parse a string that specifies a quantity.
 *
 * @param p  the string to parse
 */
jlong parseQuantityLong(char *p, const char* arg) {
    jlong val = 0;
    for (;;) {
        int ch = *p++;
        if (ch == 0) {
            break;
        } else if (ch >= '0' && ch <= '9') {
            val = (val * 10) + (ch - '0');
        } else if (ch == 'K' || ch == 'k') {
            val *= 1024;
            break;
        } else if (ch == 'M' || ch == 'm') {
            val *= (1024*1024);
            break;
        } else {
            printf("Badly formatted quantity for '%s' option\n", arg);
            stopVM(-1);
        }
    }
    return val;
}


/**
 * Parse a string that specifies a quantity.
 *
 * @param p  the string to parse
 */
int parseQuantity(char *p, const char* arg) {
    jlong res = parseQuantityLong(p, arg);
    if (res != (int)res) {
        printf("parseQuantity overflow for '%s' option\n", arg);
        stopVM(-1);
    }
    return (int)res;
}

/**
 * Gets the size of a file.
 *
 * @param file     the file to inspect
 * @return the size of the file or -1 if it doesn't exist
 */
Offset getFileSize(const char *file) {
    struct stat buf;
    if (stat(file, &buf) == 0) {
        return buf.st_size;
    } else {
        if (errno != ENOENT) {
             printf("Call to stat(%s) failed: %s\n", file, strerror(errno));
             stopVM(-1);
        }
        return -1;
    }
}

/**
 * Loads the contents of a file into a buffer.
 *
 * @param file     the file from which to load
 * @param buffer   the buffer into which the file should be loaded
 * @param size     the size of the buffer
 * @return the size of the file or -1 if 'file' does not exist. Any other errors cause
 *                 the system to exit.
 */
int readFile(const char *file, Address buffer, UWord size) {
    struct stat buf;
    int result = -1;
    if (stat(file, &buf) == 0) {
        int fd = open(file, O_RDONLY|O_BINARY);
        result = buf.st_size;
        if (fd != -1) {
            int count   = 0;
            int toRead  = buf.st_size;
            char *readPos = (char *)buffer;
            while (toRead > 0) {
                count = read(fd, readPos, toRead);
                if (count == -1) {
                    printf(format("Call to read() failed: %s (file size=%W, read=%d)\n"), strerror(errno), buf.st_size, buf.st_size-toRead);
                    stopVM(-1);
                }
                toRead -= count;
                readPos += count;
            }
            close(fd);
        } else {
             printf("Call to open(%s) failed: %s\n", file, strerror(errno));
             stopVM(-1);
        }
    } else {
        if (errno != ENOENT) {
             printf("Call to stat(%s) failed: %s\n", file, strerror(errno));
             stopVM(-1);
        }
    }
    return result;
}

/*
 * Include the functions for laoding and saving the bootstrap suite
 */
#include "suite.c"

/**
 * Print cache stats.
 */
void printCacheStats() {
    double average;
    jlong count = 0;
    fprintf(stderr, "----------------------------------");
#ifdef PROFILING
    count = instructionCount-lastStatCount;
    fprintf(stderr, format(" %4.2f M Instructions "), (double)count/1000000);
    lastStatCount = instructionCount;
#else
#ifdef TRACE
    count = getBranchCount()-lastStatCount;
    fprintf(stderr, format(" %4.2f M Branches "), (double)count/1000000);
    lastStatCount = getBranchCount();
#endif
#endif
    fprintf(stderr, "----------------------------------");

    if (count > 0) {
        fprintf(stderr, "\nTotals - ");
        fprintf(stderr, " Class:%6.2f%%",   (((double)cachedClassAccesses)/count)*100);
        fprintf(stderr, " Monitor:%6.2f%%", (((double)pendingMonitorAccesses)/count)*100);
        fprintf(stderr, " Exit:%6.2f%%",    (((double)java_lang_GC_monitorExitCount)/count)*100);
        fprintf(stderr, " New:%6.2f%%",    (((double)newCount)/count)*100);
    }

    fprintf(stderr, "\nHits   - ");
    average = (cachedClassAccesses == 0 ? 0 : ((double)cachedClassHits) / cachedClassAccesses);
    fprintf(stderr, format(" Class:%6.2f%%"), average*100);
    cachedClassHits = cachedClassAccesses = 0;
    average = (pendingMonitorAccesses == 0 ? 0 : ((double)pendingMonitorHits) / pendingMonitorAccesses);
    fprintf(stderr, format(" Monitor:%6.2f%%"), average*100);
    pendingMonitorHits = pendingMonitorAccesses = 0;
    average = (java_lang_GC_monitorExitCount == 0 ? 0 : ((double)java_lang_GC_monitorReleaseCount) / java_lang_GC_monitorExitCount);
    fprintf(stderr, format(" Exit:%6.2f%%"), average*100);
    java_lang_GC_monitorExitCount = java_lang_GC_monitorReleaseCount = 0;
    average = (newCount == 0 ? 0 : ((double)newHits) / newCount);
    fprintf(stderr, format(" New:%6.2f%%"), average*100);
    newCount = newHits = 0;
    fprintf(stderr, "\n");
#ifdef TRACE
    fprintf(stderr, format("Extends: %d slots/extend %f\n"), totol_extends, ((double)totol_slots)/totol_extends);
#endif
    fprintf(stderr, format("GCs: %d full %d partial\n"), java_lang_GC_fullCollectionCount, java_lang_GC_partialCollectionCount);

}

/**
 * Stops the VM running.
 *
 * @param exitCode the exit code
 */
void stopVM(int exitCode) {
    finalizeStreams();
    freeBuffers();
    fprintf(stderr, "\n\n");
    printCacheStats();
    fprintf(stderr, "** VM stopped");
#ifdef PROFILING
    fprintf(stderr, format(" after %L instructions"), instructionCount);
#endif
#ifdef TRACE
    fprintf(stderr, format(" after %L branches"), getBranchCount());
#endif
    fprintf(stderr, format(": exit code = %d ** "), exitCode);
#ifdef IOPORT
    if (ioport != null) {
        jlong average = (io_ops_count == 0 ? 0 : io_ops_time / io_ops_count);
        fprintf(stderr, format(" (average time for %d I/O operation: %L ms)"), io_ops_count, average);
    }
#endif
    fprintf(stderr, "\n");
    fflush(stderr);

#ifdef DB_DEBUG
    db_vm_exiting();
#endif

#ifdef _MSC_VER
    if (notrap) {
        _asm{ int 3 };
    }
#endif

    if (isCalledFromJava) {
        longjmp(vmStartScope, exitCode);
    } else {
        osfinish();
        exit(exitCode);
    }

}

/**
 * Shows the usage message for passing flags to the embedded JVM.
 */
void jvmUsage() {
    printf("    -J<flag>       pass <flag> to the embedded Java VM. Some common usages include:\n");
    printf("                       -J-Xdebug -J-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=9999\n");
    printf("                                [enables debugging of embedded JVM]\n");
    printf("                       -J-Dcio.tracing=true [enables tracing in embedded JVM]\n");
    printf("                       -J-Dcio.logging=true [enables logging in embedded JVM]\n");
}

/**
 * Shows the usage message.
 */
void usage() {

    boolean isLaunchedViaJNI = (JNI_env != null);

    printf("    -Xmx:<size>    set Squawk RAM size (default=%dKb)\n", DEFAULT_RAM_SIZE/1024);
    printf("    -Xmxnvm:<size> set Squawk NVM size (default=%dKb)\n", DEFAULT_NVM_SIZE/1024);
    printf("    -Xboot:<file>  load bootstrap suite from file (default=squawk.suite)\n");
    printf("    -Xtgc:<n>      set GC trace flags where 'n' is the sum of:\n");
    printf("                     1: minimal trace info of mem config and GC events\n");
    printf("                     2: trace allocations\n");
    printf("                     4: detailed trace of garbage collector\n");
    printf("                     8: detailed trace of object graph copying\n");
    printf("                    16: trace of heap layout at GC\n");
    printf("    -Xtgca:<n>     start GC tracing at the 'n'th collection (default=0)\n");
#ifdef TRACE
    printf("    -Xts:<n>       start tracing after 'n' backward branches\n");
    printf("    -Xte:<n>       stop tracing after 'n' backward branches\n");
    printf("    -Xtr:<n>       trace 5000 instructions after 'n' backward branches\n");
    printf("    -Xterr         trace to standard error output stream\n");
    printf("    -Xstats:<n>    dump a cache stats every 'n' backward branches\n");

#endif
#ifdef PROFILING
#ifdef OSPROF
    printf("    -Xprof:<n>     take profile sample every 'n' milliseconds\n");
#else
    printf("    -Xprof:<n>     take profile sample every 'n' instructions\n");
#endif /* OSPROF */
#endif /* PROFILING */
#ifdef IOPORT
    printf("    -Xioport:[host:]port  connect to an I/O server via a socket\n");
#endif
    printf("    -Xnotrap       don't trap VM crashes\n");
    if (!isLaunchedViaJNI) {
        jvmUsage();
    }
}

/**
 * Determines if the contents of a given string start with a given prefix.
 *
 * @param  line    the (null terminated) string to check
 * @param  prefix  the (null terminated) prefix to check against
 * @return true if line starts with prefix
 */
boolean startsWith(char *line, char *prefix) {
    int i;
    for (i = 0 ;; i++) {
        char ch = line[i];
        int prefixCh = prefix[i];
        if (prefixCh == 0) {
            return true;
        }
        if (ch == 0) {
            return false;
        }
        if (prefixCh != ch) {
            return false;
        }
    }
}

/**
 * Determines if the length and contents of two given strings are equals.
 *
 * @param  s1    the first (null terminated) string to check
 * @param  s2    the second (null terminated) string to check
 * @return true if s1 and s2 are the length and have matching contents
 */
boolean equals(char *s1, char *s2) {
    int i;
    for (i = 0 ;; i++) {
        int ch = s1[i];
        if (ch == 0) {
            return s2[i] == 0;
        }
        if (s2[i] != ch) {
            return false;
        }
    }
}

/**
 * Calculates the space required to make a deep copy of an array of C strings.
 *
 * @param  length   the number of elements in the array
 * @param  array    the array
 * @return the size (in bytes) required for a deep copy of the array
 */
int calculateSizeForCopyOfCStringArray(int length, char **array) {
    int total = length * sizeof(char *);
    while(length-- > 0) {
        total += strlen(array[length]) + 1;
    }
    return total;
}

/**
 * Writes a C string (i.e. a null-terminated char array) into the VM's memory buffer.
 *
 * @param  string  the C string to write
 * @param  offset  the offset in the VM's memory buffer at which to write
 * @return the offset one past the last byte written to the VM's memory buffer
 */
int writeCString(char *string, int offset) {
    while (*string != 0) {
        setByte(memory, offset++, *string++);
    }
    setByte(memory, offset++, 0); // write null-terminator
    return offset;
}

/**
 * Writes an array of C strings into a given buffer. The provided
 * buffer must be within the type checked memory buffer.
 *
 * @param  length   the number of elements in the array
 * @param  array    the array
 * @param  offset   the offset in the VM's memory buffer at which to write
 * @return the offset one past the last byte written to the VM's memory buffer
 */
int writeCStringArray(int length, char **array, int offset) {
    char **arrayCopy = (char **)Address_add(memory, offset);

    // Find the end of the array copy which is where the elements will be copied
    offset += length * sizeof(Address);
    while (length-- > 0) {
        setObject(arrayCopy++, 0, Address_add(memory, offset));
        offset = writeCString(*array++, offset);
    }
    return offset;
}

/**
 * Sets up the memory buffer.
 *
 * @param ramSize   the size (in bytes) requested for RAM
 * @param nvmSize   the size (in bytes) requested for NVM
 * @param argv      the command line options after the -X and -J options have been stripped
 * @param argc      the number of components in argv
 */
Address setupMemory(int ramSize, int nvmSize, int argc, char *argv[]) {

    int pageSize = getSystemPageSize();

    int serviceChunkSize = SERVICE_CHUNK_SIZE;
    int realMemorySize;
    int romSize;
    char *romFileName = java_lang_VM_romFileName;
    int romFileNameSize = strlen(romFileName) + 1;
    Address suite;
    int argvTotalSize = calculateSizeForCopyOfCStringArray(argc, argv);
    int offset;

#ifdef FLASH_MEMORY
    memorySize = 0;
    assume(!TYPEMAP);
#else
    memorySize = roundUp(getFileSize(romFileName), pageSize);
#endif
    memorySize = roundUp(memorySize + ramSize, pageSize);
    memorySize = roundUp(memorySize + nvmSize, pageSize);
    memorySize = roundUp(memorySize + serviceChunkSize, pageSize);
    memorySize = roundUp(memorySize + argvTotalSize + romFileNameSize, pageSize);

     // Double the memory buffer to allocate the type map if necessary
    realMemorySize = TYPEMAP ? memorySize * 2 : memorySize;

    // Allocate the meory buffer
    memory = newBuffer(realMemorySize, "memory", true);
    memoryEnd = Address_add(memory, memorySize);
#ifdef FLASH_MEMORY
    romSize = loadBootstrapSuiteFromFlash(&java_lang_VM_romStart, &suite, &java_lang_VM_romHash);
#else
    // ROM start at the beggining of the VM's memory buffer
    java_lang_VM_romStart = memory;
    romSize = loadBootstrapSuite(java_lang_VM_romFileName, memory, memorySize, &suite, &java_lang_VM_romHash);
#endif
    java_lang_VM_romEnd = Address_add(java_lang_VM_romStart, romSize);

#ifdef FLASH_MEMORY
    // NVM starts at the beginning of the memory buffer
    java_lang_GC_nvmStart = (Address)roundUp((UWord)memory, pageSize);
#else
    // NVM starts on the next page after the end of ROM
    java_lang_GC_nvmStart = (Address)roundUp((UWord)java_lang_VM_romEnd, pageSize);
#endif
    java_lang_GC_nvmEnd = Address_add(java_lang_GC_nvmStart, nvmSize);
    java_lang_GC_nvmAllocationPointer = java_lang_GC_nvmStart;

    // RAM starts on the next page after the end of NVM
    java_lang_GC_ramStart = (Address)roundUp((UWord)java_lang_GC_nvmEnd, pageSize);
    java_lang_GC_ramEnd = Address_add(java_lang_GC_ramStart, ramSize);

    // The stack for the service thread starts on the next page after the end of RAM.
    // The length of the stack in logical slots is written into the first word of the
    // block allocate for the stack. This length is later used in Thread.initializeThreading()
    // to format the stack as a Java object of type Klass.LOCAL_ARRAY
    java_lang_Thread_serviceStack = Address_add(roundUp((UWord)java_lang_GC_ramEnd, pageSize), TWOWORDS);
    setUWord(java_lang_Thread_serviceStack, HDR_length, SERVICE_CHUNK_SIZE_MINUS2WORDS / HDR_BYTES_PER_WORD);

    // The command line arguments for the JAM start on the next page after the end of the stack for the service thread.
    java_lang_VM_argc = argc;
    java_lang_VM_argv = (Address)roundUp((UWord)Address_add(java_lang_Thread_serviceStack, SERVICE_CHUNK_SIZE_MINUS2WORDS), pageSize);
    offset = writeCStringArray(argc, argv, Address_diff(java_lang_VM_argv, memory));

    // The name of the file from which the bootstrap image was loaded is copied into
    // memory immediately after the command line arguments for the JAM as there's no
    // need for it to be in its own page
    java_lang_VM_romFileName = Address_add(memory, offset);
    offset = writeCString(romFileName, offset);

    // Ensure that the buffer did not overflow
    assume(loeq(Address_add(memory, offset), memoryEnd));

    // Ensure all the buffers start at word aligned addresses.
    assume(isWordAligned((UWord)java_lang_VM_romStart));
    assume(isWordAligned((UWord)java_lang_GC_nvmStart));
    assume(isWordAligned((UWord)java_lang_GC_ramStart));
    assume(isWordAligned((UWord)java_lang_Thread_serviceStack));

    if (java_lang_GC_traceFlags != 0) {
        fprintf(stderr, format("ROM relocated to %A\n"), java_lang_VM_romStart);
        fprintf(stderr, format("Memory start    = %A\n"), memory);
        fprintf(stderr, format("Memory end      = %A\n"), memoryEnd);
#if TYPEMAP
        fprintf(stderr, format("Type map start  = %A\n"), getTypePointer(memory));
        fprintf(stderr, format("Type map end    = %A\n"), getTypePointer(memoryEnd));
#endif /* TYPEMAP */
        fprintf(stderr, format("ROM start       = %A\n"), java_lang_VM_romStart);
        fprintf(stderr, format("ROM end         = %A\n"), java_lang_VM_romEnd);
        fprintf(stderr, format("NVM start       = %A\n"), java_lang_GC_nvmStart);
        fprintf(stderr, format("NVM end         = %A\n"), java_lang_GC_nvmEnd);
        fprintf(stderr, format("RAM start       = %A\n"), java_lang_GC_ramStart);
        fprintf(stderr, format("RAM end         = %A\n"), java_lang_GC_ramEnd);
        fprintf(stderr, format("Bootstrap suite = %A\n"), suite);
    }

    return suite;
}

/**
 * Process the command line arguments.
 *
 * @param argv    the original command line options
 * @param argc    the number of components in argv
 * @return the pointer to the relocated bootstrap suite in ROM
 */
Address processArgs(char *argv[], const int argc) {
    boolean isLaunchedViaJNI = (JNI_env != null);
    int newIndex = 0;
    int oldIndex = 0;
    char *javaVMArgs[MAX_JVM_ARGS];
    int   javaVMArgsCount = 0;

    int nvmSize = DEFAULT_NVM_SIZE;
    int ramSize = DEFAULT_RAM_SIZE;

#ifdef PROFILING
    printf("*************** Profiling version ***************\n");
#endif

#ifdef DB_DEBUG
    printf("*************** Debug version ***************\n");
#endif

    while (oldIndex != argc) {
        char *arg = argv[oldIndex];
        if (arg[0] != '-') {
            break; /* finished VM options part */
        }

        if (arg[1] != 'X' && (isLaunchedViaJNI || arg[1] != 'J')) {
            argv[newIndex++] = arg;
        } else {
            if (arg[1] == 'X') {
                arg += 2; /* skip the '-X' */
#ifdef IOPORT
                if (startsWith(arg, "ioport:")) {
                    ioport = arg + 7;
                } else
#endif
                if (startsWith(arg, "mxnvm:")) {
                    nvmSize = parseQuantity(arg+6, "-Xmxnvm:");
                } else if (startsWith(arg, "mx:")) {
                    ramSize = parseQuantity(arg+3, "-Xmx:");
                } else if (startsWith(arg, "boot:")) {
                    java_lang_VM_romFileName = arg + 5;
                } else if (startsWith(arg, "tgca:")) {
                    java_lang_GC_traceThreshold = parseQuantity(arg+5, "-Xtgca:");
                } else if (startsWith(arg, "tgc:")) {
                    java_lang_GC_traceFlags = parseQuantity(arg+4, "-Xtgc:");
                } else if (equals(arg, "notrap")) {
                    notrap = true;
#ifdef TRACE
                } else if (equals(arg, "terr")) {
                    traceFile = stderr;
                } else if (startsWith(arg, "ts:")) {
                    setTraceStart(parseQuantityLong(arg+3, "-Xts:"));
                } else if (startsWith(arg, "te:")) {
                    setTraceEnd(parseQuantityLong(arg+3, "-Xte:"));
                } else if (startsWith(arg, "tr:")) {
                    jlong start = parseQuantityLong(arg+3, "-Xtr:");
                    setTraceStart(start);
                    setTraceEnd(start + 5000);
                } else if (startsWith(arg, "stats:")) {
                    statsFrequency = parseQuantity(arg+6, "-Xstats:");
                    if (statsFrequency == 0) {
                        printf("-Xstats:0 is invalid\n");
                        stopVM(-1);
                    }
#endif /* TRACE */
#ifdef PROFILING
                } else if (startsWith(arg, "prof:")) {
                    sampleFrequency = parseQuantity(arg+5, "-Xprof:");
                    if (sampleFrequency == 0) {
                        printf("-Xprof:0 is invalid\n");
                        stopVM(-1);
                    }
#endif
                } else {
                    if (arg[0] != 0) {
                        printf("Unrecognised option: -X%s\n", arg);
                    }
                    usage();
                    stopVM(0);
                }
            } else {
                /* '-J' flag */
                if (javaVMArgsCount >= MAX_JVM_ARGS) {
                    fatalVMError("too many '-J' flags");
                }
                javaVMArgs[javaVMArgsCount++] = arg + 2;
            }
        }
        oldIndex++;
    }

    /* Copy main class and it args */
    while (oldIndex != argc) {
        argv[newIndex++] = argv[oldIndex++];
    }

    if (!notrap) {
        signal(SIGSEGV, signalHandler);
#ifndef _MSC_VER
        signal(SIGBUS,  signalHandler);
#endif
        signal(SIGINT,  signalHandler);
    } else {
        printf("Trap handling disabled\n");
    }

    /*
     * Startup the embedded Hotspot VM if Squawk was not launched via a JNI call
     */
    if (!isCalledFromJava) {
        CIO_initialize(null, "squawk.jar", javaVMArgs, javaVMArgsCount);
    }

    /*
     * Set up the buffer that will be used for the ROM, NVM and RAM, remaining.
     */
    return setupMemory(ramSize, nvmSize, newIndex, argv);
}

/**
 * A structure used to verify that the PLATFORM_BIG_ENDIAN constant is correct.
 */
typedef union {
    int i;
    char c[4];
} EndianTest;

/**
 * Verifies that the PLATFORM_BIG_ENDIAN constant is correct, that the
 * ROM image was built with the correct endianess and that loads can be
 * unaligned if the PLATFORM_UNALIGNED_LOADS constant is true. If any of
 * these tests fail, an error message is printed and the VM will exit.
 */
void verifyBuildFlags() {
    EndianTest et;
    boolean bigEndian;

    et.i = 1;
    bigEndian = (et.c[3] == 1);

    if (bigEndian != PLATFORM_BIG_ENDIAN) {
        fprintf(stderr, "PLATFORM_BIG_ENDIAN constant is incorrect: should be %s\n", bigEndian ? "true" : "false");
        stopVM(-1);
    }

    if (ROM_BIG_ENDIAN != PLATFORM_BIG_ENDIAN) {
        fprintf(stderr, "ROM endiness not correct, build with %s\n", PLATFORM_BIG_ENDIAN ? "-big" : "-little");
        stopVM(-1);
    }

    if (SQUAWK_64 != (HDR_BYTES_PER_WORD == 8)) {
        fprintf(stderr, "A %d bit squawk executable cannot be run with a %d bit image\n", (SQUAWK_64 ? 64 : 32), HDR_BYTES_PER_WORD*8);
        stopVM(-1);
    }

    if (PLATFORM_UNALIGNED_LOADS) {
        unsigned char bytecode[] = { 0x1, 0x2, 0x3, 0x4, 0x5, 0x6, 0x7, 0x8, 0x9, 0xa, 0xb, 0xc, 0xd, 0xe, 0xf };
        int i;

        for (i = 0; i != 8; ++i) {
            ByteAddress xxip;
            unsigned char *ptr = bytecode + i;
            int b1 = *ptr++;
            int b2 = *ptr++;
            int b3 = *ptr++;
            int b4 = *ptr++;
            int expect;

            if (PLATFORM_BIG_ENDIAN) {
                expect = (b1 << 24) | (b2 << 16) | (b3 << 8) | b4;
            } else {
                expect = (b4 << 24) | (b3 << 16) | (b2 << 8) | b1;
            }

            xxip = bytecode + i;
            if (expect != *((int *)xxip)) {
                fprintf(stderr, "PLATFORM_UNALIGNED_LOADS constant is incorrect: should be false\n");
                stopVM(-1);
            }
        }
    }
}

/**
 * Program entrypoint.
 *
 * @param argc the number of command line parameters
 * @param argv the parameter argument vector
 */
Address Squawk_setup(int argc, char *argv[]) {

    Address bootstrapSuite;
    void CIO_initialize(JNIEnv *env, char *classPath, char** jvmArgs, int jvmArgc);

    /*
     * Sanity check.
     */
    assume(sizeof(UWord) == sizeof(Address));

    /*
     * Check that the build flags were correct.
     */
    verifyBuildFlags();

    /*
     * Extract the native VM options
     */
    bootstrapSuite = processArgs(argv, argc);

    /*
     * Set the global informing the VM that memory access type checking is enabled
     */
    java_lang_VM_usingTypeMap = TYPEMAP;

    /*
     * Make ROM and NVM be read-only
     */
    toggleMemoryProtection(java_lang_GC_nvmStart, java_lang_GC_nvmEnd, true);
    toggleMemoryProtection(java_lang_VM_romStart, java_lang_VM_romEnd, true);

    /*
     * Return the bootstrap suite.
     */
    return bootstrapSuite;
}



/**
 * Program entrypoint.
 *
 * @param argc the number of command line parameters
 * @param argv the parameter argument vector
 */
int Squawk_main(int argc, char *argv[]) {
    int opcode = -1;
#ifdef TRACE
    int opcodeCopy = opcode;
#endif

#ifdef MACROIZE
    int          iparm;                         /* The immediate operand value of the current bytecode. */
    ByteAddress  ip = 0;                        /* The instruction pointer. */
    UWordAddress fp = 0;                        /* The frame pointer. */
    UWordAddress sp = 0;                        /* The stack pointer. */
#endif

    Address bootstrapSuite = Squawk_setup(argc, argv);

    /*
     * Set up the VM entry point.
     */
    ip = (ByteAddress)java_lang_VM_do_startup;
    sp = (UWordAddress)Address_add(java_lang_Thread_serviceStack, SERVICE_CHUNK_SIZE_MINUS2WORDS);
    setStack(java_lang_Thread_serviceStack);

    /*
     * Push the parameters in the normal Java order.
     */
    downPushAddress(bootstrapSuite);
    downPushAddress(0); /* Dummy return address */

#ifdef PROFILING
#ifdef OSPROF
    osprofstart(sampleFrequency);
#endif
#endif

#ifdef DB_DEBUG
    db_prepare();
#endif

    /*
     * This is the main bytecode execution loop.
     */
   while(true) {
#ifdef TRACE
        const int lastOpcode = opcodeCopy;
        ByteAddress ipCopy = ip;
#endif
        opcode = fetchUByte();
#ifdef PROFILING
        opcodeCopy = opcode;
#endif
        osloop();
#ifdef DB_DEBUG
        db_checkBreak(opcode, ip, fp);
#endif

#ifdef TRACE
#ifdef MACROIZE
        /*
         * Copy the variable values into corresponding globals so that
         * printStackTrace() works in the macroized version of the VM
         */
        lastIP = ip;
        lastFP = fp;
#endif
        if (tracing) {
            trace(ipCopy, fp, sp);
        }
#endif

#ifdef PROFILING
        instructionCount++;
#ifdef OSPROF
        OSPROF(ipCopy, fp, lastOpcode);
#else
        if (sampleFrequency > 0 && (instructionCount % sampleFrequency) == 0) {
            printProfileStackTrace(ipCopy, fp, lastOpcode);
        }
#endif
#endif

        next:
#include "switch.c"
            continue;

#ifdef MACROIZE
        threadswitchstart: {
            threadswitchmain();
            continue;
        }

        invokenativestart: {
            invokenativemain();
            continue;
        }

        throw_nullCheck: {
            resetStackPointer();
            call(java_lang_VM_do_nullPointerException);
            continue;
        }

        throw_boundsCheck: {
            resetStackPointer();
            call(java_lang_VM_do_arrayIndexOutOfBoundsException);
            continue;
        }
#endif
    }

    return 0;
}

/**
 * Starts the Squawk VM via a JNI call from a Java launcher. This alternative mechanism is
 * useful when the channel code running on the JVM needs to be debugged in a JPDA debugger.
 * It is also useful on platforms where the Invocation API doesn work properly.
 *
 * @param env      the table of JNI method pointers
 * @param launcher handle to the receiver object for this invocation
 * @param args     a Java byte array containing all the command line arguments formatted as
 *                 0-delimited ASCII C strings
 * @param argc     the number of command line arguments contained in 'args'
 */
JNIEXPORT int JNICALL Java_com_sun_squawk_vm_Main_squawk(JNIEnv *env, jobject launcher, jbyteArray args, jint argc) {
    int ignore = initializeGlobals();
    int size = (*env)->GetArrayLength(env, args);
    signed char *buf = newBuffer(size, "Java_com_sun_squawk_vm_Main_squawk::buf", true);
    signed char **argv = newBuffer(argc, "Java_com_sun_squawk_vm_Main_squawk::argv", true);
    int i = 0, pos = 0;
    boolean atNextArg = true;
    int exitCode;

    /*
     * Copy the Java byte array contents into 'buf'
     */
    (*env)->GetByteArrayRegion(env, args, 0, size, buf);

    /*
     * Find the individual strings in 'args' and set the pointers in 'argv' accordingly
     */
    while (pos != size) {
        if (atNextArg) {
            argv[i++] = buf + pos;
            atNextArg = false;
        } else if (buf[pos] == 0) {
            atNextArg = true;
        }
        pos++;
    }

    assume(i == argc);
    CIO_initialize(env, null, null, -1);

    /**
     * Register the handle to the 'exit' method of com.sun.squawk.vm.Main.
     */
    isCalledFromJava = true;

    if ((exitCode = setjmp(vmStartScope)) == 0) {
        Squawk_main(argc, (char**)argv);
    }
    return exitCode;
}

#ifdef IOSERVER
#include "ioserver.c"
#endif /* IOSERVER */


#ifndef OSMAIN
/**
 * Program entrypoint.
 *
 * @param argc the number of command line parameters
 * @param argv the parameter argument vector
 */
int main(int argc, char *argv[]) {
    int ignore = initializeGlobals();
    char *executable = argv[0];

    /*
     * Loose the first argument (the path to the executable program).
     */
    argv++;
    argc--;

#ifdef IOSERVER
    if (strstr(executable, "ioserver") != null) {
        return IOServer_main(argc, argv);
    }
#endif /* IOSERVER */
    return Squawk_main(argc, argv);
}
#endif /* OSMAIN */
