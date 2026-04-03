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
            if (rhs.value == 0.0) throw Exception("Division by zero")
            lhs.value / rhs.value
        }
        "%" -> lhs.value % rhs.value
        else -> throw Exception("Unknown numeric operator: $operator")
    }

    return NumberVal(value = result)
}

/**
 * Evaluates expressions following the binary operation type.
 */
fun evalBinaryExpr(binop: BinaryExpr, env: Environment): RuntimeVal {
    val lhs = evaluate(binop.left, env)

    // 1. Logical Short-circuiting (&& and ||)
    if (binop.operator == "&&") {
        val leftBool = (lhs as? BooleanVal)?.value ?: throw Exception("LHS of && must be boolean")
        if (!leftBool) return BooleanVal(false)
        val rhs = evaluate(binop.right, env)
        return BooleanVal((rhs as? BooleanVal)?.value ?: throw Exception("RHS of && must be boolean"))
    }

    if (binop.operator == "||") {
        val leftBool = (lhs as? BooleanVal)?.value ?: throw Exception("LHS of || must be boolean")
        if (leftBool) return BooleanVal(true)
        val rhs = evaluate(binop.right, env)
        return BooleanVal((rhs as? BooleanVal)?.value ?: throw Exception("RHS of || must be boolean"))
    }

    // Evaluate RHS for all other non-short-circuiting operations
    val rhs = evaluate(binop.right, env)

    // 2. Comparison Logic (==, !=, >, <, >=, <=)
    val comparisonOps = listOf("==", "!=", ">", "<", ">=", "<=")
    if (comparisonOps.contains(binop.operator)) {
        if (lhs is NumberVal && rhs is NumberVal) {
            val l = lhs.value
            val r = rhs.value
            val result = when(binop.operator) {
                "==" -> l == r
                "!=" -> l != r
                ">" -> l > r
                "<" -> l < r
                ">=" -> l >= r
                "<=" -> l <= r
                else -> false
            }
            return BooleanVal(result)
        }

        // Support equality for other types (Strings, Null, Booleans)
        if (binop.operator == "==") return BooleanVal(lhs == rhs)
        if (binop.operator == "!=") return BooleanVal(lhs != rhs)
    }

    // 3. Numeric Math
    if (lhs is NumberVal && rhs is NumberVal) {
        return evalNumericBinaryExpr(lhs, rhs, binop.operator)
    }

    // 4. String Concatenation
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
    if (node.assigne !is Identifier && node.assigne !is MemberExpr) {
        throw Exception("Invalid LHS inside assignment expr")
    }

    val value = evaluate(node.value, env)

    // Case 1: Simple Variable (x = 10)
    if (node.assigne is Identifier) {
        return env.assignVar(node.assigne.symbol, value)
    }

    // Case 2: Member Access (obj.prop = 10)
    if (node.assigne is MemberExpr) {
        // 1. Evaluate the object part (e.g., evaluate 'obj.inner')
        val obj = evaluate(node.assigne.`object`, env)

        if (obj !is ObjectVal) {
            throw Exception("Cannot assign to property of non-object")
        }

        // 2. Identify the property key
        val propertyName = if (node.assigne.computed) {
            val evalProp = evaluate(node.assigne.property, env)
            if (evalProp !is StringVal) throw Exception("Computed property must be string")
            evalProp.value
        } else {
            (node.assigne.property as Identifier).symbol
        }

        // 3. Update the value in the object map
        obj.properties[propertyName] = value
        return value
    }

    return value
}

fun evalObjectExpr(obj: ObjectLiteral, env: Environment): RuntimeVal {
    val properties = mutableMapOf<String, RuntimeVal>()

    for (prop in obj.properties) {
        val runtimeVal = if (prop.value == null) {
            env.lookupVar(prop.key)
        } else {
            evaluate(prop.value, env)
        }
        properties[prop.key] = runtimeVal
    }

    return ObjectVal(properties = properties)
}

fun evalUnaryExpr(expr: UnaryExpr, env: Environment): RuntimeVal {
    val result = evaluate(expr.argument, env)

    return when (expr.operator) {
        "!" -> {
            // Truthiness flip
            val isTruthy = when (result) {
                is BooleanVal -> result.value
                is NumberVal -> result.value != 0.0
                is NullVal -> false
                else -> true
            }
            BooleanVal(!isTruthy)
        }
        else -> throw Exception("Unknown unary operator: ${expr.operator}")
    }
}

fun evalCallExpr(expr: CallExpr, env: Environment): RuntimeVal {
    val args = expr.args.map { arg -> evaluate(arg, env) }
    val fn = evaluate(expr.caller, env)

    return when (fn) {
        is NativeFnValue -> fn.call(args, env)
        is FunctionValue -> {
            val scope = Environment(parent = fn.declarationEnv)

            for (i in fn.parameters.indices) {
                val argValue = if (i < args.size) args[i] else MK_NULL()
                scope.declareVar(fn.parameters[i], argValue, constant = false)
            }

            try {
                var lastResult: RuntimeVal = MK_NULL()
                for (stmt in fn.body) {
                    lastResult = evaluate(stmt, scope)
                }
                return lastResult
            } catch (ret: ReturnValue) {
                return ret.value
            }
        }
        else -> throw Exception("Cannot call value that is not a function: $fn")
    }
}

fun evalMemberExpr(node: MemberExpr, env: Environment): RuntimeVal {
    // 1. Evaluate the object (this could be an Identifier OR another MemberExpr)
    val obj = evaluate(node.`object`, env)

    if (obj !is ObjectVal) {
        throw Exception("[Runtime Error]: Cannot access property of a non-object. Found: ${obj.type}")
    }

    val propertyName: String
    if (node.computed) {
        // Handle obj[expr]
        val evalProp = evaluate(node.property, env)
        if (evalProp !is StringVal) throw Exception("Computed property must be a string")
        propertyName = evalProp.value
    } else {
        // Handle obj.property
        // CRITICAL: Ensure we are pulling the string name from the Identifier node
        val propNode = node.property
        if (propNode is Identifier) {
            propertyName = propNode.symbol
        } else {
            // This is likely where your error is triggering
            throw Exception("[Runtime Error]: Dot operator requires identifier")
        }
    }

    return obj.properties[propertyName] ?: NullVal()
}