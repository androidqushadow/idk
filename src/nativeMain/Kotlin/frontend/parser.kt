package frontend

import kotlin.system.exitProcess

class Parser {
    private var tokens = mutableListOf<Token>()

    private fun notEof(): Boolean {
        return tokens[0].type != TokenType.EOF
    }

    private fun at(): Token {
        return tokens[0]
    }

    private fun eat(): Token {
        return tokens.removeAt(0)
    }

    private fun expect(type: TokenType, err: Any): Token {
        val prev = tokens.removeAt(0)
        if (prev.type != type) {
            println("Parser Error:\n$err\nFound: $prev - Expecting: $type")
            exitProcess(1)
        }
        return prev
    }

    fun produceAST(sourceCode: String): Program {
        tokens = tokenize(sourceCode).toMutableList()
        val body = mutableListOf<Stmt>()

        while (notEof()) {
            body.add(parseStmt())
        }

        return Program(body = body)
    }

    private fun parseStmt(): Stmt {
        return when (at().type) {
            TokenType.Let, TokenType.Const -> parseVarDeclaration()
            TokenType.Fn -> parseFnDeclaration()
            else -> parseExpr()
        }
    }

    private fun parseFnDeclaration(): Stmt {
        eat() // eat fn keyword
        val name = expect(TokenType.Identifier, "Expected function name following fn keyword").value
        val args = parseArgs()

        val params = args.map { arg ->
            if (arg !is Identifier) {
                throw Exception("Inside function declaration expected parameters to be of type string.")
            }
            arg.symbol
        }

        expect(TokenType.OpenBrace, "Expected function body following declaration")
        val body = mutableListOf<Stmt>()

        while (notEof() && at().type != TokenType.CloseBrace) {
            body.add(parseStmt())
        }

        expect(TokenType.CloseBrace, "Closing brace expected inside function declaration")
        return FunctionDeclaration(name = name, parameters = params, body = body)
    }

    private fun parseVarDeclaration(): Stmt {
        val isConstant = eat().type == TokenType.Const
        val identifier = expect(TokenType.Identifier, "Expected identifier name following let | const keywords.").value

        if (at().type == TokenType.Semicolon) {
            eat()
            if (isConstant) throw Exception("Must assign value to constant expression. No value provided.")
            return VarDeclaration(identifier = identifier, constant = false, value = null)
        }

        expect(TokenType.Equals, "Expected equals token following identifier in var declaration.")
        val declaration = VarDeclaration(
            value = parseExpr(),
            identifier = identifier,
            constant = isConstant
        )

        expect(TokenType.Semicolon, "Variable declaration statement must end with semicolon.")
        return declaration
    }

    private fun parseExpr(): Expr = parseAssignmentExpr()

    private fun parseAssignmentExpr(): Expr {
        val left = parseObjectExpr()

        if (at().type == TokenType.Equals) {
            eat()
            val value = parseAssignmentExpr()
            return AssignmentExpr(assigne = left, value = value)
        }

        return left
    }

    private fun parseObjectExpr(): Expr {
        if (at().type != TokenType.OpenBrace) {
            return parseAdditiveExpr()
        }

        eat() // advance past {
        val properties = mutableListOf<Property>()

        while (notEof() && at().type != TokenType.CloseBrace) {
            val key = expect(TokenType.Identifier, "Object literal key expected").value

            // Shorthand { key, } or { key }
            if (at().type == TokenType.Comma) {
                eat()
                properties.add(Property(key = key, value = null))
                continue
            } else if (at().type == TokenType.CloseBrace) {
                properties.add(Property(key = key, value = null))
                continue
            }

            expect(TokenType.Colon, "Missing colon following identifier in ObjectExpr")
            val value = parseExpr()
            properties.add(Property(key = key, value = value))

            if (at().type != TokenType.CloseBrace) {
                expect(TokenType.Comma, "Expected comma or closing bracket following property")
            }
        }

        expect(TokenType.CloseBrace, "Object literal missing closing brace.")
        return ObjectLiteral(properties = properties)
    }

    private fun parseAdditiveExpr(): Expr {
        var left = parseMultiplicativeExpr()

        while (at().value == "+" || at().value == "-") {
            val operator = eat().value
            val right = parseMultiplicativeExpr()
            left = BinaryExpr(left = left, right = right, operator = operator)
        }
        return left
    }

    private fun parseMultiplicativeExpr(): Expr {
        var left = parseCallMemberExpr()

        while (at().value == "/" || at().value == "*" || at().value == "%") {
            val operator = eat().value
            val right = parseCallMemberExpr()
            left = BinaryExpr(left = left, right = right, operator = operator)
        }
        return left
    }

    private fun parseCallMemberExpr(): Expr {
        val member = parseMemberExpr()
        if (at().type == TokenType.OpenParen) {
            return parseCallExpr(member)
        }
        return member
    }

    private fun parseCallExpr(caller: Expr): Expr {
        var callExpr: Expr = CallExpr(caller = caller, args = parseArgs())

        if (at().type == TokenType.OpenParen) {
            callExpr = parseCallExpr(callExpr)
        }
        return callExpr
    }

    private fun parseArgs(): List<Expr> {
        expect(TokenType.OpenParen, "Expected open parenthesis")
        val args = if (at().type == TokenType.CloseParen) listOf() else parseArgumentsList()
        expect(TokenType.CloseParen, "Missing closing parenthesis inside arguments list")
        return args
    }

    private fun parseArgumentsList(): List<Expr> {
        val args = mutableListOf(parseAssignmentExpr())
        while (at().type == TokenType.Comma) {
            eat()
            args.add(parseAssignmentExpr())
        }
        return args
    }

    private fun parseMemberExpr(): Expr {
        var obj = parsePrimaryExpr()

        while (at().type == TokenType.Dot || at().type == TokenType.OpenBracket) {
            val operator = eat()
            val property: Expr
            val computed: Boolean

            if (operator.type == TokenType.Dot) {
                computed = false
                property = parsePrimaryExpr()
                if (property !is Identifier) {
                    throw Exception("Cannot use dot operator without right hand side being an identifier")
                }
            } else {
                computed = true
                property = parseExpr()
                expect(TokenType.CloseBracket, "Missing closing bracket in computed value.")
            }

            obj = MemberExpr(`object` = obj, property = property, computed = computed)
        }
        return obj
    }

    private fun parsePrimaryExpr(): Expr {
        return when (val tk = at().type) {
            TokenType.String -> StringLiteral(value = eat().value)
            TokenType.Identifier -> Identifier(symbol = eat().value)
            TokenType.Number -> NumericLiteral(value = eat().value.toDouble())
            TokenType.OpenParen -> {
                eat()
                val value = parseExpr()
                expect(TokenType.CloseParen, "Expected closing parenthesis.")
                value
            }
            else -> {
                println("Unexpected token found during parsing! ${at()}")
                exitProcess(1)
            }
        }
    }
}