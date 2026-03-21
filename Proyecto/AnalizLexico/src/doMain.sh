#!/bin/bash

# 1. Validar que se haya pasado exactamente un argumento (el programa Java)
if [ -z "$1" ]; then
    echo "Error: Falta el nombre del programa."
    echo "Uso correcto: ./doMain.sh <NombrePrograma>"
    exit 1
fi

PROGRAMA="$1"
# Definimos el nombre del archivo de texto internamente
ARCHIVO_TXT="archivo_codigo.txt" 

# 2. Verificar que el archivo .java exista en el directorio actual
if [ ! -f "${PROGRAMA}.java" ]; then
    echo "Error: No se encontró el código fuente '${PROGRAMA}.java'"
    exit 1
fi

# 3. Verificar que el archivo de texto predefinido exista
if [ ! -f "$ARCHIVO_TXT" ]; then
    echo "Error: No se encontró el archivo de texto '$ARCHIVO_TXT'. Por favor, créalo antes de ejecutar."
    exit 1
fi

# 4. Compilando el archivo Java
echo "--------------------"
echo "Compilando ${PROGRAMA}.java ..."

# Limpiar el archivo .class si existe para asegurar una compilación limpia
if [ -f "${PROGRAMA}.class" ]; then
    echo "Eliminando archivo .class anterior..."
    rm "${PROGRAMA}.class"
fi

javac "${PROGRAMA}.java"

# 5. Ejecutando el programa si la compilación fue exitosa
echo "--------------------"
if [ $? -eq 0 ]; then
    echo "Ejecutando ${PROGRAMA} usando '${ARCHIVO_TXT}':"
    echo "--------------------"
    # Ejecutamos pasando el archivo fijo como argumento
    java "${PROGRAMA}" "$ARCHIVO_TXT"
else
    echo "Error: La compilación falló. Revisa la sintaxis de tu código."
fi