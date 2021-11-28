package plc.project;

import java.util.ArrayList;
import java.util.List;

/**
 * The lexer works through three main functions:
 * <p>
 * - {@link #lex()}, which repeatedly calls lexToken() and skips whitespace
 * - {@link #lexToken()}, which lexes the next token
 * - {@link CharStream}, which manages the state of the lexer and literals
 * <p>
 * If the lexer fails to parse something (such as an unterminated string) you
 * should throw a {@link ParseException} with an index at the character which is
 * invalid.
 * <p>
 * The {@link #peek(String...)} and {@link #match(String...)} functions are * helpers you need to use, they will make the implementation a lot easier.
 */
public final class Lexer {

    private final CharStream chars;
    int current_index;

    public Lexer(String input) {
        chars = new CharStream(input);
    }

    private static final String Operators = new String(new char[]{'+', '=', '-', '/', '*', '>', '<', '(', ')', ';'});


    List<Token> token_list = new ArrayList<Token>();


    /**
     * Repeatedly lexes the input using {@link #lexToken()}, also skipping over
     * whitespace where appropriate.
     */
    public List<Token> lex() {
        while (chars.has(0)) {
            char c = chars.get(0);
            current_index = chars.index;

            if (Character.isWhitespace(c)) {
                chars.advance();
                continue;
            }
            token_list.add(lexToken());
            chars.advance();
        }
        return token_list;
    }

    /**
     * This method determines the type of the next token, delegating to the
     * appropriate lex method. As such, it is best for this method to not change
     * the state of the char stream (thus, use peek not match).
     * <p>
     * The next character should start a valid token since whitespace is handled
     * by {@link #lex()}
     */
    public Token lexToken() {
        char c = chars.get(0);
        char store = c;
        boolean nextIsDigit = peek("-", "[0-9]");


        if (store == '\"') {
            return lexString();
        } else if (store == '\'') {
            return lexCharacter();
        } else if ((store == '-' && nextIsDigit) || Character.isDigit(store)) {
            return lexNumber();
        } else if (Operators.indexOf(store) >= 0) {
            return lexOperator();
        }
        
        if (Character.isLetter(store)) {
            return lexIdentifier();
        } else throw new ParseException("Invalid Token", current_index);
    }

    public Token lexIdentifier() {
        char c = chars.get(0);
        String tmp = String.valueOf(c);

        while (chars.has(1)) {
            if (peek(String.valueOf(c), "[a-zA-Z0-9]")) {
                chars.advance();
                c = chars.get(0);

                if ((!Character.isLetter(c) && !Character.isDigit(c))) {
                    throw new ParseException("Invalid character period parsing number", current_index);
                }
                tmp += c;
            } else break;
        }
        return new Token(Token.Type.IDENTIFIER, tmp, current_index);
    }

    public Token lexNumber() {

        char c = chars.get(0);
        boolean isDouble = false;
        String digits = "";
        digits += c;

        if (c == '-') {
            chars.advance();
            c = chars.get(0);
            digits += c;
        } else if (c == '0') {
            chars.advance();
            c = chars.get(0);
            if (c == '0') throw new ParseException("Leading 0", current_index);
        }

        while (chars.has(1)) {
            if (peek(String.valueOf(c), "[0-9\\.]")) {
                chars.advance();
                c = chars.get(0);

                if (c == '.') {
                    if (isDouble) throw new ParseException("Invalid character period parsing number", current_index);
                    else isDouble = true;
                }
                digits += c;
            } else break;
        }
        if (digits.endsWith(".")) {
            throw new ParseException("Trailing digit parsing number", current_index);
        }

        if (isDouble) return new Token(Token.Type.DECIMAL, digits, current_index);
        else return new Token(Token.Type.INTEGER, digits, current_index);
    }

    public Token lexCharacter() {
        String result = "";
        char tmp = chars.get(0);

        if (!chars.has(1)) {
            throw new ParseException("Unterminated", current_index);
        }

        result += tmp;
        chars.advance();
        char tmp2 = chars.get(0);

        if (tmp2 == '\'') {
            result += tmp2;
            throw new ParseException("Empty Character", current_index);
        }

        result += tmp2;
        if (tmp2 == '\\') {
            if (chars.has(1)) {
                chars.advance();
                tmp2 = chars.get(0);
                result += tmp2;

            } else {
                throw new ParseException("Unterminated", current_index);
            }
        }

        if (!chars.has(1)) {
            throw new ParseException("Unterminated", current_index);
        }


        chars.advance();
        char tmp3 = chars.get(0);
        if (tmp3 != '\'') {
            throw new ParseException("Unterminated", current_index);
        }

        result += tmp3;

        return new Token(Token.Type.CHARACTER, result, current_index);
    }

    public Token lexString() {
        char c = chars.get(0);
        String tmp = String.valueOf(c);
        boolean terminated = false;

        while (chars.has(1)) {
            chars.advance();
            c = chars.get(0);

            if (c == '"') {
                tmp += c;
                terminated = true;
                break;
            }

            if (c == '\\') {
                if (!peek("\\\\", "[b|r|n|t]")) {
                    throw new ParseException("Invalid escape", current_index);
                }
            }
            tmp += c;
        }

        if (!terminated) {
            throw new ParseException("String not terminated", chars.index + 1);
        }

        return new Token(Token.Type.STRING, tmp, current_index);

    }

    public void lexEscape() {
        /*
        char c = chars.get(0);
        String tmp = String.valueOf(c);
        boolean terminated = false;

        while (chars.has(1)) {
            chars.advance();
            c = chars.get(0);

            if (c == '"') {
                tmp += c;
                terminated = true;
                break;
            }

            if(c == '\\') {
                if(!peek("\\\\","[b|r|n|t]")){
                    continue;
                }
            }
            tmp += c;
        }

        if (!terminated) {
            throw new ParseException("String not terminated", chars.index+1);
        }*/

    }

    public Token lexOperator() {
        String word = "";
        word += chars.get(0);

        if ((chars.get(0) == '!' || chars.get(0) == '=' || chars.get(0) == '<' || chars.get(0) == '>') && (chars.get(1) == '=')) {
            chars.advance();
            word += chars.get(0);

        } else if ((chars.get(0) == '&') && (chars.get(1) == '&')) {
            chars.advance();
            word += chars.get(0);
        } else if ((chars.get(0) == '|') && (chars.get(1) == '|')) {
            chars.advance();
            word += chars.get(0);
        } else throw new ParseException("Not an operator", current_index);

        return new Token(Token.Type.OPERATOR, word, chars.index + 1 - word.length());
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
     * <p>
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
