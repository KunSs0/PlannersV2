package com.gitee.planners.module.action.rule

import com.gitee.planners.module.action.ActionArguments
import com.gitee.planners.module.action.ActionScope
import com.gitee.planners.module.action.ActionTarget
import com.gitee.planners.module.action.ActionTargeter
import com.gitee.planners.module.action.query.QueryScope
import java.util.Locale

/**
 * `filter`：按规则逐元素筛选目标容器。
 *
 * `r` 接受逗号分隔的规则列表，按书写顺序依次过滤：每条规则只看到上一条规则的存活元素，
 * 因此多条规则等价于按顺序求值的“与”，短路后不再执行后续规则。规则自身不排序、
 * 不截断，容器形态仍由目标器的书写顺序决定。
 *
 * 规则列表在加载期解析：`r` 必须是字面量，未知规则与非法参数在配置加载时直接报错，
 * 不做“执行期才发现”的延迟处理。多规则行不接受额外参数，需要参数的规则单独写一段。
 */
object FilterTargeter : ActionTargeter {

    override val id = "filter"

    override val aliases = listOf("filter")

    override val scope = ActionScope.SKILL

    /**
     * 加载期校验规则列表与规则参数。
     *
     * @param arguments 目标器参数。
     * @throws IllegalArgumentException `r` 缺失、不是字面量、含未知规则或参数非法时抛出。
     */
    override fun validate(arguments: ActionArguments) {
        val raw = arguments.literalText("r")
        if (raw == null) {
            if (arguments.contains("r")) {
                throw IllegalArgumentException(
                    "${arguments.path} 的 r 必须是加载期可解析的规则列表，不能写成动态值。",
                )
            }
            throw IllegalArgumentException("${arguments.path} requires argument 'r'.")
        }
        val ruleIds = parseRuleIds(raw, arguments.path)
        val body = arguments.without("r")
        if (ruleIds.size > 1 && body.keys.isNotEmpty()) {
            throw IllegalArgumentException(
                "${arguments.path} 多规则过滤不接受额外参数，需要参数的规则请单独写一段。",
            )
        }
        for (ruleId in ruleIds) {
            FilterRules.require(ruleId, arguments.path).validate(body)
        }
    }

    /**
     * 按顺序应用规则列表。
     *
     * @param scope 当前执行上下文。
     * @param arguments 目标器参数。
     * @param input 上游容器。
     * @return 全部规则通过的元素，顺序与上游一致。
     */
    override fun resolve(scope: QueryScope, arguments: ActionArguments, input: List<ActionTarget>): List<ActionTarget> {
        val ruleIds = parseRuleIds(arguments.requireText(scope, "r"), arguments.path)
        val body = arguments.without("r")
        var current = input
        for (ruleId in ruleIds) {
            if (current.isEmpty()) {
                // 上一轮已经清空容器，后续规则不可能再放回元素，直接结束。
                return current
            }
            val rule = FilterRules.require(ruleId, arguments.path)
            val compiled = rule.compile(body)
            val kept = ArrayList<ActionTarget>(current.size)
            for (target in current) {
                if (rule.test(scope, target, body, compiled)) {
                    kept.add(target)
                }
            }
            current = kept
        }
        return current
    }

    /**
     * 拆分规则列表，去空白、去重并保持书写顺序。
     *
     * @param raw 原始文本。
     * @param path 配置路径，用于错误信息。
     * @return 规则主键列表。
     * @throws IllegalArgumentException 列表为空时抛出。
     */
    private fun parseRuleIds(raw: String, path: String): List<String> {
        val result = LinkedHashSet<String>()
        for (part in raw.split(',')) {
            val item = part.trim().lowercase(Locale.ROOT)
            if (item.isNotEmpty()) {
                result.add(item)
            }
        }
        if (result.isEmpty()) {
            throw IllegalArgumentException("$path 的 r 至少要写一条规则。")
        }
        return ArrayList(result)
    }
}
