package frontend

import kotlin.system.exitProcess

class Parser {
    private var tokens = mutableListOf<Token>()

    private fun notEof(): Boolean = tokens.isNotEmpty() && tokens[0].type != TokenType.EOF
    private fun at(): Token = tokens[0]
    private fun eat(): Token = tokens.removeAt(0)

    private fun expect(type: TokenType, err: Any): Token {
        if (tokens.isEmpty()) {
            println("Parser Error: Unexpected end of input. Expected $type")
            exitProcess(1)
        }
        val prev = tokens.removeAt(0)
        if (prev.type != type) {
            println("--- Parser Error ---")
            println("Line: ${prev.line}")
            println("Message: $err")
            println("Found: '${prev.value}' (Type: ${prev.type})")
            println("Expected: $type")
            println("--------------------")
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
        val stmt = when (at().type) {
            TokenType.Let, TokenType.Const, TokenType.Var -> parseVarDeclaration()
            TokenType.Fn -> parseFnDeclaration()
            TokenType.Return -> parseReturnStatement()
            TokenType.If -> parseIfStatement()
            TokenType.Semicolon -> {
                eat() // Handle standalone semicolons (;;;)
                return parseStmt()
            }
            else -> parseExpr()
        }

        // Global Semicolon Eater:
        // This ensures that after any statement or expression,
        // a trailing semicolon is cleared from the token stream.
        while (notEof() && at().type == TokenType.Semicolon) {
            eat()
        }

        return stmt
    }

    private fun parseBlockStatement(): List<Stmt> {
        expect(TokenType.OpenBrace, "Expected opening brace.")
        val body = mutableListOf<Stmt>()

        while (notEof() && at().type != TokenType.CloseBrace) {
            body.add(parseStmt())
        }

        expect(TokenType.CloseBrace, "Expected closing brace.")
        return body
    }

    private fun parseIfStatement(): Stmt {
        eat() // consume 'if'
        expect(TokenType.OpenParen, "Expected '(' after if")
        val condition = parseExpr()
        expect(TokenType.CloseParen, "Expected ')' after condition")

        val body = if (at().type == TokenType.OpenBrace) {
            parseBlockStatement()
        } else {
            // Support single-line if: if (true) x = 1;
            listOf(parseStmt())
        }

        var alternate: Stmt? = null
        if (at().type == TokenType.Else) {
            eat()
            alternate = when (at().type) {
                TokenType.If -> parseIfStatement()
                TokenType.OpenBrace -> BlockStatement(parseBlockStatement())
                else -> parseStmt()
            }
        }

        return IfStatement(test = condition, body = body, alternate = alternate)
    }

    private fun parseReturnStatement(): Stmt {
        eat() // consume 'return'

        // Handle immediate return;
        if (notEof() && at().type == TokenType.Semicolon) {
            return ReturnStatement(value = null)
        }

        val value = parseExpr()
        return ReturnStatement(value = value)
    }

    private fun parseFnDeclaration(): Stmt {
        eat() // fn
        val name = expect(TokenType.Identifier, "Expected function name").value

        expect(TokenType.OpenParen, "Expected open parenthesis")
        val params = mutableListOf<String>()
        if (at().type != TokenType.CloseParen) {
            params.add(expect(TokenType.Identifier, "Expected parameter name").value)
            while (at().type == TokenType.Comma) {
                eat()
                params.add(expect(TokenType.Identifier, "Expected parameter name").value)
            }
        }
        expect(TokenType.CloseParen, "Missing closing parenthesis")

        val body = parseBlockStatement()
        return FunctionDeclaration(name = name, parameters = params, body = body)
    }

    private fun parseVarDeclaration(): Stmt {
        val keyword = eat().type
        val isConstant = keyword == TokenType.Const
        val identifier = expect(TokenType.Identifier, "Expected identifier name").value

        // Check for uninitialized var/let (const must have value)
        if (!notEof() || at().type == TokenType.Semicolon) {
            if (isConstant) throw Exception("Constants must be initialized")
            return VarDeclaration(identifier = identifier, constant = isConstant, value = null)
        }

        expect(TokenType.Equals, "Expected equals token")
        val value = parseExpr()
        return VarDeclaration(identifier = identifier, value = value, constant = isConstant)
    }

    // --- EXPRESSION PRECEDENCE ---

    private fun parseExpr(): Expr = parseAssignmentExpr()

    private fun parseAssignmentExpr(): Expr {
        val left = parseLogicalOrExpr()

        if (at().type == TokenType.Equals) {
            eat()
            val value = parseAssignmentExpr()
            return AssignmentExpr(assigne = left, value = value)
        }

        return left
    }

    private fun parseLogicalOrExpr(): Expr {
        var left = parseLogicalAndExpr()
        while (notEof() && at().value == "||") {
            val operator = eat().value
            left = BinaryExpr(left, parseLogicalAndExpr(), operator)
        }
        return left
    }

    private fun parseLogicalAndExpr(): Expr {
        var left = parseEqualityExpr()
        while (notEof() && at().value == "&&") {
            val operator = eat().value
            left = BinaryExpr(left, parseEqualityExpr(), operator)
        }
        return left
    }

    private fun parseEqualityExpr(): Expr {
        var left = parseAdditiveExpr()
        val comparisonOps = listOf("==", "!=", ">", "<", ">=", "<=")
        while (notEof() && comparisonOps.contains(at().value)) {
            val operator = eat().value
            left = BinaryExpr(left, parseAdditiveExpr(), operator)
        }
        return left
    }

    private fun parseAdditiveExpr(): Expr {
        var left = parseMultiplicativeExpr()
        while (notEof() && (at().value == "+" || at().value == "-")) {
            val operator = eat().value
            left = BinaryExpr(left, parseMultiplicativeExpr(), operator)
        }
        return left
    }

    private fun parseMultiplicativeExpr(): Expr {
        var left = parseCallMemberExpr()
        while (notEof() && (at().value == "/" || at().value == "*" || at().value == "%")) {
            val operator = eat().value
            left = BinaryExpr(left, parseCallMemberExpr(), operator)
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
        expect(TokenType.CloseParen, "Missing closing parenthesis")
        return args
    }

    private fun parseArgumentsList(): List<Expr> {
        val args = mutableListOf(parseExpr())
        while (at().type == TokenType.Comma) {
            eat()
            args.add(parseExpr())
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
                val token = expect(TokenType.Identifier, "Dot operator requires identifier")
                property = Identifier(token.value)
            } else {
                computed = true
                property = parseExpr()
                expect(TokenType.CloseBracket, "Missing closing bracket")
            }
            obj = MemberExpr(`object` = obj, property = property, computed = computed)
        }
        return obj
    }

    private fun parseObjectExpr(): Expr {
        eat() // {
        val properties = mutableListOf<Property>()
        while (notEof() && at().type != TokenType.CloseBrace) {
            val key = expect(TokenType.Identifier, "Object key expected").value

            if (at().type == TokenType.Comma || at().type == TokenType.CloseBrace) {
                if (at().type == TokenType.Comma) eat()
                properties.add(Property(key, null))
                continue
            }

            expect(TokenType.Colon, "Missing colon after object key")
            val value = parseExpr()
            properties.add(Property(key, value))

            if (at().type != TokenType.CloseBrace) {
                expect(TokenType.Comma, "Expected comma between object properties")
            }
        }
        expect(TokenType.CloseBrace, "Object missing closing brace.")
        return ObjectLiteral(properties)
    }

    private fun parsePrimaryExpr(): Expr {
        return when (at().type) {
            TokenType.Not -> {
                val op = eat().value
                UnaryExpr(op, parsePrimaryExpr())
            }
            TokenType.String -> StringLiteral(eat().value)
            TokenType.Identifier -> Identifier(eat().value)
            TokenType.Number -> NumericLiteral(eat().value.toDouble())
            TokenType.OpenBrace -> parseObjectExpr()
            TokenType.OpenParen -> {
                eat()
                val value = parseExpr()
                expect(TokenType.CloseParen, "Expected closing parenthesis.")
                value
            }
            else -> {
                println("Unexpected token on line ${at().line}: '${at().value}' (Type: ${at().type})")
                exitProcess(1)
            }
        }
    }
}