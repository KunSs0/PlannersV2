package com.gitee.planners.module.action.query

/**
 * 查询表达式的求值上下文。
 *
 * 承载本次执行涉及的宿主对象；各根节点的读取器通过函数类型注入，
 * 使查询层不依赖具体插件实现，也避免与技能定义互相引用。
 *
 * @property sender 施法者；可为 Player、LivingEntity、Location 或 null。
 * @property target 当前目标；与 [sender] 同构，由目标器解析结果决定。
 * @property skill 技能定义；用于读取标识、名称与本次已计算的变量。
 * @property profile 玩家档案；非玩家施法时为 null。
 * @property variables 本次技能已计算的变量快照。
 * @property skillFieldReader 技能字段读取器，参数为技能对象与字段名。
 * @property profileFieldReader 档案字段读取器，参数为档案对象与字段名。
 * @property dataReader 共享标记读取器，参数为完整 key。
 * @property attrReader 战斗属性读取器，参数为属性键。
 * @property naturalReader 职业自然成长读取器，参数为属性键。
 * @property nodeReader 技能树节点读取器，参数为树 ID 与节点 ID。
 * @property label 配置路径，用于错误与告警信息。
 * @property payload 本次回调的外部载荷，例如连招播放句柄。
 * @property flowRunner 流程跳转入口；由运行时注入，供动作在执行过程中回调另一个流程。
 * @property anchoredReader 仇恨锚定读取器，判断施法者是否为目标的当前仇恨目标。
 * @property onceKeys 本次执行内已命中的一次性键；同一实例在派生作用域之间共享。
 * @property playbackReader 连招播放变量读取器，参数为回调载荷与变量名。
 * @property targetCache 本次执行的目标缓存；由调用方注入以便跨动作复用。
 */
class QueryScope(
    val sender: Any? = null,
    val caster: Any? = sender,
    val target: Any? = null,
    val skill: Any? = null,
    val profile: Any? = null,
    val variables: Map<String, Any?> = emptyMap(),
    val skillFieldReader: (Any, String) -> Any? = { _, _ -> null },
    val profileFieldReader: (Any, String) -> Any? = { _, _ -> null },
    val dataReader: (String) -> Any? = { null },
    val attrReader: (String) -> Double? = { null },
    val naturalReader: (String) -> Double? = { null },
    val nodeReader: (String, String) -> Int = { _, _ -> 0 },
    val label: String = "",
    val payload: Map<String, Any?> = emptyMap(),
    val flowRunner: ((String, QueryScope) -> Unit)? = null,
    val anchoredReader: (Any?, Any?) -> Boolean = { _, _ -> false },
    val onceKeys: MutableSet<String> = LinkedHashSet(),
    val playbackReader: (Map<String, Any?>, String) -> Any? = { _, _ -> null },
    val targetCache: com.gitee.planners.module.action.TargetCache = com.gitee.planners.module.action.TargetCache(),
    val timerCache: com.gitee.planners.module.action.TimerCache = com.gitee.planners.module.action.TimerCache(),
    val area: com.gitee.planners.module.action.AreaInstance? = null,
    val lineTargets: List<com.gitee.planners.module.action.ActionTarget> = emptyList(),
    val finderCache: MutableMap<String, List<com.gitee.planners.module.action.ActionTarget>> = LinkedHashMap(),
) {

    /**
     * 派生一个只替换目标的作用域，供目标器逐个解析时使用。
     *
     * @param newTarget 新的目标对象。
     * @return 复用其他字段的新作用域。
     */
    fun withTarget(newTarget: Any?): QueryScope {
        return QueryScope(
            sender = sender,
            caster = caster,
            target = newTarget,
            skill = skill,
            profile = profile,
            variables = variables,
            skillFieldReader = skillFieldReader,
            profileFieldReader = profileFieldReader,
            dataReader = dataReader,
            attrReader = attrReader,
            naturalReader = naturalReader,
            nodeReader = nodeReader,
            label = label,
            payload = payload,
            flowRunner = flowRunner,
            anchoredReader = anchoredReader,
            onceKeys = onceKeys,
            playbackReader = playbackReader,
            targetCache = targetCache,
            timerCache = timerCache,
            area = area,
            lineTargets = lineTargets,
            finderCache = finderCache,
        )
    }

    /**
     * 派生一个追加了单个变量的作用域。
     *
     * 供动作把本轮结果传给子流程，例如命中流程读取本次实际伤害。
     *
     * @param key 变量名。
     * @param value 变量值。
     * @return 追加该变量的新作用域。
     */
    fun withVariable(key: String, value: Any?): QueryScope {
        val merged = LinkedHashMap<String, Any?>(variables)
        merged[key] = value
        return QueryScope(
            sender = sender,
            caster = caster,
            target = target,
            skill = skill,
            profile = profile,
            variables = merged,
            skillFieldReader = skillFieldReader,
            profileFieldReader = profileFieldReader,
            dataReader = dataReader,
            attrReader = attrReader,
            naturalReader = naturalReader,
            nodeReader = nodeReader,
            label = label,
            payload = payload,
            flowRunner = flowRunner,
            anchoredReader = anchoredReader,
            onceKeys = onceKeys,
            playbackReader = playbackReader,
            targetCache = targetCache,
            timerCache = timerCache,
            area = area,
            lineTargets = lineTargets,
            finderCache = finderCache,
        )
    }

    /**
     * 派生一个附带流程跳转入口的作用域。
     *
     * 供动作在执行过程中回调另一个流程，例如逐目标命中效果。
     *
     * @param runner 跳转入口。
     * @return 复用其他字段的新作用域。
     */
    fun withFlowRunner(runner: (String, QueryScope) -> Unit): QueryScope {
        return QueryScope(
            sender = sender,
            caster = caster,
            target = target,
            skill = skill,
            profile = profile,
            variables = variables,
            skillFieldReader = skillFieldReader,
            profileFieldReader = profileFieldReader,
            dataReader = dataReader,
            attrReader = attrReader,
            naturalReader = naturalReader,
            nodeReader = nodeReader,
            label = label,
            payload = payload,
            flowRunner = runner,
            anchoredReader = anchoredReader,
            onceKeys = onceKeys,
            playbackReader = playbackReader,
            targetCache = targetCache,
            timerCache = timerCache,
            area = area,
            lineTargets = lineTargets,
            finderCache = finderCache,
        )
    }

    /**
     * 派生一个附带标签的作用域，用于定位错误来源。
     *
     * @param newLabel 新的配置路径。
     * @return 复用其他字段的新作用域。
     */
    fun withLabel(newLabel: String): QueryScope {
        return QueryScope(
            sender = sender,
            caster = caster,
            target = target,
            skill = skill,
            profile = profile,
            variables = variables,
            skillFieldReader = skillFieldReader,
            profileFieldReader = profileFieldReader,
            dataReader = dataReader,
            attrReader = attrReader,
            naturalReader = naturalReader,
            nodeReader = nodeReader,
            label = newLabel,
            payload = payload,
            flowRunner = flowRunner,
            anchoredReader = anchoredReader,
            onceKeys = onceKeys,
            playbackReader = playbackReader,
            targetCache = targetCache,
            timerCache = timerCache,
            area = area,
            lineTargets = lineTargets,
            finderCache = finderCache,
        )
    }

    /**
     * 绑定当前行解析出的目标，供 `finder.now` 读取。
     *
     * @param targets 本行目标器解析出的目标。
     * @return 绑定了行目标的作用域。
     */
    fun withLineTargets(targets: List<com.gitee.planners.module.action.ActionTarget>): QueryScope {
        return QueryScope(
            sender = sender,
            caster = caster,
            target = target,
            skill = skill,
            profile = profile,
            variables = variables,
            skillFieldReader = skillFieldReader,
            profileFieldReader = profileFieldReader,
            dataReader = dataReader,
            attrReader = attrReader,
            naturalReader = naturalReader,
            nodeReader = nodeReader,
            label = label,
            payload = payload,
            flowRunner = flowRunner,
            anchoredReader = anchoredReader,
            onceKeys = onceKeys,
            playbackReader = playbackReader,
            targetCache = targetCache,
            timerCache = timerCache,
            area = area,
            lineTargets = targets,
            finderCache = finderCache,
        )
    }
}
