.class jasmin/JasminTests                                 ; tests clearing of locals
.super java/lang/Object


.method public static main2([Ljava/lang/String;)V
    .limit stack 2
    .limit locals 1

    invokestatic jasmin/JasminTests/run3()V
    iconst_0
    istore_0

start:
    iload_0
    ldc 10
    if_icmpeq finish
    invokestatic jasmin/JasminTests/run()V
    iinc 0 1
    goto start
    
finish:
    return
.end method


.method private static run()V
    .limit locals 2
    .limit stack 4

    getstatic java/lang/System/out Ljava/io/PrintStream;
    astore_1
    iconst_2
    istore_0
    aload_1
    iload_0
    invokevirtual java/io/PrintStream/println(I)V
    ldc "returning"
    astore_0
    aload_1
    aload_0
    invokevirtual java/io/PrintStream/print(Ljava/lang/String;)V
    return
.end method

.method private static run2()V
    .limit stack 2
    .limit locals 1

    iconst_0
    istore_0
    iload_0
    ifne label1
;   { 
        aconst_null
        astore_0
        goto merge
;   } else {
label1:
        invokestatic jasmin/JasminTests/getObject()Ljava/lang/Object;
        astore_0
;   }
merge:
    getstatic java/lang/System/out Ljava/io/PrintStream;
    aload_0
    invokevirtual java/io/PrintStream/println(Ljava/lang/Object;)V
    return
.end method

.method private static getObject()Ljava/lang/Object;
    .limit stack 1
    
    aconst_null
    areturn
.end method


.method private static run3()V
    .limit stack 2
    .limit locals 1

    iconst_0
    istore_0
    goto test
    
head:
    iload_0
    pop
    aconst_null
    astore_0
    goto end
    
test:
    invokestatic java/lang/VM/collectGarbage()V  ; may cause GC
    goto head

end:
    return
.end method

.method private static run4()V
    .limit stack 2
    .limit locals 3

    iconst_0
    istore_0
    goto Label0
Label1:
        iinc 0 1
        iconst_0
        istore_1
        goto Label3
Label2:
            iinc 1 1
            ldc "a string"
            astore_2
Label3:
        iload_1
        bipush 100
        if_icmplt Label2
    
Label0:
    getstatic java/lang/System/out Ljava/io/PrintStream;
    invokevirtual java/io/PrintStream/println()V
    iload_0
    bipush 100
    if_icmplt Label1
    return

.end method

.method private static run5()V
    .limit stack 2
    .limit locals 3

    iconst_0
    istore_0
    goto Label0
Label1:
        iinc 0 1
        iconst_0
        istore_1
        goto Label3
Label2:
            iinc 1 1
Label3:
        iload_1
        bipush 100
        if_icmplt Label2
    
Label0:
    ldc "a string"
    astore_2
    getstatic java/lang/System/out Ljava/io/PrintStream;
    invokevirtual java/io/PrintStream/println()V
    iload_0
    bipush 100
    if_icmplt Label1
    return

.end method

.method public static main([Ljava/lang/String;)V
    .limit stack 2
    .limit locals 1

    getstatic java/lang/System/out Ljava/io/PrintStream;
    ldc "Hello squawk"
    invokevirtual java/io/PrintStream/println(Ljava/lang/String;)V
    return

.end method
