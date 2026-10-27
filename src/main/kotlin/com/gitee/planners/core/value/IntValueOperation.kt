package com.gitee.planners.core.value

/**
 * 整数规格的修正运算。
 *
 * 只支持 `+N`、`-N`、`*N`、`/N` 四种写法，每一步都拒绝非有限结果。
 */
object IntValueOperation {

    /**
     * 按声明顺序执行修正。
     *
     * @param base 基础数值。
     * @param operations 已通过条件判断的修正表达式。
     * @return 全部修正后的数值，不做取整。
     * @throws IllegalArgumentException 基础值或中间结果非有限、除数为零时抛出。
     */
    fun resolve(base: Double, operations: List<String>): Double {
        require(base.isFinite()) { "基础数值必须是有限值" }
        var result = base
        for (operation in operations) {
            result = apply(result, operation)
            require(result.isFinite()) { "数值修正 '$operation' 的结果不是有限值" }
        }
        return result
    }

    /**
     * 执行单条修正。
     *
     * @param base 当前数值。
     * @param operation 修正表达式。
     * @return 修正后的数值。
     * @throws IllegalArgumentException 表达式格式非法、操作数非有限或除数为零时抛出。
     */
    fun apply(base: Double, operation: String): Double {
        val text = operation.trim()
        if (text.length < 2) {
            throw IllegalArgumentException("数值修正格式无效：$operation")
        }
        val operator = text[0]
        val operandText = text.substring(1).trim()
        val operand = operandText.toDoubleOrNull()
        if (operand == null || !operand.isFinite()) {
            throw IllegalArgumentException("数值修正的操作数无效：$operation")
        }
        if (operator == '+') {
            return base + operand
        }
        if (operator == '-') {
            return base - operand
        }
        if (operator == '*') {
            return base * operand
        }
        if (operator == '/') {
            if (operand == 0.0) {
                throw IllegalArgumentException("数值修正不能除以零：$operation")
            }
            return base / operand
        }
        throw IllegalArgumentException("未知的数值修正运算符：$operation")
    }
}
