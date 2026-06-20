section .data
    fmt_int db "%d", 10, 0
    str1 db "Hola main", 10, 0
    str2 db "Hola mundo", 10, 0

section .bss
    x resd 1

section .text
    global main
    extern printf

main:
    push rbp
    mov rbp, rsp

    mov rdi, str1
    mov al, 0
    call printf

    call saludar

    call sumar

    mov eax, 0
    mov rsp, rbp
    pop rbp
    ret

saludar:
    push rbp
    mov rbp, rsp

    mov rdi, str2
    mov al, 0
    call printf

    mov rsp, rbp
    pop rbp
    ret

sumar:
    push rbp
    mov rbp, rsp

    push 5
    push 3
    push 2
    pop ebx
    pop eax
    imul eax, ebx
    push eax
    pop ebx
    pop eax
    add eax, ebx
    push eax
    pop eax
    mov dword [x], eax

    mov rsp, rbp
    pop rbp
    ret

