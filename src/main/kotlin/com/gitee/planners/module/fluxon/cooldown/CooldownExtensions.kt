package com.gitee.planners.module.fluxon.cooldown

import com.gitee.planners.api.Registries
import com.gitee.planners.api.job.Skill
import com.gitee.planners.core.skill.cooler.Cooler
import org.bukkit.entity.Player
import org.tabooproject.fluxon.runtime.FluxonRuntime
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake

/**
 * Cooldown 冷却系统扩展 - 全局函数注册
 */
object CooldownExtensions {

    @Awake(LifeCycle.INIT)
    private fun init() {
        with(FluxonRuntime.getInstance()) {
            // getCooldown(skillIdOrSkill, player?) -> long
            registerFunction("getCooldown", listOf(1, 2)) { context ->
                val args = context.arguments
                val skillIdOrSkill = args[0]
                val player = getPlayerFromContext(context, args)

                val skill = when (skillIdOrSkill) {
                    is String -> Registries.SKILL.get(skillIdOrSkill)
                    is Skill -> skillIdOrSkill
                    else -> return@registerFunction 0L
                }

                Cooler.INSTANCE.get(player, skill)
            }

            // setCooldown(skillIdOrSkill, ticks, player?) -> void
            registerFunction("setCooldown", listOf(2, 3)) { context ->
                val args = context.arguments
                val skillIdOrSkill = args[0]
                val ticks = (args[1] as? Number)?.toInt() ?: return@registerFunction null
                val player = getPlayerFromContext(context, args, 2)

                val skill = when (skillIdOrSkill) {
                    is String -> Registries.SKILL.get(skillIdOrSkill)
                    is Skill -> skillIdOrSkill
                    else -> return@registerFunction null
                }

                Cooler.INSTANCE.set(player, skill, ticks)
                null
            }

            // resetCooldown(skillIdOrSkill, player?) -> void
            registerFunction("resetCooldown", listOf(1, 2)) { context ->
                val args = context.arguments
                val skillIdOrSkill = args[0]
                val player = getPlayerFromContext(context, args)

                val skill = when (skillIdOrSkill) {
                    is String -> Registries.SKILL.get(skillIdOrSkill)
                    is Skill -> skillIdOrSkill
                    else -> return@registerFunction null
                }

                Cooler.INSTANCE.set(player, skill, 0)
                null
            }

            // hasCooldown(skillIdOrSkill, player?) -> boolean
            registerFunction("hasCooldown", listOf(1, 2)) { context ->
                val args = context.arguments
                val skillIdOrSkill = args[0]
                val player = getPlayerFromContext(context, args)

                val skill = when (skillIdOrSkill) {
                    is String -> Registries.SKILL.get(skillIdOrSkill)
                    is Skill -> skillIdOrSkill
                    else -> return@registerFunction false
                }

                Cooler.INSTANCE.get(player, skill) > 0
            }
        }
    }

    /**
     * 从上下文或参数获取Player实体
     * @param context 执行上下文
     * @param args 函数参数
     * @param playerArgIndex 玩家参数的索引（默认为最后一个参数）
     */
    private fun getPlayerFromContext(context: org.tabooproject.fluxon.runtime.Context, args: Array<Any>, playerArgIndex: Int = args.size - 1): Player {
        // 如果有多个参数且最后一个是Player，则取最后一个参数
        if (args.size > playerArgIndex && args[playerArgIndex] is Player) {
            return args[playerArgIndex] as Player
        }

        // 从环境中获取player变量
        val env = context.environment
        val find = env.rootVariables["player"]
        if (find is Player) {
            return find
        }

        throw IllegalStateException("No player found in environment or arguments")
    }
}
