package com.gitee.planners.module.action

/**
 * 目标器链文本解析。
 *
 * 语法与动作行一致：每一步写作 `@名称` 或 `@名称{参数}`，步与步之间用空白分隔；
 * 同一套解析同时服务动作行、指向过滤链与 `targeter` 条件，避免多处实现漂移。
 */
object ActionTargeterChainParser {

    /**
     * 解析一段链文本。
     *
     * @param text 形如 `@crosshair{range=20} @ally` 的文本。
     * @param path 配置路径，用于错误信息。
     * @return 已编译的目标器步骤。
     */
    fun parseText(text: String, path: String): List<ActionTargeterStep> {
        return parseEntries(splitEntries(text, path), path)
    }

    /**
     * 解析一组链条目。
     *
     * @param entries 已拆分的条目文本，按声明顺序。
     * @param path 配置路径，用于错误信息。
     * @return 已编译的目标器步骤。
     */
    fun parseEntries(entries: List<String>, path: String): List<ActionTargeterStep> {
        if (entries.isEmpty()) {
            return emptyList()
        }
        val steps = ArrayList<ActionTargeterStep>(entries.size)
        var index = 0
        for (entry in entries) {
            steps.add(parseEntry(entry, index, path))
            index += 1
        }
        return steps
    }

    /**
     * 按引号与花括号外的空白拆分条目。
     *
     * @param text 链文本。
     * @param path 配置路径，用于错误信息。
     * @return 条目文本。
     */
    private fun splitEntries(text: String, path: String): List<String> {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            throw IllegalArgumentException("$path cannot be blank.")
        }
        val entries = ArrayList<String>()
        val current = StringBuilder()
        var depth = 0
        var quote: Char? = null
        for (character in trimmed) {
            if (quote != null) {
                current.append(character)
                if (character == quote) {
                    quote = null
                }
                continue
            }
            if (character == '"' || character == '\'') {
                quote = character
                current.append(character)
                continue
            }
            if (character == '{') {
                depth += 1
                current.append(character)
                continue
            }
            if (character == '}') {
                depth -= 1
                if (depth < 0) {
                    throw IllegalArgumentException("$path has an unbalanced '}'.")
                }
                current.append(character)
                continue
            }
            if (depth == 0 && character.isWhitespace()) {
                if (current.isNotEmpty()) {
                    entries.add(current.toString())
                    current.setLength(0)
                }
                continue
            }
            current.append(character)
        }
        if (depth != 0) {
            throw IllegalArgumentException("$path has an unterminated argument block.")
        }
        if (current.isNotEmpty()) {
            entries.add(current.toString())
        }
        return entries
    }

    /**
     * 解析单个链条目。
     *
     * @param entry 条目文本。
     * @param index 条目下标，从 0 开始，用于错误信息。
     * @param path 配置路径，用于错误信息。
     * @return 已编译的目标器步骤。
     */
    private fun parseEntry(entry: String, index: Int, path: String): ActionTargeterStep {
        val text = entry.trim()
        if (text.isEmpty()) {
            throw IllegalArgumentException("$path[$index] cannot be blank.")
        }
        if (text[0] != '@') {
            throw IllegalArgumentException("$path[$index] must start with '@', got '$text'.")
        }
        var cursor = 1
        val nameStart = cursor
        while (cursor < text.length && text[cursor] != '{' && !text[cursor].isWhitespace()) {
            cursor += 1
        }
        val name = text.substring(nameStart, cursor)
        if (name.isEmpty()) {
            throw IllegalArgumentException("$path[$index] is missing a targeter name after '@'.")
        }
        val targeter = ActionRegistry.targeter(name)
        if (targeter == null) {
            throw IllegalArgumentException(
                "$path[$index] references unknown targeter '@$name', expected one of: " +
                    ActionRegistry.targeterIds().toString(),
            )
        }
        val stepPath = "$path[$index].@$name"
        var values: Map<String, String> = LinkedHashMap()
        if (cursor < text.length) {
            if (text[cursor].isWhitespace()) {
                throw IllegalArgumentException("$path[$index] contains unsupported text '${text.substring(cursor)}'.")
            }
            val block = ActionTextScanner.readBlock(text, cursor, stepPath)
            values = ActionTextScanner.parseArguments(block.content, stepPath)
            cursor = block.end
            while (cursor < text.length && text[cursor].isWhitespace()) {
                cursor += 1
            }
            if (cursor < text.length) {
                throw IllegalArgumentException("$path[$index] contains unsupported text '${text.substring(cursor)}'.")
            }
        }
        // 参数在加载期完成校验，非法参数在技能加载时就报错，而不是等到玩家释放。
        val arguments = ActionArguments(values, stepPath)
        targeter.validate(arguments)
        return ActionTargeterStep(targeter, arguments, stepPath)
    }
}
