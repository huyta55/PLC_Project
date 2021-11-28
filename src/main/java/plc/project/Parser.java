package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

/**
 * The parser takes the sequence of tokens emitted by the lexer and turns that
 * into a structured representation of the program, called the Abstract Syntax
 * Tree (AST).
 * <p>
 * The parser has a similar architecture to the lexer, just with {@link Token}s
 * instead of characters. As before, {@link #peek(Object...)} and {@link
 * #match(Object...)} are helpers to make the implementation easier.
 * <p>
 * This type of parser is called <em>recursive descent</em>. Each rule in our
 * grammar will have it's own function, and reference to other rules correspond
 * to calling that functions.
 */
public final class Parser {

    private final TokenStream tokens;
    public Parser(List<Token> tokens) {
        this.tokens = new TokenStream(tokens);
    }

    private void throwException(String exceptionName, int indexType) {
        // indexType 1 = tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()
        if (indexType == 0) {
            throw new ParseException(exceptionName, tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
        } else {
            throw new ParseException(exceptionName, tokens.get(0).getIndex());
        }
    }
    private void checkToken() {
        if (!tokens.has(0)) {
            throwException("Missing Token", 0);
        }
    }

    /**
     * Parses the {@code source} rule.
     */
    public Ast.Source parseSource() throws ParseException {
        // throw new UnsupportedOperationException(); //TODO
        // A source could be a global* or function* so do while loops for them
        List<Ast.Global> globalList = new ArrayList<Ast.Global>();
        List<Ast.Function> functionList = new ArrayList<Ast.Function>();
        // Check that there is a valid keyword in the current token
        if (peek("LIST") || peek("VAR") || peek("VAL") || peek("FUN")) {
            // While the next token matches on either keywords LIST, VAR, or VAL, loop and create the global list
            while ((peek("LIST")) || (peek("VAR")) || (peek("VAL"))) {
                if (match("LIST")) {
                    globalList.add(parseList());
                    // Check for the ';' at the end
                    checkToken();
                    if (!match(";")) {
                        throwException("Expecting Semicolon", 1);
                    }
                } else {
                    globalList.add(parseGlobal());
                    checkToken();
                    if (!match(";")) {
                        throwException("Expecting Semicolon", 1);
                    }
                }
            }
            while (match("FUN")) {
                // if there is a keyword FUN there, that means that there are no global to parse, so keep parsing the functions
                functionList.add(parseFunction());
            }
        }
        // If there's another token that doesn't have the right keyword, then throw exception
        else if (tokens.has(0)) {
            throw new ParseException("Invalid Source Keyword", 0);
        }
        return new Ast.Source(globalList, functionList);

    }

    /**
     * Parses the {@code field} rule. This method should only be called if the
     * next tokens start a field, aka {@code LET}.
     */
    public Ast.Global parseGlobal() throws ParseException {
        // throw new UnsupportedOperationException(); //TODO
        if (match("VAR")) {
            return parseMutable();
        }
        // Else it's a VAL and is parsed accordingly
        else {
            return parseImmutable();
        }
    }

    /**
     * Parses the {@code list} rule. This method should only be called if the
     * next token declares a list, aka {@code LIST}.
     */
    public Ast.Global parseList() throws ParseException {
// TODO Ask TA if parseLIST is supposed to return what it's returning
        // List is immutable
        // The LIST Token is already advanced from parseGlobal()
        // Check whether the next token is an identifier
        if (!tokens.has(0)) {
            throwException("Missing Identifier", 0);
        }
        if (match(Token.Type.IDENTIFIER)) {
            String name = tokens.get(-1).getLiteral();
            // Check for the '='
            if (!tokens.has(0)) {
                throwException("Missing =", 0);
            }
            if (match("=")) {
                // Check for the [
                if (!tokens.has(0)) {
                    throwException("Missing =", 0);
                }
                if (match("[")) {
                    if (!tokens.has(0)) {
                        throwException("Missing [", 0);
                    }
                    // Check for the first expression after the [
                    List<Ast.Expression> expressionList = new ArrayList<Ast.Expression>();
                    expressionList.add(parseExpression());
                    // While there are ", expression", add them to the expression list
                    while (match(",")) {
                        // Check that there's an expression after
                        if (!tokens.has(0)) {
                            throwException("Missing token", 0);
                        }
                        expressionList.add(parseExpression());
                    }
                    Ast.Expression.PlcList plcList = new Ast.Expression.PlcList(expressionList);
                    // check for the closing ]
                    if (!tokens.has(0)) {
                        throwException("Missing Token", 0);
                    }
                    if (match("]")) {
                        return new Ast.Global(name, true, Optional.of(plcList));
                    } else {
                        throwException("Expecting Closing Bracket ]", 1);
                    }
                } else {
                    throwException("Expecting Opening Bracket [", 1);
                }
            } else {
                throwException("Expecting =", 1);
            }
        } else {
            throwException("Expecting Identifier", 1);
        }
        return null;
    }

    /**
     * Parses the {@code mutable} rule. This method should only be called if the
     * next token declares a mutable global variable, aka {@code VAR}.
     */
    public Ast.Global parseMutable() throws ParseException {
        // Check whether an identifier follows the VAR
        if (match(Token.Type.IDENTIFIER)) {
            String name = tokens.get(-1).getLiteral();
            // Check whether there is a = that follows
            if (match("=")) {
                // Check that there is a token that comes after the =
                if (!tokens.has(0)) {
                    throw new ParseException("Expecting Identifier", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
                }
                return new Ast.Global(name, true, Optional.of(parseExpression()));
            }
            // if there's a token there, but it's not the =, then throw exception
            else if (tokens.has(0)) {
                throw new ParseException("Expecting =", tokens.get(0).getIndex());
            }
            // else it's just a variable and identifier so move on accordingly
            Optional<Ast.Expression> expressionOptional = Optional.empty();
            return new Ast.Global(name, true, expressionOptional);
        } else {
            throw new ParseException("Missing Identifier", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
        }
    }

    /**
     * Parses the {@code immutable} rule. This method should only be called if the
     * next token declares an immutable global variable, aka {@code VAL}.
     */
    public Ast.Global parseImmutable() throws ParseException {
        tokens.advance();
        // Check whether there's a token after the VAL
        if (!tokens.has(0)) {
            throw new ParseException("Expecting Identifier", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
        }
        // Check whether the token after VAL is an Identifier
        if (match(Token.Type.IDENTIFIER)) {
            String name = tokens.get(-1).getLiteral();
            // Check for the =
            if (!tokens.has(0)) {
                throw new ParseException("Missing =", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
            }
            if (match("=")) {
                // Check that the next token is an expression
                if (!tokens.has(0)) {
                    throw new ParseException("Missing expression", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
                }
                return new Ast.Global(name, false, Optional.of(parseExpression()));
            } else {
                throw new ParseException("Expecting =", tokens.get(0).getIndex());
            }
        } else {
            throw new ParseException("Expecting Identifier", tokens.get(0).getIndex());
        }
    }

    /**
     * Parses the {@code method} rule. This method should only be called if the
     * next tokens start a method, aka {@code DEF}.
     */
    public Ast.Function parseFunction() throws ParseException {
        // TODO: Check whether we have to account for empty token (index wise as well as existence wise)
        // TODO: Ask again about the index stuff when throwing an exception
        // TODO: Ask if we have to account for the parsedFunction just ending abruptly instead of failing a parse test
        // Check whether there is a valid function name after the FUN keyword, else throw a parse exception
        if (match(Token.Type.IDENTIFIER)) {
            // If the first identifier matches, then grab the name of the identifier since it's the name of the function
            String functionName = tokens.get(-1).getLiteral();
            // Create the parameter list so that it could be empty if there are parameters to parse
            List<String> parameterList = new ArrayList<String>();
            // Check if there is a '('. If not, then throw an exception
            if (match("(")) {
                // Check whether there is a token, else it's missing a )
                if (!tokens.has(0)) {
                    throw new ParseException("Missing Closing Parenthesis", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
                }
                // Check whether the next token is an identifier, otherwise it must be a ) since it's a function without parameters
                if (match(Token.Type.IDENTIFIER)) {
                    // If it's an identifier, then make a while loop for , Identifier and store them all in a list of parameters
                    // TODO: Ask the TA what the different b/w new ArrayList and Collections.EmptyList is
                    String parameter1 = tokens.get(-1).getLiteral();
                    parameterList.add(parameter1);
                    // Keep the loop going while there's another comma to parse as there are more parameters
                    while (match(",")) {
                        // Check that there's a token after the ','. Else it's a trailing comma
                        if (!tokens.has(0)) {
                            throw new ParseException("Trailing Comma", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
                        }
                        // Check the there is an identifier after the ','. Else it's an invalid parameter
                        if (match(Token.Type.IDENTIFIER)) {
                            // Create and add the new parameter to the List of parameters
                            String tempParameter = tokens.get(-1).getLiteral();
                            parameterList.add(tempParameter);
                        } else {
                            throw new ParseException("Identifier Expected", tokens.get(0).getIndex());
                        }
                    }
                    // Check that there is a closing parenthesis
                    if (match(")")) {
                        // If there is a ), then move on and check that the next keyword is the DO keyword
                        if (match("DO")) {
                            // Either there will be a block of statements here or it will be empty
                            // If next token matches the END keyword, then it's an empty block of statements and carry on
                            if (match("END")) {
                                // If it's empty list of statement, then create the Ast.Function using the name and parameters from earlier;
                                List<Ast.Statement> statementList = new ArrayList<Ast.Statement>();
                                return new Ast.Function(functionName, parameterList, statementList);
                            }
                            // Else parse the statements
                            else {
                                List<Ast.Statement> statementList = parseBlock();
                                return new Ast.Function(functionName, parameterList, statementList);
                            }
                        } else {
                            throw new ParseException("Missing Do Keyword", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
                        }
                    } else {
                        throw new ParseException("Missing Closing Parenthesis", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
                    }
                } else if (match(")")) {
                    // If there is no identifier, then it's a no parameter function, check the DO keyword;
                    if (match("DO")) {
                        // Check if the next token is END which means it's an empty Block or if there is a block of statements there
                        if (match("END")) {
                            List<Ast.Statement> statementList = new ArrayList<Ast.Statement>();
                            // If the block is empty, then create the function Ast and return it
                            return new Ast.Function(functionName, parameterList, statementList);
                        } else {
                            // else parse the block into a statement list and then create the function ast and return it
                            List<Ast.Statement> statementList = parseBlock();
                            return new Ast.Function(functionName, parameterList, statementList);
                        }
                    } else {
                        throw new ParseException("Missing DO Keyword", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
                    }
                } else {
                    throw new ParseException("Invalid Parameters", tokens.get(0).getIndex());
                }
            } else {
                throw new ParseException("Missing Opening Parenthesis", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
            }
        } else {
            throw new ParseException("Missing Identifier", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
        }
    }

    /**
     * Parses the {@code block} rule. This method should only be called if the
     * preceding token indicates the opening a block.
     */
    public List<Ast.Statement> parseBlock() throws ParseException {
        ArrayList<Ast.Statement> statements = parseStatementBlock();
        return statements;
    }

    /**
     * Parses the {@code statement} rule and delegates to the necessary method.
     * If the next tokens do not start a declaration, if, while, or return
     * statement, then it is an expression/assignment statement.
     */
    public Ast.Statement parseStatement() throws ParseException {

        if (peek("LET")) return parseDeclarationStatement();
        else if (peek("IF")) return parseIfStatement();
        else if (peek("SWITCH")) return parseSwitchStatement();
        else if (peek("WHILE")) return parseWhileStatement();
        else if (peek("RETURN")) return parseReturnStatement();
        else {
            Ast.Expression current = parseExpression();
            if (peek("=")) {
                tokens.advance();
                Ast.Expression value = parseExpression();
                if (peek(";")) {
                    tokens.advance();
                    return new Ast.Statement.Assignment(current, value);
                } else {
                    if (tokens.has(0)) throw new ParseException("missing ;", tokens.index);
                    else throw new ParseException("missing  ;", tokens.index);
                }
            } else {
                if (peek(";")) {
                    tokens.advance();
                    return new Ast.Statement.Expression(current);
                }
                else {
                    if (tokens.has(0))
                        throw new ParseException("no ;", tokens.get(0).getIndex());
                    else throw new ParseException("no ;" + " INDEX:", tokens.index);
                }
            }
        }
    }

    /**
     * Parses a declaration statement from the {@code statement} rule. This
     * method should only be called if the next tokens start a declaration
     * statement, aka {@code LET}.
     */
    public Ast.Statement.Declaration parseDeclarationStatement() throws ParseException {
        tokens.advance();
        Token Name = tokens.get(0);
        tokens.advance();
        if (peek(";")) {
            parseSemicolon();
            return new Ast.Statement.Declaration(Name.getLiteral(), Optional.empty());
        }
        if (peek("=")) {
            tokens.advance();
        }
        Ast.Expression val = parseExpression();
        parseSemicolon();
        return new Ast.Statement.Declaration(Name.getLiteral(), Optional.of(val));

    }

    /**
     * Parses an if statement from the {@code statement} rule. This method
     * should only be called if the next tokens start an if statement, aka
     * {@code IF}.
     */
    public Ast.Statement.If parseIfStatement() throws ParseException {
        // throw new UnsupportedOperationException(); //TODO
        tokens.advance();
        Ast.Expression expr = parseExpression();

        parseDo();

        ArrayList<Ast.Statement> statements = parseStatementBlock();
        Token BackTrace = tokens.get(0);
        ArrayList<Ast.Statement> elseStatements = new ArrayList<>();

        if (BackTrace.getLiteral() == "ELSE") {
            tokens.advance();
            elseStatements = parseStatementBlock();
            parseDone();
            return new Ast.Statement.If(expr, statements, elseStatements);
        }
        parseDone();
        return new Ast.Statement.If(expr, statements, elseStatements);
    }

    /**
     * Parses a switch statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a switch statement, aka
     * {@code SWITCH}.
     */
    public Ast.Statement.Switch parseSwitchStatement() throws ParseException {
        //throw new UnsupportedOperationException(); //TODO
        List<Ast.Statement.Case> casesExpressionList = new ArrayList<>();
        tokens.advance();
        Ast.Expression expr = parseExpression();
        tokens.advance();

        if (peek("CASE")) {
            casesExpressionList.add(parseCaseStatement());
        }

        tokens.advance();
        if (peek("DEFAULT")) tokens.advance();
        ArrayList<Ast.Statement> statements = parseStatementBlock();

        tokens.advance();
        parseDone();

        return new Ast.Statement.Switch(expr, casesExpressionList);

    }

    /**
     * Parses a case or default statement block from the {@code switch} rule.
     * This method should only be called if the next tokens start the case or
     * default block of a switch statement, aka {@code CASE} or {@code DEFAULT}.
     */
    public Ast.Statement.Case parseCaseStatement() throws ParseException {
        // throw new UnsupportedOperationException(); //TODO
        tokens.advance();
        Ast.Expression val = parseExpression();
        // tokens.advance();
        if (peek(":")) parseColon();
        List<Ast.Statement> statements = parseBlock();
        return new Ast.Statement.Case(Optional.of(val), statements);
    }

    /**
     * Parses a while statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a while statement, aka
     * {@code WHILE}.
     */
    public Ast.Statement.While parseWhileStatement() throws ParseException {
        Token tmp = tokens.get(0);
        tokens.advance();
        checkToken();
        Ast.Expression doWhile = parseExpression();
        tmp = tokens.get(0);

        parseDo();
        List<Ast.Statement> statements = parseBlock();
        parseDone();

        return new Ast.Statement.While(doWhile, statements);
    }

    /**
     * Parses a return statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a return statement, aka
     * {@code RETURN}.
     */
    public Ast.Statement.Return parseReturnStatement() throws ParseException {
        // throw new UnsupportedOperationException(); //TODO

        tokens.advance();
        Ast.Expression return_exp = parseExpression();

        Token tmp = tokens.get(0);
        parseSemicolon();
        return new Ast.Statement.Return(return_exp);
    }

    /**
     * Parses the {@code expression} rule.
     */
    public Ast.Expression parseExpression() throws ParseException {
        return parseLogicalExpression();
    }

    /**
     * Parses the {@code logical-expression} rule.
     */
    public Ast.Expression parseLogicalExpression() throws ParseException {
        // throw new UnsupportedOperationException(); //TODO
        Ast.Expression a = parseComparisonExpression();
        while ((match("&&") || (match("||")))) {
            System.out.println("got here");
            String operator = tokens.get(-1).getLiteral();
            Ast.Expression b = new Ast.Expression.Binary(operator, a, parseComparisonExpression());
            a = b;
        }
        return a;
    }

    /**
     * Parses the {@code equality-expression} rule.
     */
    public Ast.Expression parseComparisonExpression() throws ParseException {
        // throw new UnsupportedOperationException(); //TODO
        Ast.Expression a = parseAdditiveExpression();
        while ((match("<") || (match(">")) || (match("==")) || (match("!=")))) {
            checkToken();
            String operator = tokens.get(-1).getLiteral();
            Ast.Expression b = new Ast.Expression.Binary(operator, a, parseAdditiveExpression());
            a = b;
        }
        return a;
    }

    /**
     * Parses the {@code additive-expression} rule.
     */
    public Ast.Expression parseAdditiveExpression() throws ParseException {
        // throw new UnsupportedOperationException(); //TODO
        Ast.Expression a = parseMultiplicativeExpression();
        while ((match("+") || (match("-")))) {
            checkToken();
            String operator = tokens.get(-1).getLiteral();
            Ast.Expression b = new Ast.Expression.Binary(operator, a, parseMultiplicativeExpression());
            a = b;
        }
        return a;
    }

    /**
     * Parses the {@code multiplicative-expression} rule.
     */
    public Ast.Expression parseMultiplicativeExpression() throws ParseException {
        Ast.Expression a = parsePrimaryExpression();
        while ((match("*") || (match("/")) || (match("^")))) {
            String operator = tokens.get(-1).getLiteral();
            Ast.Expression b = new Ast.Expression.Binary(operator, a, parsePrimaryExpression());
            a = b;
        }
        return a;
    }

    /**
     * Parses the {@code primary-expression} rule. This is the top-level rule
     * for expressions and includes literal values, grouping, variables, and
     * functions. It may be helpful to break these up into other methods but is
     * not strictly necessary.
     */
    public Ast.Expression parsePrimaryExpression() throws ParseException {
        //throw new UnsupportedOperationException(); //TODO
        if (match("TRUE")) {
            return new Ast.Expression.Literal(new Boolean(true));
        } else if (match("FALSE")) {
            return new Ast.Expression.Literal(new Boolean(false));
        } else if (match("NIL")) {
            return new Ast.Expression.Literal(null);
        } else if (match(Token.Type.INTEGER)) {
            return new Ast.Expression.Literal(new BigInteger(tokens.get(-1).getLiteral()));
        } else if (match(Token.Type.DECIMAL)) {
            return new Ast.Expression.Literal((new BigDecimal(tokens.get(-1).getLiteral())));
        } else if (match(Token.Type.CHARACTER)) {
            String characterLiteral = tokens.get(-1).getLiteral();
            String removed = characterLiteral.replaceAll("\'", "");
            return new Ast.Expression.Literal(removed.charAt(0));
        } else if (match(Token.Type.STRING)) {
            String stringLiteral = tokens.get(-1).getLiteral();
            String removed = stringLiteral.replaceAll("\"", "");
            removed = removed.replaceAll("\\\\n", "\n");
            removed = removed.replaceAll("\\\\r", "\r");
            removed = removed.replaceAll("\\\\b", "\b");
            removed = removed.replaceAll("\\\\t", "\t");
            removed = removed.replaceAll("\\\\", "\\");
            return new Ast.Expression.Literal(new String(removed));
        } else if (match(Token.Type.IDENTIFIER)) {
            String name = tokens.get(-1).getLiteral();
            Optional o = Optional.empty();
            if (match("[")) {
                checkToken();
                String name2 = tokens.get(0).getLiteral();
                Ast.Expression.Access access = new Ast.Expression.Access(Optional.empty(), name2);
                tokens.advance();
                checkToken();
                // Check for the "]"
                if (match("]")) {
                    return new Ast.Expression.Access(Optional.of(access), name);
                }
                else {
                    throwException("Expecting ]", 1);
                }
            }
            if (match("(")) {
                List<Ast.Expression> javaList = new ArrayList<>();
                checkToken();
                if (match(")")) {
                    Ast.Expression.PlcList plcList = new Ast.Expression.PlcList(javaList);
                    return new Ast.Expression.Function(name, plcList.getValues());
                }
                else if (match(Token.Type.OPERATOR)) {
                    throwException("Invalid Closing Bracket", 1);
                }
                else {
                    checkToken();
                    String access1Name = tokens.get(0).getLiteral();
                    tokens.advance();
                    Ast.Expression.Access access1 = new Ast.Expression.Access(Optional.empty(), access1Name);
                    javaList.add((access1));
                    while (match(",")) {
                        checkToken();
                        if (match(Token.Type.OPERATOR)) {
                            throwException("Trailing Comma", 0);
                        }
                        String tempName = tokens.get(0).getLiteral();
                        tokens.advance();
                        Ast.Expression.Access tempAccess = new Ast.Expression.Access(Optional.empty(), tempName);
                        javaList.add(tempAccess);

                    }
                    // Check for the closing )
                    checkToken();
                    if (match(")")) {
                        Ast.Expression.PlcList plcList = new Ast.Expression.PlcList(javaList);
                        return new Ast.Expression.Function(name, plcList.getValues());
                    }
                    else {
                        throwException("Expecting )", 1);
                    }
                }
            }
            return new Ast.Expression.Access(o, name);
        } else if (match("(")) {
            // If there's no expression between the ' ', then the '('expression')' rule fails and exception is thrown
            if (match(")")) {
                throw new ParseException("Missing Expression", tokens.get(-1).getIndex());
            } else {
                // if the next token after '(' isn't ')', that means that there is an expression in the middle, so we parse it
                Ast.Expression a = parseExpression();
                // Before returning a group Ast, check that there is a closing ')', else throw an exception
                if (match(")")) {
                    return new Ast.Expression.Group(a);
                } else {
                    throw new ParseException("Unterminated Group", tokens.get(-1).getIndex());
                }

            }
        } else {
            throwException("Invalid Primary Exception", 1);
        }
        return null;
        //TODO Replace -1 with actual character index
    }

    /**
     * As in the lexer, returns {@code true} if the current sequence of tokens
     * matches the given patterns. Unlike the lexer, the pattern is not a regex;
     * instead it is either a {@link Token.Type}, which matches if the token's
     * type is the same, or a {@link String}, which matches if the token's
     * literal is the same.
     * <p>
     * In other words, {@code Token(IDENTIFIER, "literal")} is matched by both
     * {@code peek(Token.Type.IDENTIFIER)} and {@code peek("literal")}.
     */
    private boolean peek(Object... patterns) {
        for (int i = 0; i < patterns.length; i++) {
            if (!tokens.has(i)) {
                return false;
            } else if (patterns[i] instanceof Token.Type) {
                if (patterns[i] != tokens.get(i).getType()) {
                    return false;
                }
            } else if (patterns[i] instanceof String) {
                if (!patterns[i].equals(tokens.get(i).getLiteral())) {
                    return false;
                }
            } else {
                throw new AssertionError("Invalid pattern object: " + patterns[i].getClass());
            }
        }
        return true;
    }

    /**
     * As in the lexer, returns {@code true} if {@link #peek(Object...)} is true
     * and advances the token stream.
     */
    private boolean match(Object... patterns) {
        boolean peek = peek(patterns);
        if (peek) {
            for (int i = 0; i < patterns.length; i++) {
                tokens.advance();
            }
        }
        return peek;
    }

    private ArrayList<Ast.Statement> parseStatementList() {
        ArrayList<Ast.Statement> statements = new ArrayList<>();
        while (tokens.has(0)) {
            Token tmp2 = tokens.get(0);
            if (peek("END")) break;
            if (peek("ELSE")) break;

            statements.add(parseStatement());
        }
        return statements;
    }
    private ArrayList<Ast.Statement> parseStatementBlock() {
        ArrayList<Ast.Statement> statements = parseStatementList();
        return statements;

    }
    private void parseSemicolon() {
        if (!peek(";")) {
            throw new ParseException("Missing ;", tokens.index);
        }
        tokens.advance();
    }
    private void parseColon() {
        if (!peek(":")) {
            throw new ParseException("Missing :", tokens.index);
        }
        tokens.advance();
    }
    private void parseDone() {
        if (!peek("END") && !peek("ELSE")) {
            throw new ParseException("BLOCK Needs End or ELSE", tokens.index);
        }
        tokens.advance();
    }
    private void parseDo() {
        if (!peek("DO")) {
            throw new ParseException("BLOCK Needs DO ", tokens.index);
        }
        tokens.advance();
    }
    private Ast.Statement parseFunctionCall() {
        Ast.Expression expr = parseExpression();
        parseSemicolon();
        return new Ast.Statement.Expression(expr);
    }
    private Ast.Statement parseAssignmentStatement() {
        Ast.Expression recv = parseExpression();
        tokens.advance();
        Ast.Expression Val = parseExpression();
        parseSemicolon();

        return new Ast.Statement.Assignment(recv, Val);
    }

    private static final class TokenStream {

        private final List<Token> tokens;
        private int index = 0;

        private TokenStream(List<Token> tokens) {
            this.tokens = tokens;
        }

        /**
         * Returns true if there is a token at index + offset.
         */
        public boolean has(int offset) {
            return index + offset < tokens.size();
        }

        /**
         * Gets the token at index + offset.
         */
        public Token get(int offset) {
            return tokens.get(index + offset);
        }

        /**
         * Advances to the next token, incrementing the index.
         */
        public void advance() {
            index++;
        }

    }

}
