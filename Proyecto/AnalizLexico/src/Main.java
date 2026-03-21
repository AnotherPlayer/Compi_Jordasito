import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    public static void escanearCodigo(String rutaArchivo) {
        String codigoFuente;
        try {
            // Leemos todo el contenido del archivo a un String
            codigoFuente = new String(Files.readAllBytes(Paths.get(rutaArchivo)));
        } catch (IOException e) {
            System.out.println("Error: No se pudo encontrar o leer el archivo '" + rutaArchivo + "'.");
            return;
        }

        // 1. DEFINIMOS LAS REGLAS DEL AUTÓMATA
        Map<String, String> reglasTokens = new LinkedHashMap<>();
        //reglasTokens.put("BLOCKCOMMENT", "/\\*[\\s\\S]*?\\*/"); // Sin guion bajo
        //reglasTokens.put("LINECOMMENT", "//.*");               // Sin guion bajo
        
        reglasTokens.put("BOOLLITERAL", "\\b(true|false)\\b"); // Sin guion bajo
        reglasTokens.put("IF", "if");
        reglasTokens.put("ELSE", "else");
        reglasTokens.put("WHILE", "while");
        reglasTokens.put("FOR", "for");
        reglasTokens.put("RETURN", "return");
        reglasTokens.put("INT", "int");
        reglasTokens.put("FLOAT", "float");
        reglasTokens.put("VOID", "void");
        reglasTokens.put("BOOL", "bool");
        reglasTokens.put("BYTE", "byte");
        reglasTokens.put("SHORT", "short");
        reglasTokens.put("LONG", "long");
        reglasTokens.put("DOUBLE", "double");
        reglasTokens.put("CHAR", "char");
        reglasTokens.put("STRING", "String");
        reglasTokens.put("FLOATLITERAL", "\\d+\\.\\d+");       // Sin guion bajo
        reglasTokens.put("INTLITERAL", "\\d+");                // Sin guion bajo
        reglasTokens.put("STRINGLITERAL", "\".*?\"");          // Sin guion bajo
        reglasTokens.put("OPERATOR", "[+\\-*/=<>!]+");
        reglasTokens.put("LPAREN", "\\(");
        reglasTokens.put("RPAREN", "\\)");
        reglasTokens.put("IBRACES", "\\{");
        reglasTokens.put("DBRACES", "\\}");
        reglasTokens.put("ISQUARE", "\\[");
        reglasTokens.put("DSQUARE", "\\]");
        reglasTokens.put("COMA", "\\,");
        reglasTokens.put("DOT", "\\.");
        reglasTokens.put("SEMICOLON", "\\;");
        reglasTokens.put("PUBLIC", "public");
        reglasTokens.put("PRIVATE", "private");
        reglasTokens.put("STATIC", "static");
        reglasTokens.put("CLASS", "class");
        reglasTokens.put("THROWS", "throws");
        reglasTokens.put("EXCEPTION", "Exception");
        reglasTokens.put("TRY", "try");
        reglasTokens.put("CATCH", "catch");

        reglasTokens.put("ID", "[a-zA-Z_][a-zA-Z0-9_]*");
        reglasTokens.put("WHITESPACE", "\\s+");
        reglasTokens.put("UNKNOWN", ".");

        // Unimos todas las reglas en la Súper Expresión Regular
        StringBuilder regexCombinada = new StringBuilder();
        for (Map.Entry<String, String> regla : reglasTokens.entrySet()) {
            if (regexCombinada.length() > 0) {
                regexCombinada.append("|");
            }
            // Agregamos el grupo con nombre: (?<Nombre>patron)
            regexCombinada.append(String.format("(?<%s>%s)", regla.getKey(), regla.getValue()));
        }

        // Compilamos la expresión (El Autómata)
        Pattern analizador = Pattern.compile(regexCombinada.toString());
        Matcher coincidencia = analizador.matcher(codigoFuente);

        System.out.println("--- Iniciando Análisis Léxico del archivo: " + rutaArchivo + " ---\n");

        int numeroLinea = 1;

        // 2. EL BUCLE DE ESCANEO
        while (coincidencia.find()) {
            String tipoToken = null;
            String lexema = coincidencia.group();

            // A diferencia de Python, Java no tiene un ".lastgroup" nativo directo,
            // así que iteramos para ver qué grupo hizo "match" y no es nulo.
            for (String nombreRegla : reglasTokens.keySet()) {
                if (coincidencia.group(nombreRegla) != null) {
                    tipoToken = nombreRegla;
                    break;
                }
            }

            // Contar saltos de línea para llevar registro
            if (lexema.contains("\n")) {
                numeroLinea += lexema.chars().filter(ch -> ch == '\n').count();
            }

            // Ignoramos espacios y comentarios
            if (tipoToken.equals("WHITESPACE") || tipoToken.equals("LINE_COMMENT") || tipoToken.equals("BLOCK_COMMENT")) {
                continue;
            }
            // Manejo de errores léxicos
            else if (tipoToken.equals("UNKNOWN")) {
                System.out.printf("[Linea %d] LEXICAL ERROR: Caracter no reconocido '%s'\n", numeroLinea, lexema);
            }
            // Imprimir token exitoso
            else {
                System.out.printf("<%s | %s>\n", tipoToken, lexema);
            }
        }

        System.out.println("\n--- Análisis Léxico Terminado ---");

    }

    // 3. PUNTO DE ENTRADA DEL PROGRAMA
    public static void main(String[] args) {
        // En Java los argumentos de la línea de comandos (args) no incluyen el nombre del script
        if (args.length < 1) {
            System.out.println("Uso correcto: java ScannerLexico <archivo_codigo.txt>");
        } else {
            String archivoAAnalizar = args[0];
            escanearCodigo(archivoAAnalizar);
        }
    }
}