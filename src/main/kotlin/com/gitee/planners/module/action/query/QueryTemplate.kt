package com.gitee.planners.module.action.query

/**
 * 带占位符的文本模板。
 *
 * 用于「一句话里嵌几个取值」的场景，例如 `&a冲锋 Lv<skill.level>`：
 * 扫描出的每个 `<...>` 都按取值引用解析，其余部分原样保留。
 *
 * 解析失败或被判定不成占位符的 `<` 按普通字符处理，避免破坏自然文案中的
 * 比较符号；代价是拼写错误不会在加载期报错。
 *
 * @property label 配置路径，用于错误信息。
 */
class QueryTemplate private constructor(
    private val segments: List<Segment>,
    val label: String,
) {

    /**
     * 渲染模板。
     *
     * @param scope 查询上下文。
     * @return 渲染后的文本；缺失的取值按空串处理。
     */
    fun render(scope: QueryScope): String {
        val builder = StringBuilder()
        for (segment in segments) {
            val ref = segment.ref
            if (ref == null) {
                builder.append(segment.text)
                continue
            }
            builder.append(ref.resolveText(scope))
        }
        return builder.toString()
    }

    /**
     * 判断模板是否含占位符。
     *
     * @return 含占位符时返回 true。
     */
    fun isDynamic(): Boolean {
        for (segment in segments) {
            if (segment.ref != null) {
                return true
            }
        }
        return false
    }

    /**
     * 模板的一段。
     *
     * @property text 纯文本段；取值段为 null。
     * @property ref 取值引用；纯文本段为 null。
     */
    class Segment(val text: String?, val ref: QueryRef?)

    companion object {

        /**
         * 判断文本是否可能是模板。
         *
         * @param text 原始文本。
         * @return 含 `<` 与 `>` 时返回 true。
         */
        fun looksLikeTemplate(text: String): Boolean {
            val open = text.indexOf('<')
            if (open < 0) {
                return false
            }
            return text.indexOf('>', open + 1) > open + 1
        }

        /**
         * 解析模板。
         *
         * @param text 原始文本。
         * @param label 配置路径，用于错误信息。
         * @return 已解析模板。
         */
        fun parse(text: String, label: String): QueryTemplate {
            val segments = ArrayList<Segment>()
            val pending = StringBuilder()
            var index = 0
            while (index < text.length) {
                val current = text[index]
                if (current != '<') {
                    pending.append(current)
                    index += 1
                    continue
                }
                val close = text.indexOf('>', index + 1)
                if (close < 0) {
                    pending.append(current)
                    index += 1
                    continue
                }
                val inner = text.substring(index + 1, close).trim()
                val ref = resolvePlaceholder(inner, label)
                if (ref == null) {
                    pending.append(current)
                    index += 1
                    continue
                }
                if (pending.isNotEmpty()) {
                    segments.add(Segment(pending.toString(), null))
                    pending.setLength(0)
                }
                segments.add(Segment(null, ref))
                index = close + 1
            }
            if (pending.isNotEmpty()) {
                segments.add(Segment(pending.toString(), null))
            }
            return QueryTemplate(segments, label)
        }

        /**
         * 把占位符内容解析为取值引用。
         *
         * @param inner 尖括号内的文本。
         * @param label 配置路径，用于错误信息。
         * @return 取值引用；不能作为取值时返回 null，按普通文本处理。
         */
        private fun resolvePlaceholder(inner: String, label: String): QueryRef? {
            if (inner.isEmpty()) {
                return null
            }
            if (QueryRoot.of(inner.substringBefore('.')) == null) {
                return null
            }
            if (QueryPath.isPlainPath(inner)) {
                return QueryRef.parsePath(inner, label)
            }
            return QueryRef.parseExpression(inner, label)
        }
    }
}
