package frontend

// We use an enum for the types, though in Kotlin the class
// type itself usually suffices for checks.
enum class NodeType {
    Program, VarDeclaration, FunctionDeclaration,
    AssignmentExpr, MemberExpr, CallExpr,
    Property, ObjectLiteral, NumericLiteral,StringLiteral,
    Identifier, BinaryExpr
}

/**
 * Statements do not result in a value at runtime.
 */
sealed class Stmt {
    abstract val kind: NodeType
}

/**
 * Expressions will result in a value at runtime unlike Statements
 */
sealed class Expr : Stmt()

// --- STATEMENTS ---

data class Program(
    val body: List<Stmt>
) : Stmt() {
    override val kind = NodeType.Program
}

data class VarDeclaration(
    val constant: Boolean,
    val identifier: String,
    val value: Expr? = null // Optional value
) : Stmt() {
    override val kind = NodeType.VarDeclaration
}

data class FunctionDeclaration(
    val parameters: List<String>,
    val name: String,
    val body: List<Stmt>
) : Stmt() {
    override val kind = NodeType.FunctionDeclaration
}

// --- EXPRESSIONS ---

data class AssignmentExpr(
    val assigne: Expr,
    val value: Expr
) : Expr() {
    override val kind = NodeType.AssignmentExpr
}

data class BinaryExpr(
    val left: Expr,
    val right: Expr,
    val operator: String
) : Expr() {
    override val kind = NodeType.BinaryExpr
}

data class CallExpr(
    val args: List<Expr>,
    val caller: Expr
) : Expr() {
    override val kind = NodeType.CallExpr
}

data class MemberExpr(
    val `object`: Expr, // 'object' is a reserved keyword in Kotlin, use backticks
    val property: Expr,
    val computed: Boolean
) : Expr() {
    override val kind = NodeType.MemberExpr
}

// --- LITERALS ---

data class Identifier(
    val symbol: String
) : Expr() {
    override val kind = NodeType.Identifier
}

data class NumericLiteral(
    val value: Double // Kotlin 'Double' is the equivalent of TS 'number'
) : Expr() {
    override val kind = NodeType.NumericLiteral
}

data class Property(
    val key: String,
    val value: Expr? = null
) : Expr() {
    override val kind = NodeType.Property
}

data class ObjectLiteral(
    val properties: List<Property>
) : Expr() {
    override val kind = NodeType.ObjectLiteral
}

data class StringLiteral(
    val value: String
) : Expr() {
    override val kind = NodeType.StringLiteral
}