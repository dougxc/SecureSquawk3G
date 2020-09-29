#
# void asm_dispatch(int func, int nwords, char *args_types, int *args, int res_type, void *result, int conv);
#		32(%ebp) is conv
#		28(%ebp) is result
#		24(%ebp) is res_type
#		20(%ebp) is args
#		16(%ebp) is arg_types
#		12(%ebp) is nwords
#		8(%ebp)  is func
#
#
# Bug 1: args_type is not checked when arguments are pushed onto stack. 64bit arguments
# (e.g. double and long long) are treated as 32bit arguments.
#
# Bug 2: What is if the function does not return a value. In the orginal code this case
# was ignored (at the moment if result_type > 5).
#

	.text
.globl asm_dispatch
	.type	asm_dispatch,@function
asm_dispatch:
	pushl	%ebp
	movl	%esp, %ebp

        pushl %esi
        movl 20(%ebp),%esi 
        movl 12(%ebp),%ecx 
        shll $2,%ecx                   # word address -> byte address
        subl $4,%ecx
        jc  args_done

    args_loop:                         # Push the last argument first.
        movl (%esi,%ecx),%eax
        pushl %eax
        subl $4,%ecx
        jge args_loop

    args_done:
        # int 3
        movl 8(%ebp),%eax
        call *%eax

        movl 32(%ebp),%ecx 
        orl %ecx,%ecx
        jnz stdcall

        movl 12(%ebp),%ecx             # Pop arguments when using the 'C' calling convention
        shll $2,%ecx
        addl %ecx,%esp

    stdcall:

        movl 28(%ebp),%esi
        movl 24(%ebp),%ecx
        decl %ecx
        jge not_p32

        movl %eax,(%esi)               # res_type=0 => result is pointer (32bit) 
        jmp done

    not_p32:
        decl %ecx
        jge not_i32

        movl %eax,(%esi)               # res_type=1 => result is integer (32bit)
        jmp done

    not_i32:
        decl %ecx
        jge not_f32

        fstps (%esi)                   # res_type=2 => result is floating point number (32bit)
        jmp done

    not_f32:
        decl %ecx
        jge not_i64

        movl %eax,(%esi)               # res_type=3 => result is integer (64bit)
        movl %edx,4(%esi)              
        jmp done

    not_i64:
        decl %ecx
        jge done

        fstpl (%esi)                   # res_type=4 => result is floating point number (64bit) 

    done:
        popl %esi

	leave
	ret
.Lfe1:
	.size	asm_dispatch,.Lfe1-asm_dispatch
	.ident	"SQUAWK Project (Dispatcher)"

