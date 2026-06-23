import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//nodo generico flexible (saquenme de aquiiii)
class ASTNode {
    String type;
    String value;
    List<ASTNode> children = new ArrayList<>();

    public ASTNode(String type) { this.type = type; this.value = null; }
    public ASTNode(String type, String value) { this.type = type; this.value = value; }

    public void addChild(ASTNode child) {
        if (child != null) this.children.add(child);
    }

    public void print(String padding) {
        System.out.println(padding + "└── [" + type + (value != null ? ": " + value : "") + "]");
        for (ASTNode child : children) {
            child.print(padding + "    ");
        }
    }
}

public class Main {

    enum TokenType {
        DATATYPE, IDENTIFIER, NUMBER, ADD_OP, MUL_OP, REL_OP, ASSIGN_OP, STRING,
        SEMICOLON, COMMA, LPAREN, RPAREN, LBRACE, RBRACE,
        IF, ELSE, FOR, WHILE, RETURN, CLASS, IMPORT, PACKAGE,
        PUBLIC, PRIVATE, STATIC, ANNOTATION, ENUM, EOF, UNKNOWN
    }

    static class Token {
        TokenType type; String value;
        Token(TokenType type, String value) { this.type = type; this.value = value; }
        @Override public String toString() { return "[" + type + ": " + value + "]"; }
    }

    //lexer (solo quiero descansar)
    public static List<Token> lexer(String filePath) throws IOException {
        List<Token> tokens = new ArrayList<>();
        String content = new String(Files.readAllBytes(Paths.get(filePath)));
        content = content.replaceAll("//.*|/\\*[\\s\\S]*?\\*/", "");

        String regex = "\"[^\"]*\"|@[a-zA-Z_][a-zA-Z0-9_]*|[a-zA-Z_][a-zA-Z0-9_.]*|[0-9]+|[=+\\-*/<>!&|]+|[;(),{}]";
        Matcher m = Pattern.compile(regex).matcher(content);

        while (m.find()) {
            String p = m.group();
            if (p.startsWith("\"")) tokens.add(new Token(TokenType.STRING, p));
            else if (p.startsWith("@")) tokens.add(new Token(TokenType.ANNOTATION, p));
            else if (p.matches("int|String|void|boolean|double|float")) tokens.add(new Token(TokenType.DATATYPE, p));
            else if (p.equals("class")) tokens.add(new Token(TokenType.CLASS, p));
            else if (p.equals("if")) tokens.add(new Token(TokenType.IF, p));
            else if (p.equals("else")) tokens.add(new Token(TokenType.ELSE, p));
            else if (p.equals("for")) tokens.add(new Token(TokenType.FOR, p));
            else if (p.equals("while")) tokens.add(new Token(TokenType.WHILE, p));
            else if (p.equals("package")) tokens.add(new Token(TokenType.PACKAGE, p));
            else if (p.equals("import")) tokens.add(new Token(TokenType.IMPORT, p));
            else if (p.equals("return")) tokens.add(new Token(TokenType.RETURN, p));
            else if (p.equals("enum")) tokens.add(new Token(TokenType.ENUM, p));
            else if (p.matches("public|private|static|protected")) tokens.add(new Token(TokenType.PUBLIC, p));
            else if (p.equals(";")) tokens.add(new Token(TokenType.SEMICOLON, p));
            else if (p.equals(",")) tokens.add(new Token(TokenType.COMMA, p));
            else if (p.equals("{")) tokens.add(new Token(TokenType.LBRACE, p));
            else if (p.equals("}")) tokens.add(new Token(TokenType.RBRACE, p));
            else if (p.equals("(")) tokens.add(new Token(TokenType.LPAREN, p));
            else if (p.equals(")")) tokens.add(new Token(TokenType.RPAREN, p));
            else if (p.matches("[a-zA-Z_][a-zA-Z0-9_.]*")) tokens.add(new Token(TokenType.IDENTIFIER, p));
            else if (p.matches("[0-9]+")) tokens.add(new Token(TokenType.NUMBER, p));
            else if (p.matches("[+\\-]")) tokens.add(new Token(TokenType.ADD_OP, p));
            else if (p.matches("[*/%]")) tokens.add(new Token(TokenType.MUL_OP, p));
            else if (p.matches("==|!=|<=|>=|<|>")) tokens.add(new Token(TokenType.REL_OP, p));
            else if (p.equals("=")) tokens.add(new Token(TokenType.ASSIGN_OP, p));
            else tokens.add(new Token(TokenType.UNKNOWN, p));
        }
        tokens.add(new Token(TokenType.EOF, "EOF"));
        return tokens;
    }

    // Parser LL(1) Tabular
    static class Parser {
        private final List<Token> tokens;
        private final Map<String, Map<TokenType, List<String>>> table = new HashMap<>();

        public Parser(List<Token> tokens) {
            this.tokens = tokens;
            initTable();
        }

        // Helper para insertar reglas en la tabla
        private void rule(String nt, TokenType t, String... prod) {
            table.computeIfAbsent(nt, k -> new HashMap<>()).put(t, Arrays.asList(prod));
        }

        // Helper para manejar las transiciones vacías (EPSILON)
        private void nullable(String nt, TokenType... exclude) {
            List<TokenType> exclusions = Arrays.asList(exclude);
            for (TokenType t : TokenType.values()) {
                if (!exclusions.contains(t)) {
                    rule(nt, t, "EPSILON");
                }
            }
        }

        private void initTable() {
            //Estructura Base
            rule("PROGRAM", TokenType.CLASS, "CLASS_DECL");
            rule("PROGRAM", TokenType.PUBLIC, "CLASS_DECL");
            rule("PROGRAM", TokenType.DATATYPE, "STATEMENT_LIST");
            rule("PROGRAM", TokenType.EOF, "EPSILON");

            //Clases
            rule("CLASS_DECL", TokenType.PUBLIC, "PUBLIC", "CLASS_DECL_TAIL");
            rule("CLASS_DECL", TokenType.CLASS, "CLASS_DECL_TAIL");
            rule("CLASS_DECL_TAIL", TokenType.CLASS, "CLASS", "IDENTIFIER", "LBRACE", "STATEMENT_LIST", "RBRACE");

            //Lista de Sentencias
            rule("STATEMENT_LIST", TokenType.DATATYPE, "STATEMENT", "STATEMENT_LIST");
            rule("STATEMENT_LIST", TokenType.IDENTIFIER, "STATEMENT", "STATEMENT_LIST");
            rule("STATEMENT_LIST", TokenType.IF, "STATEMENT", "STATEMENT_LIST");
            rule("STATEMENT_LIST", TokenType.WHILE, "STATEMENT", "STATEMENT_LIST");
            rule("STATEMENT_LIST", TokenType.RETURN, "STATEMENT", "STATEMENT_LIST");
            rule("STATEMENT_LIST", TokenType.FOR, "STATEMENT", "STATEMENT_LIST");
            rule("STATEMENT_LIST", TokenType.ENUM, "STATEMENT", "STATEMENT_LIST");
            rule("STATEMENT_LIST", TokenType.PUBLIC, "STATEMENT", "STATEMENT_LIST");
            rule("STATEMENT_LIST", TokenType.PRIVATE, "STATEMENT", "STATEMENT_LIST");
            rule("STATEMENT_LIST", TokenType.STATIC, "STATEMENT", "STATEMENT_LIST");
            rule("STATEMENT_LIST", TokenType.ANNOTATION, "STATEMENT", "STATEMENT_LIST");
            rule("STATEMENT_LIST", TokenType.LBRACE, "STATEMENT", "STATEMENT_LIST");
            rule("STATEMENT_LIST", TokenType.SEMICOLON, "STATEMENT", "STATEMENT_LIST");
            nullable("STATEMENT_LIST", TokenType.DATATYPE, TokenType.IDENTIFIER, TokenType.IF, TokenType.WHILE, TokenType.RETURN, TokenType.FOR, TokenType.ENUM, TokenType.PUBLIC, TokenType.PRIVATE, TokenType.STATIC, TokenType.ANNOTATION, TokenType.LBRACE, TokenType.SEMICOLON);

            //Sentencias
            rule("STATEMENT", TokenType.DATATYPE, "DECLARATION");
            rule("STATEMENT", TokenType.IDENTIFIER, "ID_STATEMENT");
            rule("STATEMENT", TokenType.IF, "IF", "LPAREN", "EXPRESSION", "RPAREN", "BLOCK", "ELSE_OPT");
            rule("STATEMENT", TokenType.WHILE, "WHILE", "LPAREN", "EXPRESSION", "RPAREN", "BLOCK");
            rule("STATEMENT", TokenType.FOR, "FOR", "LPAREN", "EXPRESSION_OPT", "SEMICOLON", "EXPRESSION_OPT", "SEMICOLON", "EXPRESSION_OPT", "RPAREN", "BLOCK");
            rule("STATEMENT", TokenType.RETURN, "RETURN", "EXPRESSION_OPT", "SEMICOLON");
            rule("STATEMENT", TokenType.LBRACE, "BLOCK");
            rule("STATEMENT", TokenType.PUBLIC, "MODIFIER", "STATEMENT");
            rule("STATEMENT", TokenType.PRIVATE, "MODIFIER", "STATEMENT");
            rule("STATEMENT", TokenType.STATIC, "MODIFIER", "STATEMENT");
            rule("STATEMENT", TokenType.ANNOTATION, "ANNOTATION", "STATEMENT");
            rule("STATEMENT", TokenType.SEMICOLON, "SEMICOLON");

            rule("MODIFIER", TokenType.PUBLIC, "PUBLIC");
            rule("MODIFIER", TokenType.PRIVATE, "PRIVATE");
            rule("MODIFIER", TokenType.STATIC, "STATIC");

            //Bloques y Else
            rule("BLOCK", TokenType.LBRACE, "LBRACE", "STATEMENT_LIST", "RBRACE");
            rule("ELSE_OPT", TokenType.ELSE, "ELSE", "BLOCK");
            nullable("ELSE_OPT", TokenType.ELSE);

            //Declaraciones de variables y metodos
            rule("DECLARATION", TokenType.DATATYPE, "DATATYPE", "IDENTIFIER", "DECL_TAIL");
            rule("DECL_TAIL", TokenType.SEMICOLON, "SEMICOLON");
            rule("DECL_TAIL", TokenType.ASSIGN_OP, "ASSIGN_OP", "EXPRESSION", "SEMICOLON");
            rule("DECL_TAIL", TokenType.LPAREN, "LPAREN", "PARAMS", "RPAREN", "BLOCK");

            //Parametros
            rule("PARAMS", TokenType.DATATYPE, "DATATYPE", "IDENTIFIER", "PARAMS_TAIL");
            nullable("PARAMS", TokenType.DATATYPE);
            rule("PARAMS_TAIL", TokenType.COMMA, "COMMA", "DATATYPE", "IDENTIFIER", "PARAMS_TAIL");
            nullable("PARAMS_TAIL", TokenType.COMMA);

            //Asignaciones y llamadas a metodos
            rule("ID_STATEMENT", TokenType.IDENTIFIER, "IDENTIFIER", "ID_TAIL");
            rule("ID_TAIL", TokenType.ASSIGN_OP, "ASSIGN_OP", "EXPRESSION", "SEMICOLON");
            rule("ID_TAIL", TokenType.LPAREN, "LPAREN", "ARGS", "RPAREN", "SEMICOLON");

            //Argumentos
            rule("ARGS", TokenType.IDENTIFIER, "EXPRESSION", "ARGS_TAIL");
            rule("ARGS", TokenType.NUMBER, "EXPRESSION", "ARGS_TAIL");
            rule("ARGS", TokenType.STRING, "EXPRESSION", "ARGS_TAIL");
            rule("ARGS", TokenType.LPAREN, "EXPRESSION", "ARGS_TAIL");
            nullable("ARGS", TokenType.IDENTIFIER, TokenType.NUMBER, TokenType.STRING, TokenType.LPAREN);
            rule("ARGS_TAIL", TokenType.COMMA, "COMMA", "EXPRESSION", "ARGS_TAIL");
            nullable("ARGS_TAIL", TokenType.COMMA);

            //Expresiones
            rule("EXPRESSION_OPT", TokenType.IDENTIFIER, "EXPRESSION");
            rule("EXPRESSION_OPT", TokenType.NUMBER, "EXPRESSION");
            rule("EXPRESSION_OPT", TokenType.STRING, "EXPRESSION");
            rule("EXPRESSION_OPT", TokenType.LPAREN, "EXPRESSION");
            nullable("EXPRESSION_OPT", TokenType.IDENTIFIER, TokenType.NUMBER, TokenType.STRING, TokenType.LPAREN);

            rule("EXPRESSION", TokenType.IDENTIFIER, "ARITH_EXPR", "REL_EXPR_TAIL");
            rule("EXPRESSION", TokenType.NUMBER, "ARITH_EXPR", "REL_EXPR_TAIL");
            rule("EXPRESSION", TokenType.STRING, "ARITH_EXPR", "REL_EXPR_TAIL");
            rule("EXPRESSION", TokenType.LPAREN, "ARITH_EXPR", "REL_EXPR_TAIL");

            rule("REL_EXPR_TAIL", TokenType.REL_OP, "REL_OP", "ARITH_EXPR", "REL_EXPR_TAIL");
            nullable("REL_EXPR_TAIL", TokenType.REL_OP);

            rule("ARITH_EXPR", TokenType.IDENTIFIER, "TERM", "ARITH_EXPR_TAIL");
            rule("ARITH_EXPR", TokenType.NUMBER, "TERM", "ARITH_EXPR_TAIL");
            rule("ARITH_EXPR", TokenType.STRING, "TERM", "ARITH_EXPR_TAIL");
            rule("ARITH_EXPR", TokenType.LPAREN, "TERM", "ARITH_EXPR_TAIL");

            rule("ARITH_EXPR_TAIL", TokenType.ADD_OP, "ADD_OP", "TERM", "ARITH_EXPR_TAIL");
            nullable("ARITH_EXPR_TAIL", TokenType.ADD_OP);

            rule("TERM", TokenType.IDENTIFIER, "FACTOR", "TERM_TAIL");
            rule("TERM", TokenType.NUMBER, "FACTOR", "TERM_TAIL");
            rule("TERM", TokenType.STRING, "FACTOR", "TERM_TAIL");
            rule("TERM", TokenType.LPAREN, "FACTOR", "TERM_TAIL");

            rule("TERM_TAIL", TokenType.MUL_OP, "MUL_OP", "FACTOR", "TERM_TAIL");
            nullable("TERM_TAIL", TokenType.MUL_OP);

            rule("FACTOR", TokenType.IDENTIFIER, "IDENTIFIER");
            rule("FACTOR", TokenType.NUMBER, "NUMBER");
            rule("FACTOR", TokenType.STRING, "STRING");
            rule("FACTOR", TokenType.LPAREN, "LPAREN", "EXPRESSION", "RPAREN");
        }

        private boolean isTerminal(String symbol) {
            for (TokenType type : TokenType.values()) {
                if (type.name().equals(symbol)) return true;
            }
            return false;
        }

        // Elemento de Pila que vincula la Gramaaatica con el AST
        class StackItem {
            String symbol;
            ASTNode parentNode;
            StackItem(String symbol, ASTNode parentNode) {
                this.symbol = symbol;
                this.parentNode = parentNode;
            }
        }

        public ASTNode parse() {
            System.out.println("--- INICIANDO PARSER LL(1) CONSTRUYENDO AST ---");

            ASTNode astRoot = new ASTNode("PROGRAM");
            Stack<StackItem> stack = new Stack<>();

            stack.push(new StackItem(TokenType.EOF.name(), null));
            stack.push(new StackItem("PROGRAM", astRoot));

            int current = 0;
            Token lookahead = tokens.get(current);

            try {
                while (!stack.isEmpty()) {
                    StackItem currentItem = stack.pop();
                    String top = currentItem.symbol;
                    ASTNode parent = currentItem.parentNode;

                    if (top.equals("EPSILON")) continue;

                    if (isTerminal(top)) {
                        if (top.equals(lookahead.type.name())) {
                            if (parent != null && lookahead.type != TokenType.EOF) {
                                parent.addChild(new ASTNode(lookahead.type.name(), lookahead.value));
                            }
                            current++;
                            if (current < tokens.size()) lookahead = tokens.get(current);
                        } else {
                            throw new RuntimeException("Se esperaba " + top + " pero se encontró " + lookahead.type + " ('" + lookahead.value + "')");
                        }
                    } else {
                        Map<TokenType, List<String>> transitions = table.get(top);
                        if (transitions == null || !transitions.containsKey(lookahead.type)) {
                            throw new RuntimeException("Falla de coherencia en No-Terminal [" + top + "] con Token [" + lookahead.type + "]");
                        }

                        List<String> production = transitions.get(lookahead.type);

                        ASTNode newNode = new ASTNode(top);
                        if (parent != null) parent.addChild(newNode);

                        // Apilar al revés
                        for (int i = production.size() - 1; i >= 0; i--) {
                            stack.push(new StackItem(production.get(i), newNode));
                        }
                    }
                }
                return astRoot;

            } catch (Exception e) {
                System.err.println("\n>>> ERROR SINTÁCTICO: " + e.getMessage());
                return null;
            }
        }
    }

    //generador del codigo ensamblador en NASM
    //nmms ya quiero dormir we, ya son las 3 de la mañanaaaaa
    // =========================================================================
    // CLASE GENERADORA DE CÓDIGO (CODE GENERATOR)
    // =========================================================================
    // Esta clase recorre el Árbol de Sintaxis Abstracta (AST) e implementa un
    // enfoque de arquitectura basada en pila para traducir nodos en NASM x86-64.
    static class CodeGenerator {
        
        // Enumerador para definir de forma limpia el Sistema Operativo objetivo
        public enum TargetOS { MACOS, LINUX }
        
        private TargetOS target;
        private StringBuilder dataSection; // Almacena constantes y cadenas (.data)
        private StringBuilder bssSection;  // Almacena variables globales sin inicializar (.bss)
        private StringBuilder textSection; // Almacena las instrucciones de CPU (.text)

        private int labelCounter = 0;      // Contador para generar etiquetas de saltos (L1, L2, etc.)
        private int stringCounter = 0;     // Contador para generar etiquetas de cadenas (str1, str2, etc.)
        private Set<String> variables = new HashSet<>(); // Control de variables declaradas para no duplicar

        // Constructor: Inicializa los búferes y configura las diferencias de sintaxis entre OS
        public CodeGenerator(TargetOS target) {
            this.target = target;
            
            // Regla de Oro: macOS exige que los símbolos externos/globales inicien con guion bajo (_).
            // Linux, por su parte, los utiliza en formato plano (sin prefijo).
            String mainSymbol = (target == TargetOS.MACOS) ? "_main" : "main";
            String printfSymbol = (target == TargetOS.MACOS) ? "_printf" : "printf";

            dataSection = new StringBuilder();
            // 'default rel' activa el direccionamiento relativo a RIP. Es obligatorio en Mac 
            // y altamente recomendado en Linux moderno para generar binarios PIE (Position Independent Executable).
            dataSection.append("default rel\n"); 
            dataSection.append("section .data\n");
            // Define la constante de formato para imprimir enteros con un salto de línea (\n = 10) y fin de cadena (0)
            dataSection.append("    fmt_int db \"%d\", 10, 0\n");

            bssSection = new StringBuilder();
            bssSection.append("section .bss\n");

            textSection = new StringBuilder();
            textSection.append("section .text\n");
            textSection.append("    global ").append(mainSymbol).append("\n"); // Declara el punto de entrada al enlazador
            textSection.append("    extern ").append(printfSymbol).append("\n\n"); // Declara que printf viene de una biblioteca externa
        }

        // Generadores de etiquetas únicas para evitar colisiones en los saltos (if/while) y cadenas
        private String getNewLabel() { return "L" + (++labelCounter); }
        private String getNewStringLabel() { return "str" + (++stringCounter); }

        // =========================================================================
        // FUNCIÓN DESPACHADORA PRINCIPAL (ENRUTADOR DEL AST)
        // =========================================================================
        // Realiza un recorrido en profundidad (DFS Pre-order). Analiza el tipo de nodo
        // y delega el control a funciones especialistas.
        public void generate(ASTNode node) {
            if (node == null) return;

            switch (node.type) {
                case "DECLARATION":
                    handleDeclaration(node); // Modulo especialista en variables y funciones
                    return;
                case "ID_STATEMENT":
                    handleIdStatement(node);  // Modulo especialista en asignaciones y llamadas a funciones
                    return;
                case "STATEMENT":
                    // Si es una sentencia condicional o un bucle, intercepta antes de seguir bajando
                    if (!node.children.isEmpty() && node.children.get(0).type.equals("IF")) {
                        handleIf(node);
                        return;
                    } else if (!node.children.isEmpty() && node.children.get(0).type.equals("WHILE")) {
                        handleWhile(node);
                        return;
                    }
                    break;
            }

            // Si el nodo actual no requiere lógica inmediata, procesa recursivamente a todos sus hijos
            for (ASTNode child : node.children) {
                generate(child);
            }
        }

        // =========================================================================
        // MANEJADOR DE DECLARACIONES (handleDeclaration)
        // =========================================================================
        // Procesa la creación de estructuras mayores: Métodos/Funciones o Variables Globales.
        private void handleDeclaration(ASTNode node) {
            String id = node.children.get(1).value; // Obtiene el nombre del identificador
            ASTNode tail = node.children.get(2);   // Obtiene la cola de la declaración para saber qué es

            // Si la cola contiene un paréntesis '(', significa que es la definición de una Función/Método
            if (!tail.children.isEmpty() && tail.children.get(0).type.equals("LPAREN")) {
                String mainSymbol = (target == TargetOS.MACOS) ? "_main" : "main";
                String funcName = id.equals("main") ? mainSymbol : id;
                
                // Escribe la etiqueta de la función en el código
                textSection.append(funcName).append(":\n");
                // --- PRÓLOGO DE LA FUNCIÓN ---
                // Salva el puntero base de la pila del llamador y actualiza el marco de la pila actual
                textSection.append("    push rbp\n");
                textSection.append("    mov rbp, rsp\n\n");

                // Si la función tiene cuerpo (un bloque de sentencias), lo genera recursivamente
                if (tail.children.size() > 3) {
                    ASTNode blockNode = tail.children.get(3);
                    generate(blockNode);
                }

                // Si salimos de la función principal 'main', inyecta un retorno exitoso (código de salida 0)
                if (id.equals("main")) {
                    textSection.append("    mov eax, 0\n");
                }
                
                // --- EPÍLOGO DE LA FUNCIÓN ---
                // Destruye el marco de la pila actual, restaura el RBP anterior y regresa el control
                textSection.append("    mov rsp, rbp\n");
                textSection.append("    pop rbp\n");
                textSection.append("    ret\n\n");

            } else {
                // Si no hay paréntesis, es una Declaración de Variable
                if (!variables.contains(id)) {
                    // 'resd 1' reserva un bloque de 4 bytes (Double Word / 32-bits) en la sección .bss
                    bssSection.append("    ").append(id).append(" resd 1\n");
                    variables.add(id);
                }

                // Si la declaración incluye una asignación directa (ej: int x = 5 + 3;)
                if (tail.children.size() > 1 && tail.children.get(0).type.equals("ASSIGN_OP")) {
                    ASTNode expr = tail.children.get(1);
                    evaluateExpression(expr); // Evalúa el lado derecho matemáticamente. El resultado queda en la pila.
                    textSection.append("    pop rax\n"); // Extrae el resultado de la pila a RAX
                    // Guarda los 32 bits de EAX (la parte baja de RAX) en la dirección física de la variable
                    textSection.append("    mov dword [").append(id).append("], eax\n\n");
                }
            }
        }

        // =========================================================================
        // MANEJADOR DE IDENTIFICADORES EN ACCIÓN (handleIdStatement)
        // =========================================================================
        // Procesa líneas que inician con variables ya existentes: Asignaciones o Llamadas.
        private void handleIdStatement(ASTNode node) {
            String id = node.children.get(0).value;
            ASTNode tail = node.children.get(1);

            // Escenario 1: Asignación simple (ej: x = 10;)
            if (tail.children.get(0).type.equals("ASSIGN_OP")) {
                ASTNode expr = tail.children.get(1);
                evaluateExpression(expr); // Resuelve la expresión matemática
                textSection.append("    pop rax\n"); // Recupera el resultado de la pila
                textSection.append("    mov dword [").append(id).append("], eax\n\n"); // Actualiza la variable en memoria

            // Escenario 2: Llamada a una función (ej: print(...) o System.out.println(...))
            } else if (tail.children.get(0).type.equals("LPAREN")) {
                if (id.contains("print") || id.contains("System.out")) {
                    ASTNode args = tail.children.get(1);
                    if (!args.children.isEmpty() && args.children.get(0).type.equals("EXPRESSION")) {
                        ASTNode expr = args.children.get(0);
                        String printfSymbol = (target == TargetOS.MACOS) ? "_printf" : "printf";

                        // Sub-escenario A: Es una impresión de cadena de texto estática (String)
                        if (isStringExpression(expr)) {
                            String strVal = getStringValue(expr);
                            String strLabel = getNewStringLabel();
                            // Registra el String en la sección .data añadiéndole salto de línea (10) y nulo (0)
                            dataSection.append("    ").append(strLabel).append(" db ").append(strVal).append(", 10, 0\n");
                            // LEA (Load Effective Address): Carga la dirección de memoria de la etiqueta en RDI (Primer argumento)
                            textSection.append("    lea rdi, [").append(strLabel).append("]\n"); 
                            textSection.append("    mov al, 0\n"); // AL = 0 indica a printf que no estamos pasando flotantes en registros vectoriales (XMM)
                            textSection.append("    call ").append(printfSymbol).append("\n\n");
                        
                        // Sub-escenario B: Es una impresión de una expresión o número entero
                        } else {
                            evaluateExpression(expr); // Evalúa la expresión numérica
                            textSection.append("    pop rsi\n"); // Saca el resultado a RSI (Segundo argumento para printf: el valor)
                            textSection.append("    lea rdi, [fmt_int]\n"); // Carga el formato "%d" en RDI (Primer argumento para printf)
                            textSection.append("    mov al, 0\n"); // Asegura compatibilidad con funciones variádicas de C
                            textSection.append("    call ").append(printfSymbol).append("\n\n");
                        }
                    }
                } else {
                    // Si es cualquier otra función definida por el usuario, emite un call directo
                    String mainSymbol = (target == TargetOS.MACOS) ? "_main" : "main";
                    String funcName = id.equals("main") ? mainSymbol : id;
                    textSection.append("    call ").append(funcName).append("\n\n");
                }
            }
        }

        // Métodos auxiliares para buscar si un nodo del AST oculta un token de cadena de texto
        private boolean isStringExpression(ASTNode expr) {
            ASTNode current = expr;
            while (!current.children.isEmpty() && !current.type.equals("STRING")) {
                current = current.children.get(0);
                if (current.type.equals("STRING")) return true;
            }
            return false;
        }

        private String getStringValue(ASTNode expr) {
            ASTNode current = expr;
            while (!current.children.isEmpty() && !current.type.equals("STRING")) {
                current = current.children.get(0);
                if (current.type.equals("STRING")) return current.value;
            }
            return "\"\"";
        }

        // =========================================================================
        // MANEJADOR DE CONTROL DE FLUJO: CONDICIONAL (handleIf)
        // =========================================================================
        // Implementa lógica condicional basándose en banderas de la CPU.
        private void handleIf(ASTNode node) {
            String labelElse = getNewLabel(); // Etiqueta a donde saltar si la condición es falsa
            String labelEnd = getNewLabel();  // Etiqueta de salida final del bloque condicional

            textSection.append("    ; --- INICIO IF ---\n");
            ASTNode expr = node.children.get(2); // Obtiene la condición encerrada en los paréntesis
            evaluateExpression(expr);            // Evalúa el resultado lógico (deja 1 o 0 en la pila)

            textSection.append("    pop rax\n");   // Saca el resultado a RAX
            textSection.append("    cmp rax, 0\n"); // Compara RAX con 0 (Falso)
            // JE (Jump if Equal): Si RAX es igual a 0, rompe el flujo lineal y salta directo al bloque ELSE
            textSection.append("    je ").append(labelElse).append(" ; Salta al ELSE si es falso\n\n");

            generate(node.children.get(4)); // Si no saltó, significa que es Verdadero. Procesa el bloque interno del IF.
            textSection.append("    jmp ").append(labelEnd).append("\n"); // Salto incondicional al final para no ejecutar el ELSE

            // Define el punto de entrada para el bloque alternativo
            textSection.append(labelElse).append(":\n");
            ASTNode elseOpt = node.children.get(5);
            // Si el código fuente original contenía la cláusula 'else', genera su bloque correspondiente
            if (!elseOpt.children.isEmpty() && elseOpt.children.get(0).type.equals("ELSE")) {
                generate(elseOpt.children.get(1));
            }
            // Define el punto de salida final
            textSection.append(labelEnd).append(":\n\n");
        }

        // =========================================================================
        // MANEJADOR DE CONTROL DE FLUJO: BUCLE (handleWhile)
        // =========================================================================
        // Estructura ciclos iterativos mediante retornos controlados.
        private void handleWhile(ASTNode node) {
            String labelStart = getNewLabel(); // Marca de retorno para reevaluar la condición
            String labelEnd = getNewLabel();   // Marca de escape para romper el bucle

            textSection.append("    ; --- INICIO WHILE ---\n");
            textSection.append(labelStart).append(":\n"); // Coloca el punto de reevaluación

            ASTNode expr = node.children.get(2); // Obtiene la condición del ciclo
            evaluateExpression(expr);            // Calcula el estado actual de la condición

            textSection.append("    pop rax\n");
            textSection.append("    cmp rax, 0\n");
            // Si la condición se vuelve falsa (0), salta inmediatamente fuera del bucle
            textSection.append("    je ").append(labelEnd).append(" ; Salir del bucle si es falso\n\n");

            generate(node.children.get(4)); // Procesa recursivamente todas las líneas de código dentro del bucle

            textSection.append("    jmp ").append(labelStart).append("\n"); // Bucle infinito hacia arriba para volver a revisar la condición
            textSection.append(labelEnd).append(":\n\n"); // Destino de escape
        }

        // =========================================================================
        // NÚCLEO EVALUADOR: EXPRESIONES LÓGICAS / RELACIONALES (==, !=, <, >, <=, >=)
        // =========================================================================
        private void evaluateExpression(ASTNode expr) {
            ASTNode arith = expr.children.get(0);
            evaluateArithExpr(arith); // Resuelve el primer operando aritmético

            // Si existen operadores relacionales adyacentes
            if (expr.children.size() > 1) {
                ASTNode tail = expr.children.get(1);
                while (!tail.children.isEmpty() && !tail.children.get(0).type.equals("EPSILON")) {
                    String op = tail.children.get(0).value;
                    ASTNode nextArith = tail.children.get(1);
                    evaluateArithExpr(nextArith); // Resuelve el segundo operando aritmético

                    textSection.append("    pop rbx\n"); // Operando derecho extraído a RBX
                    textSection.append("    pop rax\n"); // Operando izquierdo extraído a RAX
                    textSection.append("    cmp rax, rbx\n"); // Resta lógica interna en la CPU para alterar banderas

                    String labelTrue = getNewLabel();
                    String labelSkip = getNewLabel();

                    // Mapeo de operadores de alto nivel a instrucciones de salto condicional de la CPU
                    if (op.equals("==")) textSection.append("    je ").append(labelTrue).append("\n");
                    else if (op.equals("!=")) textSection.append("    jne ").append(labelTrue).append("\n");
                    else if (op.equals("<")) textSection.append("    jl ").append(labelTrue).append("\n");
                    else if (op.equals(">")) textSection.append("    jg ").append(labelTrue).append("\n");
                    else if (op.equals("<=")) textSection.append("    jle ").append(labelTrue).append("\n");
                    else if (op.equals(">=")) textSection.append("    jge ").append(labelTrue).append("\n");

                    // Camino por defecto (Falso): Guarda un 0 en la pila y salta el bloque verdadero
                    textSection.append("    push 0\n");
                    textSection.append("    jmp ").append(labelSkip).append("\n");

                    // Camino condicional (Verdadero): Guarda un 1 en la pila
                    textSection.append(labelTrue).append(":\n");
                    textSection.append("    push 1\n");

                    textSection.append(labelSkip).append(":\n");

                    if (tail.children.size() > 2) tail = tail.children.get(2);
                    else break;
                }
            }
        }

        // =========================================================================
        // NÚCLEO EVALUADOR: ARITMÉTICA DE SUMA Y RESTA (+, -)
        // =========================================================================
        private void evaluateArithExpr(ASTNode arith) {
            ASTNode term = arith.children.get(0);
            evaluateTerm(term); // Baja al siguiente nivel de precedencia (Multiplicaciones)

            if (arith.children.size() > 1) {
                ASTNode tail = arith.children.get(1);
                while (!tail.children.isEmpty() && !tail.children.get(0).type.equals("EPSILON")) {
                    String op = tail.children.get(0).value;
                    ASTNode nextTerm = tail.children.get(1);
                    evaluateTerm(nextTerm);

                    textSection.append("    pop rbx\n"); // Recupera operando derecho
                    textSection.append("    pop rax\n"); // Recupera operando izquierdo

                    if (op.equals("+")) {
                        textSection.append("    add rax, rbx\n"); // Suma el contenido y guarda el resultado en RAX
                        textSection.append("    push rax\n");     // Vuelve a subir el resultado acumulado a la pila
                    } else if (op.equals("-")) {
                        textSection.append("    sub rax, rbx\n"); // Resta RAX - RBX y guarda en RAX
                        textSection.append("    push rax\n");     // Lo sube a la pila
                    }

                    if (tail.children.size() > 2) tail = tail.children.get(2);
                    else break;
                }
            }
        }

        // =========================================================================
        // NÚCLEO EVALUADOR: ARITMÉTICA DE ALTA PRECEDENCIA (*, /, %)
        // =========================================================================
        private void evaluateTerm(ASTNode term) {
            ASTNode factor = term.children.get(0);
            evaluateFactor(factor); // Resuelve la unidad básica fundamental (Hojas del AST)

            if (term.children.size() > 1) {
                ASTNode tail = term.children.get(1);
                while (!tail.children.isEmpty() && !tail.children.get(0).type.equals("EPSILON")) {
                    String op = tail.children.get(0).value;
                    ASTNode nextFactor = tail.children.get(1);
                    evaluateFactor(nextFactor);

                    textSection.append("    pop rbx\n"); // Operando derecho
                    textSection.append("    pop rax\n"); // Operando izquierdo

                    if (op.equals("*")) {
                        textSection.append("    imul rax, rbx\n"); // Multiplicación entera con signo de RAX por RBX -> Resultado en RAX
                        textSection.append("    push rax\n");
                    } else if (op.equals("/") || op.equals("%")) {
                        // Crucial en x86-64: La instrucción IDIV requiere que el dividendo ocupe 128 bits distribuidos en RDX:RAX.
                        // 'cqo' (Convert Quadword to Octaword) copia el bit de signo de RAX a lo largo de todo RDX de forma segura.
                        textSection.append("    cqo\n"); 
                        textSection.append("    idiv rbx\n"); // Divide el registro combinado RDX:RAX entre RBX
                        
                        if (op.equals("/")) textSection.append("    push rax\n"); // El cociente exacto se almacena siempre en RAX
                        else textSection.append("    push rdx\n");                // El residuo matemático (%) se almacena siempre en RDX
                    }

                    if (tail.children.size() > 2) tail = tail.children.get(2);
                    else break;
                }
            }
        }

        // =========================================================================
        // MÓDULO TERMINAL O PRODUCTOR: FACTORES BÁSICOS (evaluateFactor)
        // =========================================================================
        // Es la base de la pila de evaluación. Transforma valores del AST en datos reales dentro de la CPU.
        private void evaluateFactor(ASTNode factor) {
            if (factor.children.isEmpty()) return;
            ASTNode child = factor.children.get(0);
            
            // Caso A: Es un número entero directo literal (Constante)
            if (child.type.equals("NUMBER")) {
                // Inyecta el número de forma nativa a la pila de hardware de la CPU
                textSection.append("    push ").append(child.value).append("\n");
                
            // Caso B: Es una Variable
            } else if (child.type.equals("IDENTIFIER")) {
                // 'movsxd': Mueve un entero de 32 bits desde la memoria (RAM) y realiza una extensión de signo completa a 64 bits hacia RAX.
                // Esto es fundamental para evitar datos basura en operaciones matemáticas combinadas de 64 bits.
                textSection.append("    movsxd rax, dword [").append(child.value).append("]\n"); 
                textSection.append("    push rax\n"); // Coloca el valor extendido de la variable en la pila
                
            // Caso C: Paréntesis recursivos encajonados (ej: (5 + x) * 2 )
            } else if (child.type.equals("LPAREN")) {
                // Reinicia la jerarquía de operadores evaluando recursivamente la expresión interna
                evaluateExpression(factor.children.get(1));
            }
        }

        // Consolida los tres flujos de construcción de texto en un archivo estructurado final de NASM
        public String getFinalAssembly() {
            return dataSection.toString() + "\n" + bssSection.toString() + "\n" + textSection.toString();
        }
    }
    
    //main
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Error: Falta especificar la ruta del archivo de código.");
            return;
        }

        String archivo = args[0];

        try {
            //lexer -> analizador lexicop
            List<Token> tokens = lexer(archivo);
            System.out.println("====== TOKENS GENERADOS (LEXER) ======");
            for (Token t : tokens) {
                System.out.println(t);
            }
            System.out.println("======================================\n");

            //parser -> analizador sintactico
            Parser parser = new Parser(tokens);
            ASTNode rootNode = parser.parse();

            if (rootNode != null) {
                System.out.println("\n====== ANALISIS SINTACTICO ======");
                System.out.println("El codigo analizado cumple con la gramatica LL(1) definida.");
                System.out.println("===========================================\n");

                // ast
                System.out.println("====== ARBOL DE SINTAXIS ABSTRACTA (AST) ======");
                rootNode.print("");
                System.out.println("===============================================\n");

                // generacion de codigo nasm
                CodeGenerator codegen = new CodeGenerator();
                codegen.generate(rootNode);

                String assemblyOutput = codegen.getFinalAssembly();

                // impresion del codigo nasm
                System.out.println("====== CODIGO ENSAMBLADOR NASM GENERADO ======");
                System.out.println(assemblyOutput);
                System.out.println("==============================================\n");

                // se gusrada el archivo nasm
                String outputFileName = archivo.contains(".")
                        ? archivo.substring(0, archivo.lastIndexOf('.')) + ".asm"
                        : archivo + ".asm";

                Files.write(Paths.get(outputFileName), assemblyOutput.getBytes());

                System.out.println(">> ¡Exito! Archivo NASM guardado fisicamente como: " + outputFileName);
            }
        } catch (Exception e) {
            System.err.println("\nError en el proceso de compilacion: " + e.getMessage());
        }
    }
}
