package com.gitee.planners.module.fluxon

import com.gitee.planners.api.Registries
import com.gitee.planners.api.job.Skill
import com.gitee.planners.api.job.target.LeastType
import com.gitee.planners.api.job.target.ProxyTarget
import com.gitee.planners.api.job.target.ProxyTargetContainer
import com.gitee.planners.api.job.target.asTarget
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

/**
 * 解析任意类型为 ProxyTargetContainer
 * 支持: ProxyTarget, ProxyTargetContainer, Entity, Location, List<*>, Array<*>
 */
fun resolveTargets(arg: Any?): ProxyTargetContainer {
    return when (arg) {
        null -> ProxyTargetContainer()
        is ProxyTargetContainer -> arg
        is ProxyTarget<*> -> ProxyTargetContainer.of(arg)
        is org.bukkit.entity.Entity -> ProxyTargetContainer.of(arg.asTarget())
        is org.bukkit.Location -> ProxyTargetContainer.of(arg.asTarget())
        is Iterable<*> -> ProxyTargetContainer().also { container ->
            arg.forEach { item -> container.addAll(resolveTargets(item)) }
        }
        is Array<*> -> resolveTargets(arg.toList())
        else -> ProxyTargetContainer()
    }
}

/**
 * 从上下文获取目标参数，支持 LeastType 默认填充
 */
fun FluxonFunctionContext.getTargetsArg(
    index: Int,
    leastType: LeastType = LeastType.SENDER
): ProxyTargetContainer {
    val arg = arguments.getOrNull(index)
    val targets = resolveTargets(arg)
    if (targets.isEmpty()) {
        val sender = environment.rootVariables["sender"]
        return leastType.getTargetContainer(sender)
    }
    return targets
}
