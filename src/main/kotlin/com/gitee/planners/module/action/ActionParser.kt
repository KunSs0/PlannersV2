package com.gitee.planners.module.action

import taboolib.library.configuration.ConfigurationSection
import java.util.Locale

/**
 * 把技能 YAML 的 `action` 节点解析为 [ActionSpec]。
 *
 * 加载期完成全部结构、名称、参数、作用域与取值语法校验；失败时抛出带配置
 * 路径的 [IllegalArgumentException]，由技能加载流程拒绝该技能。
 */
object ActionParser {

    /**
     * 条件列表使用的键名。
     */
    private const val CONDITIONS = "conditions"

    /**
     * 动作列表使用的键名。
     */
    private const val ACTIONS = "actions"

    /**
     * 解析一个技能的 `action` 节点。
     *
     * @param skillId 所属技能 ID。
     * @param section `action` 节点；为空表示该技能没有动作。
     * @param path 当前配置路径。
     * @return 解析结果；未配置时返回 null。
     * @throws IllegalArgumentException 结构或语法非法时抛出。
     */
    fun parse(skillId: String, section: ConfigurationSection?, path: String): ActionSpec? {
        if (section == null) {
            return null
        }
        val flows = LinkedHashMap<String, ActionFlow>()
        for (key in section.getKeys(false)) {
            val flowPath = "$path.$key"
            val raw = section.get(key)
            val flow = parseFlow(key, raw, flowPath)
            if (key == ActionSpec.SERVER) {
                rejectSkillScopedEntries(flow)
            }
            flows[key] = flow
        }
        if (flows.isEmpty()) {
            return null
        }
        return ActionSpec(skillId, flows)
    }

    /**
     * 解析一组命名流程，不限定流程名。
     *
     * 供状态定义这类「按事件名声明流程」的配置复用同一套解析与校验。
     *
     * @param section 流程所在节点；为空表示没有流程。
     * @param path 当前配置路径。
     * @param keys 只解析这些流程名；为空表示解析全部键。
     * @return 流程名到已编译流程的映射；未配置时返回空映射。
     * @throws IllegalArgumentException 结构或语法非法时抛出。
     */
    fun parseNamedFlows(
        section: ConfigurationSection?,
        path: String,
        keys: Set<String> = emptySet(),
    ): Map<String, ActionFlow> {
        if (section == null) {
            return emptyMap()
        }
        val flows = LinkedHashMap<String, ActionFlow>()
        for (key in section.getKeys(false)) {
            if (keys.isNotEmpty() && !keys.contains(key)) {
                continue
            }
            val flowPath = "$path.$key"
            flows[key] = parseFlow(key, section.get(key), flowPath)
        }
        return flows
    }

    /**
     * 拒绝 `server` 流程中依赖运行期技能上下文的条目。
     *
     * `server` 在技能注册完成时执行，此时没有玩家与目标，因此需要技能树或
     * 目标实体的条件与目标器都不能直接写在该流程里，否则一定会误判。
     *
     * @param flow 待校验的流程。
     * @throws IllegalArgumentException 出现技能作用域条目时抛出。
     */
    private fun rejectSkillScopedEntries(flow: ActionFlow) {
        for (condition in flow.conditions) {
            if (condition.condition.scope == ActionScope.SKILL) {
                throw IllegalArgumentException(
                    "${condition.path} is skill-scoped and cannot run in '${ActionSpec.SERVER}' flow.",
                )
            }
        }
        for (line in flow.actions) {
            for (step in line.targeters) {
                if (step.targeter.scope == ActionScope.SKILL) {
                    throw IllegalArgumentException(
                        "${step.path} is skill-scoped and cannot run in '${ActionSpec.SERVER}' flow.",
                    )
                }
            }
        }
    }

    /**
     * 解析单个流程。
     *
     * @param id 流程名。
     * @param raw 流程原始值：字符串、列表或含 `conditions`/`actions` 的段。
     * @param path 当前配置路径。
     * @return 已编译流程。
     */
    private fun parseFlow(id: String, raw: Any?, path: String): ActionFlow {
        if (raw == null) {
            throw IllegalArgumentException("$path cannot be empty.")
        }
        if (raw is String) {
            val line = parseLine(raw, "$path[0]")
            return ActionFlow(id, emptyList(), listOf(line), path)
        }
        if (raw is List<*>) {
            val actions = parseLines(raw, path)
            return ActionFlow(id, emptyList(), actions, path)
        }
        if (raw is ConfigurationSection) {
            return parseSectionFlow(id, raw, path)
        }
        if (raw is Map<*, *>) {
            val section = taboolib.module.configuration.Configuration.fromMap(raw)
            return parseSectionFlow(id, section, path)
        }
        throw IllegalArgumentException("$path must be a string, a list or a section with conditions/actions.")
    }

    /**
     * 解析含条件与动作的流程段。
     *
     * @param id 流程名。
     * @param section 流程段。
     * @param path 当前配置路径。
     * @return 已编译流程。
     */
    private fun parseSectionFlow(id: String, section: ConfigurationSection, path: String): ActionFlow {
        var conditions: List<ActionConditionEntry> = emptyList()
        var actions: List<ActionLine> = emptyList()
        for (key in section.getKeys(false)) {
            val normalized = key.trim().lowercase(Locale.ROOT)
            if (normalized == CONDITIONS) {
                conditions = parseConditions(section.get(key), "$path.$CONDITIONS")
                continue
            }
            if (normalized == ACTIONS) {
                val raw = section.get(key)
                if (raw !is List<*>) {
                    throw IllegalArgumentException("$path.$ACTIONS must be a list.")
                }
                actions = parseLines(raw, "$path.$ACTIONS")
                continue
            }
            throw IllegalArgumentException("$path contains unsupported field '$key'.")
        }
        if (actions.isEmpty()) {
            throw IllegalArgumentException("$path.$ACTIONS cannot be empty.")
        }
        return ActionFlow(id, conditions, actions, path)
    }

    /**
     * 解析条件列表。
     *
     * 每项形如 `条件名{参数}`，末尾可写 `true` 或 `false` 表示期望值。
     *
     * @param raw 条件列表原始值。
     * @param path 当前配置路径。
     * @return 已编译条件列表；未配置时返回空列表。
     */
    private fun parseConditions(raw: Any?, path: String): List<ActionConditionEntry> {
        if (raw == null) {
            return emptyList()
        }
        if (raw !is List<*>) {
            throw IllegalArgumentException("$path must be a list of conditions.")
        }
        val result = ArrayList<ActionConditionEntry>()
        var index = 0
        for (item in raw) {
            if (item !is String) {
                throw IllegalArgumentException("$path[$index] must be a string.")
            }
            result.add(parseCondition(item, "$path[$index]"))
            index += 1
        }
        return result
    }

    /**
     * 解析单条条件。
     *
     * @param raw 条件文本。
     * @param path 当前配置路径。
     * @return 已编译条件。
     */
    fun parseCondition(raw: String, path: String): ActionConditionEntry {
        var text = raw.trim()
        if (text.isEmpty()) {
            throw IllegalArgumentException("$path cannot be blank.")
        }
        var expected = true
        val lastSpace = text.lastIndexOf(' ')
        if (lastSpace > 0) {
            val tail = text.substring(lastSpace + 1)
            if (tail.equals("true", true)) {
                expected = true
                text = text.substring(0, lastSpace).trim()
            } else if (tail.equals("false", true)) {
                expected = false
                text = text.substring(0, lastSpace).trim()
            }
        }
        val open = text.indexOf('{')
        val name: String
        val arguments: ActionArguments
        if (open < 0) {
            name = text.trim()
            if (name.isEmpty()) {
                throw IllegalArgumentException("$path requires a condition name.")
            }
            arguments = ActionArguments(emptyMap(), path)
        } else {
            name = text.substring(0, open).trim()
            if (name.isEmpty()) {
                throw IllegalArgumentException("$path requires a condition name before the argument block.")
            }
            val block = ActionTextScanner.readBlock(text, open, path)
            val trailing = text.substring(block.end).trim()
            if (trailing.isNotEmpty()) {
                throw IllegalArgumentException("$path contains unsupported text '$trailing'.")
            }
            arguments = ActionArguments(ActionTextScanner.parseArguments(block.content, path), path)
        }
        val condition = ActionRegistry.condition(name)
        if (condition == null) {
            throw IllegalArgumentException(
                "$path references unknown condition '$name', expected one of: " +
                    ActionRegistry.conditionIds().toString(),
            )
        }
        condition.validate(arguments)
        val compiled = condition.compile(arguments)
        return ActionConditionEntry(condition, arguments, expected, path, compiled)
    }

    /**
     * 解析动作行列表。
     *
     * @param raw 动作行列表。
     * @param path 当前配置路径。
     * @return 已编译动作行。
     */
    private fun parseLines(raw: List<*>, path: String): List<ActionLine> {
        if (raw.isEmpty()) {
            throw IllegalArgumentException("$path cannot be empty.")
        }
        val result = ArrayList<ActionLine>()
        var index = 0
        for (item in raw) {
            if (item !is String) {
                throw IllegalArgumentException("$path[$index] must be a string.")
            }
            result.add(parseLine(item, "$path[$index]"))
            index += 1
        }
        return result
    }

    /**
     * 解析单行动作。
     *
     * 语序固定为：动作 → 若干目标器 → 至多一个触发器。
     *
     * @param raw 动作行文本。
     * @param path 当前配置路径。
     * @return 已编译动作行。
     * @throws IllegalArgumentException 语法非法或出现多个触发器时抛出。
     */
    fun parseLine(raw: String, path: String): ActionLine {
        val text = raw.trim()
        if (text.isEmpty()) {
            throw IllegalArgumentException("$path cannot be blank.")
        }
        var index = 0
        while (index < text.length) {
            val current = text[index]
            if (current == '{' || current == '@' || current == '~' || current.isWhitespace()) {
                break
            }
            index += 1
        }
        val actionName = text.substring(0, index).trim()
        if (actionName.isEmpty()) {
            throw IllegalArgumentException("$path requires an action name.")
        }
        var arguments = ActionArguments(emptyMap(), path)
        if (index < text.length && text[index] == '{') {
            val block = ActionTextScanner.readBlock(text, index, path)
            arguments = ActionArguments(ActionTextScanner.parseArguments(block.content, path), path)
            index = block.end
        }
        val registered = ActionRegistry.action(actionName)
        val action: Action
        if (registered != null) {
            registered.validate(arguments)
            action = registered
        } else if (actionName.contains('.')) {
            // 外部插件可能晚于 Planners 启用；带命名空间的动作先留成引用，启动后校验。
            action = ActionRegistry.defer(actionName, path, arguments)
        } else {
            throw IllegalArgumentException(
                "$path references unknown action '$actionName', expected one of: " +
                    ActionRegistry.actionIds().toString(),
            )
        }

        val targeters = ArrayList<ActionTargeterStep>()
        var trigger: ActionTrigger? = null
        var triggerName: String? = null
        var triggerArguments: ActionArguments? = null

        while (true) {
            while (index < text.length && text[index].isWhitespace()) {
                index += 1
            }
            if (index >= text.length) {
                break
            }
            val marker = text[index]
            if (marker == '@') {
                index += 1
                val name = readName(text, index, path, "@")
                index = name.end
                var values = emptyMap<String, String>()
                if (index < text.length && text[index] == '{') {
                    val block = ActionTextScanner.readBlock(text, index, path)
                    values = ActionTextScanner.parseArguments(block.content, "$path.@${name.value}")
                    index = block.end
                }
                val targeter = ActionRegistry.targeter(name.value)
                if (targeter == null) {
                    throw IllegalArgumentException(
                        "$path references unknown targeter '@${name.value}', expected one of: " +
                            ActionRegistry.targeterIds().toString(),
                    )
                }
                val stepArguments = ActionArguments(values, "$path.@${name.value}")
                targeter.validate(stepArguments)
                targeters.add(ActionTargeterStep(targeter, stepArguments, "$path.@${name.value}"))
                continue
            }
            if (marker == '~') {
                if (trigger != null) {
                    throw IllegalArgumentException(
                        "$path declares more than one trigger; '~' is single per line.",
                    )
                }
                index += 1
                val name = readTriggerName(text, index, path)
                index = name.end
                var values = emptyMap<String, String>()
                if (index < text.length && text[index] == '{') {
                    val block = ActionTextScanner.readBlock(text, index, path)
                    values = ActionTextScanner.parseArguments(block.content, "$path.~${name.value}")
                    index = block.end
                } else if (index < text.length && text[index] == ':') {
                    index += 1
                    val start = index
                    while (index < text.length && !text[index].isWhitespace()) {
                        index += 1
                    }
                    val single = text.substring(start, index)
                    values = mapOf("ticks" to single)
                }
                val resolved = ActionRegistry.trigger(name.value)
                if (resolved == null) {
                    throw IllegalArgumentException(
                        "$path references unknown trigger '~${name.value}', expected one of: " +
                            ActionRegistry.triggerIds().toString(),
                    )
                }
                trigger = resolved
                triggerName = name.value
                triggerArguments = ActionArguments(values, "$path.~${name.value}")
                resolved.validate(triggerArguments)
                continue
            }
            throw IllegalArgumentException("$path contains unsupported text '${text.substring(index)}'.")
        }

        verifyPayloadTargeters(targeters, trigger, path)

        return ActionLine(
            action = action,
            arguments = arguments,
            targeters = targeters,
            trigger = trigger,
            triggerArguments = triggerArguments,
            path = path,
            source = raw,
        )
    }

    /**
     * 校验事件实体选择器与触发器匹配。
     *
     * `@attacker` / `@defender` 这类选择器只能出现在声明了对应实体的触发器行上，
     * 否则运行期永远取不到目标。没有触发器的行不参与校验（属于普通流程）。
     *
     * @param targeters 该行的目标器链。
     * @param trigger 该行的触发器；没有触发器时为 null。
     * @param path 配置路径，用于错误信息。
     * @throws IllegalArgumentException 选择器不被该触发器提供时抛出。
     */
    private fun verifyPayloadTargeters(
        targeters: List<ActionTargeterStep>,
        trigger: ActionTrigger?,
        path: String,
    ) {
        if (trigger == null) {
            return
        }
        val provided = trigger.providedTargets
        for (step in targeters) {
            val key = step.payloadKey()
            if (key == null) {
                continue
            }
            if (provided.contains(key)) {
                continue
            }
            throw IllegalArgumentException(
                "$path uses '@$key' but trigger '${trigger.id}' does not provide it; " +
                    "available: " + provided.toList().toString(),
            )
        }
    }

    /**
     * 读取 `@` 之后的目标器名。
     *
     * @param text 完整文本。
     * @param start 名称起始位置。
     * @param path 配置路径，用于错误信息。
     * @param marker 触发本次读取的标记，用于错误信息。
     * @return 名称与结束位置。
     */
    private fun readName(text: String, start: Int, path: String, marker: String): ParsedName {
        var index = start
        while (index < text.length) {
            val current = text[index]
            if (current == '{' || current == '@' || current == '~' || current.isWhitespace()) {
                break
            }
            index += 1
        }
        val value = text.substring(start, index).trim()
        if (value.isEmpty()) {
            throw IllegalArgumentException("$path requires a targeter name after '$marker'.")
        }
        return ParsedName(value, index)
    }

    /**
     * 读取 `~` 之后的触发器名。
     *
     * @param text 完整文本。
     * @param start 名称起始位置。
     * @param path 配置路径，用于错误信息。
     * @return 名称与结束位置。
     */
    private fun readTriggerName(text: String, start: Int, path: String): ParsedName {
        var index = start
        while (index < text.length) {
            val current = text[index]
            if (current == '{' || current == ':' || current == '@' || current == '~' || current.isWhitespace()) {
                break
            }
            index += 1
        }
        val value = text.substring(start, index).trim()
        if (value.isEmpty()) {
            throw IllegalArgumentException("$path requires a trigger name after '~'.")
        }
        return ParsedName(value, index)
    }

    /**
     * 已读取的名称与结束位置。
     *
     * @property value 名称文本。
     * @property end 名称结束后的位置。
     */
    private class ParsedName(val value: String, val end: Int)
}
