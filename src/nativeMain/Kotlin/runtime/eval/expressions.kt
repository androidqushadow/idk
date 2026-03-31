package runtime.eval

import frontend.*
import runtime.*

/**
 * Handles numeric binary operations (+, -, *, /, %).
 */
private fun evalNumericBinaryExpr(
    lhs: NumberVal,
    rhs: NumberVal,
    operator: String
): NumberVal {
    val result = when (operator) {
        "+" -> lhs.value + rhs.value
        "-" -> lhs.value - rhs.value
        "*" -> lhs.value * rhs.value
        "/" -> {
            // TODO: Division by zero checks
            lhs.value / rhs.value
        }
        "%" -> lhs.value % rhs.value
        else -> throw Exception("Unknown operator: $operator")
    }

    return NumberVal(value = result)
}

/**
 * Evaluates expressions following the binary operation type.
 */
fun evalBinaryExpr(binop: BinaryExpr, env: Environment): RuntimeVal {
    val lhs = evaluate(binop.left, env)
    val rhs = evaluate(binop.right, env)

    if (lhs is NumberVal && rhs is NumberVal) {
        return evalNumericBinaryExpr(lhs, rhs, binop.operator)
    }

    // ADD THIS: Support for "String" + "Anything"
    if (binop.operator == "+") {
        if (lhs is StringVal || rhs is StringVal) {
            val leftStr = if (lhs is StringVal) lhs.value else lhs.toString()
            val rightStr = if (rhs is StringVal) rhs.value else rhs.toString()
            return StringVal(leftStr + rightStr)
        }
    }

    return MK_NULL()
}

fun evalIdentifier(ident: Identifier, env: Environment): RuntimeVal {
    return env.lookupVar(ident.symbol)
}

fun evalAssignment(node: AssignmentExpr, env: Environment): RuntimeVal {
    if (node.assigne !is Identifier) {
        throw Exception("Invalid LHS inside assignment expr: ${node.assigne}")
    }

    val varname = (node.assigne as Identifier).symbol
    return env.assignVar(varname, evaluate(node.value, env))
}

fun evalObjectExpr(obj: ObjectLiteral, env: Environment): RuntimeVal {
    val properties = mutableMapOf<String, RuntimeVal>()

    for (prop in obj.properties) {
        val value = prop.value
        val runtimeVal = if (value == null) {
            env.lookupVar(prop.key)
        } else {
            evaluate(value, env)
        }

        properties[prop.key] = runtimeVal
    }

    return ObjectVal(properties = properties)
}

fun evalCallExpr(expr: CallExpr, env: Environment): RuntimeVal {
    val args = expr.args.map { arg -> evaluate(arg, env) }
    val fn = evaluate(expr.caller, env)

    return when (fn) {
        is NativeFnValue -> {
            fn.call(args, env)
        }
        is FunctionValue -> {
            // Create a new scope based on where the function was defined (closure)
            val scope = Environment(parent = fn.declarationEnv)

            // Verify arity and map parameters to arguments
            for (i in fn.parameters.indices) {
                // TODO: Arity check (args.size vs fn.parameters.size)
                val varname = fn.parameters[i]
                scope.declareVar(varname, args[i], constant = false)
            }

            var lastResult: RuntimeVal = MK_NULL()
            // Evaluate the function body line by line
            for (stmt in fn.body) {
                lastResult = evaluate(stmt, scope)
            }

            lastResult
        }
        else -> throw Exception("Cannot call value that is not a function: $fn")
    }
}