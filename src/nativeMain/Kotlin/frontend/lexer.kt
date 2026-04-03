package frontend

import kotlin.system.exitProcess

enum class TokenType {
    Number, Identifier, String,
    Let, Const, Fn, Var, Return, If, Else,
    BinaryOperator, Equals, Comma, Dot, Colon, Semicolon,
    And, Or, Not,       // &&, ||, !
    OpenParen, CloseParen, OpenBrace, CloseBrace, OpenBracket, CloseBracket,
    EOF
}

val KEYWORDS = mapOf(
    "let" to TokenType.Let,
    "const" to TokenType.Const,
    "fn" to TokenType.Fn,
    "var" to TokenType.Var,
    "return" to TokenType.Return,
    "if" to TokenType.If,
    "else" to TokenType.Else
)

data class Token(val value: String, val type: TokenType, val line: Int)

fun isalpha(src: Char): Boolean = src.uppercaseChar() != src.lowercaseChar() || src == '_'
fun isskippable(c: Char): Boolean = c in " \n\t\r"
fun isint(c: Char): Boolean = c in '0'..'9'

fun tokenize(sourceCode: String): List<Token> {
    val tokens = mutableListOf<Token>()
    val src = sourceCode.toCharArray().toMutableList()
    var line = 1

    // Helper to look at the current char without removing it
    fun at(): Char = if (src.isNotEmpty()) src[0] else '\u0000'
    // Helper to look at the next char without removing it
    fun peek(): Char = if (src.size > 1) src[1] else '\u0000'

    while (src.isNotEmpty()) {
        val char = src[0]

        if (char == '\n') {
            line++
            src.removeAt(0)
            continue
        }

        when (char) {
            '(' -> tokens.add(Token(src.removeAt(0).toString(), TokenType.OpenParen, line))
            ')' -> tokens.add(Token(src.removeAt(0).toString(), TokenType.CloseParen, line))
            '{' -> tokens.add(Token(src.removeAt(0).toString(), TokenType.OpenBrace, line))
            '}' -> tokens.add(Token(src.removeAt(0).toString(), TokenType.CloseBrace, line))
            '[' -> tokens.add(Token(src.removeAt(0).toString(), TokenType.OpenBracket, line))
            ']' -> tokens.add(Token(src.removeAt(0).toString(), TokenType.CloseBracket, line))
            ';' -> tokens.add(Token(src.removeAt(0).toString(), TokenType.Semicolon, line))
            ':' -> tokens.add(Token(src.removeAt(0).toString(), TokenType.Colon, line))
            ',' -> tokens.add(Token(src.removeAt(0).toString(), TokenType.Comma, line))

            // Handle Dot carefully: It could be a property access (obj.prop)
            // or a leading decimal (.5).
            '.' -> {
                if (isint(peek())) {
                    // It's a number like .5! Let the number logic handle it.
                    // We fall through to the 'else' block by not adding a token here.
                } else {
                    tokens.add(Token(src.removeAt(0).toString(), TokenType.Dot, line))
                    continue
                }
            }

            '+', '-', '*', '/', '%' -> tokens.add(Token(src.removeAt(0).toString(), TokenType.BinaryOperator, line))

            '=' -> {
                if (peek() == '=') {
                    src.removeAt(0); src.removeAt(0)
                    tokens.add(Token("==", TokenType.BinaryOperator, line))
                } else {
                    tokens.add(Token(src.removeAt(0).toString(), TokenType.Equals, line))
                }
            }
            '!' -> {
                if (peek() == '=') {
                    src.removeAt(0); src.removeAt(0)
                    tokens.add(Token("!=", TokenType.BinaryOperator, line))
                } else {
                    tokens.add(Token(src.removeAt(0).toString(), TokenType.Not, line))
                }
            }
            '>', '<' -> {
                val op = src.removeAt(0).toString()
                if (at() == '=') {
                    tokens.add(Token(op + src.removeAt(0), TokenType.BinaryOperator, line))
                } else {
                    tokens.add(Token(op, TokenType.BinaryOperator, line))
                }
            }
            '&' -> {
                if (peek() == '&') {
                    src.removeAt(0); src.removeAt(0)
                    tokens.add(Token("&&", TokenType.And, line))
                } else {
                    println("Error on line $line: Single '&' is not supported. Use '&&'"); exitProcess(1)
                }
            }
            '|' -> {
                if (peek() == '|') {
                    src.removeAt(0); src.removeAt(0)
                    tokens.add(Token("||", TokenType.Or, line))
                } else {
                    println("Error on line $line: Single '|' is not supported. Use '||'"); exitProcess(1)
                }
            }

            else -> {
                if (char == '"') {
                    src.removeAt(0)
                    var str = ""
                    while (src.isNotEmpty() && src[0] != '"') str += src.removeAt(0)
                    if (src.isNotEmpty()) src.removeAt(0)
                    tokens.add(Token(str, TokenType.String, line))
                }
                // Updated Number Logic: Handles 10, 10.5, and .5
                else if (isint(char) || char == '.') {
                    var num = ""
                    var dotCount = 0
                    while (src.isNotEmpty() && (isint(src[0]) || src[0] == '.')) {
                        if (src[0] == '.') {
                            dotCount++
                            if (dotCount > 1) break // Stop if we see 1.2.3
                        }
                        num += src.removeAt(0)
                    }
                    tokens.add(Token(num, TokenType.Number, line))
                }
                else if (isalpha(char)) {
                    var ident = ""
                    while (src.isNotEmpty() && (isalpha(src[0]) || isint(src[0]))) ident += src.removeAt(0)
                    val type = KEYWORDS[ident] ?: TokenType.Identifier
                    tokens.add(Token(ident, type, line))
                } else if (isskippable(char)) {
                    src.removeAt(0)
                } else {
                    println("Error on line $line: Unrecognized character: $char")
                    exitProcess(1)
                }
            }
        }
    }
    tokens.add(Token("EndOfFile", TokenType.EOF, line))
    return tokens
}