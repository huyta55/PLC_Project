package plc.homework;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Contains JUnit tests for {@link Regex}. A framework of the test structure 
 * is provided, you will fill in the remaining pieces.
 *
 * To run tests, either click the run icon on the left margin, which can be used
 * to run all tests or only a specific test. You should make sure your tests are
 * run through IntelliJ (File > Settings > Build, Execution, Deployment > Build
 * Tools > Gradle > Run tests using <em>IntelliJ IDEA</em>). This ensures the
 * name and inputs for the tests are displayed correctly in the run window.
 */
public class RegexTests {

    /**
     * This is a parameterized test for the {@link Regex#EMAIL} regex. The
     * {@link ParameterizedTest} annotation defines this method as a
     * parameterized test, and {@link MethodSource} tells JUnit to look for the
     * static method {@link #testEmailRegex()}.
     *
     * For personal preference, I include a test name as the first parameter
     * which describes what that test should be testing - this is visible in
     * IntelliJ when running the tests (see above note if not working).
     */
    @ParameterizedTest
    @MethodSource
    public void testEmailRegex(String test, String input, boolean success) {
        test(input, Regex.EMAIL, success);
    }

    /**
     * This is the factory method providing test cases for the parameterized
     * test above - note that it is static, takes no arguments, and has the same
     * name as the test. The {@link Arguments} object contains the arguments for
     * each test to be passed to the function above.
     */
    public static Stream<Arguments> testEmailRegex() {
        return Stream.of(
                Arguments.of("Alphanumeric", "thelegend27@gmail.com", true),
                Arguments.of("UF Domain", "otherdomain@ufl.edu", true),
                Arguments.of("Missing Domain Dot", "missingdot@gmailcom", false),
                Arguments.of("Symbols", "symbols#$%@gmail.com", false),
                Arguments.of("Numbers Repeating", "00001@yahoo.net", true),
                Arguments.of("Two Dots", "huy.ta@ufl.edu", true),
                Arguments.of("Underscore", "huy_ta@ufl.edu", true),
                Arguments.of("One Character", "1@ufl.edu", false),
                Arguments.of("Two Character", "02@ufl.edu", true),
                Arguments.of("No Character", "@ufl.edu", false),
                Arguments.of("No @", "02ufl.edu", false),
                Arguments.of("One After Dot", "huyta@ufl.e", false),
                Arguments.of("Two After Dot", "Huyta2@uFL.e", false),
                Arguments.of("Three Numbers After Dot", "hUyTa3@UfL.123", false),
                Arguments.of("At Numbers", "huyta@123.com", true),
                Arguments.of("At Numbers Dot Numbers", "huyta@123.456", false),
                Arguments.of("Dot Capital", "huyta@ufl.EDU", false),
                Arguments.of("Dash after At", "huyta@u-fl.edu", false),
                Arguments.of("No After At", "huyta@.edu", false),
                Arguments.of("1 After At", "huyta@u.edu", true)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testOddStringsRegex(String test, String input, boolean success) {
        test(input, Regex.ODD_STRINGS, success);
    }

    public static Stream<Arguments> testOddStringsRegex() {
        return Stream.of(
                // what have eleven letters and starts with gas?
                Arguments.of("11 Characters", "automobiles", true),
                Arguments.of("13 Characters", "i<3pancakes13", true),
                Arguments.of("5 Characters", "5five", false),
                Arguments.of("14 Characters", "i<3pancakes14!", false),
                Arguments.of("9 Characters", "abc123de4", false),
                Arguments.of("10 Numbers", "1234567890", false),
                Arguments.of("20 Characters", "mynameishuyandiam19y", false),
                Arguments.of("25 Characters", "mynameishuyandiam19yearso", false),
                Arguments.of("Symbols + Letters + Numbers", "$$hqtISC00L#.", true),
                Arguments.of("15 Symbols", "!@#$%^&*()<>:", true),
                Arguments.of("Space in between", "My Name is Huy Ta", true),
                Arguments.of("11 Spaces", "           ", true)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testCharacterListRegex(String test, String input, boolean success) {
        test(input, Regex.CHARACTER_LIST, success);
    }

    public static Stream<Arguments> testCharacterListRegex() {
        return Stream.of(
                Arguments.of("Single Element", "['a']", true),
                Arguments.of("Multiple Elements", "['a','b','c']", true),
                Arguments.of("Missing Brackets", "'a','b','c'", false),
                Arguments.of("Missing Commas", "['a' 'b' 'c']", false),
                Arguments.of("Missing Left Bracket", "'a','b','c']", false),
                Arguments.of("Missing Right Bracket", "['a','b','c'", false),
                Arguments.of("Trailing Comma", "['a','b','c',", false),
                Arguments.of("Trailing Comma Space", "['a','b','c', ]",false),
                Arguments.of("One Space Between", "['a', 'b', 'c']",true),
                Arguments.of("Two Space Between", "['a',  'b',  'c']",false),
                Arguments.of("Space inside quote", "[' a ', 'b ', 'c ']", false),
                Arguments.of("Two Characters", "['aa','b','c']", false),
                Arguments.of("7 Characters", "['a','bbbbbbb', 'c']", false),
                Arguments.of("Non Alphabetical", "'b', 'g','a'", true),
                Arguments.of("Empty List", "[]", true),
                Arguments.of("Starting Comma", "[,'a','b','c']", false),
                Arguments.of("Starting Space", "[ 'a','b','c']", true)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testDecimalRegex(String test, String input, boolean success) {
        test(input, Regex.DECIMAL, success);
    }

    public static Stream<Arguments> testDecimalRegex() {
        return Stream.of(
                Arguments.of("Large Decimal", "10100.001", true),
                Arguments.of("Small Negative Decimal", "-1.0", true),
                Arguments.of("Small Decimal", "1.25", true),
                Arguments.of("Pi", "3.1415926535", true),
                Arguments.of("Large Negative Decimal", "-102143124.023", true),
                Arguments.of("Trailing Zeros", "12.2500000", true),
                Arguments.of("Leading Zero", "012.23", false),
                Arguments.of("No Leading Digit", ".5", false),
                Arguments.of("No Decimal", "1", false),
                Arguments.of("Single leading zero", "0.92", true),
                Arguments.of("No Digits", "a", false),
                Arguments.of("No Digits After Decimal", "2.a", false),
                Arguments.of("Only Negative Sign", "-", false),
                Arguments.of("Nothing After Decimal", "123.", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testStringRegex(String test, String input, boolean success) {
        test(input, Regex.STRING, success);
    }

    public static Stream<Arguments> testStringRegex() {
        return Stream.of(
                Arguments.of("Unterminated", "\"unterminated", false),
                Arguments.of("Invalid Escape", "\"invalid\\escape\"", false),
                Arguments.of("No Quotes", "abc", false),
                Arguments.of("Invalid Escape2", "\"\\\\\\\"", false),
                Arguments.of("Not Starting with Quotes", "example \"literal\"", false),
                Arguments.of("Empty Literal", "\"\"", true),
                Arguments.of("Hello World", "\"Hello, World\"", true),
                Arguments.of("1t2", "\"1\t2\"", true),
                Arguments.of("Symbols", "\"!@#$%^&*()_+-=[{}]|;:'\",<.>/?\"", true),
                Arguments.of("Numbers", "\"1234567890\"", true)
        );
    }

    /**
     * Asserts that the input matches the given pattern. This method doesn't do
     * much now, but you will see this concept in future assignments.
     */
    private static void test(String input, Pattern pattern, boolean success) {
        Assertions.assertEquals(success, pattern.matcher(input).matches());
    }

}
