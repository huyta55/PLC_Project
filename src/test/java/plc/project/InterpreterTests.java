package plc.project;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

final class InterpreterTests {

    @ParameterizedTest
    @MethodSource
    void testSource(String test, Ast.Source ast, Object expected) {
        test(ast, expected, new Scope(null));
    }

    private static Stream<Arguments> testSource() {
        return Stream.of(
                // FUN main() DO RETURN 0; END
                Arguments.of("Main", new Ast.Source(
                        Arrays.asList(),
                        Arrays.asList(new Ast.Function("main", Arrays.asList(), Arrays.asList(
                                new Ast.Statement.Return(new Ast.Expression.Literal(BigInteger.ZERO)))
                        ))
                ), BigInteger.ZERO),
                // VAR x = 1; VAR y = 10; FUN main() DO x + y; END
                Arguments.of("Globals & No Return", new Ast.Source(
                        Arrays.asList(
                                new Ast.Global("x", true, Optional.of(new Ast.Expression.Literal(BigInteger.ONE))),
                                new Ast.Global("y", true, Optional.of(new Ast.Expression.Literal(BigInteger.TEN)))
                        ),
                        Arrays.asList(new Ast.Function("main", Arrays.asList(), Arrays.asList(
                                new Ast.Statement.Expression(new Ast.Expression.Binary("+",
                                        new Ast.Expression.Access(Optional.empty(), "x"),
                                        new Ast.Expression.Access(Optional.empty(), "y")                                ))
                        )))
                ), Environment.NIL.getValue())
        );
    }

    @ParameterizedTest
    @MethodSource
    void testGlobal(String test, Ast.Global ast, Object expected) {
        Scope scope = test(ast, Environment.NIL.getValue(), new Scope(null));
        Assertions.assertEquals(expected, scope.lookupVariable(ast.getName()).getValue().getValue());
    }

    private static Stream<Arguments> testGlobal() {
        return Stream.of(
                // VAR name;
                Arguments.of("Mutable", new Ast.Global("name", true, Optional.empty()), Environment.NIL.getValue()),
                // VAL name = 1;
                Arguments.of("Immutable", new Ast.Global("name", false, Optional.of(new Ast.Expression.Literal(BigInteger.ONE))), BigInteger.ONE),
                // Var z = 3;
                Arguments.of("Var z = 3", new Ast.Global("z", true, Optional.of(new Ast.Expression.Literal(BigInteger.valueOf(3)))), BigInteger.valueOf(3))
        );
    }

    @Test
    void testList() {
        // LIST list = [1, 5, 10];

        List<Object> expected = Arrays.asList(BigInteger.ONE, BigInteger.valueOf(5), BigInteger.TEN);

        List<Ast.Expression> values = Arrays.asList(new Ast.Expression.Literal(BigInteger.ONE),
                new Ast.Expression.Literal(BigInteger.valueOf(5)),
                new Ast.Expression.Literal(BigInteger.TEN));

        Optional<Ast.Expression> value = Optional.of(new Ast.Expression.PlcList(values));
        Ast.Global ast = new Ast.Global("list", true, value);

        Scope scope = test(ast, Environment.NIL.getValue(), new Scope(null));
        Assertions.assertEquals(expected, scope.lookupVariable(ast.getName()).getValue().getValue());
    }

    @ParameterizedTest
    @MethodSource
    void testFunction(String test, Ast.Function ast, List<Environment.PlcObject> args, Object expected) {
        Scope scope = test(ast, Environment.NIL.getValue(), new Scope(null));
        Assertions.assertEquals(expected, scope.lookupFunction(ast.getName(), args.size()).invoke(args).getValue());
    }

    private static Stream<Arguments> testFunction() {
        return Stream.of(
                // FUN main() DO RETURN 0; END
                Arguments.of("Main",
                        new Ast.Function("main", Arrays.asList(), Arrays.asList(
                                new Ast.Statement.Return(new Ast.Expression.Literal(BigInteger.ZERO)))
                        ),
                        Arrays.asList(),
                        BigInteger.ZERO
                ),
                // FUN square(x) DO RETURN x * x; END
                Arguments.of("Arguments",
                        new Ast.Function("main", Arrays.asList("x"), Arrays.asList(
                                new Ast.Statement.Return(new Ast.Expression.Binary("*",
                                        new Ast.Expression.Access(Optional.empty(), "x"),
                                        new Ast.Expression.Access(Optional.empty(), "x")
                                ))
                        )),
                        Arrays.asList(Environment.create(BigInteger.TEN)),
                        BigInteger.valueOf(100)
                )

        );
    }

    @Test
    void testExpressionStatement() {
        // print("Hello, World!");
        PrintStream sysout = System.out;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));
        try {
            test(new Ast.Statement.Expression(
                    new Ast.Expression.Function("print", Arrays.asList(new Ast.Expression.Literal("Hello, World!")))
            ), Environment.NIL.getValue(), new Scope(null));
            Assertions.assertEquals("Hello, World!" + System.lineSeparator(), out.toString());
        } finally {
            System.setOut(sysout);
        }
    }

    @ParameterizedTest
    @MethodSource
    void testDeclarationStatement(String test, Ast.Statement.Declaration ast, Object expected) {
        Scope scope = test(ast, Environment.NIL.getValue(), new Scope(null));
        Assertions.assertEquals(expected, scope.lookupVariable(ast.getName()).getValue().getValue());
    }

    private static Stream<Arguments> testDeclarationStatement() {
        return Stream.of(
                // LET name;
                Arguments.of("Declaration",
                        new Ast.Statement.Declaration("name", Optional.empty()),
                        Environment.NIL.getValue()
                ),
                // LET name = 1;
                Arguments.of("Initialization",
                        new Ast.Statement.Declaration("name", Optional.of(new Ast.Expression.Literal(BigInteger.ONE))),
                        BigInteger.ONE
                ),
                // Let y = 4
                Arguments.of("Y = 4",
                        new Ast.Statement.Declaration("y", Optional.of(new Ast.Expression.Literal(BigInteger.ONE))),
                        BigInteger.ONE
                )
        );
    }

    @Test
    void testVariableAssignmentStatement() {
        // variable = 1;
        Scope scope = new Scope(null);
        scope.defineVariable("variable", true, Environment.create("variable"));
        test(new Ast.Statement.Assignment(
                new Ast.Expression.Access(Optional.empty(),"variable"),
                new Ast.Expression.Literal(BigInteger.ONE)
        ), Environment.NIL.getValue(), scope);
        Assertions.assertEquals(BigInteger.ONE, scope.lookupVariable("variable").getValue().getValue());
    }

    @Test
    void testListAssignmentStatement() {
        // list[2] = 3;

        List<Object> expected = Arrays.asList(BigInteger.ONE, BigInteger.valueOf(5), BigInteger.valueOf(3));
        List<Object> list = Arrays.asList(BigInteger.ONE, BigInteger.valueOf(5), BigInteger.TEN);

        Scope scope = new Scope(null);
        scope.defineVariable("list", true, Environment.create(list));
        test(new Ast.Statement.Assignment(
                new Ast.Expression.Access(Optional.of(new Ast.Expression.Literal(BigInteger.valueOf(2))), "list"),
                new Ast.Expression.Literal(BigInteger.valueOf(3))
        ), Environment.NIL.getValue(), scope);

        Assertions.assertEquals(expected, scope.lookupVariable("list").getValue().getValue());
    }

    @ParameterizedTest
    @MethodSource
    void testIfStatement(String test, Ast.Statement.If ast, Object expected) {
        Scope scope = new Scope(null);
        scope.defineVariable("num", true, Environment.NIL);
        test(ast, Environment.NIL.getValue(), scope);
        Assertions.assertEquals(expected, scope.lookupVariable("num").getValue().getValue());
    }

    private static Stream<Arguments> testIfStatement() {
        return Stream.of(
                // IF TRUE DO num = 1; END
                Arguments.of("True Condition",
                        new Ast.Statement.If(
                                new Ast.Expression.Literal(true),
                                Arrays.asList(new Ast.Statement.Assignment(new Ast.Expression.Access(Optional.empty(),"num"), new Ast.Expression.Literal(BigInteger.ONE))),
                                Arrays.asList()
                        ),
                        BigInteger.ONE
                ),
                // IF FALSE DO ELSE num = 10; END
                Arguments.of("False Condition",
                        new Ast.Statement.If(
                                new Ast.Expression.Literal(false),
                                Arrays.asList(),
                                Arrays.asList(new Ast.Statement.Assignment(new Ast.Expression.Access(Optional.empty(),"num"), new Ast.Expression.Literal(BigInteger.TEN)))
                        ),
                        BigInteger.TEN
                )
        );
    }

    @Test
    void testSwitchStatement() {
        // SWITCH letter CASE 'y': print("yes"); letter = 'n'; DEFAULT: print("no"); END

        Scope scope = new Scope(null);
        scope.defineVariable("letter", true, Environment.create(new Character('y')));

        List<Ast.Statement> statements = Arrays.asList(
                new Ast.Statement.Expression(new Ast.Expression.Function("print", Arrays.asList(new Ast.Expression.Literal("yes")))),
                new Ast.Statement.Assignment(new Ast.Expression.Access(Optional.empty(), "letter"),
                        new Ast.Expression.Literal(new Character('n')))
        );

        List<Ast.Statement.Case> cases = Arrays.asList(
                new Ast.Statement.Case(Optional.of(new Ast.Expression.Literal(new Character('y'))), statements),
                new Ast.Statement.Case(Optional.empty(), Arrays.asList(new Ast.Statement.Expression(new Ast.Expression.Function("print", Arrays.asList(new Ast.Expression.Literal("no"))))))
        );

        Ast.Statement.Switch ast = new Ast.Statement.Switch(new Ast.Expression.Access(Optional.empty(), "letter"), cases);

        PrintStream sysout = System.out;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));
        try {
            test(ast, Environment.NIL.getValue(), scope);
            Assertions.assertEquals("yes" + System.lineSeparator(), out.toString());
        } finally {
            System.setOut(sysout);
        }

        Assertions.assertEquals(new Character('n'), scope.lookupVariable("letter").getValue().getValue());
    }

    @Test
    void testWhileStatement() {
        // WHILE num < 10 DO num = num + 1; END
        Scope scope = new Scope(null);
        scope.defineVariable("num", true, Environment.create(BigInteger.ZERO));
        test(new Ast.Statement.While(
                new Ast.Expression.Binary("<",
                        new Ast.Expression.Access(Optional.empty(),"num"),
                        new Ast.Expression.Literal(BigInteger.TEN)
                ),
                Arrays.asList(new Ast.Statement.Assignment(
                        new Ast.Expression.Access(Optional.empty(),"num"),
                        new Ast.Expression.Binary("+",
                                new Ast.Expression.Access(Optional.empty(),"num"),
                                new Ast.Expression.Literal(BigInteger.ONE)
                        )
                ))
        ),Environment.NIL.getValue(), scope);
        Assertions.assertEquals(BigInteger.TEN, scope.lookupVariable("num").getValue().getValue());
    }

    @ParameterizedTest
    @MethodSource
    void testLiteralExpression(String test, Ast ast, Object expected) {
        test(ast, expected, new Scope(null));
    }

    private static Stream<Arguments> testLiteralExpression() {
        return Stream.of(
                // NIL
                Arguments.of("Nil", new Ast.Expression.Literal(null), Environment.NIL.getValue()), //remember, special case
                // TRUE
                Arguments.of("Boolean", new Ast.Expression.Literal(true), true),
                // 1
                Arguments.of("Integer", new Ast.Expression.Literal(BigInteger.ONE), BigInteger.ONE),
                // 1.0
                Arguments.of("Decimal", new Ast.Expression.Literal(BigDecimal.ONE), BigDecimal.ONE),
                // 'c'
                Arguments.of("Character", new Ast.Expression.Literal('c'), 'c'),
                // "string"
                Arguments.of("String", new Ast.Expression.Literal("string"), "string")
        );
    }

    @ParameterizedTest
    @MethodSource
    void testGroupExpression(String test, Ast ast, Object expected) {
        test(ast, expected, new Scope(null));
    }

    private static Stream<Arguments> testGroupExpression() {
        return Stream.of(
                // (1)
                Arguments.of("Literal", new Ast.Expression.Group(new Ast.Expression.Literal(BigInteger.ONE)), BigInteger.ONE),
                // "a"
                Arguments.of("Literal", new Ast.Expression.Group(new Ast.Expression.Literal("a")), "a"),
                // (1 + 10)
                Arguments.of("Binary",
                        new Ast.Expression.Group(new Ast.Expression.Binary("+",
                                new Ast.Expression.Literal(BigInteger.ONE),
                                new Ast.Expression.Literal(BigInteger.TEN)
                        )),
                        BigInteger.valueOf(11)
                ),
                // "a" + "b"
                Arguments.of("Binary Concat",
                        new Ast.Expression.Group(new Ast.Expression.Binary("+",
                                new Ast.Expression.Literal("a"),
                                new Ast.Expression.Literal("b")
                        )),
                        "ab"
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    // TODO: Ask to check the exponent test cases
    void testBinaryExpression(String test, Ast ast, Object expected) {
        test(ast, expected, new Scope(null));
    }

    private static Stream<Arguments> testBinaryExpression() {
        return Stream.of(
                // TRUE && FALSE
                Arguments.of("And",
                        new Ast.Expression.Binary("&&",
                                new Ast.Expression.Literal(true),
                                new Ast.Expression.Literal(false)
                        ),
                        false
                ),
                // False && False
                Arguments.of("False && False",
                        new Ast.Expression.Binary("&&",
                                new Ast.Expression.Literal(false),
                                new Ast.Expression.Literal(false)
                        ),
                        false
                ),
                // True && True
                Arguments.of("True && True",
                        new Ast.Expression.Binary("&&",
                                new Ast.Expression.Literal(true),
                                new Ast.Expression.Literal(true)
                        ),
                        true
                ),
                // TRUE || undefined
                Arguments.of("Or (Short Circuit)",
                        new Ast.Expression.Binary("||",
                                new Ast.Expression.Literal(true),
                                new Ast.Expression.Access(Optional.empty(), "undefined")
                        ),
                        true
                ),
                // False || undefined
                Arguments.of("Or (Short Circuit) False",
                        new Ast.Expression.Binary("||",
                                new Ast.Expression.Literal(false),
                                new Ast.Expression.Access(Optional.empty(), "undefined")
                        ),
                        false
                ),
                // Undefined Left Side
                Arguments.of("Undefined Left Side",
                        new Ast.Expression.Binary("||",
                                new Ast.Expression.Access(Optional.empty(), "undefined"),
                                new Ast.Expression.Literal(true)
                        ),
                        null
                ),
                // 1 < 10
                Arguments.of("Less Than",
                        new Ast.Expression.Binary("<",
                                new Ast.Expression.Literal(BigInteger.ONE),
                                new Ast.Expression.Literal(BigInteger.TEN)
                        ),
                        true
                ),
                // 1 > 3
                Arguments.of("Greater Than False",
                        new Ast.Expression.Binary(">",
                                new Ast.Expression.Literal(BigInteger.ONE),
                                new Ast.Expression.Literal(BigInteger.valueOf(3))
                        ),
                        false
                ),
                // 3 > 2
                Arguments.of("Greater Than True",
                        new Ast.Expression.Binary(">",
                                new Ast.Expression.Literal(BigInteger.valueOf(3)),
                                new Ast.Expression.Literal(BigInteger.valueOf(2))
                        ),
                        true
                ),
                // -2 < -1
                Arguments.of("Negative",
                        new Ast.Expression.Binary("<",
                                new Ast.Expression.Literal(BigInteger.valueOf(-2)),
                                new Ast.Expression.Literal(BigInteger.valueOf(-1))
                        ),
                        true
                ),
                // 1 == 10
                Arguments.of("Equal",
                        new Ast.Expression.Binary("==",
                                new Ast.Expression.Literal(BigInteger.ONE),
                                new Ast.Expression.Literal(BigInteger.TEN)
                        ),
                        false
                ),
                // 1 != 10
                Arguments.of("not Equal",
                        new Ast.Expression.Binary("!=",
                                new Ast.Expression.Literal(BigInteger.ONE),
                                new Ast.Expression.Literal(BigInteger.TEN)
                        ),
                        true
                ),
                // 10 != 10
                Arguments.of("not Equal False",
                        new Ast.Expression.Binary("!=",
                                new Ast.Expression.Literal(BigInteger.TEN),
                                new Ast.Expression.Literal(BigInteger.TEN)
                        ),
                        false
                ),
                // "a" + "b"
                Arguments.of("Concatenation",
                        new Ast.Expression.Binary("+",
                                new Ast.Expression.Literal("a"),
                                new Ast.Expression.Literal("b")
                        ),
                        "ab"
                ),
                // "a" + 1
                Arguments.of("String + BigInteger",
                        new Ast.Expression.Binary("+",
                                new Ast.Expression.Literal("a"),
                                new Ast.Expression.Literal(BigInteger.ONE)
                        ),
                        null
                ),
                // 1 + 10
                Arguments.of("Addition",
                        new Ast.Expression.Binary("+",
                                new Ast.Expression.Literal(BigInteger.ONE),
                                new Ast.Expression.Literal(BigInteger.TEN)
                        ),
                        BigInteger.valueOf(11)
                ),
                // 1 - 3
                Arguments.of("Subtraction 1 - 3",
                        new Ast.Expression.Binary("-",
                                new Ast.Expression.Literal(BigInteger.ONE),
                                new Ast.Expression.Literal(BigInteger.valueOf(3))
                        ),
                        BigInteger.valueOf(-2)
                ),
                // -2 * -5
                Arguments.of("Multiply -2 * -5",
                        new Ast.Expression.Binary("*",
                                new Ast.Expression.Literal(BigInteger.valueOf(-2)),
                                new Ast.Expression.Literal(BigInteger.valueOf(-5))
                        ),
                        BigInteger.valueOf(10)
                ),
                // -2.0 * 4.4
                Arguments.of("Multiply -2.0 * 4.4",
                        new Ast.Expression.Binary("*",
                                new Ast.Expression.Literal(new BigDecimal(-2.0)),
                                new Ast.Expression.Literal(new BigDecimal(4.4))
                        ),
                        new BigDecimal(-2.0 * 4.4)
                ),
                // 1 / 3.4
                Arguments.of("Invalid Division",
                        new Ast.Expression.Binary("/",
                                new Ast.Expression.Literal(BigInteger.valueOf(1)),
                                new Ast.Expression.Literal(new BigDecimal("3.4"))
                        ),
                        null
                ),
                // 1.2 / 3.4
                Arguments.of("Division",
                        new Ast.Expression.Binary("/",
                                new Ast.Expression.Literal(new BigDecimal("1.2")),
                                new Ast.Expression.Literal(new BigDecimal("3.4"))
                        ),
                        new BigDecimal("0.4")
                ),
                // 2 ^ 3
                Arguments.of("Exponent Int",
                        new Ast.Expression.Binary("^",
                            new Ast.Expression.Literal(BigInteger.valueOf(2)),
                            new Ast.Expression.Literal(BigInteger.valueOf(3))
                        ),
                        BigInteger.valueOf(8)
                ),
                // -2 ^ 3
                Arguments.of("Exponent Int Negative",
                        new Ast.Expression.Binary("^",
                                new Ast.Expression.Literal(BigInteger.valueOf(-2)),
                                new Ast.Expression.Literal(BigInteger.valueOf(3))
                        ),
                        BigInteger.valueOf(-8)
                ),
                // -2.2 ^ 3
                Arguments.of("Exponent Decimal",
                        new Ast.Expression.Binary("^",
                            new Ast.Expression.Literal(new BigDecimal(-2.2)),
                            new Ast.Expression.Literal(BigInteger.valueOf(3))
                        ),
                        BigDecimal.valueOf(-10.648)

                ),
                // 2 ^ -3
                Arguments.of("Negative Exponent Int",
                        new Ast.Expression.Binary("^",
                                new Ast.Expression.Literal(BigInteger.valueOf(2)),
                                new Ast.Expression.Literal(BigInteger.valueOf(-3))
                        ),
                        BigDecimal.valueOf(1.0/8.0)
                ),
                // -2.3 ^ -2
                Arguments.of("Negative Exponent Int",
                        new Ast.Expression.Binary("^",
                                new Ast.Expression.Literal(BigDecimal.valueOf(-2.2)),
                                new Ast.Expression.Literal(BigInteger.valueOf(-2))
                        ),
                        BigDecimal.valueOf(1.0/4.84)
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testAccessExpression(String test, Ast ast, Object expected) {
        Scope scope = new Scope(null);
        scope.defineVariable("variable", true, Environment.create("variable"));
        test(ast, expected, scope);
    }

    // TODO: Ask about what the list[1] test should look like
    private static Stream<Arguments> testAccessExpression() {
        return Stream.of(
                // variable
                Arguments.of("Variable",
                        new Ast.Expression.Access(Optional.empty(), "variable"),
                        "variable"
                )

                // list[1]
        );
    }

    @Test
    void testListAccessExpression() {
        // list[1]

        List<Object> list = Arrays.asList(BigInteger.ONE, BigInteger.valueOf(5), BigInteger.TEN);

        Scope scope = new Scope(null);
        scope.defineVariable("list", true, Environment.create(list));
        test(new Ast.Expression.Access(Optional.of(new Ast.Expression.Literal(BigInteger.valueOf(1))), "list"), BigInteger.valueOf(5), scope);
    }

    @ParameterizedTest
    @MethodSource
    void testFunctionExpression(String test, Ast ast, Object expected) {
        Scope scope = new Scope(null);
        scope.defineFunction("function", 0, args -> Environment.create("function"));
        test(ast, expected, scope);
    }

    private static Stream<Arguments> testFunctionExpression() {
        return Stream.of(
                // function()
                Arguments.of("Function",
                        new Ast.Expression.Function("function", Arrays.asList()),
                        "function"
                ),
                // Log(1)
                Arguments.of("Log(1)",
                        new Ast.Expression.Function("log", Arrays.asList(new Ast.Expression.Literal(new BigDecimal(1)))),
                        Environment.NIL.getValue()
                ),
                // print("Hello, World!")
                Arguments.of("Print",
                        new Ast.Expression.Function("print", Arrays.asList(new Ast.Expression.Literal("Hello, World!"))),
                        Environment.NIL.getValue()
                )
        );
    }

    @Test
    void testPlcList() {
        // [1, 5, 10]

        List<Object> expected = Arrays.asList(BigInteger.ONE, BigInteger.valueOf(5), BigInteger.TEN);

        List<Ast.Expression> values = Arrays.asList(new Ast.Expression.Literal(BigInteger.ONE),
                new Ast.Expression.Literal(BigInteger.valueOf(5)),
                new Ast.Expression.Literal(BigInteger.TEN));

        Ast ast = new Ast.Expression.PlcList(values);

        test(ast, expected, new Scope(null));
    }

    private static Scope test(Ast ast, Object expected, Scope scope) {
        Interpreter interpreter = new Interpreter(scope);
        if (expected != null) {
            Assertions.assertEquals(expected, interpreter.visit(ast).getValue());
        } else {
            Assertions.assertThrows(RuntimeException.class, () -> interpreter.visit(ast));
        }
        return interpreter.getScope();
    }

}
