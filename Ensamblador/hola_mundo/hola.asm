section .data
    msg db '¡Hola Mundo!', 0xa  ; Cadena con salto de línea
    len equ $ - msg             ; Calcular longitud

section .text
    global _start

_start:
    ; sys_write (4)
    mov eax, 4
    mov ebx, 1
    mov ecx, msg
    mov edx, len
    int 0x80

    ; sys_exit (1)
    mov eax, 1
    mov ebx, 0
    int 0x80
