/*
 * Copyright 1994-2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 */

/*
 * Copies the arguments from the given array to C stack, invoke the
 * target function, and copy the result back.
 */
void asm_dispatch(int func, int nwords, char *args_types, int *args, int res_type, void *result, int conv) {
    __asm {
        push esi
        mov esi, args
        mov ecx, nwords
        shl ecx, 2                      // word address -> byte address
        sub ecx, 4
        jc  args_done

    args_loop:                          // Push the last argument first.
        mov eax, DWORD PTR [esi+ecx]
        push eax
        sub ecx, 4
        jge SHORT args_loop

    args_done:
        //int 3
        mov eax, func
        call eax

        mov ecx, conv
        or ecx, ecx
        jnz stdcall
        mov ecx, nwords                 // Pop arguments when using the 'C' calling convention
        shl ecx, 2
        add esp, ecx
    stdcall:

        mov esi, result
        mov ecx, res_type
        dec ecx
        jge not_p32

        mov [esi], eax                  // p32
        jmp done

    not_p32:
        dec ecx
        jge not_i32

        mov [esi], eax                  // i32
        jmp done

    not_i32:
        dec ecx
        jge not_f32

        fstp DWORD PTR [esi]            // f32
        jmp done

    not_f32:
        dec ecx
        jge not_i64

        mov [esi], eax                  // i64
        mov [esi+4], edx
        jmp done

    not_i64:
        fstp QWORD PTR [esi]            // f64

    done:
        pop esi
    }
}
