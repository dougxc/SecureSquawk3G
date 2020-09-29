/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */

#include <stdlib.h>
#include <sys/time.h>
#include <dlfcn.h>
#include <link.h>
#include <jni.h>

#define jlong  long long

jlong sysTimeMicro(void) {
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
    void* libVM;
    jint (JNICALL *CreateJavaVM)(JavaVM **jvm, void **env, void *args) = 0;
    char *name = "libjvm.so";
    libVM = dlopen(name, RTLD_LAZY);
    if (libVM == 0) {
        fprintf(stderr, "Cannot load %s: %s\n", name, dlerror());
        fprintf(stderr, "Please add the directories containing libjvm.so and libverify.so\n");
        fprintf(stderr, "to the LD_LIBRARY_PATH environment variable.\n\n");
        return false;
    }

    CreateJavaVM = (jint (JNICALL *)(JavaVM **,void **, void *)) dlsym(libVM, "JNI_CreateJavaVM");

    if (CreateJavaVM == 0) {
        fprintf(stderr,"Cannot resolve JNI_CreateJavaVM in %s\n", name);
        return false;
    }

    return CreateJavaVM(jvm, env, args) == 0;
}


void startTicker(int interval) {
    fprintf(stderr, "Profiling not implemented");
    exit(0);
}

#define osloop()        /**/
#define osbackbranch()  /**/
#define osfinish()      /**/
