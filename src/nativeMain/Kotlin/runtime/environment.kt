package runtime

import kotlinx.datetime.Clock
import frontend.*
import runtime.eval.*

/**
 * Global helper to turn a RuntimeVal into a user-friendly string.
 */
fun stringify(arg: RuntimeVal): String {
    return when (arg) {
        is NumberVal -> {
            val v = arg.value
            if (v == v.toInt().toDouble()) v.toInt().toString() else v.toString()
        }
        is StringVal -> arg.value
        is BooleanVal -> arg.value.toString()
        is NullVal -> "null"
        is ObjectVal -> {
            val builder = StringBuilder("{ ")
            val entries = arg.properties.entries.toList()
            for (i in entries.indices) {
                val entry = entries[i]
                builder.append("${entry.key}: ${stringify(entry.value)}")
                if (i < entries.size - 1) builder.append(", ")
            }
            builder.append(" }")
            builder.toString()
        }
        is FunctionValue -> "[Function: ${arg.name}]"
        is NativeFnValue -> "[Native Function]"
        else -> arg.toString()
    }
}

fun createGlobalEnv(): Environment {
    val env = Environment()

    // Core Constants
    env.declareVar("true", MK_BOOL(true), constant = true)
    env.declareVar("false", MK_BOOL(false), constant = true)
    env.declareVar("null", MK_NULL(), constant = true)

    // 1. The "print" Function (Clean View)
    env.declareVar(
        "print",
        MK_NATIVE_FN { args, _ ->
            val output = args.joinToString(" ") { arg -> stringify(arg) }
            println(output)
            MK_NULL()
        },
        constant = true
    )

    // 2. The "log" Function (Dirty/Internal View)
    env.declareVar(
        "log",
        MK_NATIVE_FN { args, _ ->
            // Uses raw .toString() from your Values.kt data classes
            println(args.joinToString(" "))
            MK_NULL()
        },
        constant = true
    )

    // 3. Time Utility
    env.declareVar(
        "gettime",
        MK_NATIVE_FN { _, _ ->
            MK_NUMBER(Clock.System.now().toEpochMilliseconds().toDouble())
        },
        constant = true
    )

    return env
}

class Environment(private val parent: Environment? = null) {
    private val variables = mutableMapOf<String, RuntimeVal>()
    private val constants = hashSetOf<String>()

    fun declareVar(varname: String, value: RuntimeVal, constant: Boolean): RuntimeVal {
        if (variables.containsKey(varname)) {
            throw Exception("Cannot declare variable '$varname' as it is already defined.")
        }
        variables[varname] = value
        if (constant) constants.add(varname)
        return value
    }

    fun assignVar(varname: String, value: RuntimeVal): RuntimeVal {
        val env = resolve(varname)
        if (env.constants.contains(varname)) {
            throw Exception("Cannot reassign to '$varname' as it is constant.")
        }
        env.variables[varname] = value
        return value
    }

    fun lookupVar(varname: String): RuntimeVal {
        val env = resolve(varname)
        return env.variables[varname] ?: MK_NULL()
    }

    fun resolve(varname: String): Environment {
        if (variables.containsKey(varname)) return this
        if (parent == null) throw Exception("Cannot resolve '$varname'.")
        return parent.resolve(varname)
    }
}