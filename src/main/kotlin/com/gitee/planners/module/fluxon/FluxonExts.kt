package com.gitee.planners.module.fluxon

import com.gitee.planners.api.Registries
import com.gitee.planners.api.job.Skill
import com.gitee.planners.api.job.target.ProxyTarget
import org.bukkit.command.CommandSender
import org.bukkit.entity.LivingEntity

/**
 * 解析参数为 LivingEntity
 *
 * 可传入 LivingEntity 实例或 ProxyTarget.BukkitEntity 实例
 */
fun resolveLivingEntity(arg: Any?): LivingEntity? {
    return when (arg) {
        is ProxyTarget.BukkitEntity -> arg.instance as? LivingEntity
        is LivingEntity -> arg
        else -> null
    }
}

/**
 * 解析参数为 Skill
 *
 * 可传入 Skill ID 或 Skill 实例
 */
fun resolveSkill(arg: Any?): Skill? {
    return when (arg) {
        is String -> Registries.SKILL.get(arg)
        is Skill -> arg
        else -> null
    }
}

/**
 * 从上下文解析技能
 *
 * 可传入 Skill ID 或 Skill 实例
 */
fun FluxonFunctionContext.resolveSkill(index: Int): Skill? {
    return resolveSkill(arguments.getOrNull(index))
}

/**
 * 从上下文获取 CommandSender 参数
 *
 * 可传入 CommandSender 实例或 ProxyTarget.BukkitEntity 实例
 */
fun FluxonFunctionContext.getSenderArg(index: Int): CommandSender {
    if (arguments.size > index) {
        return arguments[index] as? CommandSender
            ?: throw IllegalStateException("Argument at $index is not a CommandSender")
    }
    return (environment.rootVariables["sender"] ?: environment.rootVariables["player"]) as? CommandSender
        ?: throw IllegalStateException("No sender found in environment")
}
