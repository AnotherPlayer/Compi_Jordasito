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
    static class CodeGenerator {
        private StringBuilder dataSection;
        private StringBuilder bssSection;
        private StringBuilder textSection;

        private int labelCounter = 0;
        private int stringCounter = 0;
        private Set<String> variables = new HashSet<>();

        public CodeGenerator() {
            dataSection = new StringBuilder();
            dataSection.append("default rel\n"); // MAGIA PARA MAC: Direccionamiento relativo
            dataSection.append("section .data\n");
            dataSection.append("    fmt_int db \"%d\", 10, 0\n");

            bssSection = new StringBuilder();
            bssSection.append("section .bss\n");

            textSection = new StringBuilder();
            textSection.append("section .text\n");
            textSection.append("    global _main\n"); // MAC requiere guion bajo
            textSection.append("    extern _printf\n\n"); // MAC requiere guion bajo
        }

        private String getNewLabel() { return "L" + (++labelCounter); }
        private String getNewStringLabel() { return "str" + (++stringCounter); }

        public void generate(ASTNode node) {
            if (node == null) return;

            switch (node.type) {
                case "DECLARATION":
                    handleDeclaration(node);
                    return;
                case "ID_STATEMENT":
                    handleIdStatement(node);
                    return;
                case "STATEMENT":
                    if (!node.children.isEmpty() && node.children.get(0).type.equals("IF")) {
                        handleIf(node);
                        return;
                    } else if (!node.children.isEmpty() && node.children.get(0).type.equals("WHILE")) {
                        handleWhile(node);
                        return;
                    }
                    break;
            }

            for (ASTNode child : node.children) {
                generate(child);
            }
        }

        private void handleDeclaration(ASTNode node) {
            String id = node.children.get(1).value;
            ASTNode tail = node.children.get(2);

            if (!tail.children.isEmpty() && tail.children.get(0).type.equals("LPAREN")) {
                String funcName = id.equals("main") ? "_main" : id;
                textSection.append(funcName).append(":\n");
                textSection.append("    push rbp\n");
                textSection.append("    mov rbp, rsp\n\n");

                if (tail.children.size() > 3) {
                    ASTNode blockNode = tail.children.get(3);
                    generate(blockNode);
                }

                if (id.equals("main")) {
                    textSection.append("    mov eax, 0\n");
                }
                textSection.append("    mov rsp, rbp\n");
                textSection.append("    pop rbp\n");
                textSection.append("    ret\n\n");

            } else {
                if (!variables.contains(id)) {
                    bssSection.append("    ").append(id).append(" resd 1\n");
                    variables.add(id);
                }

                if (tail.children.size() > 1 && tail.children.get(0).type.equals("ASSIGN_OP")) {
                    ASTNode expr = tail.children.get(1);
                    evaluateExpression(expr);
                    textSection.append("    pop rax\n"); // 64 bits
                    textSection.append("    mov dword [").append(id).append("], eax\n\n");
                }
            }
        }

        private void handleIdStatement(ASTNode node) {
            String id = node.children.get(0).value;
            ASTNode tail = node.children.get(1);

            if (tail.children.get(0).type.equals("ASSIGN_OP")) {
                ASTNode expr = tail.children.get(1);
                evaluateExpression(expr);
                textSection.append("    pop rax\n"); // 64 bits
                textSection.append("    mov dword [").append(id).append("], eax\n\n");

            } else if (tail.children.get(0).type.equals("LPAREN")) {
                if (id.contains("print") || id.contains("System.out")) {
                    ASTNode args = tail.children.get(1);
                    if (!args.children.isEmpty() && args.children.get(0).type.equals("EXPRESSION")) {
                        ASTNode expr = args.children.get(0);

                        if (isStringExpression(expr)) {
                            String strVal = getStringValue(expr);
                            String strLabel = getNewStringLabel();
                            dataSection.append("    ").append(strLabel).append(" db ").append(strVal).append(", 10, 0\n");
                            textSection.append("    lea rdi, [").append(strLabel).append("]\n"); // MAC LEA rel
                            textSection.append("    mov al, 0\n");
                            textSection.append("    call _printf\n\n");
                        } else {
                            evaluateExpression(expr);
                            textSection.append("    pop rsi\n");
                            textSection.append("    lea rdi, [fmt_int]\n"); // MAC LEA rel
                            textSection.append("    mov al, 0\n");
                            textSection.append("    call _printf\n\n");
                        }
                    }
                } else {
                    String funcName = id.equals("main") ? "_main" : id;
                    textSection.append("    call ").append(funcName).append("\n\n");
                }
            }
        }

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

        private void handleIf(ASTNode node) {
            String labelElse = getNewLabel();
            String labelEnd = getNewLabel();

            textSection.append("    ; --- INICIO IF ---\n");
            ASTNode expr = node.children.get(2);
            evaluateExpression(expr);

            textSection.append("    pop rax\n");
            textSection.append("    cmp rax, 0\n");
            textSection.append("    je ").append(labelElse).append(" ; Salta al ELSE si es falso\n\n");

            generate(node.children.get(4));
            textSection.append("    jmp ").append(labelEnd).append("\n");

            textSection.append(labelElse).append(":\n");
            ASTNode elseOpt = node.children.get(5);
            if (!elseOpt.children.isEmpty() && elseOpt.children.get(0).type.equals("ELSE")) {
                generate(elseOpt.children.get(1));
            }
            textSection.append(labelEnd).append(":\n\n");
        }

        private void handleWhile(ASTNode node) {
            String labelStart = getNewLabel();
            String labelEnd = getNewLabel();

            textSection.append("    ; --- INICIO WHILE ---\n");
            textSection.append(labelStart).append(":\n");

            ASTNode expr = node.children.get(2);
            evaluateExpression(expr);

            textSection.append("    pop rax\n");
            textSection.append("    cmp rax, 0\n");
            textSection.append("    je ").append(labelEnd).append(" ; Salir del bucle si es falso\n\n");

            generate(node.children.get(4));

            textSection.append("    jmp ").append(labelStart).append("\n");
            textSection.append(labelEnd).append(":\n\n");
        }

        private void evaluateExpression(ASTNode expr) {
            ASTNode arith = expr.children.get(0);
            evaluateArithExpr(arith);

            if (expr.children.size() > 1) {
                ASTNode tail = expr.children.get(1);
                while (!tail.children.isEmpty() && !tail.children.get(0).type.equals("EPSILON")) {
                    String op = tail.children.get(0).value;
                    ASTNode nextArith = tail.children.get(1);
                    evaluateArithExpr(nextArith);

                    textSection.append("    pop rbx\n"); // 64 bits
                    textSection.append("    pop rax\n");
                    textSection.append("    cmp rax, rbx\n");

                    String labelTrue = getNewLabel();
                    String labelSkip = getNewLabel();

                    if (op.equals("==")) textSection.append("    je ").append(labelTrue).append("\n");
                    else if (op.equals("!=")) textSection.append("    jne ").append(labelTrue).append("\n");
                    else if (op.equals("<")) textSection.append("    jl ").append(labelTrue).append("\n");
                    else if (op.equals(">")) textSection.append("    jg ").append(labelTrue).append("\n");
                    else if (op.equals("<=")) textSection.append("    jle ").append(labelTrue).append("\n");
                    else if (op.equals(">=")) textSection.append("    jge ").append(labelTrue).append("\n");

                    textSection.append("    push 0\n");
                    textSection.append("    jmp ").append(labelSkip).append("\n");

                    textSection.append(labelTrue).append(":\n");
                    textSection.append("    push 1\n");

                    textSection.append(labelSkip).append(":\n");

                    if (tail.children.size() > 2) tail = tail.children.get(2);
                    else break;
                }
            }
        }

        private void evaluateArithExpr(ASTNode arith) {
            ASTNode term = arith.children.get(0);
            evaluateTerm(term);

            if (arith.children.size() > 1) {
                ASTNode tail = arith.children.get(1);
                while (!tail.children.isEmpty() && !tail.children.get(0).type.equals("EPSILON")) {
                    String op = tail.children.get(0).value;
                    ASTNode nextTerm = tail.children.get(1);
                    evaluateTerm(nextTerm);

                    textSection.append("    pop rbx\n");
                    textSection.append("    pop rax\n");

                    if (op.equals("+")) {
                        textSection.append("    add rax, rbx\n");
                        textSection.append("    push rax\n");
                    } else if (op.equals("-")) {
                        textSection.append("    sub rax, rbx\n");
                        textSection.append("    push rax\n");
                    }

                    if (tail.children.size() > 2) tail = tail.children.get(2);
                    else break;
                }
            }
        }

        private void evaluateTerm(ASTNode term) {
            ASTNode factor = term.children.get(0);
            evaluateFactor(factor);

            if (term.children.size() > 1) {
                ASTNode tail = term.children.get(1);
                while (!tail.children.isEmpty() && !tail.children.get(0).type.equals("EPSILON")) {
                    String op = tail.children.get(0).value;
                    ASTNode nextFactor = tail.children.get(1);
                    evaluateFactor(nextFactor);

                    textSection.append("    pop rbx\n");
                    textSection.append("    pop rax\n");

                    if (op.equals("*")) {
                        textSection.append("    imul rax, rbx\n");
                        textSection.append("    push rax\n");
                    } else if (op.equals("/") || op.equals("%")) {
                        textSection.append("    cqo\n"); // 64-bit Sign Extend RAX a RDX:RAX
                        textSection.append("    idiv rbx\n");
                        if (op.equals("/")) textSection.append("    push rax\n");
                        else textSection.append("    push rdx\n");
                    }

                    if (tail.children.size() > 2) tail = tail.children.get(2);
                    else break;
                }
            }
        }

        private void evaluateFactor(ASTNode factor) {
            if (factor.children.isEmpty()) return;
            ASTNode child = factor.children.get(0);
            if (child.type.equals("NUMBER")) {
                textSection.append("    push ").append(child.value).append("\n");
            } else if (child.type.equals("IDENTIFIER")) {
                textSection.append("    movsxd rax, dword [").append(child.value).append("]\n"); // Extiende la variable a 64 bits
                textSection.append("    push rax\n");
            } else if (child.type.equals("LPAREN")) {
                evaluateExpression(factor.children.get(1));
            }
        }

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
