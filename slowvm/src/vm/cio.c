#include IODOTC

//#define UNICODE true

inline static jlong makeLong(int high, int low) {
    return (((jlong)high) << 32) | (((jlong)low) & 0x00000000FFFFFFFFL);
}

/**
 * Execute a channel operation.
 */
 void cioExecute(void) {
    int     context = java_lang_ServiceOperation_context;
    int     op      = java_lang_ServiceOperation_op;
    int     channel = java_lang_ServiceOperation_channel;
    int     i1      = java_lang_ServiceOperation_i1;
    int     i2      = java_lang_ServiceOperation_i2;
    int     i3      = java_lang_ServiceOperation_i3;
    int     i4      = java_lang_ServiceOperation_i4;
    int     i5      = java_lang_ServiceOperation_i5;
    int     i6      = java_lang_ServiceOperation_i6;
    Address o1      = java_lang_ServiceOperation_o1;
    Address o2      = java_lang_ServiceOperation_o2;
    FILE   *vmOut   = streams[currentStream];

    switch (op) {

        case ChannelConstants_INTERNAL_SETSTREAM: {
            java_lang_ServiceOperation_result = currentStream;
            currentStream = i1;
            if (streams[currentStream] == null) {
                switch(currentStream) {
                    case java_lang_VM_STREAM_SYMBOLS: {
                        streams[currentStream] = fopen("squawk_dynamic.sym", "w");
                        break;
                    }
                    default: {
                        fatalVMError("Bad INTERNAL_SETSTREAM");
                    }
                }
            }
            assume(streams[currentStream] != null);
            break;
        }

        case ChannelConstants_INTERNAL_PRINTSTRING: {
            int i;
            Address str = o1;
            if (str == null) {
                fprintf(vmOut, "null");
            } else {
                int lth = getArrayLength(str);
#ifdef UNICODE
                Address cls = getClass(str);
                if (java_lang_Class_classID(cls) == java_lang_StringOfBytes) {
#endif
                    unsigned char *chars = (unsigned char *)str;
                    for (i = 0 ; i < lth ; i++) {
                        fprintf(vmOut, "%c", chars[i]);
                    }
#ifdef UNICODE
                } else {
                    unsigned short *chars = (unsigned short *)str;
                    if (java_lang_Class_classID(cls) != java_lang_String) {
                        fatalVMError("java_lang_VM_printString was not passed a string");
                    }
                    for (i = 0 ; i < lth ; i++) {
                        fprintf(vmOut, "%c", chars[i]);
                    }
                }
#endif
            }
            fflush(vmOut);
            break;
        }

        case ChannelConstants_INTERNAL_PRINTCHAR: {
            fprintf(vmOut, "%c", i1);
            fflush(vmOut);
            break;
        }

        case ChannelConstants_INTERNAL_PRINTINT: {
            fprintf(vmOut, "%d", i1);
            fflush(vmOut);
            break;
        }

        case ChannelConstants_INTERNAL_PRINTUWORD: {
            //ujlong val = ((ujlong)i1) << 32 | ((ujlong)i2);
            jlong val = makeLong(i1, i2);
            fprintf(vmOut, format("%A"), (UWord)val);
            fflush(vmOut);
            break;
        }

        case ChannelConstants_INTERNAL_PRINTOFFSET: {
            //jlong val = ((jlong)i1) << 32 | ((jlong)i2);
            jlong val = makeLong(i1, i2);
            fprintf(vmOut, format("%W"), val);
            fflush(vmOut);
            break;
        }

        case ChannelConstants_INTERNAL_PRINTLONG: {
            //ujlong val = ((ujlong)i1) << 32 | ((ujlong)i2);
            jlong val = makeLong(i1, i2);
            fprintf(vmOut, format("%L"), val);
            fflush(vmOut);
            break;
        }

        case ChannelConstants_INTERNAL_PRINTADDRESS: {
            Address val = o1;
            fprintf(vmOut, format("%A"), val);
            if (hieq(val, java_lang_VM_romStart) && lo(val, java_lang_VM_romEnd)) {
                fprintf(vmOut, format(" (image @ %W)"), Address_sub(val, java_lang_VM_romStart));
            }
            fflush(vmOut);
            break;
        }

        case ChannelConstants_INTERNAL_PRINTCONFIGURATION: {
            fprintf(stderr, "native VM build flags: %s\n", BUILD_FLAGS);
            break;
        }

        case ChannelConstants_INTERNAL_PRINTGLOBALS: {
            printGlobals();
            fflush(vmOut);
            break;
        }

        case ChannelConstants_INTERNAL_PRINTGLOBALOOPNAME: {
#ifdef TRACE
            fprintf(vmOut, "%s", getGlobalOopName(i1));
#else
            fprintf(vmOut, "Global oop:%d", i1);
#endif
            fflush(vmOut);
            break;
        }

        case ChannelConstants_INTERNAL_GETPATHSEPARATORCHAR: {
            java_lang_ServiceOperation_result = pathSeparatorChar;
            break;
        }

        case ChannelConstants_INTERNAL_GETFILESEPARATORCHAR:  {
            java_lang_ServiceOperation_result = fileSeparatorChar;
            break;
        }

        case ChannelConstants_INTERNAL_COPYBYTES:  {
            int     lth       = i1;
            int     srcoffset = i2;
            int     dstoffset = i3;
            boolean nvmDst    = i4 == 1;
            Address src       = o1;
            Address dst       = o2;
//fprintf(stderr, format("copying  %d bytes from %A at offset %d to %A at offset %d nvmDst=%d\n"), lth, src, srcoffset, dst, dstoffset, nvmDst);
            if (nvmDst) {
                toggleMemoryProtection(java_lang_GC_nvmStart, java_lang_GC_nvmEnd, false);
            }
            memmove(Address_add(dst, dstoffset), Address_add(src, srcoffset), lth);
            checkPostWrite(Address_add(dst, dstoffset), lth);
            if (nvmDst) {
                toggleMemoryProtection(java_lang_GC_nvmStart, java_lang_GC_nvmEnd, true);
            }
            break;
        }

        case ChannelConstants_INTERNAL_GETTIME_HIGH: {
            lastTime = sysTimeMillis();
            java_lang_ServiceOperation_result = (int)(lastTime >> 32);
            break;
        }

        case ChannelConstants_INTERNAL_GETTIME_LOW: {
            java_lang_ServiceOperation_result = (int)lastTime;
            break;
        }

        case ChannelConstants_INTERNAL_STOPVM: {
            stopVM(i1);
            break;
        }

        case ChannelConstants_INTERNAL_MATH: {
            fatalVMError("Unimplemented internal channel I/O operation");
        }

        default: {
            ioExecute();
        }
    }
}
