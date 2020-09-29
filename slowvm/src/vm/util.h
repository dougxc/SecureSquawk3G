/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */

/*---------------------------------------------------------------------------*\
 *                               mprotect                                    *
\*---------------------------------------------------------------------------*/

/**
 * Gets the page size (in bytes) of the system.
 *
 * @return the page size (in bytes) of the system
 */
int getSystemPageSize(void) {
#ifdef _MSC_VER
    SYSTEM_INFO systemInfo;
    GetSystemInfo(&systemInfo);
    return systemInfo.dwPageSize;
#else
    return sysconf(_SC_PAGESIZE);
#endif /* _MSC_VER */
}

/**
 * Sets a region of memory read-only or reverts it to read & write.
 *
 * @param start    the start of the memory region
 * @param end      one byte past the end of the region
 * @param readonly specifies if read-only protection is to be enabled or disabled
 */
void toggleMemoryProtection(Address start, Address end, boolean readonly) {
    UWord len = Address_diff(end, start);
#ifdef _MSC_VER
    unsigned long old;
//fprintf(stderr, format("toggle memory protection: start=%A len=%d end=%A readonly=%s\n"), start, len, end,  readonly ? "true" : "false");
    if (VirtualProtect(start, len, readonly ? PAGE_READONLY : PAGE_READWRITE, &old) == 0) {
        fprintf(stderr, format("Could not toggle memory protection: errno=%d addr=%A len=%d readonly=%s\n"), GetLastError(), start, len, readonly ? "true" : "false");
    }
#else
    if (mprotect(start, len, readonly ? PROT_READ : PROT_READ | PROT_WRITE) != 0) {
        fprintf(stderr, "Could not toggle memory protection: %s\n", strerror(errno));
    }
#endif
}

/*---------------------------------------------------------------------------*\
 *                                  Errors                                   *
\*---------------------------------------------------------------------------*/

/**
 * Exits the VM with an error message.
 */
void fatalVMError(char *msg) {
    static boolean inFatalVMError = false;
    void stopVM(int);

    if (msg == null) {
        msg = "fatal VM error";
    }
    if (inFatalVMError) {
        fprintf(stderr, "Recursive call to fatalVMError(%s)\n", msg);
        fflush(stderr);
//exit(1);
    } else {
        fprintf(stderr, "%s\n", msg);
        fflush(stderr);
        inFatalVMError = true;
#if TRACE
        printStackTrace(msg);
#endif
    }
    stopVM(-1);
}

#if defined(ASSUME) && ASSUME != 0
#define assume(x) if (!(x))  { fprintf(stderr, "Assertion failed: \"%s\", at %s:%d\n", #x, __FILE__, __LINE__); fatalVMError(""); }
#define shouldNotReachHere() { fprintf(stderr, "shouldNotReachHere -- %s:%d\n", __FILE__, __LINE__); fatalVMError(""); }
#else
#define assume(x) /**/
#define shouldNotReachHere() /**/
#endif /* ASSUME */


#include <signal.h>
void signalHandler(int signum) {
    char* strsignal(int signum);
//fprintf(stderr, "caught signal %d\n", signum);
    fatalVMError(strsignal(signum));
}

/*---------------------------------------------------------------------------*\
 *                               alignment                                   *
\*---------------------------------------------------------------------------*/

    /**
     * Determines if a given value is a power of 2.
     *
     * @param value  the value to test
     * @return true if 'value' is a power of 2
     */
    inline boolean isPowerOf2(UWord value) {
        return ((value) & (value - 1)) == 0;
    }

    /**
     * Rounds up a value based on a given alignment.
     *
     * @param value      the value to be rounded up
     * @param alignment  <code>value</value> is rounded up to be a multiple of this value
     * @return the aligned value
     */
    inline UWord roundUp(UWord value, UWord alignment) {
        assume(isPowerOf2(alignment));
        return (value + (alignment - 1)) & ~(alignment - 1);
    }

    /**
     * Rounds up a value to the next word boundry.
     *
     * @param value  the value to round up
     * @return the result
     */
    inline UWord roundUpToWord(UWord value) {
        return (value + (HDR_BYTES_PER_WORD - 1)) & ~(HDR_BYTES_PER_WORD - 1);
    }

    /**
     * Rounds down a value based on a given alignment.
     *
     * @param value      the value to be rounded down
     * @param alignment  <code>value</value> is rounded down to be a multiple of this value
     * @return the aligned value
     */
    inline UWord roundDown(UWord value, UWord alignment) {
        assume(isPowerOf2(alignment));
        return value & ~(alignment - 1);
    }

    /**
     * Rounds down a value to the next word boundry.
     *
     * @param value  the value to round down
     * @return the result
     */
    inline UWord roundDownToWord(UWord value) {
        return value & ~(HDR_BYTES_PER_WORD - 1);
    }

    /**
     * Determines if a given value is word aligned.
     *
     * @param value  the value to test
     * @return true if <code>value</code> is word aligned
     */
    inline boolean isWordAligned(UWord value) {
        return value == roundDownToWord(value);
    }

    /**
     * Determines if a given value is aligned with respect to a given alignment.
     *
     * @param value      the value to test
     * @param alignment  the alignment
     * @return true if <code>value</code> is a mutliple of <code>alignment</code>
     */
    inline boolean isAligned(UWord value, UWord alignment) {
        return value == roundDown(value, alignment);
    }

/*---------------------------------------------------------------------------*\
 *                            Low level operations                           *
\*---------------------------------------------------------------------------*/

inline jlong  slll(jlong a, int b)         { return a<<(b&63);                         }
inline jlong  sral(jlong a, int b)         { return a>>(b&63);                         }
inline jlong  srll(jlong a, int b)         { return ((ujlong)a)>>(b&63);               }
inline int    sll(int a, int b)            { return a<<(b&31);                         }
inline int    sra(int a, int b)            { return a>>(b&31);                         }
inline int    srl(int a, int b)            { return ((unsigned)a)>>(b&31);             }
inline int    i2b(int i)                   { return (byte)i;                           }
inline int    i2s(int i)                   { return (short)i;                          }
inline int    i2c(int i)                   { return (char)i;                           }
inline jlong  i2l(int i)                   { return (jlong)i;                          }
inline int    l2i(jlong l)                 { return (int)l;                            }

union  uu                                  { int i; float f; jlong l; double d;        };
inline float  ib2f(int i)                  { union  uu x; x.i = i; return x.f;         }
inline double lb2d(jlong l)                { union  uu x; x.l = l; return x.d;         }
inline int    f2ib(float f)                { union  uu x; x.f = f; return x.i;         }
inline jlong  d2lb(double d)               { union  uu x; x.d = d; return x.l;         }
#ifdef _MSC_VER
#  if _MSC_VER < 1400 /* fmodf is defined in MSC version 14.00 and greater */
inline float  fmodf(float a, float b)      { return (float)fmod(a, b);                 }
#  endif
#else /* _MSC_VER */
#  ifndef __APPLE__
inline float  fmodf(float a, float b)      { return (float)fmod(a, b);                 }
#  endif /* __APPLE__ */
#endif /* _MSC_VER */
inline double fmodd(double a, double b)    { return fmod(a, b);                        }

inline int    addf(int l, int r)           { return f2ib(ib2f(l) + ib2f(r));           }
inline int    subf(int l, int r)           { return f2ib(ib2f(l) - ib2f(r));           }
inline int    mulf(int l, int r)           { return f2ib(ib2f(l) * ib2f(r));           }
inline int    divf(int l, int r)           { return f2ib(ib2f(l) / ib2f(r));           }
inline int    remf(int l, int r)           { return f2ib(fmodf(ib2f(l), ib2f(r)));     }
inline int    negf(int l)                  { return f2ib(((float)0) - ib2f(l));        }
inline jlong  addd(jlong l, jlong r)       { return d2lb(lb2d(l) + lb2d(r));           }
inline jlong  subd(jlong l, jlong r)       { return d2lb(lb2d(l) - lb2d(r));           }
inline jlong  muld(jlong l, jlong r)       { return d2lb(lb2d(l) * lb2d(r));           }
inline jlong  divd(jlong l, jlong r)       { return d2lb(lb2d(l) / lb2d(r));           }
inline jlong  remd(jlong l, jlong r)       { return d2lb(fmodd(lb2d(l), lb2d(r)));     }
inline jlong  negd(jlong l)                { return d2lb(((double)0) - lb2d(l));       }

inline int    i2f(int i)                   { return f2ib((float)i);                    }
inline jlong  i2d(int i)                   { return d2lb((double)i);                   }
inline int    l2f(jlong l)                 { return f2ib((float)l);                    }
inline jlong  l2d(jlong l)                 { return d2lb((double)l);                   }
inline int    f2i(int f)                   { return (int)ib2f(f);                      }
inline jlong  f2l(int f)                   { return (jlong)ib2f(f);                    }
inline jlong  f2d(int f)                   { return d2lb((double)ib2f(f));             }
inline int    d2i(jlong l)                 { return (int)lb2d(l);                      }
inline jlong  d2l(jlong l)                 { return (jlong)lb2d(l);                    }
inline int    d2f(jlong l)                 { return f2ib((float)lb2d(l));              }

/*---------------------------------------------------------------------------*\
 *                                Math functions                             *
\*---------------------------------------------------------------------------*/

inline jlong math(int op, jlong rs1_l, jlong rs2_l) {
    double rs1 = lb2d(rs1_l);
    double rs2 = lb2d(rs2_l);
    double res = 0.0;
    switch (op) {
        case MathOpcodes_SIN:            res =  sin(rs1);                       break;
        case MathOpcodes_COS:            res =  cos(rs1);                       break;
        case MathOpcodes_TAN:            res =  tan(rs1);                       break;
        case MathOpcodes_ASIN:           res =  asin(rs1);                      break;
        case MathOpcodes_ACOS:           res =  acos(rs1);                      break;
        case MathOpcodes_ATAN:           res =  atan(rs1);                      break;
        case MathOpcodes_EXP:            res =  exp(rs1);                       break;
        case MathOpcodes_LOG:            res =  log(rs1);                       break;
        case MathOpcodes_SQRT:           res =  sqrt(rs1);                      break;
        case MathOpcodes_CEIL:           res =  ceil(rs1);                      break;
        case MathOpcodes_FLOOR:          res =  floor(rs1);                     break;
        case MathOpcodes_ATAN2:          res =  atan2(rs1, rs2);                break;
        case MathOpcodes_POW:            res =  pow(rs1, rs2);                  break;
        case MathOpcodes_IEEE_REMAINDER: {
            double q = fmod(rs1, rs2);
            double d = fabs(rs2);
            if (q < 0) {
                if (-q > d / 2) {
                    q += d;
                }
            } else {
                if (q > d / 2) {
                    q -= d;
                }
            }
            res = q;
            break;
        }
        default:
            shouldNotReachHere();
    }
    return d2lb(res);
}
