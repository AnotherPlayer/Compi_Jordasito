import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    enum TokenType {
        DATATYPE, KEYWORD, IDENTIFIER, NUMBER, OPERATOR, STRING,
        SEMICOLON, COLON, COMMA, LPAREN, RPAREN, LBRACE, RBRACE, ANNOTATION, EOF, UNKNOWN,
        IF, ELSE, FOR, WHILE, SWITCH, WHEN, RETURN, ENUM, IMPORT, PACKAGE, CLASS
    }

    static class Token {
        TokenType type;
        String value;
        Token(TokenType type, String value) { this.type = type; this.value = value; }
        @Override
        public String toString() { return "[" + type + ": " + value + "]"; }
    }

    private final List<Token> tokens;
    private int current = 0;

    private final Set<String> tablaSimbolos = new HashSet<>();
    private final Set<String> libreriasImportadas = new HashSet<>();

    public Main(List<Token> tokens) { this.tokens = tokens; }

    public void analizar() {
        if (tokens.isEmpty() || tokens.get(0).type == TokenType.EOF) {
            System.out.println("El archivo está vacío o solo contiene comentarios.");
            return;
        }
        try {
            System.out.println("--- INICIANDO ANALISIS ---");
            while (!isAtEnd()) statement();
            System.out.println("\n>>> EXITO: El código es sintácticamente correcto.");
            System.out.println("Variables en memoria: " + tablaSimbolos);
        } catch (RuntimeException e) {
            System.err.println("\n>>> ERROR SINTACTICO/SEMANTICO: " + e.getMessage());
        }
    }

    private void statement() {
        if (isAtEnd()) return;

        while (check(TokenType.ANNOTATION)) advance();

        List<String> modificadores = new ArrayList<>();
        while (checkKeyword("public") || checkKeyword("private") || checkKeyword("protected") || checkKeyword("static")) {
            modificadores.add(advance().value);
        }

        if (match(TokenType.CLASS)) {
            handleClass(modificadores);
        } else if (match(TokenType.DATATYPE)) {
            String tipoDato = previous().value;
            Token id = consume(TokenType.IDENTIFIER, "Falta el identificador despues de '" + tipoDato + "'");
            if (check(TokenType.LPAREN)) declaracionMetodo(modificadores, tipoDato, id);
            else declaracionVariable(modificadores, tipoDato, id);
        } else if (!modificadores.isEmpty()) {
            throw new RuntimeException("Modificador suelto sin clase o variable: " + peek().value);
        } else if (check(TokenType.IDENTIFIER)) {
            Token id = advance();
            if (check(TokenType.LPAREN)) {
                llamadaMetodoLocal(id);
            } else {
                operacionVariable(id);
            }
        } else if (match(TokenType.IF)) {
            handleIf();
        } else if (match(TokenType.FOR)) {
            handleFor();
        } else if (match(TokenType.WHILE)) {
            handleWhile();
        } else if (match(TokenType.ENUM)) {
            handleEnum();
        } else if (match(TokenType.IMPORT)) {
            handleImport();
        } else if (match(TokenType.PACKAGE)) {
            handlePackage();
        } else if (match(TokenType.RETURN)) {
            ignorarHastaPuntoYComa();
        } else if (match(TokenType.KEYWORD)) {
            ignorarHastaPuntoYComa();
        } else if (match(TokenType.LBRACE)) {
            handleBody();
        } else {
            advance();
        }
    }

    private void llamadaMetodoLocal(Token id) {
        String nombreMetodo = id.value;
        consume(TokenType.LPAREN, "Falta '(' al llamar a " + nombreMetodo);
        skipToClosingParen();
        consume(TokenType.SEMICOLON, "Falta ';' al final de la instruccion");
        if (nombreMetodo.equals("System.out.println") || nombreMetodo.equals("System.out.print")) {
            System.out.println("   -> [CONSOLA] Impresion detectada: " + nombreMetodo + "(...)");
        } else {
            System.out.println("   -> [LLAMADA METODO] Ejecutando: " + nombreMetodo + "(...)");
        }
    }

    private void operacionVariable(Token id) {
        String nombreVariable = id.value;
        boolean usaThis = false;
        if (nombreVariable.startsWith("this.")) {
            nombreVariable = nombreVariable.substring(5);
            usaThis = true;
        }
        if (!tablaSimbolos.contains(nombreVariable)) {
            throw new RuntimeException("Coherencia: Uso de variable no declarada -> '" + nombreVariable + "'");
        }
        while (!check(TokenType.SEMICOLON) && !isAtEnd()) advance();
        consume(TokenType.SEMICOLON, "Falta ';' después de usar la variable");
        if (usaThis) System.out.println("   -> [USO THIS] Acceso a: this." + nombreVariable);
        else System.out.println("   -> [USO VAR] Operación con: " + nombreVariable);
    }

    private void declaracionVariable(List<String> modificadores, String tipoDato, Token id) {
        if (id.value.startsWith("this.")) throw new RuntimeException("No puedes declarar una variable usando 'this.' (" + id.value + ")");
        if (tablaSimbolos.contains(id.value)) throw new RuntimeException("La variable '" + id.value + "' ya existe.");
        tablaSimbolos.add(id.value);
        if (check(TokenType.OPERATOR) && peek().value.equals("=")) {
            advance();
            while (!check(TokenType.SEMICOLON) && !isAtEnd()) advance();
        }
        consume(TokenType.SEMICOLON, "Falta ';'");
        System.out.println("   -> [VARIABLE] " + tipoDato + " " + id.value);
    }

    private void handleClass(List<String> modificadores) {
        Token id = consume(TokenType.IDENTIFIER, "Se esperaba el nombre de la clase");
        if (tablaSimbolos.contains(id.value)) throw new RuntimeException("El nombre de la clase ya está en uso.");
        tablaSimbolos.add(id.value);
        System.out.println("   -> [CLASE] " + id.value);
        consume(TokenType.LBRACE, "Falta '{'");
        while (!check(TokenType.RBRACE) && !isAtEnd()) statement();
        consume(TokenType.RBRACE, "Falta '}'");
    }

    private void declaracionMetodo(List<String> modificadores, String tipoDato, Token id) {
        System.out.println("   -> [METODO] " + tipoDato + " " + id.value + "()");
        consume(TokenType.LPAREN, "Falta '('"); skipToClosingParen();
        consume(TokenType.LBRACE, "Falta '{'");
        while (!check(TokenType.RBRACE) && !isAtEnd()) statement();
        consume(TokenType.RBRACE, "Falta '}'");
    }

    private void handleEnum() {
        Token id = consume(TokenType.IDENTIFIER, "Se esperaba nombre del enum");
        tablaSimbolos.add(id.value);
        consume(TokenType.LBRACE, "Falta '{'");
        if (!check(TokenType.RBRACE)) {
            do { if(check(TokenType.IDENTIFIER)) tablaSimbolos.add(advance().value); } while (match(TokenType.COMMA));
        }
        consume(TokenType.RBRACE, "Falta '}'");
        if (check(TokenType.SEMICOLON)) advance();
        System.out.println("   -> [ENUM] " + id.value);
    }

    private void handleImport() {
        Token ruta = consume(TokenType.IDENTIFIER, "Falta ruta");
        String n = ruta.value; if (check(TokenType.OPERATOR) && peek().value.equals("*")) n += advance().value;
        consume(TokenType.SEMICOLON, "Falta ';'"); libreriasImportadas.add(n);
    }
    private void handlePackage() { consume(TokenType.IDENTIFIER, "Falta nombre"); consume(TokenType.SEMICOLON, "Falta ';'"); }
    private void handleIf() { consume(TokenType.LPAREN, "Falta '('"); skipToClosingParen(); handleBody(); if (match(TokenType.ELSE)) { handleBody(); } }
    private void handleFor() { consume(TokenType.LPAREN, "Falta '('"); skipToClosingParen(); handleBody(); }
    private void handleWhile() { consume(TokenType.LPAREN, "Falta '('"); skipToClosingParen(); handleBody(); }

    private void handleBody() {
        if (match(TokenType.LBRACE)) {
            while (!check(TokenType.RBRACE) && !isAtEnd()) statement();
            consume(TokenType.RBRACE, "Falta '}'");
        } else statement();
    }
    private void ignorarHastaPuntoYComa() { while (!check(TokenType.SEMICOLON) && !check(TokenType.LBRACE) && !isAtEnd()) advance(); if (check(TokenType.SEMICOLON)) advance(); }

    private void skipToClosingParen() {
        int count = 1;
        while (count > 0 && !isAtEnd()) {
            if (check(TokenType.LPAREN)) count++;
            if (check(TokenType.RPAREN)) count--;
            advance();
        }
    }

    private boolean checkKeyword(String kw) { return check(TokenType.KEYWORD) && peek().value.equals(kw); }
    private Token consume(TokenType type, String msg) { if (check(type)) return advance(); throw new RuntimeException(msg + " ('" + peek().value + "')"); }
    private boolean match(TokenType... types) { for (TokenType t : types) if (check(t)) { advance(); return true; } return false; }
    private boolean check(TokenType t) { return !isAtEnd() && peek().type == t; }
    private Token advance() { if (!isAtEnd()) current++; return tokens.get(current - 1); }
    private boolean isAtEnd() { return current >= tokens.size() || peek().type == TokenType.EOF; }
    private Token peek() { return tokens.get(current); }
    private Token previous() { return tokens.get(current - 1); }

    public static List<Token> lexer(String filePath) throws IOException {
        List<Token> tokens = new ArrayList<>();
        String content = new String(Files.readAllBytes(Paths.get(filePath)));

        content = content.replaceAll("/\\*[\\s\\S]*?\\*/", "");
        content = content.replaceAll("//.*", "");

        String regex = "\"[^\"]*\"|[a-zA-Z_][a-zA-Z0-9_.]*|[0-9]+(\\.[0-9]+)?|[=+\\-*/<>!&|]+|[;(),{}:]";
        Matcher m = Pattern.compile(regex).matcher(content);

        while (m.find()) {
            String part = m.group();

            if (part.startsWith("\"")) tokens.add(new Token(TokenType.STRING, part));
            else if (part.equals(";")) tokens.add(new Token(TokenType.SEMICOLON, ";"));
            else if (part.equals(",")) tokens.add(new Token(TokenType.COMMA, ","));
            else if (part.equals("(")) tokens.add(new Token(TokenType.LPAREN, "("));
            else if (part.equals(")")) tokens.add(new Token(TokenType.RPAREN, ")"));
            else if (part.equals("{")) tokens.add(new Token(TokenType.LBRACE, "{"));
            else if (part.equals("}")) tokens.add(new Token(TokenType.RBRACE, "}"));
            else if (part.matches("@[a-zA-Z_][a-zA-Z0-9_]*")) tokens.add(new Token(TokenType.ANNOTATION, part));
            else if (part.matches("int|String|float|double|boolean|char|byte|short|long|void"))
                tokens.add(new Token(TokenType.DATATYPE, part));
            else if (part.equals("if")) tokens.add(new Token(TokenType.IF, part));
            else if (part.equals("else")) tokens.add(new Token(TokenType.ELSE, part));
            else if (part.equals("for")) tokens.add(new Token(TokenType.FOR, part));
            else if (part.equals("while")) tokens.add(new Token(TokenType.WHILE, part));
            else if (part.equals("when")) tokens.add(new Token(TokenType.WHEN, part));
            else if (part.equals("switch")) tokens.add(new Token(TokenType.SWITCH, part));
            else if (part.equals("return")) tokens.add(new Token(TokenType.RETURN, part));
            else if (part.equals("class")) tokens.add(new Token(TokenType.CLASS, part));
            else if (part.equals("enum")) tokens.add(new Token(TokenType.ENUM, part));
            else if (part.equals("import")) tokens.add(new Token(TokenType.IMPORT, part));
            else if (part.equals("package")) tokens.add(new Token(TokenType.PACKAGE, part));
            else if (part.matches("public|private|protected|static|this|do"))
                tokens.add(new Token(TokenType.KEYWORD, part));
            else if (part.matches("[0-9]+(\\.[0-9]+)?")) tokens.add(new Token(TokenType.NUMBER, part));
            else if (part.matches("[=+\\-*/<>!&|]+")) tokens.add(new Token(TokenType.OPERATOR, part));
            else if (part.matches("[a-zA-Z_][a-zA-Z0-9_.]*")) tokens.add(new Token(TokenType.IDENTIFIER, part));
            else tokens.add(new Token(TokenType.UNKNOWN, part));
        }
        tokens.add(new Token(TokenType.EOF, "EOF"));
        return tokens;
    }

    public static void main(String[] args) {
        if (args.length < 1) return;
        try {
            new Main(lexer(args[0])).analizar();
        } catch (Exception e) { System.err.println(e.getMessage()); }
    }
}
