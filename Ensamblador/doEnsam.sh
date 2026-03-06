#!/bin/bash

if [ -z "$1" ]; then
    echo "Escrbir el nombre de un programa"
    exit 1
fi

if [ ! -d "$1" ]; then
    echo "No se encontró la carpeta '$1'"
    exit 1
fi

echo "Entrando a la carpeta $1"
cd "$1"

#Compilando
echo "--------------------"
echo "Creando object ..."
if [ -f "$1.o" ]; then
    echo "Eliminando archivo .o (object)"
    rm "$1.o"
fi

nasm -f elf64 "$1.asm" -o "$1.o"

#Enlazando
echo "--------------------"
echo "Creando ejecutable ..."
if [ -f "$1.exe" ]; then
    echo "Eliminando archivo .exe (ejecutable)"
    rm "$1.exe"
fi

ld "$1.o" -o "$1.exe"

#Ejecutando
echo "--------------------"
echo "Ejecutando ..."
if [ $? -eq 0 ]; then
    echo "Ejecutando $1:"
    echo "--------------------"
    ./"$1.exe"
else
    echo "Error: La compilación falló y no se generó un nuevo archivo."
fi