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

    // TODO: Fix Switch Statement
    // TODO: Fix Statement Declaration
    // TODO: Fix If Statement

    private final TokenStream tokens;
    public Parser(List<Token> tokens) {
        this.tokens = new TokenStream(tokens);
    }

    private void throwException(String exceptionName, int indexType) {
        // indexType 0 = tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()
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
    private void parseSemicolon() {
        if (!peek(";")) {
            throwException("Missing ;", 0);
        }
        tokens.advance();
    }
    private void parseColon() {
        if (!peek(":")) {
            throwException("Missing :", 0);
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
            // Check for the :
            checkToken();
            if (match(":")) {
                // Check for the identifier after the :
                if (!tokens.has(0)) {
                    throwException("Expecting identifier", 0);
                }
                if (match(Token.Type.IDENTIFIER)) {
                    String typeName = tokens.get(-1).getLiteral();
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
                                return new Ast.Global(name, typeName,true, Optional.of(plcList));
                            } else {
                                throwException("Expecting Closing Bracket ]", 1);
                            }
                        } else {
                            throwException("Expecting Opening Bracket [", 1);
                        }
                    } else {
                        throwException("Expecting =", 1);
                    }
                }
                else {
                    throwException("Expecting Identifier", 1);
                }

            }
            else {
                throwException("Expecting :", 1);
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
            // Check for the :
            checkToken();
            if (!match(":")) {
                throwException("Expecting :", 0);
            }
            // Check for the identifier
            checkToken();
            if (!match(Token.Type.IDENTIFIER)) {
                throwException("Expecting identifier", 0);
            }
            String typeName = tokens.get(-1).getLiteral();
            // Check whether there is a = that follows
            if (match("=")) {
                // Check that there is a token that comes after the =
                if (!tokens.has(0)) {
                    throw new ParseException("Expecting Identifier", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
                }
                Optional expr = Optional.of(parseExpression());
                // check for the closing semicolon
                if (!match(";")) {
                    throwException("Missing Semicolon", 0);
                }
                return new Ast.Global(name, typeName, true, expr);
            }
            // if there's a token there, but it's not the =, then throw exception
            else if (tokens.has(0) && !match(";")) {
                throw new ParseException("Expecting =", tokens.get(0).getIndex());
            }
            // else it's just a variable and identifier so move on accordingly
            Optional<Ast.Expression> expressionOptional = Optional.empty();
            return new Ast.Global(name, typeName, true, expressionOptional);
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
        checkToken();
        // Check whether the token after VAL is an Identifier
        if (match(Token.Type.IDENTIFIER)) {
            String name = tokens.get(-1).getLiteral();
            // Check for the :
            checkToken();
            if (!match(":")) {
                throwException("Missing :", 0);
            }
            // Check for the Identifier
            checkToken();
            if (!match(Token.Type.IDENTIFIER)) {
                throwException("Expecting Identifier", 0);
            }
            String typeName = tokens.get(-1).getLiteral();
            // Check for the =
            checkToken();
            if (match("=")) {
                // Check that the next token is an expression
                if (!tokens.has(0)) {
                    throw new ParseException("Missing expression", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
                }
                return new Ast.Global(name, typeName,false, Optional.of(parseExpression()));
            }
            else {
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
        if (peek("FUN")) {
            tokens.advance();
        }
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
                boolean type = false;
                String returnType = "";
                List<String> parameterTypes = new ArrayList<>();
                // Check whether the next token is an identifier, otherwise it must be a ) since it's a function without parameters
                if (match(Token.Type.IDENTIFIER)) {
                    // If it's an identifier, then make a while loop for , Identifier and store them all in a list of parameters
                    String parameter1 = tokens.get(-1).getLiteral();
                    parameterList.add(parameter1);
                    // Checking the : after the 2nd identifier
                    checkToken();
                    if (!match(":")) {
                        throwException("Expecting :", 1);
                    }
                    // Checking the 2nd identifier after the ( that tells the type
                    checkToken();
                    if (!match(Token.Type.IDENTIFIER)) {
                        throwException("Expecting Identifier", 1);
                    }
                    Optional<String> typeName = Optional.of(tokens.get(-1).getLiteral());
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
                        // Check for the : identifier for the type
                        checkToken();
                        if (!match(":")) {
                            throwException("Expecting :", 1);
                        }
                        checkToken();
                        if (!match(Token.Type.IDENTIFIER)) {
                            throwException("Expecting IDENTIFIER", 1);
                        }
                        String tempParameterType = tokens.get(-1).getLiteral();
                        parameterTypes.add(tempParameterType);
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
                                return new Ast.Function(functionName, parameterTypes, parameterList, typeName, statementList);
                            }
                            // Else parse the statements
                            else {
                                List<Ast.Statement> statementList = parseBlock();
                                // check for the semicolon
                                parseSemicolon();
                                // check for end
                                if (!match("END")) {
                                    throwException("Missing End", 0);
                                }
                                return new Ast.Function(functionName, parameterTypes, parameterList, typeName, statementList);
                            }
                        } else {
                            throw new ParseException("Missing Do Keyword", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
                        }
                    }
                    else {
                        throw new ParseException("Missing Closing Parenthesis", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
                    }
                // Function with no Parameters
                } else if (match(")")) {
                    // if there is a colon, then there's a type
                    if (match(":")) {
                        checkToken();
                        if (!match(Token.Type.IDENTIFIER)) {
                            throwException("Expecting Identifier", 1);
                        }
                        returnType = tokens.get(-1).getLiteral();
                        type = true;
                    }
                    // If there is no identifier, then it's a no parameter function, check the DO keyword;
                    if (match("DO")) {
                        // Check if the next token is END which means it's an empty Block or if there is a block of statements there
                        if (match("END")) {
                            List<Ast.Statement> statementList = new ArrayList<Ast.Statement>();
                            // If the block is empty, then create the function Ast and return it
                            // if type is true, then return with a type
                            if (type) {
                                return new Ast.Function(functionName, parameterList, parameterTypes, Optional.of(returnType), statementList);
                            }
                            return new Ast.Function(functionName, parameterList, statementList);
                        } else {
                            // else parse the block into a statement list and then create the function ast and return it
                            List<Ast.Statement> statementList = parseBlock();
                            // Check for the DO
                            if (!match("END")) {
                                throwException("Missing End", 0);
                            }
                            if (type) {
                                return new Ast.Function(functionName, parameterList, parameterTypes, Optional.of(returnType), statementList);
                            }
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
        List<Ast.Statement> statements = new ArrayList<>();
        while (tokens.has(0)) {
            if (peek("END")) {
                break;
            }
            if (peek("ELSE")) {
                break;
            }
            if (peek("DEFAULT")) {
                break;
            }
            if (peek("CASE")) {
                break;
            }
            statements.add(parseStatement());
        }
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
                    if (tokens.has(0)) throw new ParseException("missing ;", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
                    else throw new ParseException("missing  ;", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
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
        // if the token that follows is a ";", then it's just Let IDENTIFIER;
        if (peek(";")) {
            parseSemicolon();
            return new Ast.Statement.Declaration(Name.getLiteral(), Optional.empty());
        }
        // Using boolean to see if there is a typeName at all
        boolean typeCheck = false;
        Optional<String> typeName = Optional.of("hi");
        // Checking for the :
        checkToken();
        if (match(":")) {
            // Checking for the Identifier
            checkToken();
            if (!match(Token.Type.IDENTIFIER)) {
                throwException("Expecting Identifier", 1);
            }
            typeCheck = true;
            typeName = Optional.of(tokens.get(-1).getLiteral());
        }
        if (peek(";")) {
            parseSemicolon();
            if (typeCheck) {
                return new Ast.Statement.Declaration(Name.getLiteral(), typeName, Optional.empty());
            }
            else {
                return new Ast.Statement.Declaration(Name.getLiteral(), Optional.empty());
            }

        }
        if (peek("=")) {
            tokens.advance();
        }
        Ast.Expression val = parseExpression();
        parseSemicolon();
        if (typeCheck) {
            return new Ast.Statement.Declaration(Name.getLiteral(), typeName, Optional.of(val));
        }
        else {
            return new Ast.Statement.Declaration(Name.getLiteral(), Optional.of(val));
        }
    }

    /**
     * Parses an if statement from the {@code statement} rule. This method
     * should only be called if the next tokens start an if statement, aka
     * {@code IF}.
     */
    public Ast.Statement.If parseIfStatement() throws ParseException {
        tokens.advance();
        Ast.Expression expr = parseExpression();

        parseDo();

        List<Ast.Statement> statements = parseBlock();

        List<Ast.Statement> elseStatements = new ArrayList<>();
        if (match("ELSE")) {
            elseStatements = parseBlock();
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
        List<Ast.Statement.Case> casesList = new ArrayList<>();
        // advancing pass the SWITCH
        tokens.advance();
        // Checking the expression
        Ast.Expression expr = parseExpression();
        // Check for a case
        while(match("CASE")) {
            casesList.add(parseCaseStatement());
        }
        if (match("DEFAULT")) {
            // Parses the case statements
            List<Ast.Statement> caseStatements = parseBlock();
            casesList.add(new Ast.Statement.Case(Optional.empty(), caseStatements));
        }
        else {
            throwException("Missing Default Case", 0);
        }
        parseDone();

        return new Ast.Statement.Switch(expr, casesList);
    }

    /**
     * Parses a case or default statement block from the {@code switch} rule.
     * This method should only be called if the next tokens start the case or
     * default block of a switch statement, aka {@code CASE} or {@code DEFAULT}.
     */
    public Ast.Statement.Case parseCaseStatement() throws ParseException {
        Ast.Expression val = parseExpression();
        // Check for the colon
        parseColon();
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

        tokens.advance();
        Ast.Expression return_exp = parseExpression();

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
        Ast.Expression a = parseComparisonExpression();
        while ((match("&&") || (match("||")))) {
            String operator = tokens.get(-1).getLiteral();
            // Checking that there's not a missing operand
            if (!tokens.has(0)) {
                throwException("Missing Operand", 0);
            }
            Ast.Expression b = new Ast.Expression.Binary(operator, a, parseComparisonExpression());
            a = b;
        }
        return a;
    }

    /**
     * Parses the {@code equality-expression} rule.
     */
    public Ast.Expression parseComparisonExpression() throws ParseException {
        Ast.Expression a = parseAdditiveExpression();
        while ((match("<") || (match(">")) || (match("==")) || (match("!=")))) {
            checkToken();
            String operator = tokens.get(-1).getLiteral();
            // Checking that there's not a missing operand
            if (!tokens.has(0)) {
                throwException("Missing Operand", 0);
            }
            Ast.Expression b = new Ast.Expression.Binary(operator, a, parseAdditiveExpression());
            a = b;
        }
        return a;
    }

    /**
     * Parses the {@code additive-expression} rule.
     */
    public Ast.Expression parseAdditiveExpression() throws ParseException {
        Ast.Expression a = parseMultiplicativeExpression();
        while ((match("+") || (match("-")))) {
            checkToken();
            String operator = tokens.get(-1).getLiteral();
            if (!tokens.has(0)) {
                throwException("Missing Operand", 0);
            }
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
            if (!tokens.has(0)) {
                throwException("Missing Operand", 0);
            }
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
                // parse the expression
                Optional expr = Optional.of(parseExpression());
                // check for the "]"
                if (!match("]")) {
                    throwException("Missing ]", 0);
                }
                return new Ast.Expression.Access(expr, name);
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
                throw new ParseException("Missing Expression", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
            } else {
                // if the next token after '(' isn't ')', that means that there is an expression in the middle, so we parse it
                Ast.Expression a = parseExpression();
                // Before returning a group Ast, check that there is a closing ')', else throw an exception
                if (match(")")) {
                    return new Ast.Expression.Group(a);
                } else {
                    throw new ParseException("Unterminated Group", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
                }

            }
        } else {
            throwException("Invalid Primary Expression", 1);
        }
        return null;
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
