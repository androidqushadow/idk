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
    // This ensures every value has a "Dirty" string representation
    abstract override fun toString(): String
}

// --- VALUE DEFINITIONS ---

data class NullVal(val value: Nothing? = null) : RuntimeVal() {
    override val type = ValueType.Null
    override fun toString() = "NullVal"
}

data class BooleanVal(val value: Boolean) : RuntimeVal() {
    override val type = ValueType.Boolean
    override fun toString() = "BooleanVal(value=$value)"
}

data class NumberVal(val value: Double) : RuntimeVal() {
    override val type = ValueType.Number
    override fun toString() = "NumberVal(value=$value)"
}

data class ObjectVal(val properties: MutableMap<String, RuntimeVal>) : RuntimeVal() {
    override val type = ValueType.Object
    override fun toString(): String {
        val props = properties.map { "${it.key}=${it.value}" }.joinToString(", ")
        return "ObjectVal(properties={$props})"
    }
}

typealias FunctionCall = (args: List<RuntimeVal>, env: Environment) -> RuntimeVal

data class NativeFnValue(val call: FunctionCall) : RuntimeVal() {
    override val type = ValueType.NativeFn
    override fun toString() = "NativeFnValue(built-in)"
}

data class StringVal(val value: String) : RuntimeVal() {
    override val type = ValueType.String
    override fun toString() = "StringVal(value=\"$value\")"
}

data class FunctionValue(
    val name: String,
    val parameters: List<String>,
    val declarationEnv: Environment,
    val body: List<Stmt>
) : RuntimeVal() {
    override val type = ValueType.Function
    override fun toString() = "FunctionValue(name=$name, params=$parameters)"
}

// --- HELPER FACTORY FUNCTIONS ---

fun MK_NULL() = NullVal()
fun MK_BOOL(b: Boolean = true) = BooleanVal(b)
fun MK_NUMBER(n: Double = 0.0) = NumberVal(n)
fun MK_NATIVE_FN(call: FunctionCall) = NativeFnValue(call)