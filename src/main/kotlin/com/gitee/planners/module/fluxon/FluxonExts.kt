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
 * 解析参数为 [LivingEntity]
 *
 * 支持的输入类型:
 * - [ProxyTarget.BukkitEntity]: 从代理目标中提取实体
 * - [LivingEntity]: 直接返回
 *
 * @param arg 待解析的参数，可以是 ProxyTarget 或 LivingEntity
 * @return 解析出的 LivingEntity，如果无法解析则返回 null
 */
fun resolveLivingEntity(arg: Any?): LivingEntity? {
    return when (arg) {
        is ProxyTarget.BukkitEntity -> arg.instance as? LivingEntity
        is LivingEntity -> arg
        else -> null
    }
}

/**
 * 解析参数为 [Skill]
 *
 * 支持的输入类型:
 * - [String]: 通过技能 ID 从注册表查找
 * - [Skill]: 直接返回
 *
 * @param arg 待解析的参数，可以是技能 ID 字符串或 Skill 对象
 * @return 解析出的 Skill，如果无法解析则返回 null
 */
fun resolveSkill(arg: Any?): Skill? {
    return when (arg) {
        is String -> Registries.SKILL.get(arg)
        is Skill -> arg
        else -> null
    }
}

/**
 * 解析任意类型为 [ProxyTargetContainer]
 *
 * 支持的输入类型:
 * - `null`: 返回空容器
 * - [ProxyTargetContainer]: 直接返回
 * - [ProxyTarget]: 包装为单元素容器
 * - [org.bukkit.entity.Entity]: 转换为代理目标后包装
 * - [org.bukkit.Location]: 转换为代理目标后包装
 * - [Iterable]: 递归解析每个元素
 * - [Array]: 转换为列表后递归解析
 *
 * @param arg 待解析的参数
 * @return 解析出的目标容器，永不为 null
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
 * 从 Fluxon 函数上下文获取 Player 参数
 *
 * @param index 参数索引，-1 表示从环境变量中获取 sender/player
 * @return 解析出的 Player，如果无法解析则返回 null
 *
 * 解析逻辑:
 * 1. 如果 index >= 0 且参数存在，尝试将该参数转换为 Player
 * 2. 否则从环境变量中查找 "sender" 或 "player"
 */
fun FunctionContext<*>.getPlayerArg(index: Int): Player? {
    if (index >= 0 && argumentCount > index) {
        return getRef(index) as? Player
    }
    return (environment.rootVariables["sender"] ?: environment.rootVariables["player"]) as? Player
}

/**
 * 从 Fluxon 函数上下文获取目标参数
 *
 * @param index 参数索引，-1 表示不从参数获取
 * @param leastType 当目标为空时的默认填充类型
 * @return 解析出的目标容器
 *
 * 解析逻辑:
 * 1. 如果 index >= 0 且参数存在，使用 [resolveTargets] 解析该参数
 * 2. 如果解析结果为空，根据 [leastType] 从环境变量 sender 获取默认目标
 *
 * @see LeastType
 * @see resolveTargets
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
