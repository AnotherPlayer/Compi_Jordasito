section .data
    msg db 'Iteracion: ', 0
    msg_len equ $ - msg
    newline db 10                  ; Salto de línea (ASCII 10)

section .bss
    num resb 1                     ; Espacio para un carácter

section .text
    global _start

_start:
    mov rcx, 1                     ; Inicializamos nuestro contador en 1

ciclo:
    ; --- Bloque para imprimir "Iteracion: " ---
    push rcx                       ; Guardamos rcx (el sistema puede alterarlo)
    mov rax, 1                     ; syscall: sys_write
    mov rdi, 1                     ; file descriptor: stdout
    mov rsi, msg                   ; dirección del mensaje
    mov rdx, msg_len               ; longitud del mensaje
    syscall

    ; --- Bloque para imprimir el número actual ---
    pop rcx                        ; Recuperamos nuestro contador
    mov rax, rcx                   ; Movemos el contador a rax
    add rax, '0'                   ; Convertimos el número a su valor ASCII
    mov [num], al                  ; Lo movemos a memoria

    push rcx                       ; Volvemos a guardar rcx
    mov rax, 1
    mov rdi, 1
    mov rsi, num
    mov rdx, 1
    syscall

    ; Imprimir salto de línea
    mov rax, 1
    mov rdi, 1
    mov rsi, newline
    mov rdx, 1
    syscall
    pop rcx                        ; Recuperamos rcx para la lógica del bucle

    ; --- Lógica del Bucle y Condicional ---
    inc rcx                        ; Incrementamos el contador (rcx++)
    cmp rcx, 6                     ; ¿Es rcx igual a 6? (Condicional)
    jne ciclo                      ; Si NO es igual (Jump if Not Equal), vuelve a 'ciclo'

exit:
    ; Terminar el programa
    mov rax, 60                    ; syscall: sys_exit
    xor rdi, rdi                   ; error code 0
    syscall