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
            // Using the MK_NUMBER helper (which I assume is in your values.kt)
            NumberVal(value = astNode.value)
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
        is StringLiteral -> StringVal(value = astNode.value)

        // Handle cases that aren't specifically matched (if any)
        else -> {
            println("This AST Node has not yet been setup for interpretation.\n$astNode")
            exitProcess(0)
        }
    }
}