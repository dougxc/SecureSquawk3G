/*
 * Copyright 1994-2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 */

#include <jni.h>

/*
 * A CPU-dependent assembly routine that passes the arguments to C stack and invoke the function.
 */
extern void asm_dispatch(int func, int nwords, char *args_types, int *args, int res_type, void *result, int conv);

/*
 * Return types
 */
typedef enum {
    TY_POINTER = 0,
    TY_INTEGER,
    TY_FLOAT,
    TY_LONG,
    TY_DOUBLE,
} ty_t;

/*
 * Class and field handles
 */
jclass Class_BaseParm;
jfieldID FID_BaseParm_next;
jfieldID FID_BaseParm_ivalue;
jfieldID FID_BaseParm_ovalue;

/*
 * Throw an exception by name
 */
static void JNU_ThrowByName(JNIEnv *env, const char *name, const char *msg) {
    jclass cls = (*env)->FindClass(env, name);
    if (cls != 0) {                         /* Otherwise an exception has already been thrown */
        (*env)->ThrowNew(env, cls, msg);
    }
    (*env)->DeleteLocalRef(env, cls);       /* It's a good practice to clean up the local references. */
}

/*
 * Test that jlongs are passed currectly
 */
jlong ltest(jlong p) {
    printf("ltest = %I64d\n", p);
    return p;
}

/*
 * Test that jfloats are passed currectly
 */
jfloat ftest(jfloat p) {
    printf("ftest = %f\n", p);
    return p;
}

/*
 * Test that jlongs are passed currectly
 */
jdouble dtest(jdouble p) {
    printf("dtest = %f\n", p);
    return p;
}

/*
 * Class:     com_sun_squawk_os_CSystem
 * Method:    lookup
 * Signature: (Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_com_sun_squawk_os_CSystem__1lookup(JNIEnv *env, jclass klass, jstring jstr) {
    const char *str = (*env)->GetStringUTFChars(env, jstr, NULL);
    int res = 0;
    if (str == NULL) {
        return 0; /* OutOfMemoryError already thrown */
    }

    //_asm { int 3 }
    //printf ("str = %s\n",str);
    if (strcmp(str, "printf") == 0) {
        res = (int)printf;
    }
    if (strcmp(str, "ltest") == 0) {
        res = (int)ltest;
    }
    if (strcmp(str, "ftest") == 0) {
        res = (int)ftest;
    }
    if (strcmp(str, "dtest") == 0) {
        res = (int)dtest;
    }
    (*env)->ReleaseStringUTFChars(env, jstr, str);
    return res;
}

/*
 * Class:     com_sun_squawk_os_CSystem
 * Method:    malloc
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_com_sun_squawk_os_CSystem__1malloc(JNIEnv *env, jclass klass, jint size) {
    return (jint)malloc(size);
}

/*
 * Class:     com_sun_squawk_os_CSystem
 * Method:    setByte
 * Signature: (II)V
 */
JNIEXPORT void JNICALL Java_com_sun_squawk_os_CSystem__1setByte(JNIEnv *env, jclass klass, jint addr , jint value) {
    ((char *)addr)[0] = (char)value;
}

/*
 * Class:     com_sun_squawk_os_CSystem
 * Method:    getByte
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_com_sun_squawk_os_CSystem__1getByte(JNIEnv *env, jclass klass, jint addr) {

    return ((char *)addr)[0] & 0xFF;
}




/*
 * Class:     com_sun_squawk_os_CSystem
 * Method:    free
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_com_sun_squawk_os_CSystem__1free(JNIEnv *env, jclass klass, jint addr) {
    free((void *)addr);
}



#define MAXPARMS 32

/*
 * Copies the parameters from the chain of Java objects into an integer buffer
 * and then calls the assembler dispatch routine to call the function.
 */
static void fncall(JNIEnv *env, jobject parms, jvalue* result, int resultType) {
    int buf[MAXPARMS];
    int i = 0;
    int fn      = (*env)->GetIntField(env, parms, FID_BaseParm_ivalue);
    int jnicall = (*env)->GetObjectField(env, parms, FID_BaseParm_ovalue) != NULL;
    parms = (*env)->GetObjectField(env, parms, FID_BaseParm_next);
    while (parms != NULL) {
        if (i == MAXPARMS) {
            JNU_ThrowByName(env, "java/lang/IllegalArgumentException", "too many arguments");
            return;
        }
        buf[i++] = (*env)->GetIntField(env, parms, FID_BaseParm_ivalue);
        parms = (*env)->GetObjectField(env, parms, FID_BaseParm_next);
    }
    (*env)->DeleteLocalRef(env, parms);
    asm_dispatch(fn, i, NULL, buf, resultType, result, jnicall);
}

/*
 * Class:     com_sun_squawk_os_CSystem
 * Method:    _icall
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_com_sun_squawk_os_CSystem__1icall(JNIEnv *env, jclass klass, jobject parms) {
    jvalue result;
    fncall(env, parms, &result, TY_INTEGER);
    return result.i;
}

/*
 * Class:     com_sun_squawk_os_CSystem
 * Method:    _lcall
 * Signature: (I)I
 */
JNIEXPORT jlong JNICALL Java_com_sun_squawk_os_CSystem__1lcall(JNIEnv *env, jclass klass, jobject parms) {
    jvalue result;
    fncall(env, parms, &result, TY_LONG);
    return result.j;
}

/*
 * Class:     com_sun_squawk_os_CSystem
 * Method:    _fcall
 * Signature: (I)I
 */
JNIEXPORT jfloat JNICALL Java_com_sun_squawk_os_CSystem__1fcall(JNIEnv *env, jclass klass, jobject parms) {
    jvalue result;
    fncall(env, parms, &result, TY_FLOAT);
    return result.f;
}

/*
 * Class:     com_sun_squawk_os_CSystem
 * Method:    _dcall
 * Signature: (I)I
 */
JNIEXPORT jdouble JNICALL Java_com_sun_squawk_os_CSystem__1dcall(JNIEnv *env, jclass klass, jobject parms) {
    jvalue result;
    fncall(env, parms, &result, TY_DOUBLE);
    return result.d;
}

/*
 * Class:     com_sun_squawk_os_CSystem
 * Method:    _ocall
 * Signature: (I)I
 */
JNIEXPORT jobject JNICALL Java_com_sun_squawk_os_CSystem__1ocall(JNIEnv *env, jclass klass, jobject parms) {
    jvalue result;
    fncall(env, parms, &result, TY_POINTER);
    return result.l;
}

/*
 * Class:     com_sun_squawk_os_CSystem
 * Method:    _pcall
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_com_sun_squawk_os_CSystem__1pcall(JNIEnv *env, jclass klass, jobject parms) {
    return (jint)Java_com_sun_squawk_os_CSystem__1ocall(env, klass, parms);
}

/*
 * Find a class
 */
static jclass findClass(JNIEnv *env, char *name) {
    jclass res = (*env)->FindClass(env, name);
    if (res == NULL) {
        printf("Cannot find class %s\n", name);
        exit(1);
    }
    return res;
}

/*
 * Find a field
 */
static jfieldID findField(JNIEnv *env, jclass cls, char *name, char *sig) {
    jfieldID res = (*env)->GetFieldID(env, cls, name, sig);
    if (res == NULL) {
        printf("Cannot find field %s\n", name);
        exit(1);
    }
    return res;
}

/*
 * Routine to calculate the endieness of jlongs
 */
static jlong longtest(p1) jlong p1; {
    return p1;
}

/*
 * Class:     com_sun_squawk_os_CSystem
 * Method:    _setup
 * Signature: (I)I
 *
 * This function return a jlong of 1 if the left-to-right ordering
 * (Java parameter order) is low word before high word.
 */
JNIEXPORT jlong JNICALL Java_com_sun_squawk_os_CSystem__1setup(JNIEnv *env, jclass cls) {
    Class_BaseParm       = findClass(env, "com/sun/squawk/os/BaseParm");
    FID_BaseParm_ivalue  = findField(env, Class_BaseParm, "ivalue", "I");
    FID_BaseParm_ovalue  = findField(env, Class_BaseParm, "ovalue", "Ljava/lang/Object;");
    FID_BaseParm_next    = findField(env, Class_BaseParm, "next", "Lcom/sun/squawk/os/BaseParm;");
    return longtest(1, 0);

}


