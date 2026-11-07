package com.gitee.planners.module.action.query

/**
 * 求值表达式的词法分析器。
 *
 * 标识符允许点号与连字符，因此路径 `node.knight_warder.knight-F-01_a`
 * 可以被整体识别。为避免歧义，**减法两侧必须留空格**：
 * `a-b` 会读成一个标识符，`a - b` 才是减法。
 *
 * @property source 源文本。
 * @property label 配置路径，用于错误信息。
 */
class QueryLexer(private val source: String, private val label: String) {

    private var index = 0

    /**
     * 切分全部词法单元。
     *
     * @return 以 [QueryTokenType.END] 结尾的单元列表。
     * @throws IllegalArgumentException 出现非法字符或未闭合字符串时抛出。
     */
    fun tokenize(): List<QueryToken> {
        val result = ArrayList<QueryToken>()
        while (true) {
            skipWhitespace()
            if (index >= source.length) {
                result.add(QueryToken(QueryTokenType.END, "", null, index))
                return result
            }
            val start = index
            val current = source[index]
            if (current.isDigit()) {
                result.add(readNumber(start))
                continue
            }
            if (current == '"' || current == '\'') {
                result.add(readString(start, current))
                continue
            }
            if (isIdentifierStart(current)) {
                result.add(readIdentifier(start))
                continue
            }
            result.add(readOperator(start, current))
        }
    }

    /** 跳过多余空白。 */
    private fun skipWhitespace() {
        while (index < source.length && source[index].isWhitespace()) {
            index += 1
        }
    }

    /** 判断字符能否作为标识符起始。 */
    private fun isIdentifierStart(character: Char): Boolean {
        return character.isLetter() || character == '_'
    }

    /** 判断字符能否作为标识符内容。 */
    private fun isIdentifierPart(character: Char): Boolean {
        val direct = character.isLetterOrDigit() || character == '_' || character == '.'
        if (direct) {
            return true
        }
        if (character != '-') {
            return false
        }
        if (index <= 0) {
            return false
        }
        val previous = source[index - 1]
        return previous.isLetterOrDigit() || previous == '_' || previous == '-'
    }

    /** 读取数字字面量。 */
    private fun readNumber(start: Int): QueryToken {
        while (index < source.length && source[index].isDigit()) {
            index += 1
        }
        if (index < source.length && source[index] == '.') {
            index += 1
            while (index < source.length && source[index].isDigit()) {
                index += 1
            }
        }
        val text = source.substring(start, index)
        val number = text.toDoubleOrNull()
        if (number == null) {
            throw IllegalArgumentException("$label has an invalid number '$text' at position $start.")
        }
        return QueryToken(QueryTokenType.NUMBER, text, number, start)
    }

    /** 读取引号字符串。 */
    private fun readString(start: Int, quote: Char): QueryToken {
        index += 1
        val builder = StringBuilder()
        while (index < source.length) {
            val current = source[index]
            if (current == quote) {
                index += 1
                return QueryToken(QueryTokenType.STRING, builder.toString(), null, start)
            }
            builder.append(current)
            index += 1
        }
        throw IllegalArgumentException("$label has an unterminated string at position $start.")
    }

    /** 读取标识符或路径。 */
    private fun readIdentifier(start: Int): QueryToken {
        index += 1
        while (index < source.length) {
            val current = source[index]
            if (!isIdentifierPart(current)) {
                break
            }
            index += 1
        }
        val text = source.substring(start, index)
        if (index < source.length && source[index] == '(') {
            throw IllegalArgumentException(
                "$label does not support function calls; found '$text(' at position $start.",
            )
        }
        return QueryToken(QueryTokenType.IDENTIFIER, text, null, start)
    }

    /** 读取运算符。 */
    private fun readOperator(start: Int, current: Char): QueryToken {
        val next = if (index + 1 < source.length) source[index + 1] else ' '
        val twoCharacter = "" + current + next
        val type = when (twoCharacter) {
            "==" -> QueryTokenType.EQUAL
            "!=" -> QueryTokenType.NOT_EQUAL
            "<=" -> QueryTokenType.LESS_EQUAL
            ">=" -> QueryTokenType.GREATER_EQUAL
            "&&" -> QueryTokenType.AND
            "||" -> QueryTokenType.OR
            else -> null
        }
        if (type != null) {
            index += 2
            return QueryToken(type, twoCharacter, null, start)
        }
        val single = when (current) {
            '+' -> QueryTokenType.PLUS
            '-' -> QueryTokenType.MINUS
            '*' -> QueryTokenType.STAR
            '/' -> QueryTokenType.SLASH
            '%' -> QueryTokenType.PERCENT
            '<' -> QueryTokenType.LESS
            '>' -> QueryTokenType.GREATER
            '!' -> QueryTokenType.NOT
            '?' -> QueryTokenType.QUESTION
            ':' -> QueryTokenType.COLON
            '(' -> QueryTokenType.LEFT_PAREN
            ')' -> QueryTokenType.RIGHT_PAREN
            '[' -> QueryTokenType.LEFT_BRACKET
            ']' -> QueryTokenType.RIGHT_BRACKET
            else -> null
        }
        if (single == null) {
            throw IllegalArgumentException("$label has an unsupported character '$current' at position $start.")
        }
        index += 1
        return QueryToken(single, "" + current, null, start)
    }
}
