package com.gitee.planners.module.action

import com.gitee.planners.api.PlayerTemplateAPI.plannersTemplate
import com.gitee.planners.api.job.target.ProxyTarget
import com.gitee.planners.core.config.ImmutableSkill
import com.gitee.planners.core.player.PlayerRoute
import com.gitee.planners.core.player.PlayerTemplate
import com.gitee.planners.module.action.query.QueryScope
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player

/**
 * 执行上下文的构造入口。
 *
 * 把 Bukkit 玩家、技能定义与本次变量装配成 [QueryScope]，并接好各查询根的
 * 读取器；属性类读取器由战斗插件安装，未安装时返回 null。
 */
object ActionScopeFactory {

    /**
     * 为一条触发器派发构造上下文，绑定该流程所属技能与其学习等级。
     *
     * 触发器按主键派发给所有声明它的技能，因此每个技能都要拿到自己的 `skill`
     * 与 `level`，否则被动流程无法用 `skill.level > 0` 判断「是否学会」。
     *
     * @param spec 本次派发的技能结构。
     * @param player 触发本次执行的玩家。
     * @param variables 事件载荷注入的变量。
     * @param target 事件目标；无目标传 null。
     * @param payload 事件载荷，供流程读取。
     * @return 查询上下文。
     */
    fun forSpec(
        spec: ActionSpec,
        player: Player,
        variables: Map<String, Any?> = emptyMap(),
        target: Any? = null,
        payload: Map<String, Any?> = emptyMap(),
    ): QueryScope {
        val skill = com.gitee.planners.api.Registries.SKILL.getOrNull(spec.skillId)
        val level = if (skill == null) 0 else resolveSkillLevel(player, spec.skillId)
        return forPlayer(
            player,
            skill,
            level,
            variables,
            target,
            "trigger:" + spec.skillId,
            payload,
        )
    }

    /**
     * 为某技能在指定玩家上构造上下文，并把该技能的变量算好。
     *
     * 供 `call` 逐目标展开使用：子流程里 `<skill.变量>` 拿到的是**这个玩家**
     * 在该技能等级下的值，因此属性、护盾这类被动不需要额外的取值动作。
     *
     * @param spec 本次执行所属技能结构。
     * @param player 目标玩家。
     * @param payload 事件载荷。
     * @return 查询上下文；技能未注册时返回只绑定玩家的上下文。
     */
    fun forSpecWithVariables(
        spec: ActionSpec,
        player: Player,
        payload: Map<String, Any?> = emptyMap(),
    ): QueryScope {
        val skill = com.gitee.planners.api.Registries.SKILL.getOrNull(spec.skillId)
        if (skill == null) {
            return forPlayer(player, null, 0, payload, null, "trigger:" + spec.skillId, payload)
        }
        val level = resolveSkillLevel(player, spec.skillId)
        val execution = com.gitee.planners.core.skill.context.SkillExecutionContext.create(
            player,
            level,
            skill,
            emptyMap(),
        )
        // 技能变量先放，事件载荷后覆盖：<skill.damage> 这类事件字段优先，
        // 其余 <skill.参数> 仍能读到该玩家在当前等级下的值。
        val variables = LinkedHashMap<String, Any?>()
        variables.putAll(skill.evaluateDisplayVariables(execution))
        variables.putAll(payload)
        return forPlayer(player, skill, level, variables, null, "trigger:" + spec.skillId, payload)
    }

    /**
     * 解析玩家某个技能的学习等级。
     *
     * @param player 玩家。
     * @param skillId 技能 ID。
     * @return 技能等级；档案未加载或未学习时返回 0。
     */
    private fun resolveSkillLevel(player: Player, skillId: String): Int {
        val template = resolveTemplate(player)
        if (template == null) {
            return 0
        }
        return com.gitee.planners.core.skilltree.SkillTreeNodeEffectService.getSkillLevel(template, skillId)
    }

    /**
     * 为一次技能事件构造上下文。
     *
     * @param player 触发本次执行的玩家。
     * @param skill 相关技能；非技能事件传 null。
     * @param level 技能等级；无技能时传 0。
     * @param variables 本次已计算的技能变量。
     * @param target 当前目标；无目标传 null。
     * @param label 配置路径前缀，用于错误信息。
     * @return 查询上下文。
     */
    fun forPlayer(
        player: Player,
        skill: ImmutableSkill? = null,
        level: Int = 0,
        variables: Map<String, Any?> = emptyMap(),
        target: Any? = null,
        label: String = "",
        payload: Map<String, Any?> = emptyMap(),
    ): QueryScope {
        val template = resolveTemplate(player)
        return QueryScope(
            sender = player,
            target = target,
            skill = skill,
            profile = template,
            variables = variables,
            skillFieldReader = { value, field ->
                readSkillField(value, field, level)
            },
            profileFieldReader = { value, field ->
                readProfileField(value, field)
            },
            dataReader = { key ->
                readMetadata(player, key)
            },
            attrReader = { key ->
                readAttr(player, key)
            },
            naturalReader = { key ->
                ActionAttributeAccess.natural(player, key)
            },
            nodeReader = { tree, node ->
                readNodeLevel(template, tree, node)
            },
            label = label,
            payload = payload,
            anchoredReader = com.gitee.planners.module.action.ActionCombatFacts.anchoredReader,
            playbackReader = com.gitee.planners.module.action.ActionCombatFacts.playbackReaderFunction,
        )
    }

    /**
     * 为一次技能执行构造上下文。
     *
     * 变量按声明顺序求值时需要一个可累积的映射：这里把同一份映射交给上下文，
     * 后续写入的变量对已构造的上下文立即可见。
     *
     * @param execution 本次技能执行上下文。
     * @param variables 可累积的变量映射。
     * @return 查询上下文。
     */
    fun forExecution(
        execution: com.gitee.planners.core.skill.context.SkillExecutionContext,
        variables: MutableMap<String, Any?>,
        payload: Map<String, Any?> = emptyMap(),
    ): QueryScope {
        val player = resolvePlayer(execution.sender)
        val label = "skill:" + execution.skill.id
        if (player == null) {
            return QueryScope(
                sender = execution.sender,
                skill = execution.skill,
                profile = execution.profile,
                variables = variables,
                label = label,
                payload = payload,
            )
        }
        val base = forPlayer(
            player,
            execution.skill,
            execution.level,
            variables,
            null,
            label,
            payload,
        )
        val origin = execution.origin
        if (origin == null) {
            return base
        }
        return QueryScope(
            sender = base.sender,
            target = base.target,
            skill = base.skill,
            profile = base.profile,
            variables = base.variables,
            skillFieldReader = base.skillFieldReader,
            profileFieldReader = base.profileFieldReader,
            dataReader = base.dataReader,
            attrReader = base.attrReader,
            naturalReader = base.naturalReader,
            nodeReader = base.nodeReader,
            label = base.label,
            payload = base.payload,
            targetCache = base.targetCache,
        )
    }

    /**
     * 从施法者对象解析玩家。
     *
     * @param sender 施法者，可能是玩家、代理目标或位置。
     * @return 玩家；不是玩家上下文时返回 null。
     */
    private fun resolvePlayer(sender: Any): Player? {
        if (sender is Player) {
            return sender
        }
        if (sender is com.gitee.planners.api.job.target.ProxyTarget<*>) {
            val instance = sender.instance
            if (instance is Player) {
                return instance
            }
        }
        return null
    }

    /**
     * 为一次没有玩家的执行构造上下文。
     *
     * 用于技能加载期与诊断；所有查询根读到空值。
     *
     * @param label 配置路径前缀。
     * @return 查询上下文。
     */
    fun forSystem(label: String): QueryScope {
        return QueryScope(label = label)
    }

    /** 解析玩家档案；档案未加载时返回 null。 */
    private fun resolveTemplate(player: Player): PlayerTemplate? {
        return try {
            val template = player.plannersTemplate
            template
        } catch (exception: IllegalStateException) {
            null
        }
    }

    /** 读取技能字段。 */
    private fun readSkillField(value: Any, field: String, level: Int): Any? {
        if (value !is ImmutableSkill) {
            return null
        }
        return when (field) {
            "id" -> value.id
            "name" -> value.name
            "level" -> level
            else -> null
        }
    }

    /** 读取档案字段。 */
    private fun readProfileField(value: Any, field: String): Any? {
        if (value !is PlayerTemplate) {
            return null
        }
        return when (field) {
            "level" -> value.level
            "experience" -> value.experience
            "job" -> readJobId(value)
            "route" -> readRouteId(value)
            "skill_points" -> readSkillPoints(value)
            else -> null
        }
    }

    /** 读取当前职业 ID。 */
    private fun readJobId(template: PlayerTemplate): String? {
        val router = template.playerRouter
        if (router == null) {
            return null
        }
        return router.getCurrentJob().id
    }

    /** 读取当前职业线 ID。 */
    private fun readRouteId(template: PlayerTemplate): String? {
        val router = template.playerRouter
        if (router == null) {
            return null
        }
        return router.getCurrentJob().id
    }

    /** 读取可用技能点。 */
    private fun readSkillPoints(template: PlayerTemplate): Int? {
        val router = template.playerRouter
        if (router == null) {
            return null
        }
        return router.skillPointsCurrent
    }

    /** 读取共享标记。 */
    private fun readMetadata(player: Player, key: String): Any? {
        val proxy = ProxyTarget.of(player)
        val metadata = proxy.getMetadata(key)
        if (metadata == null) {
            return null
        }
        return metadata.asString()
    }

    /** 读取战斗属性。 */
    private fun readAttr(player: Player, key: String): Double? {
        return ActionAttributeAccess.attr(player, key)
    }

    /** 沿职业继承链取节点最大学习等级。 */
    private fun readNodeLevel(template: PlayerTemplate?, tree: String, node: String): Int {
        if (template == null) {
            return 0
        }
        val router = template.playerRouter
        if (router == null) {
            return 0
        }
        var level = 0
        for (route in router.routeLine) {
            level = maxOf(level, readRouteNodeLevel(route, tree, node))
        }
        return level
    }

    /** 读取单条职业线的节点等级。 */
    private fun readRouteNodeLevel(route: PlayerRoute, tree: String, node: String): Int {
        return route.getNodeLevel(tree, node)
    }
}
