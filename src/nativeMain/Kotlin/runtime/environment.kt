package runtime

import kotlinx.datetime.Clock

fun createGlobalEnv(): Environment {
    val env = Environment()
    env.declareVar("true", MK_BOOL(true), constant = true)
    env.declareVar("false", MK_BOOL(false), constant = true)
    env.declareVar("null", MK_NULL(), constant = true)

    env.declareVar(
        "print",
        MK_NATIVE_FN { args, _ ->
            val output = args.joinToString(" ") { arg ->
                when (arg) {
                    is NumberVal -> {
                        if (arg.value % 1 == 0.0) arg.value.toInt().toString()
                        else arg.value.toString()
                    }
                    is StringVal -> arg.value
                    is BooleanVal -> arg.value.toString()
                    is NullVal -> "null"
                    else -> arg.toString()
                }
            }
            println(output)
            MK_NULL()
        },
        constant = true
    )

    // 3. The "log" Function (Debug Output)
    env.declareVar(
        "log",
        MK_NATIVE_FN { args, _ ->
            println(args.joinToString(" "))
            MK_NULL()
        },
        constant = true
    )


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