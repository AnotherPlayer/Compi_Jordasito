package Proyecto.AnalizSintactico;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// --- 1. Definición de Tokens ---
enum TokenType {
    NUMERO, IDENTIFICADOR, SUMA, RESTA, MULTIPLICACION, DIVISION, PARENTESIS_IZQ, PARENTESIS_DER,
    ASIGNACION, PUNTO_COMA, MENOR_QUE, MAYOR_QUE, LLAVE_IZQ, LLAVE_DER, CADENA_TEXTO
}

class Token {
    TokenType tipo;
    String valor;

    public Token(TokenType tipo, String valor) {
        this.tipo = tipo;
        this.valor = valor;
    }
}

// --- 2. Analizador Léxico (Lexer) ---
class AnalizadorLexico {
    private String codigoFuente;
    private int posicionActual = 0;

    public AnalizadorLexico(String codigoFuente) {
        this.codigoFuente = codigoFuente;
    }

    public List<Token> escanearTokens() {
        List<Token> tokens = new ArrayList<>();

        while (posicionActual < codigoFuente.length()) {
            char c = codigoFuente.charAt(posicionActual);

            // Ignorar espacios y saltos de línea
            if (Character.isWhitespace(c)) {
                posicionActual++;
                continue;
            }

            // Detectar Números
            if (Character.isDigit(c)) {
                StringBuilder numero = new StringBuilder();
                while (posicionActual < codigoFuente.length() && Character.isDigit(codigoFuente.charAt(posicionActual))) {
                    numero.append(codigoFuente.charAt(posicionActual));
                    posicionActual++;
                }
                tokens.add(new Token(TokenType.NUMERO, numero.toString()));
                continue;
            }

            // Detectar Identificadores (Variables simples de 1 letra para probar)
            if (Character.isLetter(c)) {
                StringBuilder id = new StringBuilder();
                while (posicionActual < codigoFuente.length() && Character.isLetterOrDigit(codigoFuente.charAt(posicionActual))) {
                    id.append(codigoFuente.charAt(posicionActual));
                    posicionActual++;
                }
                tokens.add(new Token(TokenType.IDENTIFICADOR, id.toString()));
                continue;
            }

            // Detectar Cadenas de Texto (ej. "Hola we\n")
            if (c == '"') {
                StringBuilder cadena = new StringBuilder();
                posicionActual++; // Saltamos la primera comilla

                // Leemos todo hasta encontrar la otra comilla
                while (posicionActual < codigoFuente.length() && codigoFuente.charAt(posicionActual) != '"') {
                    cadena.append(codigoFuente.charAt(posicionActual));
                    posicionActual++;
                }

                tokens.add(new Token(TokenType.CADENA_TEXTO, cadena.toString()));
                posicionActual++; // Saltamos la comilla final
                continue; // Volvemos al inicio del while principal
            }

            // Detectar Operadores y Símbolos
            switch (c) {
                case '+': tokens.add(new Token(TokenType.SUMA, "+")); break;
                case '-': tokens.add(new Token(TokenType.RESTA, "-")); break;
                case '*': tokens.add(new Token(TokenType.MULTIPLICACION, "*")); break;
                case '/': tokens.add(new Token(TokenType.DIVISION, "/")); break;
                case '(': tokens.add(new Token(TokenType.PARENTESIS_IZQ, "(")); break;
                case ')': tokens.add(new Token(TokenType.PARENTESIS_DER, ")")); break;
                case '=': tokens.add(new Token(TokenType.ASIGNACION, "=")); break;
                case ';': tokens.add(new Token(TokenType.PUNTO_COMA, ";")); break;
                case '<': tokens.add(new Token(TokenType.MENOR_QUE, "<")); break;
                case '>': tokens.add(new Token(TokenType.MAYOR_QUE, ">")); break;
                case '{': tokens.add(new Token(TokenType.LLAVE_IZQ, "{")); break;
                case '}': tokens.add(new Token(TokenType.LLAVE_DER, "}")); break;
                default:
                    throw new RuntimeException("Error Léxico: Carácter no reconocido '" + c + "' en la posición " + posicionActual);
            }
            posicionActual++;
        }

        return tokens;
    }
}

// --- 3. Analizador Sintáctico (Parser) ---
class Parser {
    private List<Token> tokens;
    private int posicionActual = 0;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    public void parse() {
        parseExpresion();
        // Si termina sin lanzar excepciones y consumió todos los tokens (o llegó al final de la expresión válida)
        System.out.println("¡Análisis sintáctico exitoso! La estructura es correcta.");
    }

    private void parseExpresion() {
        parseTermino();
        while (match(TokenType.SUMA) || match(TokenType.RESTA)) {
            parseTermino();
        }
    }

    private void parseTermino() {
        parseFactor();
        while (match(TokenType.MULTIPLICACION) || match(TokenType.DIVISION)) {
            parseFactor();
        }
    }

    private void parseFactor() {
        if (match(TokenType.NUMERO)) {
            System.out.println("  -> Nodo hoja: Número (" + tokenAnterior().valor + ")");
        } else if (match(TokenType.IDENTIFICADOR)) {
            System.out.println("  -> Nodo hoja: Identificador (" + tokenAnterior().valor + ")");
        } else if (match(TokenType.PARENTESIS_IZQ)) {
            System.out.println("  -> Entrando a sub-expresión '('");
            parseExpresion();
            consume(TokenType.PARENTESIS_DER, "Error Sintáctico: Se esperaba ')'");
            System.out.println("  -> Saliendo de sub-expresión ')'");
        } else {
            throw new RuntimeException("Error Sintáctico: Token inesperado en la posición " + posicionActual);
        }
    }

    // --- Métodos Auxiliares del Parser ---
    private boolean match(TokenType tipoEsperado) {
        if (posicionActual < tokens.size() && tokens.get(posicionActual).tipo == tipoEsperado) {
            posicionActual++;
            return true;
        }
        return false;
    }

    private void consume(TokenType tipoEsperado, String mensajeError) {
        if (!match(tipoEsperado)) {
            throw new RuntimeException(mensajeError);
        }
    }

    private Token tokenAnterior() {
        return tokens.get(posicionActual - 1);
    }
}

// --- 4. Clase Principal ---
public class Main {
    public static void main(String[] args) {
        // Verificar argumentos
        if (args.length == 0) {
            System.err.println("Error: No se proporcionó ningún archivo de entrada.");
            System.out.println("Uso correcto: java Compilador <ruta_del_archivo.txt>");
            return;
        }

        String rutaArchivo = args[0];
        String codigoFuente = "";

        // Leer el archivo
        try {
            codigoFuente = Files.readString(Paths.get(rutaArchivo));
        } catch (IOException e) {
            System.err.println("Error al intentar leer el archivo '" + rutaArchivo + "': " + e.getMessage());
            return;
        }

        System.out.println("=== INICIANDO COMPILACIÓN ===");
        System.out.println("Archivo: " + rutaArchivo);
        System.out.println("Contenido:\n" + codigoFuente.trim());
        System.out.println("-----------------------------");

        try {
            // Fase 1: Análisis Léxico
            AnalizadorLexico lexer = new AnalizadorLexico(codigoFuente);
            List<Token> tokens = lexer.escanearTokens();
            System.out.println("[OK] Análisis Léxico completado. Tokens generados: " + tokens.size());

            // Fase 2: Análisis Sintáctico
            Parser parser = new Parser(tokens);
            parser.parse();

        } catch (Exception e) {
            System.err.println("\n[X] FALLO EN LA COMPILACIÓN: " + e.getMessage());
        }
    }
}