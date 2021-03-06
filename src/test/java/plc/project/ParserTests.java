package plc.project;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Standard JUnit5 parameterized tests. See the RegexTests file from Homework 1
 * or the LexerTests file from the last project part for more information.
 *
 * Tests the TYPED Parser grammar.
 */
final class ParserTests {

    @ParameterizedTest
    @MethodSource
    void testSource(String test, List<Token> tokens, Ast.Source expected) {
        test(tokens, expected, Parser::parseSource);
    }
    private static Stream<Arguments> testSource() {
        return Stream.of(
                Arguments.of("Zero Statements",
                        Arrays.asList(),
                        new Ast.Source(Arrays.asList(), Arrays.asList())
                ),
                Arguments.of("Global - Immutable",
                        Arrays.asList(
                                //LET name: Type = expr;
                                new Token(Token.Type.IDENTIFIER, "VAL", 0),
                                new Token(Token.Type.IDENTIFIER, "name", 4),
                                new Token(Token.Type.OPERATOR, ":", 9),
                                new Token(Token.Type.IDENTIFIER, "Type", 11),
                                new Token(Token.Type.OPERATOR, "=", 15),
                                new Token(Token.Type.IDENTIFIER, "expr", 17),
                                new Token(Token.Type.OPERATOR, ";", 21)
                        ),
                        new Ast.Source(
                                Arrays.asList(new Ast.Global("name", "Type", false, Optional.of(new Ast.Expression.Access(Optional.empty(), "expr")))),
                                Arrays.asList()
                        )
                ),
                Arguments.of("Function",
                        Arrays.asList(
                                //FUN name(): Type DO stmt; END
                                new Token(Token.Type.IDENTIFIER, "FUN", 0),
                                new Token(Token.Type.IDENTIFIER, "name", 4),
                                new Token(Token.Type.OPERATOR, "(", 8),
                                new Token(Token.Type.OPERATOR, ")", 9),
                                new Token(Token.Type.OPERATOR, ":", 10),
                                new Token(Token.Type.IDENTIFIER, "Type", 12),
                                new Token(Token.Type.IDENTIFIER, "DO", 17),
                                new Token(Token.Type.IDENTIFIER, "stmt", 20),
                                new Token(Token.Type.OPERATOR, ";", 24),
                                new Token(Token.Type.IDENTIFIER, "END", 26)
                        ),
                        new Ast.Source(
                                Arrays.asList(),
                                Arrays.asList(new Ast.Function("name", Arrays.asList(), Arrays.asList(), Optional.of("Type"), Arrays.asList(
                                        new Ast.Statement.Expression(new Ast.Expression.Access(Optional.empty(), "stmt"))
                                )))
                        )
                ),
                Arguments.of("Function Missing End Final",
                        Arrays.asList(
                                //FUN name(): Type DO stmt; END
                                new Token(Token.Type.IDENTIFIER, "FUN", 0),
                                new Token(Token.Type.IDENTIFIER, "name", 4),
                                new Token(Token.Type.OPERATOR, "(", 8),
                                new Token(Token.Type.OPERATOR, ")", 9),
                                new Token(Token.Type.OPERATOR, ":", 10),
                                new Token(Token.Type.IDENTIFIER, "Type", 12),
                                new Token(Token.Type.IDENTIFIER, "DO", 17),
                                new Token(Token.Type.IDENTIFIER, "stmt", 20),
                                new Token(Token.Type.OPERATOR, ";", 24)
                        ),
                        null
                ),
                Arguments.of("Multiple Globals",
                        Arrays.asList(
                                // VAR x: Type = expr; VAR y: Type = expr; VAR z: Type = expr;
                                new Token(Token.Type.IDENTIFIER, "VAR", 0),
                                new Token(Token.Type.IDENTIFIER, "x", 4),
                                new Token(Token.Type.OPERATOR, ":", 5),
                                new Token(Token.Type.IDENTIFIER, "Integer", 7),
                                new Token(Token.Type.OPERATOR, "=", 12),
                                new Token(Token.Type.IDENTIFIER, "expr", 14),
                                new Token(Token.Type.OPERATOR, ";", 19),
                                //
                                new Token(Token.Type.IDENTIFIER, "VAR", 0),
                                new Token(Token.Type.IDENTIFIER, "y", 4),
                                new Token(Token.Type.OPERATOR, ":", 5),
                                new Token(Token.Type.IDENTIFIER, "Integer", 7),
                                new Token(Token.Type.OPERATOR, "=", 12),
                                new Token(Token.Type.IDENTIFIER, "expr", 14),
                                new Token(Token.Type.OPERATOR, ";", 19),
                                //
                                new Token(Token.Type.IDENTIFIER, "VAR", 0),
                                new Token(Token.Type.IDENTIFIER, "z", 4),
                                new Token(Token.Type.OPERATOR, ":", 5),
                                new Token(Token.Type.IDENTIFIER, "Integer", 7),
                                new Token(Token.Type.OPERATOR, "=", 12),
                                new Token(Token.Type.IDENTIFIER, "expr", 14),
                                new Token(Token.Type.OPERATOR, ";", 19)
                        ),
                        new Ast.Source(
                                Arrays.asList(new Ast.Global("x", "Integer", true, Optional.of(new Ast.Expression.Access(Optional.empty(), "expr"))), new Ast.Global("y", "Integer", true, Optional.of(new Ast.Expression.Access(Optional.empty(), "expr"))), new Ast.Global("z", "Integer", true, Optional.of(new Ast.Expression.Access(Optional.empty(), "expr")))),
                                Arrays.asList()
                        )
                ),
                Arguments.of("Global - Mutable Declaration",
                        Arrays.asList(
                                // VAR x: Decimal;
                                new Token(Token.Type.IDENTIFIER, "VAR", 0),
                                new Token(Token.Type.IDENTIFIER, "x", 4),
                                new Token(Token.Type.OPERATOR, ":", 5),
                                new Token(Token.Type.IDENTIFIER, "Decimal", 7),
                                new Token(Token.Type.OPERATOR, ";", 14)
                        ),
                        new Ast.Source(
                                Arrays.asList(new Ast.Global("x", "Decimal", true, Optional.empty())),
                                Arrays.asList()
                        )
                )

        );
    }

    @ParameterizedTest
    @MethodSource
    void testFunction(String test, List<Token> tokens, Ast.Function expected) {
        test(tokens, expected, Parser::parseFunction);
    }
    private static Stream<Arguments> testFunction() {
        return Stream.of(
                Arguments.of("Function Modified Final",
                    Arrays.asList(
                            //FUN name(): Type DO stmt; END
                            new Token(Token.Type.IDENTIFIER, "FUN", 0),
                            new Token(Token.Type.IDENTIFIER, "name", 4),
                            new Token(Token.Type.OPERATOR, "(", 8),
                            new Token(Token.Type.OPERATOR, ")", 9),
                            new Token(Token.Type.OPERATOR, ":", 10),
                            new Token(Token.Type.IDENTIFIER, "Type", 12),
                            new Token(Token.Type.IDENTIFIER, "DO", 17),
                            new Token(Token.Type.IDENTIFIER, "stmt", 20),
                            new Token(Token.Type.OPERATOR, ";", 24),
                            new Token(Token.Type.IDENTIFIER, "END", 26)
                    ),
                    new Ast.Function("name", Arrays.asList(), Arrays.asList(), Optional.of("Type"), Arrays.asList(
                            new Ast.Statement.Expression(new Ast.Expression.Access(Optional.empty(), "stmt"))
                    ))
                ),
                Arguments.of("Function Missing End Modified Final",
                        Arrays.asList(
                                //FUN name(): Type DO stmt;
                                new Token(Token.Type.IDENTIFIER, "FUN", 0),
                                new Token(Token.Type.IDENTIFIER, "name", 4),
                                new Token(Token.Type.OPERATOR, "(", 8),
                                new Token(Token.Type.OPERATOR, ")", 9),
                                new Token(Token.Type.OPERATOR, ":", 10),
                                new Token(Token.Type.IDENTIFIER, "Type", 12),
                                new Token(Token.Type.IDENTIFIER, "DO", 17),
                                new Token(Token.Type.IDENTIFIER, "stmt", 20),
                                new Token(Token.Type.OPERATOR, ";", 24)
                        ),
                        null
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testExpressionStatement(String test, List<Token> tokens, Ast.Statement.Expression expected) {
        test(tokens, expected, Parser::parseStatement);
    }

    private static Stream<Arguments> testExpressionStatement() {
        return Stream.of(
                Arguments.of("Function Expression Modified Final",
                        Arrays.asList(
                                //name();
                                new Token(Token.Type.IDENTIFIER, "name", 0),
                                new Token(Token.Type.OPERATOR, "(", 4),
                                new Token(Token.Type.OPERATOR, ")", 5),
                                new Token(Token.Type.OPERATOR, ";", 6)
                        ),
                        new Ast.Statement.Expression(new Ast.Expression.Function("name", Arrays.asList()))
                ),
                Arguments.of("Missing Semicolon Expression Modified Final",
                        Arrays.asList(
                                // x
                                new Token(Token.Type.IDENTIFIER, "x", 0)
                        ),
                        null
                ),
                Arguments.of("Variable Modified Final",
                        Arrays.asList(
                                //expr;
                                new Token(Token.Type.IDENTIFIER, "expr", 0),
                                new Token(Token.Type.OPERATOR, ";", 4)
                        ),
                        new Ast.Statement.Expression(new Ast.Expression.Access(Optional.empty(), "expr"))
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testDeclarationStatement(String test, List<Token> tokens, Ast.Statement.Declaration expected) {
        test(tokens, expected, Parser::parseStatement);
    }
    private static Stream<Arguments> testDeclarationStatement() {
        return Stream.of(
                Arguments.of("Definition Modified Final",
                        Arrays.asList(
                                //LET name: Type;
                                new Token(Token.Type.IDENTIFIER, "LET", 0),
                                new Token(Token.Type.IDENTIFIER, "name", 4),
                                new Token(Token.Type.OPERATOR, ":", 8),
                                new Token(Token.Type.IDENTIFIER, "Type", 10),
                                new Token(Token.Type.OPERATOR, ";", 14)
                        ),
                        new Ast.Statement.Declaration("name", Optional.of("Type"), Optional.empty())
                ),
                Arguments.of("Initialization Modified Final",
                        Arrays.asList(
                                //LET name = expr;
                                new Token(Token.Type.IDENTIFIER, "LET", 0),
                                new Token(Token.Type.IDENTIFIER, "name", 4),
                                new Token(Token.Type.OPERATOR, "=", 9),
                                new Token(Token.Type.IDENTIFIER, "expr", 11),
                                new Token(Token.Type.OPERATOR, ";", 15)
                        ),
                        new Ast.Statement.Declaration("name", Optional.empty(), Optional.of(new Ast.Expression.Access(Optional.empty(), "expr")))
                ),
                Arguments.of("Type Annotation Modified Final",
                        Arrays.asList(
                                //LET name: Type = expr
                                new Token(Token.Type.IDENTIFIER, "LET", 0),
                                new Token(Token.Type.IDENTIFIER, "name", 4),
                                new Token(Token.Type.OPERATOR, ":", 8),
                                new Token(Token.Type.IDENTIFIER, "Type", 10),
                                new Token(Token.Type.OPERATOR, "=", 15),
                                new Token(Token.Type.IDENTIFIER, "expr", 17),
                                new Token(Token.Type.OPERATOR, ";", 21)
                        ),
                        new Ast.Statement.Declaration("name", Optional.of("Type"), Optional.of(new Ast.Expression.Access(Optional.empty(), "expr")))
                ),
                Arguments.of("Missing Semicolon Modified Final",
                        Arrays.asList(
                                //LET name
                                new Token(Token.Type.IDENTIFIER, "LET", 0),
                                new Token(Token.Type.IDENTIFIER, "name", 4)
                        ),
                        null
                ),
                Arguments.of("Missing Expression Modified Final",
                        Arrays.asList(
                                //LET name: Type = expr
                                new Token(Token.Type.IDENTIFIER, "LET", 0),
                                new Token(Token.Type.IDENTIFIER, "name", 4),
                                new Token(Token.Type.OPERATOR, "=", 9),
                                new Token(Token.Type.OPERATOR, ";", 11)
                        ),
                        null
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testAssignmentStatement(String test, List<Token> tokens, Ast.Statement.Assignment expected) {
        test(tokens, expected, Parser::parseStatement);
    }
    private static Stream<Arguments> testAssignmentStatement() {
        return Stream.of(
                Arguments.of("Assignment",
                        Arrays.asList(
                                //name = value;
                                new Token(Token.Type.IDENTIFIER, "name", 0),
                                new Token(Token.Type.OPERATOR, "=", 5),
                                new Token(Token.Type.IDENTIFIER, "value", 7),
                                new Token(Token.Type.OPERATOR, ";", 12)
                        ),
                        new Ast.Statement.Assignment(
                                new Ast.Expression.Access(Optional.empty(), "name"),
                                new Ast.Expression.Access(Optional.empty(), "value")
                        )
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testSwitchStatement(String test, List<Token> tokens, Ast.Statement.Switch expected) {
        test(tokens, expected, Parser::parseStatement);
    }
    private static Stream<Arguments> testSwitchStatement() {
        return Stream.of(
                Arguments.of("Basic Switch Modified Final",
                        Arrays.asList(
                                new Token(Token.Type.IDENTIFIER, "SWITCH", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 7),
                                new Token(Token.Type.IDENTIFIER, "DEFAULT", 12),
                                new Token(Token.Type.IDENTIFIER, "stmt",20),
                                new Token(Token.Type.OPERATOR, ";", 24),
                                new Token(Token.Type.IDENTIFIER, "END", 26)
                        ),
                        new Ast.Statement.Switch(
                                new Ast.Expression.Access(Optional.empty(), "expr"),
                                Arrays.asList(
                                        new Ast.Statement.Case(Optional.empty(),
                                        Arrays.asList(new Ast.Statement.Expression(new Ast.Expression.Access(Optional.empty(), "stmt")))
                                        )
                                )
                        )
                ),
                Arguments.of("Case Switch Modified Final",
                        Arrays.asList(
                                new Token(Token.Type.IDENTIFIER, "SWITCH", 0),
                                new Token(Token.Type.IDENTIFIER, "expr1", 7),
                                new Token(Token.Type.IDENTIFIER, "CASE", 13),
                                new Token(Token.Type.IDENTIFIER, "expr2",18),
                                new Token(Token.Type.OPERATOR, ":", 24),
                                new Token(Token.Type.IDENTIFIER, "stmt1", 26),
                                new Token(Token.Type.OPERATOR, ";", 31),
                                new Token(Token.Type.IDENTIFIER, "DEFAULT", 33),
                                new Token(Token.Type.IDENTIFIER, "stmt2", 41),
                                new Token(Token.Type.OPERATOR, ";", 42),
                                new Token(Token.Type.IDENTIFIER, "END", 44)
                        ),
                        new Ast.Statement.Switch(
                                new Ast.Expression.Access(Optional.empty(), "expr1"),
                                Arrays.asList(
                                        new Ast.Statement.Case(Optional.of(new Ast.Expression.Access(Optional.empty(), ("expr2"))),
                                                Arrays.asList(new Ast.Statement.Expression(new Ast.Expression.Access(Optional.empty(), "stmt1")))),
                                        new Ast.Statement.Case(Optional.empty(),
                                                Arrays.asList(new Ast.Statement.Expression(new Ast.Expression.Access(Optional.empty(), "stmt2")))
                                        )
                                )
                        )
                ),
                Arguments.of("Empty Switch Modified Final",
                        Arrays.asList(
                                new Token(Token.Type.IDENTIFIER, "SWITCH", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 7),
                                new Token(Token.Type.IDENTIFIER, "DEFAULT", 12),
                                new Token(Token.Type.IDENTIFIER, "END", 20)
                        ),
                        null
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testIfStatement(String test, List<Token> tokens, Ast.Statement.If expected) {
        test(tokens, expected, Parser::parseStatement);
    }

    private static Stream<Arguments> testIfStatement() {
        return Stream.of(
                Arguments.of("If Modified Final",
                        Arrays.asList(
                                //IF expr DO stmt; END
                                new Token(Token.Type.IDENTIFIER, "IF", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 3),
                                new Token(Token.Type.IDENTIFIER, "DO", 8),
                                new Token(Token.Type.IDENTIFIER, "stmt", 11),
                                new Token(Token.Type.OPERATOR, ";", 15),
                                new Token(Token.Type.IDENTIFIER, "END", 17)
                        ),
                        new Ast.Statement.If(
                                new Ast.Expression.Access(Optional.empty(), "expr"),
                                Arrays.asList(new Ast.Statement.Expression(new Ast.Expression.Access(Optional.empty(), "stmt"))),
                                Arrays.asList()
                        )
                ),
                Arguments.of("Else Modified Final",
                        Arrays.asList(
                                //IF expr DO stmt1; ELSE stmt2; END
                                new Token(Token.Type.IDENTIFIER, "IF", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 3),
                                new Token(Token.Type.IDENTIFIER, "DO", 8),
                                new Token(Token.Type.IDENTIFIER, "stmt1", 11),
                                new Token(Token.Type.OPERATOR, ";", 16),
                                new Token(Token.Type.IDENTIFIER, "ELSE", 18),
                                new Token(Token.Type.IDENTIFIER, "stmt2", 23),
                                new Token(Token.Type.OPERATOR, ";", 28),
                                new Token(Token.Type.IDENTIFIER, "END", 30)
                        ),
                        new Ast.Statement.If(
                                new Ast.Expression.Access(Optional.empty(), "expr"),
                                Arrays.asList(new Ast.Statement.Expression(new Ast.Expression.Access(Optional.empty(), "stmt1"))),
                                Arrays.asList(new Ast.Statement.Expression(new Ast.Expression.Access(Optional.empty(), "stmt2")))
                        )
                ),
                Arguments.of("Multiple If Statements Modified Final",
                        Arrays.asList(
                                // IF expr DO stmt1; stmt2; stmt3; END
                                new Token(Token.Type.IDENTIFIER, "IF", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 3),
                                new Token(Token.Type.IDENTIFIER, "DO", 8),
                                new Token(Token.Type.IDENTIFIER, "stmt1", 11),
                                new Token(Token.Type.OPERATOR, ";", 16),
                                new Token(Token.Type.IDENTIFIER, "stmt2", 18),
                                new Token(Token.Type.OPERATOR, ";", 23),
                                new Token(Token.Type.IDENTIFIER, "stmt3", 25),
                                new Token(Token.Type.OPERATOR, ";", 30),
                                new Token(Token.Type.IDENTIFIER, "END", 32)
                        ),
                        new Ast.Statement.If(
                                new Ast.Expression.Access(Optional.empty(), "expr"),
                                Arrays.asList(new Ast.Statement.Expression(new Ast.Expression.Access(Optional.empty(), "stmt1")), new Ast.Statement.Expression(new Ast.Expression.Access(Optional.empty(), "stmt2")), new Ast.Statement.Expression(new Ast.Expression.Access(Optional.empty(), "stmt3"))),
                                Arrays.asList()
                        )
                ),
                Arguments.of("If No Statements Modified Final",
                        Arrays.asList(
                                //IF expr DO stmt; END
                                new Token(Token.Type.IDENTIFIER, "IF", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 3),
                                new Token(Token.Type.IDENTIFIER, "DO", 8),
                                new Token(Token.Type.IDENTIFIER, "END", 11)
                        ),
                        new Ast.Statement.If(
                                new Ast.Expression.Access(Optional.empty(), "expr"),
                                Arrays.asList(),
                                Arrays.asList()
                        )
                ),
                Arguments.of("If Missing Do Modified Final",
                        Arrays.asList(
                                //IF expr DO stmt; END
                                new Token(Token.Type.IDENTIFIER, "IF", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 3),
                                new Token(Token.Type.IDENTIFIER, "stmt", 8),
                                new Token(Token.Type.OPERATOR, ";", 13),
                                new Token(Token.Type.IDENTIFIER, "END", 15)
                        ),
                        null
                ),
                Arguments.of("If Missing End Modified Final",
                        Arrays.asList(
                                //IF expr DO stmt; END
                                new Token(Token.Type.IDENTIFIER, "IF", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 3),
                                new Token(Token.Type.IDENTIFIER, "DO", 8),
                                new Token(Token.Type.IDENTIFIER, "stmt", 11),
                                new Token(Token.Type.OPERATOR, ";", 15)
                        ),
                        null
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testWhileStatement(String test, List<Token> tokens, Ast.Statement.While expected) {
        test(tokens, expected, Parser::parseStatement);
    }

    private static Stream<Arguments> testWhileStatement() {
        return Stream.of(
                Arguments.of("While",
                        Arrays.asList(
                                //WHILE expr DO stmt; END
                                new Token(Token.Type.IDENTIFIER, "WHILE", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 6),
                                new Token(Token.Type.IDENTIFIER, "DO", 11),
                                new Token(Token.Type.IDENTIFIER, "stmt", 14),
                                new Token(Token.Type.OPERATOR, ";", 18),
                                new Token(Token.Type.IDENTIFIER, "END", 20)
                        ),
                        new Ast.Statement.While(
                                new Ast.Expression.Access(Optional.empty(), "expr"),
                                Arrays.asList(new Ast.Statement.Expression(new Ast.Expression.Access(Optional.empty(), "stmt")))
                        )
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testReturnStatement(String test, List<Token> tokens, Ast.Statement.Return expected) {
        test(tokens, expected, Parser::parseStatement);
    }
    private static Stream<Arguments> testReturnStatement() {
        return Stream.of(
                Arguments.of("Return Statement Modified Final",
                        Arrays.asList(
                                //RETURN expr;
                                new Token(Token.Type.IDENTIFIER, "RETURN", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 7),
                                new Token(Token.Type.OPERATOR, ";", 11)
                        ),
                        new Ast.Statement.Return(new Ast.Expression.Access(Optional.empty(), "expr"))
                ),
                Arguments.of("Return Missing Value Modified Final",
                        Arrays.asList(
                                //RETURN;
                                new Token(Token.Type.IDENTIFIER, "RETURN", 0),
                                new Token(Token.Type.OPERATOR, ";", 6)
                        ),
                        null
                ),
                Arguments.of("Return Missing Semicolon",
                        Arrays.asList(
                                //RETURN expr
                                new Token(Token.Type.IDENTIFIER, "RETURN", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 7)
                        ),
                        null
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testLiteralExpression(String test, List<Token> tokens, Ast.Expression.Literal expected) {
        test(tokens, expected, Parser::parseExpression);
    }
    private static Stream<Arguments> testLiteralExpression() {
        return Stream.of(
                Arguments.of("Decimal Literal Final",
                        Arrays.asList(new Token(Token.Type.DECIMAL, "2.0", 0)),
                        new Ast.Expression.Literal(new BigDecimal("2.0"))
                ),
                Arguments.of("Escape Character Final",
                        Arrays.asList(new Token(Token.Type.STRING, "\"Hello,\\nWorld!\"", 0)),
                        new Ast.Expression.Literal("Hello,\nWorld!")
                ),
                //
                // Modified Parser Tests
                //
                Arguments.of("Boolean TRUE Literal Modified Final",
                        Arrays.asList(new Token(Token.Type.IDENTIFIER, "TRUE", 0)),
                        new Ast.Expression.Literal(Boolean.TRUE)
                ),
                Arguments.of("Boolean FALSE Literal Modified Final",
                        Arrays.asList(new Token(Token.Type.IDENTIFIER, "FALSE", 0)),
                        new Ast.Expression.Literal(Boolean.FALSE)
                ),
                Arguments.of("Integer Literal Modified Final",
                        Arrays.asList(new Token(Token.Type.INTEGER, "1", 0)),
                        new Ast.Expression.Literal(new BigInteger("1"))
                ),
                Arguments.of("Nil Literal Modified Final",
                        Arrays.asList(new Token(Token.Type.IDENTIFIER, "NIL", 0)),
                        new Ast.Expression.Literal(null)
                ),
                Arguments.of("Integer Literal (Big Integer) Modified Final",
                        Arrays.asList(new Token(Token.Type.INTEGER, "123456789123456789123456789", 0)),
                        new Ast.Expression.Literal(new BigInteger("123456789123456789123456789"))
                ),
                Arguments.of("Integer Decimal (Big Integer) Modified Final",
                        Arrays.asList(new Token(Token.Type.DECIMAL, "123456789123456789123456789.9999999", 0)),
                        new Ast.Expression.Literal(new BigDecimal("123456789123456789123456789.9999999"))
                ),

                Arguments.of("Character Literal Modified Final 'c'",
                        Arrays.asList(new Token(Token.Type.CHARACTER, "'c'", 0)),
                        new Ast.Expression.Literal('c')
                ),
                Arguments.of("String Literal Modified Final",
                        Arrays.asList(new Token(Token.Type.STRING, "\"This is a string\"", 0)),
                        new Ast.Expression.Literal("This is a string")
                ),
                Arguments.of("Char Escape b Modified Final",
                        Arrays.asList(new Token(Token.Type.CHARACTER, "\'\b\'", 0)),
                        new Ast.Expression.Literal('\b')
                ),
                Arguments.of("String Escape n Modified Final",
                        Arrays.asList(new Token(Token.Type.STRING, "\"\b\"", 0)),
                        new Ast.Expression.Literal("\b")
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testGroupExpression(String test, List<Token> tokens, Ast.Expression.Group expected) {
        test(tokens, expected, Parser::parseExpression);
    }

    private static Stream<Arguments> testGroupExpression() {
        return Stream.of(
                Arguments.of("Grouped Variable Modified Final",
                        Arrays.asList(
                                //(expr)
                                new Token(Token.Type.OPERATOR, "(", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 1),
                                new Token(Token.Type.OPERATOR, ")", 5)
                        ),
                        new Ast.Expression.Group(new Ast.Expression.Access(Optional.empty(), "expr"))
                ),
                Arguments.of("Grouped Binary Modified Final",
                        Arrays.asList(
                                //(expr1 + expr2)
                                new Token(Token.Type.OPERATOR, "(", 0),
                                new Token(Token.Type.IDENTIFIER, "expr1", 1),
                                new Token(Token.Type.OPERATOR, "+", 7),
                                new Token(Token.Type.IDENTIFIER, "expr2", 9),
                                new Token(Token.Type.OPERATOR, ")", 14)
                        ),
                        new Ast.Expression.Group(new Ast.Expression.Binary("+",
                                new Ast.Expression.Access(Optional.empty(), "expr1"),
                                new Ast.Expression.Access(Optional.empty(), "expr2")
                        ))
                ),
                Arguments.of("Missing Closing Parenthesis Modified Final",
                        Arrays.asList(
                                //(expr)
                                new Token(Token.Type.OPERATOR, "(", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 1)
                        ),
                        null
                ),
                Arguments.of("Missing Expression Modified Final",
                        Arrays.asList(
                                //(expr)
                                new Token(Token.Type.OPERATOR, "(", 0),
                                new Token(Token.Type.OPERATOR, ")", 1)
                        ),
                        null
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testBinaryExpression(String test, List<Token> tokens, Ast.Expression.Binary expected) {
        test(tokens, expected, Parser::parseExpression);
    }

    private static Stream<Arguments> testBinaryExpression() {
        return Stream.of(
                Arguments.of("Binary And Modified Final",
                        Arrays.asList(
                                //expr1 && expr2
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "&&", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 9)
                        ),
                        new Ast.Expression.Binary("&&",
                                new Ast.Expression.Access(Optional.empty(), "expr1"),
                                new Ast.Expression.Access(Optional.empty(), "expr2")
                        )
                ),
                Arguments.of("Binary OR Modified Final",
                        Arrays.asList(
                                //expr1 || expr2
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "||", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 9)
                        ),
                        new Ast.Expression.Binary("||",
                                new Ast.Expression.Access(Optional.empty(), "expr1"),
                                new Ast.Expression.Access(Optional.empty(), "expr2")
                        )
                ),
                Arguments.of("Binary Equality Modified Final",
                        Arrays.asList(
                                //expr1 == expr2
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "==", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 9)
                        ),
                        new Ast.Expression.Binary("==",
                                new Ast.Expression.Access(Optional.empty(), "expr1"),
                                new Ast.Expression.Access(Optional.empty(), "expr2")
                        )
                ),
                Arguments.of("Binary Inequality Modified Final",
                        Arrays.asList(
                                //expr1 != expr2
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "!=", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 9)
                        ),
                        new Ast.Expression.Binary("!=",
                                new Ast.Expression.Access(Optional.empty(), "expr1"),
                                new Ast.Expression.Access(Optional.empty(), "expr2")
                        )
                ),
                Arguments.of("Binary Greater Than Modified Final",
                        Arrays.asList(
                                //expr1 > expr2
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, ">", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 8)
                        ),
                        new Ast.Expression.Binary(">",
                                new Ast.Expression.Access(Optional.empty(), "expr1"),
                                new Ast.Expression.Access(Optional.empty(), "expr2")
                        )
                ),
                Arguments.of("Binary Less Than Modified Final",
                        Arrays.asList(
                                //expr1 < expr2
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "<", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 8)
                        ),
                        new Ast.Expression.Binary("<",
                                new Ast.Expression.Access(Optional.empty(), "expr1"),
                                new Ast.Expression.Access(Optional.empty(), "expr2")
                        )
                ),
                Arguments.of("Binary Addition Modified Final",
                        Arrays.asList(
                                //expr1 + expr2
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "+", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 8)
                        ),
                        new Ast.Expression.Binary("+",
                                new Ast.Expression.Access(Optional.empty(), "expr1"),
                                new Ast.Expression.Access(Optional.empty(), "expr2")
                        )
                ),
                Arguments.of("Binary Subtraction Modified Final",
                        Arrays.asList(
                                //expr1 - expr2
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "-", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 8)
                        ),
                        new Ast.Expression.Binary("-",
                                new Ast.Expression.Access(Optional.empty(), "expr1"),
                                new Ast.Expression.Access(Optional.empty(), "expr2")
                        )
                ),
                Arguments.of("Binary Multiplication Modified Final",
                        Arrays.asList(
                                //expr1 * expr2
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "*", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 8)
                        ),
                        new Ast.Expression.Binary("*",
                                new Ast.Expression.Access(Optional.empty(), "expr1"),
                                new Ast.Expression.Access(Optional.empty(), "expr2")
                        )
                ),
                Arguments.of("Binary Multiplication Modified Final",
                        Arrays.asList(
                                //expr1 / expr2
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "/", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 8)
                        ),
                        new Ast.Expression.Binary("/",
                                new Ast.Expression.Access(Optional.empty(), "expr1"),
                                new Ast.Expression.Access(Optional.empty(), "expr2")
                        )
                ),
                Arguments.of("Binary Exponent Modified Final",
                        Arrays.asList(
                                //expr1 * expr2
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "^", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 8)
                        ),
                        new Ast.Expression.Binary("^",
                                new Ast.Expression.Access(Optional.empty(), "expr1"),
                                new Ast.Expression.Access(Optional.empty(), "expr2")
                        )
                ),
                Arguments.of("Binary Multiple Logical Ands Modified Final",
                        Arrays.asList(
                                //expr1 && expr2 && expr3
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "&&", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 9),
                                new Token(Token.Type.OPERATOR, "&&", 15),
                                new Token(Token.Type.IDENTIFIER, "expr3", 18)
                        ),
                        new Ast.Expression.Binary("&&",
                                new Ast.Expression.Binary("&&",
                                        new Ast.Expression.Access(Optional.empty(), "expr1"),
                                        new Ast.Expression.Access(Optional.empty(), "expr2")
                                ),
                                new Ast.Expression.Access(Optional.empty(), "expr3")
                        )
                ),
                Arguments.of("Binary Multiple Logical ORs Modified Final",
                        Arrays.asList(
                                //expr1 || expr2 || expr3
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "||", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 9),
                                new Token(Token.Type.OPERATOR, "||", 15),
                                new Token(Token.Type.IDENTIFIER, "expr3", 18)
                        ),
                        new Ast.Expression.Binary("||",
                                new Ast.Expression.Binary("||",
                                        new Ast.Expression.Access(Optional.empty(), "expr1"),
                                        new Ast.Expression.Access(Optional.empty(), "expr2")
                                ),
                                new Ast.Expression.Access(Optional.empty(), "expr3")
                        )
                ),
                Arguments.of("Binary Multiple Comparison == Modified Final",
                        Arrays.asList(
                                //expr1 || expr2 || expr3
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "==", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 9),
                                new Token(Token.Type.OPERATOR, "==", 15),
                                new Token(Token.Type.IDENTIFIER, "expr3", 18)
                        ),
                        new Ast.Expression.Binary("==",
                                new Ast.Expression.Binary("==",
                                        new Ast.Expression.Access(Optional.empty(), "expr1"),
                                        new Ast.Expression.Access(Optional.empty(), "expr2")
                                ),
                                new Ast.Expression.Access(Optional.empty(), "expr3")
                        )
                ),
                Arguments.of("Binary Multiple Comparison != Modified Final",
                        Arrays.asList(
                                //expr1 || expr2 || expr3
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "!=", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 9),
                                new Token(Token.Type.OPERATOR, "!=", 15),
                                new Token(Token.Type.IDENTIFIER, "expr3", 18)
                        ),
                        new Ast.Expression.Binary("!=",
                                new Ast.Expression.Binary("!=",
                                        new Ast.Expression.Access(Optional.empty(), "expr1"),
                                        new Ast.Expression.Access(Optional.empty(), "expr2")
                                ),
                                new Ast.Expression.Access(Optional.empty(), "expr3")
                        )
                ),
                Arguments.of("Binary Multiple Comparison < Modified Final",
                        Arrays.asList(
                                //expr1 < expr2 < expr3
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "<", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 8),
                                new Token(Token.Type.OPERATOR, "<", 14),
                                new Token(Token.Type.IDENTIFIER, "expr3", 16)
                        ),
                        new Ast.Expression.Binary("<",
                                new Ast.Expression.Binary("<",
                                        new Ast.Expression.Access(Optional.empty(), "expr1"),
                                        new Ast.Expression.Access(Optional.empty(), "expr2")
                                ),
                                new Ast.Expression.Access(Optional.empty(), "expr3")
                        )
                ),
                Arguments.of("Binary Multiple Comparison > Modified Final",
                        Arrays.asList(
                                //expr1 < expr2 < expr3
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, ">", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 8),
                                new Token(Token.Type.OPERATOR, ">", 14),
                                new Token(Token.Type.IDENTIFIER, "expr3", 16)
                        ),
                        new Ast.Expression.Binary(">",
                                new Ast.Expression.Binary(">",
                                        new Ast.Expression.Access(Optional.empty(), "expr1"),
                                        new Ast.Expression.Access(Optional.empty(), "expr2")
                                ),
                                new Ast.Expression.Access(Optional.empty(), "expr3")
                        )
                ),
                Arguments.of("Binary Multiple Addition Modified Final",
                        Arrays.asList(
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "+", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 8),
                                new Token(Token.Type.OPERATOR, "+", 14),
                                new Token(Token.Type.IDENTIFIER, "expr3", 16)
                        ),
                        new Ast.Expression.Binary("+",
                                new Ast.Expression.Binary("+",
                                        new Ast.Expression.Access(Optional.empty(), "expr1"),
                                        new Ast.Expression.Access(Optional.empty(), "expr2")
                                ),
                                new Ast.Expression.Access(Optional.empty(), "expr3")
                        )
                ),
                Arguments.of("Binary Multiple Subtraction Modified Final",
                        Arrays.asList(
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "-", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 8),
                                new Token(Token.Type.OPERATOR, "-", 14),
                                new Token(Token.Type.IDENTIFIER, "expr3", 16)
                        ),
                        new Ast.Expression.Binary("-",
                                new Ast.Expression.Binary("-",
                                        new Ast.Expression.Access(Optional.empty(), "expr1"),
                                        new Ast.Expression.Access(Optional.empty(), "expr2")
                                ),
                                new Ast.Expression.Access(Optional.empty(), "expr3")
                        )
                ),
                Arguments.of("Binary Multiple Multiplication Modified Final",
                        Arrays.asList(
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "*", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 8),
                                new Token(Token.Type.OPERATOR, "*", 14),
                                new Token(Token.Type.IDENTIFIER, "expr3", 16)
                        ),
                        new Ast.Expression.Binary("*",
                                new Ast.Expression.Binary("*",
                                        new Ast.Expression.Access(Optional.empty(), "expr1"),
                                        new Ast.Expression.Access(Optional.empty(), "expr2")
                                ),
                                new Ast.Expression.Access(Optional.empty(), "expr3")
                        )
                ),
                Arguments.of("Binary Multiple Division Modified Final",
                        Arrays.asList(
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "/", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 8),
                                new Token(Token.Type.OPERATOR, "/", 14),
                                new Token(Token.Type.IDENTIFIER, "expr3", 16)
                        ),
                        new Ast.Expression.Binary("/",
                                new Ast.Expression.Binary("/",
                                        new Ast.Expression.Access(Optional.empty(), "expr1"),
                                        new Ast.Expression.Access(Optional.empty(), "expr2")
                                ),
                                new Ast.Expression.Access(Optional.empty(), "expr3")
                        )
                ),
                Arguments.of("Binary Multiple Exponent Modified Final",
                        Arrays.asList(
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "^", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 8),
                                new Token(Token.Type.OPERATOR, "^", 14),
                                new Token(Token.Type.IDENTIFIER, "expr3", 16)
                        ),
                        new Ast.Expression.Binary("^",
                                new Ast.Expression.Binary("^",
                                        new Ast.Expression.Access(Optional.empty(), "expr1"),
                                        new Ast.Expression.Access(Optional.empty(), "expr2")
                                ),
                                new Ast.Expression.Access(Optional.empty(), "expr3")
                        )
                ),
                Arguments.of("Binary Missing Operand AND Modified Final",
                        Arrays.asList(
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "&&", 6)
                        ),
                        null
                ),
                Arguments.of("Binary Missing Operand OR Modified Final",
                        Arrays.asList(
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "||", 6)
                        ),
                        null
                ),
                Arguments.of("Binary Missing Operand < Modified Final",
                        Arrays.asList(
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "<", 6)
                        ),
                        null
                ),
                Arguments.of("Binary Missing Operand > Modified Final",
                        Arrays.asList(
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, ">", 6)
                        ),
                        null
                ),
                Arguments.of("Binary Missing Operand == Modified Final",
                        Arrays.asList(
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "==", 6)
                        ),
                        null
                ),
                Arguments.of("Binary Missing Operand != Modified Final",
                        Arrays.asList(
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "!=", 6)
                        ),
                        null
                ),
                Arguments.of("Binary Missing Operand + Modified Final",
                        Arrays.asList(
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "+", 6)
                        ),
                        null
                ),
                Arguments.of("Binary Missing Operand - Modified Final",
                        Arrays.asList(
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "-", 6)
                        ),
                        null
                ),
                Arguments.of("Binary Missing Operand * Modified Final",
                        Arrays.asList(
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "*", 6)
                        ),
                        null
                ),
                Arguments.of("Binary Missing Operand / Modified Final",
                        Arrays.asList(
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "/", 6)
                        ),
                        null
                ),
                Arguments.of("Binary Missing Operand ^ Modified Final",
                        Arrays.asList(
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "^", 6)
                        ),
                        null
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testAccessExpression(String test, List<Token> tokens, Ast.Expression.Access expected) {
        test(tokens, expected, Parser::parseExpression);
    }

    private static Stream<Arguments> testAccessExpression() {
        return Stream.of(
                Arguments.of("Variable Modified Final",
                        Arrays.asList(new Token(Token.Type.IDENTIFIER, "name", 0)),
                        new Ast.Expression.Access(Optional.empty(), "name")
                ),
                Arguments.of("List Index Access Modified Final",
                        Arrays.asList(
                                //list[expr]
                                new Token(Token.Type.IDENTIFIER, "list", 0),
                                new Token(Token.Type.OPERATOR, "[", 4),
                                new Token(Token.Type.IDENTIFIER, "offset", 5),
                                new Token(Token.Type.OPERATOR, "]", 11)
                        ),
                        new Ast.Expression.Access(Optional.of(new Ast.Expression.Access(Optional.empty(), "offset")), "list")
                ),
                Arguments.of("Complex Expression Modified Final",
                        Arrays.asList(
                                //list[e1 + e2]
                                new Token(Token.Type.IDENTIFIER, "list", 0),
                                new Token(Token.Type.OPERATOR, "[", 4),
                                new Token(Token.Type.IDENTIFIER, "e1", 5),
                                new Token(Token.Type.OPERATOR, "+", 7),
                                new Token(Token.Type.IDENTIFIER, "e2", 9),
                                new Token(Token.Type.OPERATOR, "]", 10)
                        ),
                        new Ast.Expression.Access(Optional.of(new Ast.Expression.Binary("+", new Ast.Expression.Access(Optional.empty(), "e1"), new Ast.Expression.Access(Optional.empty(), "e2"))), "list")
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testFunctionExpression(String test, List<Token> tokens, Ast.Expression.Function expected) {
        test(tokens, expected, Parser::parseExpression);
    }

    private static Stream<Arguments> testFunctionExpression() {
        return Stream.of(
                Arguments.of("Zero Arguments",
                        Arrays.asList(
                                //name()
                                new Token(Token.Type.IDENTIFIER, "name", 0),
                                new Token(Token.Type.OPERATOR, "(", 4),
                                new Token(Token.Type.OPERATOR, ")", 5)
                        ),
                        new Ast.Expression.Function("name", Arrays.asList())
                ),
                Arguments.of("Multiple Arguments",
                        Arrays.asList(
                                //name(expr1, expr2, expr3)
                                new Token(Token.Type.IDENTIFIER, "name", 0),
                                new Token(Token.Type.OPERATOR, "(", 4),
                                new Token(Token.Type.IDENTIFIER, "expr1", 5),
                                new Token(Token.Type.OPERATOR, ",", 10),
                                new Token(Token.Type.IDENTIFIER, "expr2", 12),
                                new Token(Token.Type.OPERATOR, ",", 17),
                                new Token(Token.Type.IDENTIFIER, "expr3", 19),
                                new Token(Token.Type.OPERATOR, ")", 24)
                        ),
                        new Ast.Expression.Function("name", Arrays.asList(
                                new Ast.Expression.Access(Optional.empty(), "expr1"),
                                new Ast.Expression.Access(Optional.empty(), "expr2"),
                                new Ast.Expression.Access(Optional.empty(), "expr3")
                        ))
                )
        );
    }

    @Test
    void testExample1() {
        List<Token> input = Arrays.asList(
                /* VAR first: Integer = 1;
                 * FUN main(): Integer DO
                 *     WHILE first != 10 DO
                 *         print(first);
                 *         first = first + 1;
                 *     END
                 * END
                 */
                //VAR first: Integer = 1;
                new Token(Token.Type.IDENTIFIER, "VAR", 0),
                new Token(Token.Type.IDENTIFIER, "first", 4),
                new Token(Token.Type.OPERATOR, ":", 10),
                new Token(Token.Type.IDENTIFIER, "Integer", 11),
                new Token(Token.Type.OPERATOR, "=", 19),
                new Token(Token.Type.INTEGER, "1", 21),
                new Token(Token.Type.OPERATOR, ";", 22),
                //FUN main(): Integer DO
                new Token(Token.Type.IDENTIFIER, "FUN", 24),
                new Token(Token.Type.IDENTIFIER, "main", 28),
                new Token(Token.Type.OPERATOR, "(", 32),
                new Token(Token.Type.OPERATOR, ")", 33),
                new Token(Token.Type.OPERATOR, ":", 34),
                new Token(Token.Type.IDENTIFIER, "Integer", 36),
                new Token(Token.Type.IDENTIFIER, "DO", 44),
                //    WHILE first != 10 DO
                new Token(Token.Type.IDENTIFIER, "WHILE", 51),
                new Token(Token.Type.IDENTIFIER, "first", 57),
                new Token(Token.Type.OPERATOR, "!=", 63),
                new Token(Token.Type.INTEGER, "10", 66),
                new Token(Token.Type.IDENTIFIER, "DO", 69),
                //        print(first);
                new Token(Token.Type.IDENTIFIER, "print", 80),
                new Token(Token.Type.OPERATOR, "(", 85),
                new Token(Token.Type.IDENTIFIER, "first", 86),
                new Token(Token.Type.OPERATOR, ")", 91),
                new Token(Token.Type.OPERATOR, ";", 92),
                //        first = first + 1;
                new Token(Token.Type.IDENTIFIER, "first", 102),
                new Token(Token.Type.OPERATOR, "=", 108),
                new Token(Token.Type.IDENTIFIER, "first", 110),
                new Token(Token.Type.OPERATOR, "+", 116),
                new Token(Token.Type.INTEGER, "1", 118),
                new Token(Token.Type.OPERATOR, ";", 119),
                //    END
                new Token(Token.Type.IDENTIFIER, "END", 125),
                //END
                new Token(Token.Type.IDENTIFIER, "END", 129)
        );
        Ast.Source expected = new Ast.Source(
                Arrays.asList(new Ast.Global("first", "Integer", true, Optional.of(new Ast.Expression.Literal(BigInteger.ONE)))),
                Arrays.asList(new Ast.Function("main", Arrays.asList(), Arrays.asList(), Optional.of("Integer"), Arrays.asList(
                        new Ast.Statement.While(
                                new Ast.Expression.Binary("!=",
                                        new Ast.Expression.Access(Optional.empty(), "first"),
                                        new Ast.Expression.Literal(BigInteger.TEN)
                                ),
                                Arrays.asList(
                                        new Ast.Statement.Expression(
                                                new Ast.Expression.Function("print", Arrays.asList(
                                                        new Ast.Expression.Access(Optional.empty(), "first"))
                                                )
                                        ),
                                        new Ast.Statement.Assignment(
                                                new Ast.Expression.Access(Optional.empty(), "first"),
                                                new Ast.Expression.Binary("+",
                                                        new Ast.Expression.Access(Optional.empty(), "first"),
                                                        new Ast.Expression.Literal(BigInteger.ONE)
                                                )
                                        )
                                )
                        )
                ))
        ));
        test(input, expected, Parser::parseSource);
    }

    /**
     * Standard test function. If expected is null, a ParseException is expected
     * to be thrown (not used in the provided tests).
     */
    private static <T extends Ast> void test(List<Token> tokens, T expected, Function<Parser, T> function) {
        Parser parser = new Parser(tokens);
        if (expected != null) {
            Assertions.assertEquals(expected, function.apply(parser));
        } else {
            Assertions.assertThrows(ParseException.class, () -> function.apply(parser));
        }
    }

}
