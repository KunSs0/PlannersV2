package com.gitee.planners.core.skill.context

import com.gitee.planners.api.PlayerTemplateAPI.plannersTemplate
import com.gitee.planners.api.job.target.ProxyTarget
import com.gitee.planners.core.config.ImmutableSkill
import com.gitee.planners.core.player.PlayerTemplate
import org.bukkit.entity.Entity
import org.bukkit.entity.Player

/**
 * 一次技能计算与执行共享的强类型业务上下文。
 *
 * 纯计算入口从该对象读取明确字段并作为 Nova 函数参数传递；只有允许创建任务或回调的
 * 技能 action 会在调用边界生成隔离 bindings。
 */
class SkillExecutionContext private constructor(
    val sender: Any,
    val level: Int,
    val skill: ImmutableSkill,
    val context: SkillContext,
    val profile: PlayerTemplate?,
    var origin: Any?
) {

    private val variables = LinkedHashMap<String, Any?>()

    /** 写入本次技能执行的业务变量。 */
    fun setVariable(id: String, value: Any?) {
        variables[id] = value
    }

    /** 读取已经计算或由调用方提供的业务变量。 */
    fun getVariable(id: String): Any? {
        return variables[id]
    }

    /** 返回当前变量的只读快照。 */
    fun variableSnapshot(): Map<String, Any?> {
        return LinkedHashMap(variables)
    }

    /** 为资源型 action 生成一次性 Nova 隔离绑定。 */
    fun actionBindings(): Map<String, Any?> {
        val bindings = LinkedHashMap<String, Any?>()
        bindings["sender"] = sender
        bindings["level"] = level
        bindings["ctx"] = context
        bindings["skill"] = skill
        bindings["origin"] = origin
        val profile = this.profile
        if (profile != null) {
            bindings["profile"] = profile
        }
        bindings.putAll(variables)
        return bindings
    }

    companion object {

        /** 根据宿主对象建立统一的技能执行上下文。 */
        fun create(
            sender: Any,
            level: Int,
            skill: ImmutableSkill,
            extraVariables: Map<String, Any?> = emptyMap()
        ): SkillExecutionContext {
            val proxyTarget: ProxyTarget<*>?
            if (sender is ProxyTarget<*>) {
                proxyTarget = sender
            } else if (sender is Entity) {
                proxyTarget = ProxyTarget.BukkitEntity(sender)
            } else {
                proxyTarget = null
            }

            val player: Player?
            if (sender is Player) {
                player = sender
            } else if (sender is ProxyTarget<*>) {
                val instance = sender.instance
                if (instance is Player) {
                    player = instance
                } else {
                    player = null
                }
            } else {
                player = null
            }

            val profile: PlayerTemplate?
            if (player == null) {
                profile = null
            } else {
                profile = player.plannersTemplate
            }
            val skillContext = SkillContext(proxyTarget, skill, level)
            val result = SkillExecutionContext(sender, level, skill, skillContext, profile, sender)
            for ((id, value) in extraVariables) {
                result.setVariable(id, value)
            }
            return result
        }
    }
}
