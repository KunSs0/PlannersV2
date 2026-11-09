package com.gitee.planners.core.config

import com.gitee.planners.module.action.query.QueryRef
import com.gitee.planners.module.action.query.QueryScope

/**
 * 一段可插值的展示文本。
 *
 * 与动作参数共用同一套语法：文本里的 `<...>` 被抽取为求值表达式，其余部分原样保留；
 * 整段不含占位符时按纯文本处理，渲染不触发任何求值。
 *
 * @property raw 配置原文。
 * @property ref 已编译的取值引用；纯文本时为 null。
 */
class DisplayText private constructor(
    val raw: String,
    private val ref: QueryRef?,
) {

    /**
     * 是否含占位符。
     */
    val dynamic: Boolean = ref != null

    /**
     * 渲染文本。
     *
     * @param scope 查询上下文。
     * @return 渲染结果；缺失的取值按空串处理，与动作参数行为一致。
     */
    fun render(scope: QueryScope): String {
        if (ref == null) {
            return raw
        }
        return ref.resolveText(scope)
    }

    companion object {

        /**
         * 旧版双花括号写法。
         *
         * 展示文本已统一为 `<...>` 求值表达式，加载期遇到该序列直接报错，
         * 避免旧配置被当成普通文案静默输出。
         */
        private const val LEGACY_DELIMITER = "{{"

        /**
         * 编译一段展示文本。
         *
         * @param raw 配置原文。
         * @param label 配置路径，用于错误信息。
         * @return 已编译的展示文本。
         * @throws IllegalArgumentException 仍使用 `{{...}}` 写法或表达式语法非法时抛出。
         */
        fun compile(raw: String, label: String): DisplayText {
            if (raw.contains(LEGACY_DELIMITER)) {
                throw IllegalArgumentException(
                    "$label 仍在使用 {{...}} 写法；展示文本已统一为 <...> 求值表达式。",
                )
            }
            if (raw.isBlank()) {
                return DisplayText(raw, null)
            }
            val ref = QueryRef.parse(raw, label)
            if (ref.isLiteral()) {
                return DisplayText(raw, null)
            }
            return DisplayText(raw, ref)
        }

        /**
         * 编译可缺省的一段展示文本。
         *
         * @param raw 配置原文；未配置时传 null。
         * @param label 配置路径，用于错误信息。
         * @return 已编译的展示文本；未配置时返回 null。
         */
        fun compileOrNull(raw: String?, label: String): DisplayText? {
            if (raw == null) {
                return null
            }
            return compile(raw, label)
        }

        /**
         * 编译多行展示文本。
         *
         * @param raw 配置原文列表。
         * @param label 配置路径前缀，行号按下标追加。
         * @return 已编译的展示文本列表。
         */
        fun compileList(raw: List<String>, label: String): List<DisplayText> {
            val result = ArrayList<DisplayText>(raw.size)
            for (index in raw.indices) {
                result.add(compile(raw[index], label + "[" + index + "]"))
            }
            return result
        }
    }
}
