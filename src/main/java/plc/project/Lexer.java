package plc.project;

import java.util.ArrayList;
import java.util.List;

/**
 * The lexer works through three main functions:
 *
 *  - {@link #lex()}, which repeatedly calls lexToken() and skips whitespace
 *  - {@link #lexToken()}, which lexes the next token
 *  - {@link CharStream}, which manages the state of the lexer and literals
 *
 * If the lexer fails to parse something (such as an unterminated string) you
 * should throw a {@link ParseException} with an index at the character which is
 * invalid.
 *
 * The {@link #peek(String...)} and {@link #match(String...)} functions are * helpers you need to use, they will make the implementation a lot easier. */
public final class Lexer {

    private final CharStream chars;
    private boolean checkRange(int type, String number) {
        // if type = 1, then check for Integer if it's in range of Long
        if (type == 1) {
            String longMax = String.valueOf(Long.MAX_VALUE);
            // if the string size is >, then it's out of range
            if (number.length() > longMax.length()) {
                return true;
            }
            else if (number.length() < longMax.length()) {
                return false;
            }
            // else loop through the Strings and compare each number from left to right
            for (int i = 0; i < number.length(); ++i) {
                if ((int) number.charAt(i) > (int) longMax.charAt(i)) {
                    return true;
                }
            }
        }
        // if type = 2, then check for Decimal if it's in range of Integer
        else {
            String intMax = String.valueOf(Integer.MAX_VALUE);
            // if the string size is >, then it's out of range
            if (number.length() > intMax.length()) {
                return true;
            }
            else if (number.length() < intMax.length()) {
                return false;
            }
            // else loop through the Strings and compare each number from left to right
            for (int i = 0; i < number.length(); ++i) {
                // if the current character is a period, then skip it
                if (number.charAt(i) == '.') {
                    continue;
                }
                if ((int) number.charAt(i) > (int) intMax.charAt(i)) {
                    return true;
                }
            }
        }
        return false;
    }

    public Lexer(String input) {
        chars = new CharStream(input);
    }
    // TODO: Ask WTF is happening with the indexes
    List<Token> tokenList = new ArrayList<Token>();
    private static final String whiteSpace = new String(new char[] {'\n', '\b', '\r', '\t', ' '});
    /**
     * Repeatedly lexes the input using {@link #lexToken()}, also skipping over
     * whitespace where appropriate.
     */
    public List<Token> lex() {
        while(chars.has(0)) {
            // skip over white space
            if (peek("[ \n\b\r\t]")) {
                chars.advance();
                chars.skip();
            }
            else {
                tokenList.add(lexToken());
            }
        }
        return tokenList;
    }

    /**
     * This method determines the type of the next token, delegating to the
     * appropriate lex method. As such, it is best for this method to not change
     * the state of the char stream (thus, use peek not match).
     *
     * The next character should start a valid token since whitespace is handled
     * by {@link #lex()}
     */
    public Token lexToken() {
        // delegate to the correct function based on what the 1st character is
        char currentChar = chars.get(0);

        // if it's ", then lex String
        if (currentChar == '\"') {
            return lexString();
        }
        // if it's ', then lex Character
        else if (currentChar == '\'') {
            return lexCharacter();
        }
        // if it's equal to "-", but the next character is a number, or if it's a number, then lex Number
        else if (((currentChar == '-') && (chars.has(1)) && (!Character.isWhitespace(chars.get(1)))) || (Character.isDigit(currentChar))) {
            return lexNumber();
        }
        // If it's a letter without " or ', then lex Identifier
        else if (peek("[A-Za-z@]")) {
            return lexIdentifier();
        }
        // else lex the operator
        else {
            return lexOperator();
        }
    }
    // DONE
    public Token lexIdentifier() {
        int startIndex = chars.index;
        // getting the 1st character in the stream
        char currentToken = chars.get(0);
        String currentString = String.valueOf(currentToken);
        // Check that the token doesn't start with a digit, underscore, or a hyphen
        if (Character.isDigit(currentToken) || currentToken == '_' || currentToken == '-') {
            // if it's any of these, throw a parse exception
            throw new ParseException("Invalid Identifier", chars.index);
        }
        chars.advance();
        // while the token stream still has more characters, check that they match the requirements to be an identifier
        while (chars.has(0)) {
            if (peek("[a-zA-Z0-9_-]")) {
                currentString += String.valueOf(chars.get(0));
                chars.advance();
            }
            else if (match(" ")) {
                return new Token(Token.Type.IDENTIFIER, currentString, startIndex);
            }
            else {
                break;
            }
        }
        // looping through the stream to get to the end of the token
        return new Token(Token.Type.IDENTIFIER, currentString, startIndex);
    }
    // DONE
    public Token lexNumber() {
        int startIndex = chars.index;
        boolean decimal = false;
        String number = "";
        // check that the first character is a digit
        if (!Character.isDigit(chars.get(0))) {
            // Check whether it's a negative sign
            if (chars.get(0) == '-') {
                number += chars.get(0);
                if (!chars.has(1)) {
                    throw new ParseException("Lone -", chars.index);
                }
                chars.advance();
                if (!Character.isDigit(chars.get(0))) {
                    throw new ParseException("Not a digit", chars.index);
                }
            }
            else {
                throw new ParseException("Not a digit", chars.index);
            }
        }
        number += chars.get(0);
        // check that if the character was a 0, then there's nothing that comes after it unless it's a decimal
        if (chars.get(0) == '0') {
            if (!(chars.has(1))) {
                chars.advance();
                return new Token(Token.Type.INTEGER, number, startIndex);
            }
            chars.advance();
            // else check that there's a decimal point that follows
            if (peek(String.valueOf(chars.get(0)), ".")) {
                decimal = true;
                if (!(chars.has(1))) {
                    throw new ParseException("Trailing Decimal", chars.index);
                }
            }
        }
        chars.advance();
        // while there are more characters in the token, go to them and check
        while (chars.has(0)) {
            // check that current character is a digit
            if (!Character.isDigit(chars.get(0))) {
                // Check if it's a decimal
                if ((chars.get(0) == '.')) {
                    // Check that there's only one decimal
                    if (decimal) {
                        throw new ParseException("Not a digit!", chars.index);
                    }
                    decimal = true;
                    // Check that there's no lingering decimal
                    if (!chars.has(1)) {
                        throw new ParseException("Lingering Decimal", chars.index);
                    }
                }
                else {
                    break;
                }
            }
            number+= chars.get(0);
            chars.advance();
        }
        // return the correct token type based on decimal
        if (decimal) {
            /*if (checkRange(2, number)) {
                throw new ParseException("Number is out of range!", startIndex);
            }*/
            return new Token(Token.Type.DECIMAL, number, startIndex);
        }
        else {
            // Checks whether the string is within range
            /*if (checkRange(1, number)) {
                throw new ParseException("Number is out of range!", startIndex);
            }*/
            return new Token(Token.Type.INTEGER, number, startIndex);
        }

    }
    // Done
    public Token lexCharacter() {
        int startIndex = chars.index;
        String character = String.valueOf(chars.get(0));
        if (!chars.has(1)) {
            throw new ParseException("Unterminated Character", chars.index);
        }
        chars.advance();
        // check whether there is an escape character, if not then make sure the next char is '
        if (chars.get(0) == '\\') {
            character += chars.get(0);
            lexEscape();
            character += chars.get(0);
        }
        else {
            // either the closing quote is here or in the next one, no more
            if (chars.get(0) == '\'') {
                throw new ParseException("Empty character", chars.index);
            }
            // throw exception if there's a newline
            else if (chars.get(0) == '\n') {
                throw new ParseException("Newline Prohibited", chars.index);
            }
            else {
                character += chars.get(0);
            }
        }
        if (!chars.has(1)) {
            throw new ParseException("Unterminated Char", chars.index);
        }
        chars.advance();
        // check that the last character is a single quote
        if (!(chars.get(0) == '\'')) {
            throw new ParseException("Unterminated Char", chars.index);
        }
        character += chars.get(0);
        chars.advance();
        return new Token(Token.Type.CHARACTER, character, startIndex);
    }
    // DONE
    public Token lexString() {
        boolean checkQuotes = false;
        int startIndex = chars.index;
        String token = "";
        // Check for the beginning quote
        // getting the " character
        token += String.valueOf(chars.get(0));
        // while there are more tokens, we check that they match the conditions of a string
        while(chars.has(1)) {
            chars.advance();
            // if the character is a ", then we close the string and then exit the loop
            if (chars.get(0) == '\"') {
                checkQuotes = true;
                token += chars.get(0);
                break;
            }
            else if (chars.get(0) == '\n') {
                throw new ParseException("Newline not allowed", chars.index);
            }
            // else check if there is an escape
            else if (chars.get(0) == '\\') {
                token += chars.get(0);
                lexEscape();
                // getting the 2nd part of the escape
                token += chars.get(0);
                continue;

            }
            // else that means it's just a character, so add it to the string
            token += (chars.get(0));
        }
        if (!checkQuotes) {
            throw new ParseException("Unterminated", ++chars.index);
        }
        chars.advance();
        return new Token(Token.Type.STRING, token, startIndex);
    }
    // DONE
    public void lexEscape() {
        // Check that there is a character following the first escape \
        if (!chars.has(1)) {
            throw new ParseException("Expecting Valid Escape Character", chars.index);
        }
        chars.advance();
        // Then check that the next character follows the rule of an escape character according to the grammar
        if (!peek("[b | n | r | t | \\\\ | ' | \"]")) {
            throw new ParseException("Invalid Escape", chars.index);
        }
        // else nothing happens
    }

    public Token lexOperator() {
        String op = "";
        int startIndex = chars.index;
        // Lexing &
        if (chars.get(0) == '&') {
            op += chars.get(0);
            // check that the char after is also a &
            if (!chars.has(1)) {
                throw new ParseException("Invalid &", chars.index);
            }
            chars.advance();
            if (chars.get(0) == '&') {
                op += chars.get(0);
                return new Token(Token.Type.OPERATOR, op, startIndex);
            }
            else {
                throw new ParseException("Invalid &", chars.index);
            }
        }
        // lexing |
        else if (chars.get(0) == '|') {
            op += chars.get(0);
            // check that the char after is also a &
            if (!chars.has(1)) {
                throw new ParseException("Invalid |", chars.index);
            }
            chars.advance();
            if (chars.get(0) == '|') {
                op += chars.get(0);
                return new Token(Token.Type.OPERATOR, op, startIndex);
            }
            else {
                throw new ParseException("Invalid |", chars.index);
            }
        }
        // lexing !=
        else if (chars.get(0) == '!') {
            op += chars.get(0);
            // check that the char after is also a &
            if (!chars.has(1)) {
                throw new ParseException("Invalid !=", chars.index);
            }
            chars.advance();
            if (chars.get(0) == '=') {
                op += chars.get(0);
                chars.advance();
                return new Token(Token.Type.OPERATOR, op, startIndex);
            }
            else {
                throw new ParseException("Invalid !=", chars.index);
            }
        }
        // lexing ==
        else if (chars.get(0) == '=') {
            op += chars.get(0);
            // check that the char after is also a =
            if ((!chars.has(1)) || (chars.get(1) == ' ')) {
                // lex it as a single =
                chars.advance();
                return new Token(Token.Type.OPERATOR, op, startIndex);
            }
            chars.advance();
            if (chars.get(0) == '=') {
                op += chars.get(0);
                chars.advance();
                return new Token(Token.Type.OPERATOR, op, startIndex);
            }
            else {
                throw new ParseException("Invalid ==", chars.index);
            }
        }
        // check whitespace
        else if (Character.isWhitespace(chars.get(0))) {
            throw new ParseException("White Space is not a valid operator", chars.index);
        }
        // lexing all other single operators
        else {
            op += chars.get(0);
            chars.advance();
            return new Token(Token.Type.OPERATOR, op, startIndex);
        }

    }

    /**
     * Returns true if the next sequence of characters match the given patterns,
     * which should be a regex. For example, {@code peek("a", "b", "c")} would
     * return true if the next characters are {@code 'a', 'b', 'c'}.
     */
    public boolean peek(String... patterns) {
        for (int i = 0; i < patterns.length; i++) {
            if (!chars.has(i) || !String.valueOf(chars.get(i)).matches(patterns[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns true in the same way as {@link #peek(String...)}, but also
     * advances the character stream past all matched characters if peek returns
     * true. Hint - it's easiest to have this method simply call peek.
     */
    public boolean match(String... patterns) {
        boolean peak = peek(patterns);
        if (peak) {
            for (int i = 0; i < patterns.length; i++) {
                chars.advance();
            }
        }
        return peak;
    }

    /**
     * A helper class maintaining the input string, current index of the char
     * stream, and the current length of the token being matched.
     *
     * You should rely on peek/match for state management in nearly all cases.
     * The only field you need to access is {@link #index} for any {@link
     * ParseException} which is thrown.
     */
    public static final class CharStream {

        private final String input;
        private int index = 0;
        private int length = 0;

        public CharStream(String input) {
            this.input = input;
        }

        public boolean has(int offset) {
            return index + offset < input.length();
        }

        public char get(int offset) {
            return input.charAt(index + offset);
        }

        public void advance() {
            index++;
            length++;
        }
        public void skip() {
            length = 0;
        }

        public Token emit(Token.Type type) {
            int start = index - length;
            skip();
            return new Token(type, input.substring(start, index), start);
        }

    }

}
