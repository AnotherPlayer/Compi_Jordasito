section .data
    msg1 db "Numero 1: ", 0
    len1 equ $ - msg1
    msg2 db "Numero 2: ", 0
    len2 equ $ - msg2
    
    txt_sum db 10, "Suma: ", 0
    txt_res db 10, "Resta: ", 0
    txt_mul db 10, "Multiplicacion: ", 0
    txt_div db 10, "Division (Cociente): ", 0
    txt_mod db 10, "Modulo (Residuo): ", 0
    newline db 10, 0 ; El carácter de salto de línea
    sign_minus db "-" ; Agregado para imprimir el signo negativo

section .bss
    buffer resb 32    
    n1     resq 1     
    n2     resq 1     
    temp   resb 32    

section .text
    global _start

_start:
    ; --- Leer Num 1 ---
    mov rsi, msg1
    mov rdx, len1
    call print_string
    call read_and_convert
    mov [n1], rax

    ; --- Leer Num 2 ---
    mov rsi, msg2
    mov rdx, len2
    call print_string
    call read_and_convert
    mov [n2], rax

    ; --- SUMA ---
    mov rsi, txt_sum
    mov rdx, 7
    call print_string
    mov rax, [n1]
    add rax, [n2]
    call print_number

    ; --- RESTA ---
    mov rsi, txt_res
    mov rdx, 8
    call print_string
    
    mov rax, [n1]
    mov rbx, [n2]
    cmp rax, rbx        ; Comparamos n1 con n2
    jge .resta_positiva ; Si n1 >= n2, saltamos a la resta normal
    
    ; Si n1 < n2 (Resultado negativo)
    push rax            ; Guardamos rax para no perderlo
    mov rax, 1          ; Preparar syscall write
    mov rdi, 1          ; stdout
    mov rsi, sign_minus ; Usamos la variable para el signo '-'
    mov rdx, 1          ; Longitud 1
    syscall
    pop rax             ; Recuperamos n1
    
    sub rbx, rax        ; Hacemos la resta inversa (n2 - n1) para obtener el valor absoluto
    mov rax, rbx
    jmp .imprimir_resta

.resta_positiva:
    sub rax, rbx        ; n1 - n2 normal

.imprimir_resta:
    call print_number

    ; --- MULTIPLICACIÓN ---
    mov rsi, txt_mul
    mov rdx, 17
    call print_string
    mov rax, [n1]
    imul rax, [n2]
    call print_number

    ; --- DIVISIÓN Y MÓDULO ---
    mov rsi, txt_div
    mov rdx, 22
    call print_string
    mov rax, [n1]
    xor rdx, rdx    
    mov rbx, [n2]
    div rbx         
    push rdx        
    call print_number

    mov rsi, txt_mod
    mov rdx, 18
    call print_string
    pop rax         
    call print_number

    ; --- SALTO DE LÍNEA FINAL ---
    mov rax, 1          ; sys_write
    mov rdi, 1          ; stdout
    mov rsi, newline    ; dirección del carácter 10
    mov rdx, 1          ; longitud 1
    syscall

    ; --- Salir ---
    mov rax, 60
    xor rdi, rdi
    syscall

; --- FUNCIONES AUXILIARES (Sin cambios) ---

read_and_convert:
    mov rax, 0
    mov rdi, 0
    mov rsi, buffer
    mov rdx, 32
    syscall
    xor rax, rax
    mov rsi, buffer
.loop:
    movzx rcx, byte [rsi]
    cmp rcx, 10
    je .done
    sub rcx, '0'
    imul rax, 10
    add rax, rcx
    inc rsi
    jmp .loop
.done: ret

print_number:
    mov rsi, temp + 31
    mov byte [rsi], 0
    mov rbx, 10
.loop:
    xor rdx, rdx
    div rbx
    add dl, '0'
    dec rsi
    mov [rsi], dl
    test rax, rax
    jnz .loop
    mov rdx, temp + 31
    sub rdx, rsi
    mov rax, 1
    mov rdi, 1
    syscall
    ret

print_string:
    mov rax, 1
    mov rdi, 1
    syscall
    ret