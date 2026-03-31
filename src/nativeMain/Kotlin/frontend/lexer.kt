package frontend

import kotlin.system.exitProcess

enum class TokenType {
    // Literal Types
    Number,
    Identifier,

    // Keywords
    Let,
    Const,
    Fn,

    String,
    // Grouping & Operators
    BinaryOperator,
    Equals,
    Comma,
    Dot,
    Colon,
    Semicolon,
    OpenParen,    // (
    CloseParen,   // )
    OpenBrace,    // {
    CloseBrace,   // }
    OpenBracket,  // [
    CloseBracket, // ]
    EOF           // Signified the end of file
}

/**
 * Constant lookup for keywords and known identifiers + symbols.
 */
val KEYWORDS = mapOf(
    "let" to TokenType.Let,
    "const" to TokenType.Const,
    "fn" to TokenType.Fn
)

/**
 * Represents a single token from the source-code.
 */
data class Token(
    val value: String,
    val type: TokenType
)

/**
 * Returns whether the character passed in is alphabetic -> [a-zA-Z]
 */
fun isalpha(src: Char): Boolean {
    return src.uppercaseChar() != src.lowercaseChar()
}

/**
 * Returns true if the character is whitespace
 */
fun isskippable(c: Char): Boolean {
    return c == ' ' || c == '\n' || c == '\t' || c == '\r'
}

/**
 * Return whether the character is a valid integer -> [0-9]
 */
fun isint(c: Char): Boolean {
    return c in '0'..'9'
}

fun tokenize(sourceCode: String): List<Token> {
    val tokens = mutableListOf<Token>()
    // Convert string to a MutableList of Chars so we can use removeAt(0) like shift()
    val src = sourceCode.toCharArray().toMutableList()

    while (src.isNotEmpty()) {
        val char = src[0]

        // BEGIN PARSING ONE CHARACTER TOKENS
        when (char) {
            '(' -> tokens.add(Token(src.removeAt(0).toString(), TokenType.OpenParen))
            ')' -> tokens.add(Token(src.removeAt(0).toString(), TokenType.CloseParen))
            '{' -> tokens.add(Token(src.removeAt(0).toString(), TokenType.OpenBrace))
            '}' -> tokens.add(Token(src.removeAt(0).toString(), TokenType.CloseBrace))
            '[' -> tokens.add(Token(src.removeAt(0).toString(), TokenType.OpenBracket))
            ']' -> tokens.add(Token(src.removeAt(0).toString(), TokenType.CloseBracket))
            // HANDLE BINARY OPERATORS
            '+', '-', '*', '/', '%' -> {
                tokens.add(Token(src.removeAt(0).toString(), TokenType.BinaryOperator))
            }
            // Handle Conditional & Assignment Tokens
            '=' -> tokens.add(Token(src.removeAt(0).toString(), TokenType.Equals))
            ';' -> tokens.add(Token(src.removeAt(0).toString(), TokenType.Semicolon))
            ':' -> tokens.add(Token(src.removeAt(0).toString(), TokenType.Colon))
            ',' -> tokens.add(Token(src.removeAt(0).toString(), TokenType.Comma))
            '.' -> tokens.add(Token(src.removeAt(0).toString(), TokenType.Dot))
            else -> {

                if (char == '"') {
                    src.removeAt(0) // Remove opening quote
                    var str = ""
                    while (src.isNotEmpty() && src[0] != '"') {
                        str += src.removeAt(0)
                    }
                    src.removeAt(0) // Remove closing quote
                    tokens.add(Token(str, TokenType.String))
                }
                // HANDLE MULTI-CHARACTER TOKENS
                else if (isint(char)) {
                    var num = ""
                    while (src.isNotEmpty() && isint(src[0])) {
                        num += src.removeAt(0)
                    }
                    tokens.add(Token(num, TokenType.Number))
                } else if (isalpha(char)) {
                    var ident = ""
                    while (src.isNotEmpty() && isalpha(src[0])) {
                        ident += src.removeAt(0)
                    }
                    // CHECK FOR RESERVED KEYWORDS
                    val reserved = KEYWORDS[ident]
                    if (reserved != null) {
                        tokens.add(Token(ident, reserved))
                    } else {
                        tokens.add(Token(ident, TokenType.Identifier))
                    }
                } else if (isskippable(char)) {
                    src.removeAt(0)
                } else {
                    println("Error: Unrecognized character found in source: ${char.code} ($char)")
                    exitProcess(1)
                }
            }
        }
    }

    tokens.add(Token("EndOfFile", TokenType.EOF))
    return tokens
}