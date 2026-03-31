package runtime.eval

import frontend.FunctionDeclaration
import frontend.Program
import frontend.VarDeclaration
import runtime.Environment
import runtime.FunctionValue
import runtime.MK_NULL
import runtime.RuntimeVal
import runtime.evaluate

/**
 * Evaluates a full program by iterating through every statement.
 * Returns the result of the last evaluated statement.
 */
fun evalProgram(program: Program, env: Environment): RuntimeVal {
    var lastEvaluated: RuntimeVal = MK_NULL()
    for (statement in program.body) {
        lastEvaluated = evaluate(statement, env)
    }
    return lastEvaluated
}

/**
 * Handles variable declarations (let/const).
 * If no value is provided, it defaults to MK_NULL.
 */
fun evalVarDeclaration(
    declaration: VarDeclaration,
    env: Environment
): RuntimeVal {
    val value = declaration.value?.let { evaluate(it, env) } ?: MK_NULL()

    return env.declareVar(
        varname = declaration.identifier,
        value = value,
        constant = declaration.constant
    )
}

/**
 * Handles function declarations by storing the function metadata
 * and its closure (the environment where it was defined).
 */
fun evalFunctionDeclaration(
    declaration: FunctionDeclaration,
    env: Environment
): RuntimeVal {
    // Create the function value.
    // In Kotlin, we just use the constructor of the data class.
    val fn = FunctionValue(
        name = declaration.name,
        parameters = declaration.parameters,
        declarationEnv = env,
        body = declaration.body
    )

    // Functions are generally treated as constants (true) in this setup
    return env.declareVar(declaration.name, fn, constant = true)
}