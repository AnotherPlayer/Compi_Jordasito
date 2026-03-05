section .text
    global _start

_start:
    ; --- SUMA (10 + 5) ---
    mov eax, 10
    add eax, 5          ; EAX = 15

    ; --- RESTA (15 - 3) ---
    sub eax, 3          ; EAX = 12

    ; --- MULTIPLICACIÓN (12 * 2) ---
    mov ebx, 2
    imul ebx            ; EAX = EAX * EBX = 24

    ; --- DIVISIÓN Y MÓDULO (24 / 7) ---
    mov eax, 24         ; Dividendo
    cdq                 ; Extiende EAX a EDX:EAX para la división
    mov ebx, 7          ; Divisor
    idiv ebx            ; Cociente en EAX (3), Residuo en EDX (3)

    ; Finalizar programa
    mov eax, 60         ; syscall: exit
    xor edi, edi        ; status: 0
    syscall