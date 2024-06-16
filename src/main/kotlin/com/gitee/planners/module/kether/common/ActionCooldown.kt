package com.gitee.planners.module.kether.common

import com.gitee.planners.api.RegistryBuiltin
import com.gitee.planners.api.common.script.kether.CombinationKetherParser
import com.gitee.planners.api.common.script.kether.KetherHelper
import com.gitee.planners.api.common.script.kether.MultipleKetherParser
import com.gitee.planners.api.job.target.TargetBukkitEntity
import com.gitee.planners.core.config.ImmutableSkill
import com.gitee.planners.core.skill.cooler.Cooler
import com.gitee.planners.module.kether.commandObjectiveOrSender
import com.gitee.planners.module.kether.context.ImmutableSkillContext
import com.gitee.planners.module.kether.getEnvironmentContext
import org.bukkit.entity.Player
import taboolib.common5.Coerce
import taboolib.module.kether.ScriptFrame
import taboolib.module.kether.actionFuture
import taboolib.module.kether.expects
import taboolib.module.kether.scriptParser
import java.util.Optional

@CombinationKetherParser.Used
object ActionCooldown : MultipleKetherParser("cooldown", "cd") {

    // cooldown [skill <id>] [at objective:TargetContainer(sender)]
    val main = KetherHelper.combinedKetherParser {
        it.group(
            command("skill", then = text()).optional(),
            commandObjectiveOrSender()
        ).apply(it) { skill, objective ->
            now {
                val immutableSkill = getImmutableSkill(skill, this)
                val entity =
                    objective.filterIsInstance<TargetBukkitEntity>().filter { it.instance is Player }.firstOrNull()
                if (entity != null) {
                    Cooler.INSTANCE.get(entity.instance as Player, immutableSkill)
                } else {
                    -1
                }
            }
        }
    }

    fun p() = scriptParser {
        val token = it.expects("true", "false")
        actionFuture {
            it.complete(token)
        }
    }

    val get = main

    // cooldown reset [skill <id>] [at objective:TargetContainer(sender)]
    val reset = process { player, skill ->
        Cooler.INSTANCE.set(player, skill, 0)
    }

    // cooldown add <value> [skill <id>] [at objective:TargetContainer(sender)]
    val add = process { player, skill, data ->
        val last = Cooler.INSTANCE.get(player, skill)
        Cooler.INSTANCE.set(player, skill, Coerce.toInteger(last + data))
    }

    // cooldown set <value> [skill <id>] [at objective:TargetContainer(sender)]
    val set = process { player, skill, data ->
        Cooler.INSTANCE.set(player, skill, Coerce.toInteger(data))
    }

    private fun getImmutableSkill(opt: Optional<String>, frame: ScriptFrame): ImmutableSkill {
        return RegistryBuiltin.SKILL.get(opt.orElseGet {
            val ctx = frame.getEnvironmentContext()
            (ctx as? ImmutableSkillContext)?.skill?.id ?: error("")
        })
    }

    private fun process(func: (player: Player, skill: ImmutableSkill) -> Unit) = KetherHelper.combinedKetherParser {
        it.group(
            command("skill", then = text()).optional(),
            commandObjectiveOrSender()
        ).apply(it) { skill, objective ->
            now {
                val immutableSkill = getImmutableSkill(skill, this)
                objective.filterIsInstance<TargetBukkitEntity>().mapNotNull { it.instance as? Player }.forEach {
                    func(it, immutableSkill)
                }
            }
        }
    }

    private fun process(func: (player: Player, skill: ImmutableSkill, data: Long) -> Unit) =
        KetherHelper.combinedKetherParser {
            it.group(
                long(),
                command("skill", then = text()).optional(),
                commandObjectiveOrSender()
            ).apply(it) { data, skill, objective ->
                now {
                    val immutableSkill = getImmutableSkill(skill, this)
                    objective.filterIsInstance<TargetBukkitEntity>().mapNotNull { it.instance as? Player }.forEach {
                        func(it, immutableSkill, Coerce.toLong(data))
                    }
                }
            }
        }

}
