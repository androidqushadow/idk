package runtime

import frontend.Stmt

/**
 * The types of values available at runtime.
 */
enum class ValueType {
    Null, Number, Boolean, Object, NativeFn, Function, String
}

/**
 * Base interface for all values handled by the interpreter at runtime.
 */
sealed class RuntimeVal {
    abstract val type: ValueType
}

// --- VALUE DEFINITIONS ---

data class NullVal(
    val value: Nothing? = null
) : RuntimeVal() {
    override val type = ValueType.Null
}

data class BooleanVal(
    val value: Boolean
) : RuntimeVal() {
    override val type = ValueType.Boolean
}

data class NumberVal(
    val value: Double
) : RuntimeVal() {
    override val type = ValueType.Number
}

data class ObjectVal(
    val properties: MutableMap<String, RuntimeVal>
) : RuntimeVal() {
    override val type = ValueType.Object
}

// Type alias for the function signature to keep it clean
typealias FunctionCall = (args: List<RuntimeVal>, env: Environment) -> RuntimeVal

data class NativeFnValue(
    val call: FunctionCall
) : RuntimeVal() {
    override val type = ValueType.NativeFn
}

data class StringVal(
    val value: String
) : RuntimeVal() {
    override val type = ValueType.String // Add 'String' to ValueType enum too!
}

data class FunctionValue(
    val name: String,
    val parameters: List<String>,
    val declarationEnv: Environment,
    val body: List<Stmt>
) : RuntimeVal() {
    override val type = ValueType.Function
}

// --- HELPER FACTORY FUNCTIONS (MK_ methods) ---

fun MK_NULL() = NullVal()

fun MK_BOOL(b: Boolean = true) = BooleanVal(b)

fun MK_NUMBER(n: Double = 0.0) = NumberVal(n)

fun MK_NATIVE_FN(call: FunctionCall) = NativeFnValue(call)