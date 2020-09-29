/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */

#include <stdlib.h>
#include <sys/signal.h>
#include <sys/time.h>
#include "jni.h"

/* This "standard" C function is not provided on Mac OS X */
char* strsignal(int signum) {
    switch (signum) {
        case SIGABRT:     return "SIGABRT: Abnormal termination";
        case SIGFPE:      return "SIGFPE: Floating-point error";
        case SIGILL:      return "SIGILL: Illegal instruction";
        case SIGINT:      return "SIGINT: CTRL+C signal";
        case SIGSEGV:     return "SIGSEGV: Illegal storage access";
        case SIGTERM:     return "SIGTERM: Termination request";
        default:          return "<unknown signal>";
    }
}

jlong sysTimeMicro() {
    struct timeval tv;
    long long result;
    gettimeofday(&tv, NULL);
    /* We adjust to 1000 ticks per second */
    result = (jlong)tv.tv_sec * 1000000 + tv.tv_usec;
    return result;
}

jlong sysTimeMillis(void) {
    return sysTimeMicro() / 1000;
}

jint createJVM(JavaVM **jvm, void **env, void *args) {
    return JNI_CreateJavaVM(jvm, env, args) == 0;
}


void startTicker(int interval) {
    fprintf(stderr, "Profiling not implemented");
    exit(0);
}

#define osloop()        /**/
#define osbackbranch()  /**/
#define osfinish()      /**/