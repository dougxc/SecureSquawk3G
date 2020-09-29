/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */

#ifdef TRACE

int lastThreadID = -2;

/**
 * Open tbe trace file if not already opened.
 *
 * @return true if the trace file was opened by this call, false if it
 *         was already open
 */
boolean openTraceFile() {
    if (!traceFileOpen) {
        if (traceFile == NULL) {
            traceFile = fopen("trace", "w");
        }
        traceFileOpen = true;
        fprintf(traceFile, format("*TRACE*:*ROM*:%A:%A:*NVM*:%A:%A:*%d*\n"),
                java_lang_VM_romStart, java_lang_VM_romEnd, java_lang_GC_nvmStart, java_lang_GC_nvmEnd,
                SQUAWK_64 ? 64 : 32);
        return true;
    } else {
        return false;
    }
}

/**
 * A struct for storing a decoded instruction.
 */
typedef struct {

    /**
     * Specifies if the instruction has no operands (0), an int operand (1), a long operand (2), a float operand(3) or a double operand (4)
     */
    int tag;

    /**
     * The WIDE_* or ESCAPE_* prefix of the instruction or -1 if neither applies.
     */
    int prefix;

    /**
     * The opcode of the instruction.
     */
    int opcode;

    /**
     * The int/float/long/double operand.
     */
    union {
        int i;
        jlong l;
    } operand;

#if TYPEMAP
    /**
     * The mutation type of the instruction.
     */
    char mutationType;
#endif /* TYPEMAP */

} DecodedInstruction;

/**
 * Decodes an instruction.
 *
 * @param instruction  the structure in which the decoded information is returned
 * @param traceIP      the instruction pointer which is currently at the start of an instruction
 */
void decodeInstruction(DecodedInstruction *instruction, ByteAddress traceIP) {
    int opcode;
#ifdef MACROIZE
    ByteAddress ip;  // local version of IP
#endif /* MACROIZE */
    ip = traceIP;
    opcode = fetchUByte();

    // Initialize the return struct for the common case
    (*instruction).tag = 1;
    (*instruction).prefix = -1;
#if TYPEMAP
    (*instruction).mutationType = getMutationType();
#endif /* TYPEMAP */

    // Sanity
    (*instruction).opcode = -1;

    switch (opcode) {
        case OPC_WIDE_M1: {
            (*instruction).prefix = opcode;
            (*instruction).opcode = fetchUByte();
            (*instruction).operand.i =  0xFFFFFF00|fetchUByte();
            break;
        }
        case OPC_WIDE_0: {
            (*instruction).prefix = opcode;
            (*instruction).opcode = fetchUByte();
            (*instruction).operand.i = fetchUByte();
            break;
        }
        case OPC_WIDE_1: {
            (*instruction).prefix = opcode;
            (*instruction).opcode = fetchUByte();
            (*instruction).operand.i = 0x100|fetchUByte();
            break;
        }
        case OPC_WIDE_SHORT: {
            int fparm;
            fetchShort();
            (*instruction).prefix = opcode;
            (*instruction).opcode = fetchUByte();
            (*instruction).operand.i = fparm;
            break;
        }
        case OPC_WIDE_INT: {
            int fparm;
            fetchInt();
            (*instruction).prefix = opcode;
            (*instruction).opcode = fetchUByte();
            (*instruction).operand.i = fparm;
            break;
        }
        case OPC_ESCAPE: {
            int eopcode = fetchUByte()+256;
            (*instruction).opcode = eopcode;
            switch (eopcode) {
#ifdef java_lang_VM_fcmpl
                case OPC_CONST_FLOAT: {
                    int fparm;
                    fetchInt();
                    (*instruction).tag = 3;
                    (*instruction).operand.i = fparm;
                    break;
                }
                case OPC_CONST_DOUBLE: {
                    jlong flparm;
                    fetchLong();
                    (*instruction).tag = 4;
                    (*instruction).operand.l = flparm;
                    break;
                }
#endif /* java_lang_VM_fcmpl */
                default: {
                    (*instruction).prefix = opcode;
                    if (eopcode >= OPC_FIRST_ESCAPE_PARM_BYTECODE && eopcode < OPC_FIRST_ESCAPE_PARM_BYTECODE+OPC_ESCAPE_PARM_BYTECODE_COUNT) {
                        (*instruction).operand.i = fetchUByte();
                    } else {
                        (*instruction).tag = 0;
                    }
                    break;
                }
            }
            break;
        }
        case OPC_ESCAPE_WIDE_M1: {
            (*instruction).prefix = opcode;
            (*instruction).opcode = fetchUByte()+256;
            (*instruction).operand.i =  0xFFFFFF00|fetchUByte();
            break;
        }
        case OPC_ESCAPE_WIDE_0: {
            (*instruction).prefix = opcode;
            (*instruction).opcode = fetchUByte()+256;
            (*instruction).operand.i = fetchUByte();
            break;
            break;
        }
        case OPC_ESCAPE_WIDE_1: {
            (*instruction).prefix = opcode;
            (*instruction).opcode = fetchUByte()+256;
            (*instruction).operand.i = 0x100|fetchUByte();
            break;
        }
        case OPC_ESCAPE_WIDE_SHORT: {
            int fparm;
            fetchShort();
            (*instruction).prefix = opcode;
            (*instruction).opcode = fetchUByte()+256;
            (*instruction).operand.i = fparm;
            break;
        }
        case OPC_ESCAPE_WIDE_INT: {
            int fparm;
            fetchInt();
            (*instruction).prefix = opcode;
            (*instruction).opcode = fetchUByte()+256;
            (*instruction).operand.i = fparm;
            break;
        }
        case OPC_CONST_BYTE: {
            (*instruction).opcode = opcode;
            (*instruction).operand.i = fetchByte();
            break;
        }
        case OPC_CONST_SHORT: {
            int fparm;
            fetchShort();
            (*instruction).opcode = opcode;
            (*instruction).operand.i = fparm;
            break;
        }
        case OPC_CONST_CHAR: {
            int fparm;
            fetchUShort();
            (*instruction).opcode = opcode;
            (*instruction).operand.i = fparm;
            break;
        }
        case OPC_CONST_INT: {
            int fparm;
            fetchInt();
            (*instruction).opcode = opcode;
            (*instruction).operand.i = fparm;
            break;
        }
        case OPC_CONST_LONG: {
            jlong flparm;
            fetchLong();
            (*instruction).tag = 2;
            (*instruction).opcode = opcode;
            (*instruction).operand.l = flparm;
            break;
        }
        default: {
            (*instruction).opcode = opcode;
            if (opcode >= OPC_FIRST_PARM_BYTECODE && opcode < OPC_FIRST_PARM_BYTECODE+OPC_PARM_BYTECODE_COUNT) {
                (*instruction).operand.i = fetchByte();
            } else {
                (*instruction).tag = 0;
            }
            break;
        }
    }

    assume((*instruction).opcode != -1);
}

/**
 * Calculates the call depth based on a given frame pointer.
 *
 * @param traceFP  a frame pointer within a call stack
 * @return the number of frames (inclusive) between top of the stack and the frame denoted by traceFP
 */
int calculateCallDepth(UWordAddress traceFP) {
    // Determine the call depth level
    int depth = 0;
    while (traceFP != 0) {
        ++depth;
        if (depth > 2000) {
            fatalVMError("**Call depth suspiciously deep - infinite recursion?**");
        }
        traceFP = (UWordAddress)getObject(traceFP, FP_returnFP);
    }
    return depth;
}

/**
 * Writes the values in the local variable slots and on the operand stack.
 *
 * @param opcode    the opcode of the instruction being traced
 * @param traceFP   the current frame pointer which will not be valid if opcode at 'traceIP' is OPC_EXTEND
 * @param traceSP   the current operand stack pointer
 */
void traceLocalsAndStack(int opcode, ByteAddress traceMP, UWordAddress traceFP, UWordAddress traceSP) {
    int nlocals = getLocalCount(traceMP);
    int nstack = getStackCount(traceMP);
    if (opcode == OPC_EXTEND || opcode == OPC_EXTEND0) {
        fprintf(traceFile, format("%d:%d,%d:"), nlocals, nstack, (traceSP - sl));
    } else {
        UWordAddress p   = traceFP;
        UWordAddress sp0 = traceFP - nlocals;
#if ROM_REVERSE_PARAMETERS
        UWordAddress xsp = traceSP;
#else
        UWordAddress xsp = traceFP - nlocals - getStackCount(traceMP) + 1;
#endif

        while (p >= xsp) {
#if TYPEMAP
            UWord value = getUWordTyped(p, 0, getType(p));
#else
            UWord value = getUWordTyped(p, 0, 0);
#endif
            if (value == DEADBEEF) {
                fprintf(traceFile, "X");
            } else {
                fprintf(traceFile, format("%W"), value);
            }
#if TYPEMAP
            fprintf(traceFile, "#%d", getType(p));
#endif /* TYPEMAP */
            --p;
            if (p == sp0) {
                fprintf(traceFile, ":");
#if !ROM_REVERSE_PARAMETERS
                p = traceSP - 1;
#endif
            } else if (p >= xsp) {
                fprintf(traceFile, ",");
            }
        }
        fprintf(traceFile, ":");
    }
}

/**
 * Walks the frames in a call stack from an inner frame to the top most frame, printing a
 * record to the trace file for each frame.
 *
 * @param  opcode    the opcode of the instruction about to be executed or -1 if in the middle of an instruction
 * @param  traceIP   the address of 'opcode' or the current IP if opcode == -1
 * @param  traceFP   the current frame pointer which will not be valid if opcode is OPC_EXTEND
 * @param  forThreadSwiitch specifies if this is called for a *THREADSWITCH* trace line
 */
void traceFrames(int opcode, ByteAddress traceIP, UWordAddress traceFP, boolean forThreadSwitch) {
    int recursionGuard = 0;
    while (traceFP != 0) {
        ByteAddress traceMP;
        int pc;

        // Initialize the method pointer
        if (opcode == OPC_EXTEND || opcode == OPC_EXTEND0) {
            // The frame pointer will not be setup before the extend is executed.
            traceMP = traceIP;

            // From now on, the frame pointer is valid
            opcode = -1;
        } else {
            traceMP = (ByteAddress)getObject(traceFP, FP_method);
        }

        // Get the bytecode offset relative to the start of the method
        pc = (int)(traceIP - traceMP);
        assume(pc >= 0);
        if (forThreadSwitch) {
            fprintf(traceFile, format(":%A@%d"), traceMP, pc);
        } else {
            fprintf(traceFile, format("*STACKTRACE*:%A:%d\n"), traceMP, pc);
        }

        recursionGuard++;
        if (recursionGuard > 2000) {
            fatalVMError("**** Call stack suspiciously deep - infinite recursion? ****");
        }

        traceIP = (ByteAddress)getObject(traceFP, FP_returnIP);
        traceFP = (UWordAddress)getObject(traceFP, FP_returnFP);
    }
}

/**
 * Writes a trace record.
 *
 * @param  traceIP   the address of instruction about to be executed
 * @param  traceFP   the current frame pointer which will not be valid if opcode at 'traceIP' is OPC_EXTEND
 * @param  traceSP   the current operand stack pointer
 */
void trace(ByteAddress traceIP, UWordAddress traceFP, UWordAddress traceSP) {
#ifndef MACROIZE
    ByteAddress saveIP = ip;   // save the global IP
#endif /* MACROIZE */
    DecodedInstruction instruction;
    int pc;                    // bytecode offset from start of method
    int level;                 // current call depth level
    ByteAddress traceMP;       // current method pointer
    int threadID;              // numerical thread ID

    // Initialize the thread ID
    if (java_lang_Thread_currentThread != 0) {
        threadID = java_lang_Thread_threadNumber(java_lang_Thread_currentThread);
    } else {
        // System thread
        threadID = -1;
    }

    // Write THREADSWITCH line
    if (openTraceFile() || threadID != lastThreadID) {
        lastThreadID = threadID;

        fprintf(traceFile, "*THREADSWITCH*:%d", threadID);
        traceFrames(-1, traceIP, traceFP, true);
        fprintf(traceFile, "\n");
    }

    // Decode the next instruction
    decodeInstruction(&instruction, traceIP);

    // Set up the method pointer
    if (instruction.opcode == OPC_EXTEND || instruction.opcode == OPC_EXTEND0) {
        // The frame pointer will not be setup before the extend is executed.
        traceMP = traceIP;
        level = 0;
    } else {
        assume(traceFP != null);
        level = -1;
        traceMP = (ByteAddress)getObject(traceFP, FP_method);
    }

    // Calculate the bytecode offset
    pc = (int)(traceIP - traceMP);

    // Determine the call depth level
    level += calculateCallDepth(traceFP);

    // Write out the first part of the trace line which has the following ':' separated components:
    fprintf(traceFile, format("*TRACE*:%d:%d:%A:%d:"), threadID, level, traceMP, pc);

    // Write out the decoded instruction
#if TYPEMAP
        fprintf(traceFile, "%d#%d,%d", instruction.opcode, instruction.mutationType, instruction.prefix);
#else
        fprintf(traceFile, "%d,%d", instruction.opcode, instruction.prefix);
#endif /* TYPEMAP */
    switch (instruction.tag) {
        case 0: fprintf(traceFile, ":");                                        break;
        case 1: fprintf(traceFile, ",%d:", instruction.operand.i);              break;
        case 2: fprintf(traceFile, format(",%L:"), instruction.operand.l);      break;
        case 3: fprintf(traceFile, ",%f:" , ib2f(instruction.operand.i));       break;
        case 4: fprintf(traceFile, format(",%D:"), lb2d(instruction.operand.l));break;
        default: shouldNotReachHere();
    }

    // Write out the local and stack values
    traceLocalsAndStack(instruction.opcode, traceMP, traceFP, traceSP);

    // Write out the stack pointer, branch count, and remaining number of stack words and complete the trace line
    fprintf(traceFile, format("%A:%L:%d\n"), traceSP, getBranchCount(), (traceSP - sl));
    fflush(traceFile);

#ifndef MACROIZE
    // Restore the value of the global IP
    ip = saveIP;
#endif /* MACROIZE */
}

//void closeTraceFile(void) {
//    if(traceFileOpen) {
//        fprintf(traceFile, "\n+++ Trace file closed +++\n");
//        traceFileOpen = false;
//        fclose(traceFile);
//    }
//}


/**
 * Prints a stack trace to the trace file.
 *
 * @param  opcode    the opcode of the instruction about to be executed or -1 if in the middle of an instruction
 * @param  traceIP   the address of 'opcode' or the current IP if opcode == -1
 * @param  traceFP   the current frame pointer which will not be valid if opcode is OPC_EXTEND
 * @param  msg       some message to append to the first stack trace line
 * @param  mnemonic  the mnemonic for the last executed instruction or null if this is not known
 * @param  msg       the trace message
 */
void printStackTracePrim(int opcode, ByteAddress traceIP, UWordAddress traceFP, const char* msg, const char* mnemonic) {
    openTraceFile();

    // Print the first line of the trace
    assume(msg != null);
    fprintf(traceFile, format("*STACKTRACESTART*:%L:%s"), getBranchCount(), msg);
    if (mnemonic != null) {
        fprintf(traceFile, ":%s\n", mnemonic);
    } else {
        fprintf(traceFile, "\n");
    }

    // Print the frames
    traceFrames(opcode, traceIP, traceFP, false);

    fprintf(traceFile, "*STACKTRACEEND*\n");
    fflush(traceFile);
}

/**
 * Prints a profile stack trace to the trace file.
 *
 * @param  traceIP     the address of the instruction about to be executed
 * @param  traceFP     the current frame pointer which will not be valid if the opcode at traceIP is OPC_EXTEND*
 * @param  lastOpcode  the opcode of previously executed instruction
 */
void printProfileStackTrace(ByteAddress traceIP, UWordAddress traceFP, int lastOpcode) {
    int opcode = getUByteTyped(traceIP, 0, AddressType_BYTECODE);
    printStackTracePrim(opcode, traceIP, traceFP, "*PROFILE TRACE*", getOpcodeName(lastOpcode));
}

/**
 * Prints a stack trace to the trace file.
 *
 * @param msg the trace message
 */
void printStackTrace(const char *msg) {
#ifndef MACROIZE
    ByteAddress lastIP = ip;
    UWordAddress lastFP = fp;
#endif /* MACROIZE */
    printStackTracePrim(-1, lastIP, lastFP, msg, null);
}

#endif /* TRACE */
