package com.gitee.planners.module.fluxon

import com.gitee.planners.api.Registries
import com.gitee.planners.api.job.Skill
import com.gitee.planners.api.job.target.LeastType
import com.gitee.planners.api.job.target.ProxyTarget
import com.gitee.planners.api.job.target.ProxyTargetContainer
import com.gitee.planners.api.job.target.asTarget
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.tabooproject.fluxon.runtime.FunctionContext

/**
 * 解析参数为 LivingEntity
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
 */
fun resolveSkill(arg: Any?): Skill? {
    return when (arg) {
        is String -> Registries.SKILL.get(arg)
        is Skill -> arg
        else -> null
    }
}

/**
 * 解析任意类型为 ProxyTargetContainer
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
 * 从上下文获取 Player 参数
 */
fun FunctionContext<*>.getPlayerArg(index: Int): Player? {
    if (index >= 0 && argumentCount > index) {
        return getRef(index) as? Player
    }
    return (environment.rootVariables["sender"] ?: environment.rootVariables["player"]) as? Player
}

/**
 * 从上下文获取目标参数，支持 LeastType 默认填充
 */
fun FunctionContext<*>.getTargetsArg(
    index: Int,
    leastType: LeastType = LeastType.SENDER
): ProxyTargetContainer {
    val arg = if (index >= 0 && argumentCount > index) getRef(index) else null
    val targets = resolveTargets(arg)
    if (targets.isEmpty()) {
        val sender = environment.rootVariables["sender"]
        return leastType.getTargetContainer(sender)
    }
    return targets
}
