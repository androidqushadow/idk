package runtime.eval

import frontend.*
import runtime.*
import kotlin.system.exitProcess

/**
 * Custom exception used to break out of function execution.
 */
class ReturnValue(val value: RuntimeVal) : Exception()

fun evalProgram(program: Program, env: Environment): RuntimeVal {
    var lastEvaluated: RuntimeVal = MK_NULL()
    for (statement in program.body) {
        lastEvaluated = evaluate(statement, env)
    }
    return lastEvaluated
}

fun evalVarDeclaration(declaration: VarDeclaration, env: Environment): RuntimeVal {
    val value = if (declaration.value != null) evaluate(declaration.value, env) else MK_NULL()
    return env.declareVar(declaration.identifier, value, declaration.constant)
}

fun evalFunctionDeclaration(declaration: FunctionDeclaration, env: Environment): RuntimeVal {
    val fn = FunctionValue(
        name = declaration.name,
        parameters = declaration.parameters,
        declarationEnv = env,
        body = declaration.body
    )
    return env.declareVar(declaration.name, fn, constant = true)
}

fun evalBlockStatement(block: BlockStatement, env: Environment): RuntimeVal {
    val scope = Environment(parent = env)
    var lastEvaluated: RuntimeVal = MK_NULL()
    for (statement in block.body) {
        lastEvaluated = evaluate(statement, scope)
    }
    return lastEvaluated
}

fun evalIfStatement(declaration: IfStatement, env: Environment): RuntimeVal {
    val test = evaluate(declaration.test, env)

    // Krisp Truthiness: non-zero numbers and boolean 'true' are truthy
    val isTruthy = when (test) {
        is BooleanVal -> test.value
        is NumberVal -> test.value != 0.0
        is NullVal -> false
        else -> true
    }

    if (isTruthy) {
        // If the body is already a BlockStatement, evaluate it directly
        // Otherwise, execute the list of statements in a new scope
        val scope = Environment(parent = env)
        var lastResult: RuntimeVal = MK_NULL()
        for (stat in declaration.body) {
            lastResult = evaluate(stat, scope)
        }
        return lastResult
    } else if (declaration.alternate != null) {
        // This handles 'else' or 'else if' recursion
        return evaluate(declaration.alternate, env)
    }

    return MK_NULL()
}

fun evalReturnStatement(stmt: ReturnStatement, env: Environment): RuntimeVal {
    val returnValue = if (stmt.value != null) evaluate(stmt.value, env) else MK_NULL()
    throw ReturnValue(returnValue)
}