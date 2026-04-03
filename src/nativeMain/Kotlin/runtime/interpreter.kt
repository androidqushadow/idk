package runtime

import frontend.*
import runtime.eval.*
import kotlin.system.exitProcess

/**
 * The main evaluation function.
 * Dispatches AST nodes to their specific evaluation functions.
 */
fun evaluate(astNode: Stmt, env: Environment): RuntimeVal {
    return when (astNode) {
        // --- LITERALS ---
        is NumericLiteral -> {
            NumberVal(value = astNode.value)
        }

        is StringLiteral -> {
            StringVal(value = astNode.value)
        }

        is Identifier -> {
            evalIdentifier(astNode, env)
        }

        is ObjectLiteral -> {
            evalObjectExpr(astNode, env)
        }

        // --- EXPRESSIONS ---
        is CallExpr -> {
            evalCallExpr(astNode, env)
        }

        is AssignmentExpr -> {
            evalAssignment(astNode, env)
        }

        is BinaryExpr -> {
            evalBinaryExpr(astNode, env)
        }

        is UnaryExpr -> {
            evalUnaryExpr(astNode, env)
        }

        // --- STATEMENTS ---
        is Program -> {
            evalProgram(astNode, env)
        }

        is VarDeclaration -> {
            evalVarDeclaration(astNode, env)
        }

        is FunctionDeclaration -> {
            evalFunctionDeclaration(astNode, env)
        }

        is ReturnStatement -> {
            evalReturnStatement(astNode, env)
        }

        is MemberExpr -> evalMemberExpr(astNode, env)

        // --- NEW LOGIC STATEMENTS ---
        is IfStatement -> {
            evalIfStatement(astNode, env)
        }

        is BlockStatement -> {
            evalBlockStatement(astNode, env)
        }

        // --- ERROR HANDLING ---
        else -> {
            println("Runtime Error: This AST Node has not yet been setup for interpretation.")
            println("Node Kind: ${astNode::class.simpleName}")
            println("Full Node: $astNode")
            exitProcess(1)
        }
    }
}