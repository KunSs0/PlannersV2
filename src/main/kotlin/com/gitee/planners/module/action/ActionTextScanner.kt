package com.gitee.planners.module.action

/**
 * 动作行的文本扫描工具。
 *
 * 负责花括号块的定位、参数按分号拆分、引号处理；
 * 不读取配置对象，便于单独测试。
 */
object ActionTextScanner {

    /**
     * 读取一个花括号块。
     *
     * 引号内的 `}` 不视为块结束，因此参数值可以包含右花括号。
     *
     * @param text 完整文本。
     * @param start 左花括号的位置。
     * @param path 配置路径，用于错误信息。
     * @return 块内容与块结束后的位置。
     * @throws IllegalArgumentException 花括号未闭合时抛出。
     */
    fun readBlock(text: String, start: Int, path: String): Block {
        if (start >= text.length || text[start] != '{') {
            throw IllegalArgumentException("$path expected '{' at position $start.")
        }
        var index = start + 1
        var quote: Char? = null
        while (index < text.length) {
            val current = text[index]
            if (quote != null) {
                if (current == quote) {
                    quote = null
                }
                index += 1
                continue
            }
            if (current == '"' || current == '\'') {
                quote = current
                index += 1
                continue
            }
            if (current == '}') {
                return Block(text.substring(start + 1, index), index + 1)
            }
            index += 1
        }
        throw IllegalArgumentException("$path has an unterminated argument block.")
    }

    /**
     * 拆分 `key=value;key2=value` 形式的参数。
     *
     * @param content 花括号内的内容。
     * @param path 配置路径，用于错误信息。
     * @return 参数映射，按声明顺序。
     * @throws IllegalArgumentException 缺少等号、键为空或重复时抛出。
     */
    fun parseArguments(content: String, path: String): Map<String, String> {
        val result = LinkedHashMap<String, String>()
        if (content.isBlank()) {
            return result
        }
        val parts = splitOutsideQuotes(content)
        for (part in parts) {
            val piece = part.trim()
            if (piece.isEmpty()) {
                continue
            }
            val separator = indexOfOutsideQuotes(piece, '=')
            if (separator < 0) {
                throw IllegalArgumentException("$path argument '$piece' must use key=value.")
            }
            val key = piece.substring(0, separator).trim()
            val value = unquote(piece.substring(separator + 1).trim())
            if (key.isEmpty()) {
                throw IllegalArgumentException("$path contains an argument with a blank key.")
            }
            if (containsKeyIgnoreCase(result, key)) {
                throw IllegalArgumentException("$path contains duplicate argument '$key'.")
            }
            result[key] = value
        }
        return result
    }

    /**
     * 按分号拆分，忽略引号内的分号。
     *
     * @param content 待拆分内容。
     * @return 拆分后的片段。
     */
    fun splitOutsideQuotes(content: String): List<String> {
        val result = ArrayList<String>()
        var quote: Char? = null
        var start = 0
        var index = 0
        while (index < content.length) {
            val current = content[index]
            if (quote != null) {
                if (current == quote) {
                    quote = null
                }
                index += 1
                continue
            }
            if (current == '"' || current == '\'') {
                quote = current
                index += 1
                continue
            }
            if (current == ';') {
                result.add(content.substring(start, index))
                start = index + 1
            }
            index += 1
        }
        result.add(content.substring(start))
        return result
    }

    /**
     * 查找引号外的字符位置。
     *
     * @param text 文本。
     * @param target 目标字符。
     * @return 位置；未找到时返回 -1。
     */
    fun indexOfOutsideQuotes(text: String, target: Char): Int {
        var quote: Char? = null
        var index = 0
        while (index < text.length) {
            val current = text[index]
            if (quote != null) {
                if (current == quote) {
                    quote = null
                }
                index += 1
                continue
            }
            if (current == '"' || current == '\'') {
                quote = current
                index += 1
                continue
            }
            if (current == target) {
                return index
            }
            index += 1
        }
        return -1
    }

    /**
     * 去掉成对的单引号或双引号。
     *
     * @param value 原始值。
     * @return 去引号后的值。
     */
    fun unquote(value: String): String {
        if (value.length < 2) {
            return value
        }
        val first = value[0]
        val last = value[value.length - 1]
        if (first != last) {
            return value
        }
        if (first != '"' && first != '\'') {
            return value
        }
        return value.substring(1, value.length - 1)
    }

    /**
     * 判断参数表中是否已存在同名键，大小写不敏感。
     *
     * @param values 已解析参数。
     * @param key 待查找键。
     * @return 已存在时返回 true。
     */
    private fun containsKeyIgnoreCase(values: Map<String, String>, key: String): Boolean {
        for (existing in values.keys) {
            if (existing.equals(key, true)) {
                return true
            }
        }
        return false
    }

    /**
     * 已读取的花括号块。
     *
     * @property content 块内容，不含花括号。
     * @property end 块结束后的位置。
     */
    class Block(val content: String, val end: Int)
}
