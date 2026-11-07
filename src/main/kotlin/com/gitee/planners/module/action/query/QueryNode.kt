package com.gitee.planners.module.action.query

/**
 * 求值表达式的抽象语法树节点。
 *
 * 节点在加载期构造，运行期只做遍历求值，不再解析文本。
 */
sealed class QueryNode {

    /**
     * 常量节点。
     *
     * @property value 常量值。
     */
    class Literal(val value: Any?) : QueryNode()

    /**
     * 路径节点。
     *
     * @property path 已解析的查询路径。
     */
    class Path(val path: QueryPath) : QueryNode()

    /**
     * 一元运算节点。
     *
     * @property operator 运算符类型，仅支持逻辑非与负号。
     * @property operand 操作数。
     */
    class Unary(val operator: QueryTokenType, val operand: QueryNode) : QueryNode()

    /**
     * 二元运算节点。
     *
     * @property operator 运算符类型。
     * @property left 左操作数。
     * @property right 右操作数。
     */
    class Binary(val operator: QueryTokenType, val left: QueryNode, val right: QueryNode) : QueryNode()

    /**
     * 三元运算节点。
     *
     * @property condition 条件。
     * @property whenTrue 条件成立时的分支。
     * @property whenFalse 条件不成立时的分支。
     */
    class Ternary(val condition: QueryNode, val whenTrue: QueryNode, val whenFalse: QueryNode) : QueryNode()

    /**
     * 列表下标取值节点。
     *
     * 目标求值为列表时返回对应元素；越界或目标不是列表时返回 null。
     *
     * @property target 列表表达式，通常是配置里声明的数组变量。
     * @property position 下标表达式。
     */
    class Index(val target: QueryNode, val position: QueryNode) : QueryNode()
}
