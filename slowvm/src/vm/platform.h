/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */

#define true 1
#define false 0
#define boolean int
#define null 0
#define byte signed char
//#define jlong long long  -- defined in jni_md.h
#define ujlong unsigned jlong

#include <stdio.h>
#include <stdlib.h>

#ifdef __APPLE__
#    define PLATFORM_BIG_ENDIAN true
#else
#    include <malloc.h>
#endif /* __APPLE__ */

#include <string.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <errno.h>
#include <string.h>
#include <signal.h>
#include <math.h>
#include <setjmp.h>

#ifdef _MSC_VER
#    include <io.h>
#    undef  ujlong
#    define ujlong unsigned __int64
#    define PLATFORM_BIG_ENDIAN false
#    define PLATFORM_UNALIGNED_LOADS true
#    define C_PARMS_RIGHT_TO_LEFT true
#    define WIN32_LEAN_AND_MEAN
#    ifdef MAXINLINE
#        define inline __forceinline
#    else
#        define inline __inline
#    endif
#    include <windows.h>
#    define pathSeparatorChar ';'
#    define fileSeparatorChar '\\'
#else
#    define pathSeparatorChar ':'
#    define fileSeparatorChar '/'
#endif /* _MSC_VER */

#ifndef _MSC_VER
#    define O_BINARY 0 /* for compatibility with open() and close() on Windows */
#    include <sys/mman.h>
#    ifdef __GNUC__
#        include <unistd.h>
#        undef  ujlong
#        define PLATFORM_UNALIGNED_LOADS true
#        ifdef sun
#            define ujlong uint64_t
#            define PLATFORM_BIG_ENDIAN true
#        else /* sun */
#            define ujlong u_int64_t
#            ifndef PLATFORM_BIG_ENDIAN
#            define PLATFORM_BIG_ENDIAN false
#            endif
#        endif /* sun */
#    else /* assume CC */
#        define PLATFORM_UNALIGNED_LOADS false
#        define PLATFORM_BIG_ENDIAN true
#    endif /* __GNUC__ */
#endif /* _MSC_VER */

#ifndef inline
#    define inline /**/
#endif
#ifndef inline_off
#    define inline_off /**/
#endif
#ifndef inline_on
#    define inline_on /**/
#endif

/**
 * These two conditional compilation macros are also used as values in certain parts and
 * as such must be given a value if they are not defined. This also means that they must
 * used with the '#if' as opposed to '#ifdef' preprocessor directive when surrounding
 * conditional code.
 */
#if defined(ASSUME) && ASSUME != 0
#undef ASSUME
#define ASSUME true
#else
#define ASSUME false
#endif

#if defined(SQUAWK_64) && SQUAWK_64 != 0
#undef SQUAWK_64
#define SQUAWK_64 true
#else
#define SQUAWK_64 false
#endif
