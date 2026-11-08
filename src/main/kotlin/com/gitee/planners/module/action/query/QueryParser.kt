package com.gitee.planners.module.action.query

/**
 * 求值表达式的语法分析器。
 *
 * 优先级由低到高：三元 → 逻辑或 → 逻辑与 → 相等 → 比较 → 加减 → 乘除模 → 一元 → 原子。
 *
 * @property tokens 词法单元列表。
 * @property label 配置路径，用于错误信息。
 * @property allowVariables 是否允许裸标识符按上下文变量解析。
 */
class QueryParser(
    private val tokens: List<QueryToken>,
    private val label: String,
    private val allowVariables: Boolean = false,
) {

    private var index = 0

    /**
     * 解析整个表达式。
     *
     * @return 语法树根节点。
     * @throws IllegalArgumentException 语法错误或存在未消费内容时抛出。
     */
    fun parse(): QueryNode {
        val node = parseTernary()
        val current = peek()
        if (!current.isType(QueryTokenType.END)) {
            throw IllegalArgumentException("$label has unexpected '${current.text}' at position ${current.position}.")
        }
        return node
    }

    /** 解析三元表达式。 */
    private fun parseTernary(): QueryNode {
        val condition = parseOr()
        if (!peek().isType(QueryTokenType.QUESTION)) {
            return condition
        }
        consume()
        val whenTrue = parseTernary()
        val separator = peek()
        if (!separator.isType(QueryTokenType.COLON)) {
            throw IllegalArgumentException(
                "$label expects ':' at position ${separator.position} in a ternary expression.",
            )
        }
        consume()
        val whenFalse = parseTernary()
        return QueryNode.Ternary(condition, whenTrue, whenFalse)
    }

    /** 解析逻辑或。 */
    private fun parseOr(): QueryNode {
        var left = parseAnd()
        while (peek().isType(QueryTokenType.OR)) {
            consume()
            val right = parseAnd()
            left = QueryNode.Binary(QueryTokenType.OR, left, right)
        }
        return left
    }

    /** 解析逻辑与。 */
    private fun parseAnd(): QueryNode {
        var left = parseEquality()
        while (peek().isType(QueryTokenType.AND)) {
            consume()
            val right = parseEquality()
            left = QueryNode.Binary(QueryTokenType.AND, left, right)
        }
        return left
    }

    /** 解析相等比较。 */
    private fun parseEquality(): QueryNode {
        var left = parseComparison()
        while (true) {
            val current = peek()
            if (!current.isType(QueryTokenType.EQUAL) && !current.isType(QueryTokenType.NOT_EQUAL)) {
                return left
            }
            consume()
            val right = parseComparison()
            left = QueryNode.Binary(current.type, left, right)
        }
    }

    /** 解析大小比较。 */
    private fun parseComparison(): QueryNode {
        var left = parseAdditive()
        while (true) {
            val current = peek()
            val comparison = current.isType(QueryTokenType.LESS) ||
                current.isType(QueryTokenType.LESS_EQUAL) ||
                current.isType(QueryTokenType.GREATER) ||
                current.isType(QueryTokenType.GREATER_EQUAL)
            if (!comparison) {
                return left
            }
            consume()
            val right = parseAdditive()
            left = QueryNode.Binary(current.type, left, right)
        }
    }

    /** 解析加减。 */
    private fun parseAdditive(): QueryNode {
        var left = parseMultiplicative()
        while (true) {
            val current = peek()
            if (!current.isType(QueryTokenType.PLUS) && !current.isType(QueryTokenType.MINUS)) {
                return left
            }
            consume()
            val right = parseMultiplicative()
            left = QueryNode.Binary(current.type, left, right)
        }
    }

    /** 解析乘除模。 */
    private fun parseMultiplicative(): QueryNode {
        var left = parseUnary()
        while (true) {
            val current = peek()
            val arithmetic = current.isType(QueryTokenType.STAR) ||
                current.isType(QueryTokenType.SLASH) ||
                current.isType(QueryTokenType.PERCENT)
            if (!arithmetic) {
                return left
            }
            consume()
            val right = parseUnary()
            left = QueryNode.Binary(current.type, left, right)
        }
    }

    /** 解析一元运算。 */
    private fun parseUnary(): QueryNode {
        val current = peek()
        if (current.isType(QueryTokenType.NOT)) {
            consume()
            return QueryNode.Unary(QueryTokenType.NOT, parseUnary())
        }
        if (current.isType(QueryTokenType.MINUS)) {
            consume()
            return QueryNode.Unary(QueryTokenType.MINUS, parseUnary())
        }
        return parsePostfix()
    }

    /**
     * 解析下标取值。
     *
     * 支持连续下标，例如 `skill.table[1][0]`；下标本身可以是任意表达式。
     */
    private fun parsePostfix(): QueryNode {
        var node = parsePrimary()
        while (peek().isType(QueryTokenType.LEFT_BRACKET)) {
            consume()
            val position = parseTernary()
            val closing = peek()
            if (!closing.isType(QueryTokenType.RIGHT_BRACKET)) {
                throw IllegalArgumentException("$label expects ']' at position ${closing.position}.")
            }
            consume()
            node = QueryNode.Index(node, position)
        }
        return node
    }

    /** 解析原子：常量、路径、括号。 */
    private fun parsePrimary(): QueryNode {
        val current = peek()
        if (current.isType(QueryTokenType.NUMBER)) {
            consume()
            return QueryNode.Literal(current.number)
        }
        if (current.isType(QueryTokenType.STRING)) {
            consume()
            return QueryNode.Literal(current.text)
        }
        if (current.isType(QueryTokenType.IDENTIFIER)) {
            consume()
            val lower = current.text.lowercase()
            if (lower == "true") {
                return QueryNode.Literal(true)
            }
            if (lower == "false") {
                return QueryNode.Literal(false)
            }
            if (lower == "null") {
                return QueryNode.Literal(null)
            }
            val root = current.text.substringBefore('.')
            if (QueryRoot.of(root) == null) {
                // 只有显式允许的场景（等级表这类按名取值）才把裸标识符当变量；
                // 其余情况仍然报错，让调用方回退成字面量文本。
                if (!allowVariables) {
                    throw IllegalArgumentException(
                        "$label references unknown identifier '${current.text}', " +
                            "expected a query path starting with one of: ${QueryRoot.keywords()}",
                    )
                }
                return QueryNode.Variable(current.text)
            }
            val path = QueryPath.parse(current.text, label)
            return QueryNode.Path(path)
        }
        if (current.isType(QueryTokenType.LEFT_BRACKET)) {
            return parseListLiteral()
        }
        if (current.isType(QueryTokenType.LEFT_PAREN)) {
            consume()
            val inner = parseTernary()
            val closing = peek()
            if (!closing.isType(QueryTokenType.RIGHT_PAREN)) {
                throw IllegalArgumentException("$label expects ')' at position ${closing.position}.")
            }
            consume()
            return inner
        }
        throw IllegalArgumentException("$label has unexpected '${current.text}' at position ${current.position}.")
    }

    /** 解析列表字面量，例如 `[300, 450, 630]`。 */
    private fun parseListLiteral(): QueryNode {
        consume()
        val items = ArrayList<QueryNode>()
        if (peek().isType(QueryTokenType.RIGHT_BRACKET)) {
            consume()
            return QueryNode.ListLiteral(items)
        }
        while (true) {
            items.add(parseTernary())
            val current = peek()
            if (current.isType(QueryTokenType.COMMA)) {
                consume()
                continue
            }
            if (current.isType(QueryTokenType.RIGHT_BRACKET)) {
                consume()
                return QueryNode.ListLiteral(items)
            }
            throw IllegalArgumentException("$label expects ',' or ']' at position ${current.position}.")
        }
    }

    /** 查看当前单元但不前进。 */
    private fun peek(): QueryToken {
        return tokens[index]
    }

    /** 消费当前单元。 */
    private fun consume() {
        if (index < tokens.size - 1) {
            index += 1
        }
    }
}
