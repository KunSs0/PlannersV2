package com.gitee.planners.module.action.query

/**
 * 求值表达式入口。
 *
 * 负责加载期语法校验与运行期求值。表达式只读、无副作用；
 * 不支持函数调用，运算全部由运算符与三元表达式表达。
 */
object QueryEvaluator {

    /**
     * 加载期校验表达式语法。
     *
     * @param source 表达式文本。
     * @param label 配置路径，用于错误信息。
     * @throws IllegalArgumentException 语法非法时抛出。
     */
    fun validate(source: String, label: String) {
        validate(source, label, false)
    }

    /**
     * 加载期校验表达式语法。
     *
     * @param source 表达式文本。
     * @param label 配置路径，用于错误信息。
     * @param allowVariables 是否允许裸标识符按上下文变量解析。
     * @throws IllegalArgumentException 语法非法时抛出。
     */
    fun validate(source: String, label: String, allowVariables: Boolean) {
        val tokens = QueryLexer(source, label).tokenize()
        QueryParser(tokens, label, allowVariables).parse()
    }

    /**
     * 运行期求值。
     *
     * @param source 表达式文本。
     * @param scope 查询上下文。
     * @param label 配置路径，用于错误信息。
     * @return 求值结果；缺失时返回 null。
     * @throws IllegalArgumentException 语法非法或类型不匹配时抛出。
     */
    fun evaluate(source: String, scope: QueryScope, label: String): Any? {
        return evaluate(source, scope, label, false)
    }

    /**
     * 运行期求值。
     *
     * @param source 表达式文本。
     * @param scope 查询上下文。
     * @param label 配置路径，用于错误信息。
     * @param allowVariables 是否允许裸标识符按上下文变量解析。
     * @return 求值结果；缺失时返回 null。
     * @throws IllegalArgumentException 语法非法或类型不匹配时抛出。
     */
    fun evaluate(source: String, scope: QueryScope, label: String, allowVariables: Boolean): Any? {
        val tokens = QueryLexer(source, label).tokenize()
        val node = QueryParser(tokens, label, allowVariables).parse()
        return evaluateNode(node, scope, label)
    }

    /**
     * 按节点类型求值。
     *
     * @param node 语法树节点。
     * @param scope 查询上下文。
     * @param label 配置路径，用于错误信息。
     * @return 求值结果。
     */
    private fun evaluateNode(node: QueryNode, scope: QueryScope, label: String): Any? {
        if (node is QueryNode.Literal) {
            return node.value
        }
        if (node is QueryNode.Path) {
            return node.path.resolve(scope)
        }
        if (node is QueryNode.Ternary) {
            return evaluateTernary(node, scope, label)
        }
        if (node is QueryNode.Unary) {
            return evaluateUnary(node, scope, label)
        }
        if (node is QueryNode.Binary) {
            return evaluateBinary(node, scope, label)
        }
        if (node is QueryNode.Index) {
            return evaluateIndex(node, scope, label)
        }
        if (node is QueryNode.ListLiteral) {
            return evaluateList(node, scope, label)
        }
        if (node is QueryNode.Variable) {
            return evaluateVariable(node, scope)
        }
        throw IllegalArgumentException("$label has an unsupported expression node.")
    }

    /** 求值列表字面量，逐项求值后按原顺序返回。 */
    private fun evaluateList(node: QueryNode.ListLiteral, scope: QueryScope, label: String): List<Any?> {
        val result = ArrayList<Any?>(node.items.size)
        for (item in node.items) {
            result.add(evaluateNode(item, scope, label))
        }
        return result
    }

    /**
     * 求值裸标识符。
     *
     * 先查查询上下文的变量表，再回落到技能自身字段（`level` / `id` / `name`）；
     * 都取不到时返回 null，让调用方按缺失处理。
     */
    private fun evaluateVariable(node: QueryNode.Variable, scope: QueryScope): Any? {
        if (scope.variables.containsKey(node.name)) {
            return scope.variables[node.name]
        }
        val key = node.name.lowercase(java.util.Locale.ROOT)
        if (scope.variables.containsKey(key)) {
            return scope.variables[key]
        }
        val skill = scope.skill
        if (skill == null) {
            return null
        }
        if (key == "level" || key == "id" || key == "name") {
            return scope.skillFieldReader(skill, key)
        }
        return null
    }

    /**
     * 求值下标取值。
     *
     * 越界或目标不是列表时返回 null，由调用方按缺失处理，避免配置差一档就抛异常。
     */
    private fun evaluateIndex(node: QueryNode.Index, scope: QueryScope, label: String): Any? {
        val target = evaluateNode(node.target, scope, label)
        if (target !is List<*>) {
            return null
        }
        val raw = evaluateNode(node.position, scope, label)
        val position = QueryText.requireDouble(raw, label)
        val rounded = Math.round(position).toInt()
        if (rounded < 0 || rounded >= target.size) {
            return null
        }
        return target[rounded]
    }

    /** 求值三元表达式。 */
    private fun evaluateTernary(node: QueryNode.Ternary, scope: QueryScope, label: String): Any? {
        val condition = QueryText.requireBoolean(evaluateNode(node.condition, scope, label), label)
        if (condition) {
            return evaluateNode(node.whenTrue, scope, label)
        }
        return evaluateNode(node.whenFalse, scope, label)
    }

    /** 求值一元运算。 */
    private fun evaluateUnary(node: QueryNode.Unary, scope: QueryScope, label: String): Any? {
        val value = evaluateNode(node.operand, scope, label)
        if (node.operator == QueryTokenType.NOT) {
            val flag = QueryText.requireBoolean(value, label)
            return !flag
        }
        val number = QueryText.requireDouble(value, label)
        return -number
    }

    /** 求值二元运算。 */
    private fun evaluateBinary(node: QueryNode.Binary, scope: QueryScope, label: String): Any? {
        if (node.operator == QueryTokenType.AND) {
            val left = QueryText.requireBoolean(evaluateNode(node.left, scope, label), label)
            if (!left) {
                return false
            }
            return QueryText.requireBoolean(evaluateNode(node.right, scope, label), label)
        }
        if (node.operator == QueryTokenType.OR) {
            val left = QueryText.requireBoolean(evaluateNode(node.left, scope, label), label)
            if (left) {
                return true
            }
            return QueryText.requireBoolean(evaluateNode(node.right, scope, label), label)
        }
        val left = evaluateNode(node.left, scope, label)
        val right = evaluateNode(node.right, scope, label)
        if (node.operator == QueryTokenType.EQUAL) {
            return equalsValue(left, right)
        }
        if (node.operator == QueryTokenType.NOT_EQUAL) {
            return !equalsValue(left, right)
        }
        if (isArithmetic(node.operator)) {
            return evaluateArithmetic(node.operator, left, right, label)
        }
        return evaluateComparison(node.operator, left, right, label)
    }

    /** 判断是否为算术运算符。 */
    private fun isArithmetic(operator: QueryTokenType): Boolean {
        return operator == QueryTokenType.PLUS ||
            operator == QueryTokenType.MINUS ||
            operator == QueryTokenType.STAR ||
            operator == QueryTokenType.SLASH ||
            operator == QueryTokenType.PERCENT
    }

    /** 求值算术运算。 */
    private fun evaluateArithmetic(operator: QueryTokenType, left: Any?, right: Any?, label: String): Double {
        if (operator == QueryTokenType.PLUS && (left is String || right is String)) {
            val text = QueryText.renderValue(left) + QueryText.renderValue(right)
            return QueryText.requireDouble(text, label)
        }
        val leftNumber = QueryText.requireDouble(left, label)
        val rightNumber = QueryText.requireDouble(right, label)
        return when (operator) {
            QueryTokenType.PLUS -> leftNumber + rightNumber
            QueryTokenType.MINUS -> leftNumber - rightNumber
            QueryTokenType.STAR -> leftNumber * rightNumber
            QueryTokenType.SLASH -> {
                if (rightNumber == 0.0) {
                    throw IllegalArgumentException("$label divides by zero.")
                }
                leftNumber / rightNumber
            }

            QueryTokenType.PERCENT -> {
                if (rightNumber == 0.0) {
                    throw IllegalArgumentException("$label divides by zero.")
                }
                leftNumber % rightNumber
            }

            else -> throw IllegalArgumentException("$label has an unsupported arithmetic operator.")
        }
    }

    /** 求值比较运算。 */
    private fun evaluateComparison(operator: QueryTokenType, left: Any?, right: Any?, label: String): Boolean {
        if (left is String && right is String) {
            val compared = left.compareTo(right)
            return applyComparison(operator, compared.toDouble(), 0.0, label)
        }
        val leftNumber = QueryText.requireDouble(left, label)
        val rightNumber = QueryText.requireDouble(right, label)
        return applyComparison(operator, leftNumber, rightNumber, label)
    }

    /** 按运算符比较两个数值。 */
    private fun applyComparison(operator: QueryTokenType, left: Double, right: Double, label: String): Boolean {
        return when (operator) {
            QueryTokenType.LESS -> left < right
            QueryTokenType.LESS_EQUAL -> left <= right
            QueryTokenType.GREATER -> left > right
            QueryTokenType.GREATER_EQUAL -> left >= right
            else -> throw IllegalArgumentException("$label has an unsupported comparison operator.")
        }
    }

    /** 判断两个值是否相等。 */
    private fun equalsValue(left: Any?, right: Any?): Boolean {
        if (left == null && right == null) {
            return true
        }
        if (left == null || right == null) {
            return false
        }
        // 两侧都能读成数值时按数值比较，使元数据里的 "1" 与数字 1 相等。
        val leftNumber = QueryText.numberOrNull(left)
        val rightNumber = QueryText.numberOrNull(right)
        if (leftNumber != null && rightNumber != null) {
            return leftNumber == rightNumber
        }
        return QueryText.renderValue(left) == QueryText.renderValue(right)
    }
}
