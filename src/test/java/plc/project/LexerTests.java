package plc.project;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class LexerTests {

    @ParameterizedTest
    @MethodSource
    void testIdentifier(String test, String input, boolean success) {
        test(input, Token.Type.IDENTIFIER, success);
    }

    private static Stream<Arguments> testIdentifier() {
        return Stream.of(
                Arguments.of("Alphabetic", "getName", true),
                Arguments.of("Alphanumeric", "thelegend27", true),
                Arguments.of("Leading Hyphen", "-five", false),
                Arguments.of("Leading Digit", "1fish2fish3fishbluefish", false),
                Arguments.of("Alphabetic Final", "abc", true),
                Arguments.of("Alphanumeric Final", "abc123", true),
                Arguments.of("Underscores", "a_b_c", true),
                Arguments.of("Hyphens", "a-b-c", true),
                Arguments.of("Leading AT", "@abc", true),
                Arguments.of("Middling AT", "a@bc", false),
                Arguments.of("Leading Underscore", "_abc", false),
                Arguments.of("Digit Letters", "1abc", false),
                Arguments.of("Capitals", "ABC", true),
                Arguments.of("Short Identifier", "a", true),
                Arguments.of("Long Identifier", "abcdefghijklmnopqrstuvwxyz012346789_-", true)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testInteger(String test, String input, boolean success) {
        test(input, Token.Type.INTEGER, success);
    }

    private static Stream<Arguments> testInteger() {
        return Stream.of(
                Arguments.of("Single Digit", "1", true),
                Arguments.of("Multiple Digits", "12345", true),
                Arguments.of("Negative", "-1", true),
                Arguments.of("Leading Zero", "01", false),
                Arguments.of("Multiple Digits Final", "123", true),
                Arguments.of("Negative Final", "-1", true),
                Arguments.of("Above Long Max", "123456789123456789123456789", true),
                Arguments.of("Zero", "0", true)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testDecimal(String test, String input, boolean success) {
        test(input, Token.Type.DECIMAL, success);
    }

    private static Stream<Arguments> testDecimal() {
        return Stream.of(
                Arguments.of("Multiple Digits", "123.456", true),
                Arguments.of("Negative Decimal", "-1.0", true),
                Arguments.of("Trailing Decimal", "1.", false),
                Arguments.of("Leading Decimal", ".5", false),
                //
                Arguments.of("Multiple Decimals", "123.456.789", false),
                Arguments.of("Letters after Decimals", "123.letters", false),
                Arguments.of("Leading Zeros", "0000000000.5", false),
                Arguments.of("No Number Decimal", "a.b", false),
                Arguments.of("Single Digits Final", "1.0", true),
                Arguments.of("Multiple Digits Final", "123.456", true),
                Arguments.of("Above Integer Precision", "9007199254740993.0", true),
                Arguments.of("Trailing Zeros", "111.000", true)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testCharacter(String test, String input, boolean success) {
        test(input, Token.Type.CHARACTER, success);
    }

    private static Stream<Arguments> testCharacter() {
        return Stream.of(
                Arguments.of("Alphabetic", "\'c\'", true),
                Arguments.of("Newline Escape", "\'\\n\'", true),
                Arguments.of("Empty", "\'\'", false),
                Arguments.of("Multiple", "\'abc\'", false),
                //
                Arguments.of("Unterminated", "\'a", false),
                Arguments.of("Character + newline", "\'a\n\'", false),
                Arguments.of("Character + space", "\'a \'", false),
                Arguments.of("Only Escape","\'\\\'", false),
                Arguments.of("Correct Escape b", "\'\\b\'", true),
                Arguments.of("Correct Escape r", "\'\\r\'", true),
                Arguments.of("Correct Escape t", "\'\\t\'", true),
                Arguments.of("Correct Escape \'", "\'\\\'\'", true),
                Arguments.of("Correct Escape \"", "\'\\\"\'", true),
                Arguments.of("Correct Escape \\", "\'\\\\\'", true),
                Arguments.of("Only \'", "\'\'\'", false),
                Arguments.of("Multiple lines", "\'abc\ndef", false),
                //
                Arguments.of("Digit", "\'1\'", true),
                Arguments.of("Unicode", "\'ρ\'", true),
                Arguments.of("Space", "\' \'", true),
                Arguments.of("Single Quote Escape", "\'\\\'\'", true),
                Arguments.of("Unterminated Newline", "\'\n\'", false),
                Arguments.of("Unterminated Empty", "\'", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testString(String test, String input, boolean success) {
        test(input, Token.Type.STRING, success);
    }

    private static Stream<Arguments> testString() {
        return Stream.of(
                Arguments.of("Empty", "\"\"", true),
                Arguments.of("Alphabetic", "\"abc\"", true),
                Arguments.of("Newline Escape", "\"Hello,\\nWorld\"", true),
                Arguments.of("Unterminated", "\"unterminated", false),
                Arguments.of("Invalid Escape", "\"invalid\\escape\"", false),
                // Custom Test Cases Below
                Arguments.of("Single Character", "\"a\"", true),
                Arguments.of("Numeric", "\"123\"", true),
                Arguments.of("Symbols", "\"!@#$%^&*\"", true),
                Arguments.of("Unicode", "\"ρ★⚡\"", true),
                Arguments.of("White Space", "\" \"", true),
                Arguments.of("Alphabetic Escapes", "\"a\\bcdefghijklm\\nopq\\rs\\tuvwxyz\"", true),
                Arguments.of("Special Escapes", "\"sq\\'dq\\\"bs\\\\\"", true),
                Arguments.of("Invalid Escapes", "\"abc\\0123\"", false),
                Arguments.of("Unicode Escapes", "\"a\\u0000b\\u12ABc\"", false),
                Arguments.of("Unterminated Newline", "\"unterminated \n\"", false),
                Arguments.of("Unterminated Empty", "\"", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testOperator(String test, String input, boolean success) {
        //this test requires our lex() method, since that's where whitespace is handled.
        test(input, Arrays.asList(new Token(Token.Type.OPERATOR, input, 0)), success);
    }

    private static Stream<Arguments> testOperator() {
        return Stream.of(
                Arguments.of("Character", "(", true),
                Arguments.of("Comparison", "!=", true),
                Arguments.of("Space", " ", false),
                Arguments.of("Tab", "\t", false),
                //
                Arguments.of("Remainder", "%", true),
                // Arguments.of("Unicode", "ρ", true),
                Arguments.of("Equality", "==", true),
                Arguments.of("Greater Than", ">", true),
                Arguments.of("Not Equals", "!=", true),
                Arguments.of("Plus", "+", true),
                Arguments.of("Hyphen", "-", true),
                Arguments.of("Form Feed", "\b", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testExamples(String test, String input, List<Token> expected) {
        test(input, expected, true);
    }

    private static Stream<Arguments> testExamples() {
        return Stream.of(
                Arguments.of("Example 1", "LET x = 5;", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "LET", 0),
                        new Token(Token.Type.IDENTIFIER, "x", 4),
                        new Token(Token.Type.OPERATOR, "=", 6),
                        new Token(Token.Type.INTEGER, "5", 8),
                        new Token(Token.Type.OPERATOR, ";", 9)
                )),
                Arguments.of("Example 2", "print(\"Hello, World!\");", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "print", 0),
                        new Token(Token.Type.OPERATOR, "(", 5),
                        new Token(Token.Type.STRING, "\"Hello, World!\"", 6),
                        new Token(Token.Type.OPERATOR, ")", 21),
                        new Token(Token.Type.OPERATOR, ";", 22)
                ))
        );
    }

    @Test
    void testException() {
        ParseException exception = Assertions.assertThrows(ParseException.class,
                () -> new Lexer("\"unterminated").lex());
        Assertions.assertEquals(13, exception.getIndex());
    }

    /**
     * Tests that lexing the input through {@link Lexer#lexToken()} produces a
     * single token with the expected type and literal matching the input.
     */
    private static void test(String input, Token.Type expected, boolean success) {
        try {
            if (success) {
                Assertions.assertEquals(new Token(expected, input, 0), new Lexer(input).lexToken());
            } else {
                Assertions.assertNotEquals(new Token(expected, input, 0), new Lexer(input).lexToken());
            }
        } catch (ParseException e) {
            Assertions.assertFalse(success, e.getMessage());
        }
    }

    /**
     * Tests that lexing the input through {@link Lexer#lex()} matches the
     * expected token list.
     */
    private static void test(String input, List<Token> expected, boolean success) {
        try {
            if (success) {
                Assertions.assertEquals(expected, new Lexer(input).lex());
            } else {
                Assertions.assertNotEquals(expected, new Lexer(input).lex());
            }
        } catch (ParseException e) {
            Assertions.assertFalse(success, e.getMessage());
        }
    }

}
