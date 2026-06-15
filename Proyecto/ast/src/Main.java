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
        DATATYPE, IDENTIFIER, NUMBER, OPERATOR, STRING,
        SEMICOLON, COMMA, LPAREN, RPAREN, LBRACE, RBRACE,
        IF, ELSE, FOR, WHILE, RETURN, CLASS, IMPORT, PACKAGE,
        PUBLIC, PRIVATE, STATIC, ANNOTATION, ENUM, EOF, UNKNOWN
    }

    static class Token {
        TokenType type; String value;
        Token(TokenType type, String value) { this.type = type; this.value = value; }
        @Override public String toString() { return "[" + type + ": " + value + "]"; }
    }

    // lexer (solo quiero descansar)
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
            else if (p.matches("[=+\\-*/<>!&|]+")) tokens.add(new Token(TokenType.OPERATOR, p));
            else tokens.add(new Token(TokenType.UNKNOWN, p));
        }
        tokens.add(new Token(TokenType.EOF, "EOF"));
        return tokens;
    }

    //Nuevo Parser LL(1) Tabular
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
            rule("DECL_TAIL", TokenType.OPERATOR, "OPERATOR", "EXPRESSION", "SEMICOLON");
            rule("DECL_TAIL", TokenType.LPAREN, "LPAREN", "PARAMS", "RPAREN", "BLOCK");

            //Parametros
            rule("PARAMS", TokenType.DATATYPE, "DATATYPE", "IDENTIFIER", "PARAMS_TAIL");
            nullable("PARAMS", TokenType.DATATYPE);
            rule("PARAMS_TAIL", TokenType.COMMA, "COMMA", "DATATYPE", "IDENTIFIER", "PARAMS_TAIL");
            nullable("PARAMS_TAIL", TokenType.COMMA);

            //Asignaciones y llamadas a metodos
            rule("ID_STATEMENT", TokenType.IDENTIFIER, "IDENTIFIER", "ID_TAIL");
            rule("ID_TAIL", TokenType.OPERATOR, "OPERATOR", "EXPRESSION", "SEMICOLON");
            rule("ID_TAIL", TokenType.LPAREN, "LPAREN", "ARGS", "RPAREN", "SEMICOLON");

            //Argumentos
            rule("ARGS", TokenType.IDENTIFIER, "EXPRESSION", "ARGS_TAIL");
            rule("ARGS", TokenType.NUMBER, "EXPRESSION", "ARGS_TAIL");
            rule("ARGS", TokenType.STRING, "EXPRESSION", "ARGS_TAIL");
            nullable("ARGS", TokenType.IDENTIFIER, TokenType.NUMBER, TokenType.STRING);
            rule("ARGS_TAIL", TokenType.COMMA, "COMMA", "EXPRESSION", "ARGS_TAIL");
            nullable("ARGS_TAIL", TokenType.COMMA);

            //Expresiones
            rule("EXPRESSION_OPT", TokenType.IDENTIFIER, "EXPRESSION");
            rule("EXPRESSION_OPT", TokenType.NUMBER, "EXPRESSION");
            rule("EXPRESSION_OPT", TokenType.STRING, "EXPRESSION");
            nullable("EXPRESSION_OPT", TokenType.IDENTIFIER, TokenType.NUMBER, TokenType.STRING);

            rule("EXPRESSION", TokenType.IDENTIFIER, "TERM", "EXPR_TAIL");
            rule("EXPRESSION", TokenType.NUMBER, "TERM", "EXPR_TAIL");
            rule("EXPRESSION", TokenType.STRING, "TERM", "EXPR_TAIL");
            rule("EXPRESSION", TokenType.LPAREN, "TERM", "EXPR_TAIL");

            rule("EXPR_TAIL", TokenType.OPERATOR, "OPERATOR", "TERM", "EXPR_TAIL");
            nullable("EXPR_TAIL", TokenType.OPERATOR);

            rule("TERM", TokenType.IDENTIFIER, "IDENTIFIER");
            rule("TERM", TokenType.NUMBER, "NUMBER");
            rule("TERM", TokenType.STRING, "STRING");
            rule("TERM", TokenType.LPAREN, "LPAREN", "EXPRESSION", "RPAREN");
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

                    if (top.equals("EPSILON")) {
                        continue;
                    }

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
                            throw new RuntimeException("Falla en No-Terminal [" + top + "] con Token [" + lookahead.type + "]");
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

    //Main
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Error: Falta especificar la ruta del archivo de código.");
            System.err.println("Uso correcto: java Main <ruta_del_archivo.txt>");
            return;
        }

        String archivo = args[0];

        try {
            List<Token> tokens = lexer(archivo);
            Parser parser = new Parser(tokens);
            ASTNode rootNode = parser.parse();

            if (rootNode != null) {
                System.out.println("\n====== ÁRBOL DE SINTAXIS ABSTRACTA (AST) GENERADO ======");
                rootNode.print("");
                System.out.println("========================================================");
            }
        } catch (Exception e) {
            System.err.println("Error de lectura o procesamiento: " + e.getMessage());
        }
    }
}