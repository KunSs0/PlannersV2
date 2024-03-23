package com.gitee.planners.module.kether.common

import com.gitee.planners.api.ProfileAPI.plannersProfile
import com.gitee.planners.api.RegistryBuiltin
import com.gitee.planners.api.common.script.KetherEditor
import com.gitee.planners.api.common.script.kether.CombinationKetherParser
import com.gitee.planners.api.common.script.kether.KetherHelper
import com.gitee.planners.api.common.script.kether.MultipleKetherParser
import com.gitee.planners.module.kether.context.ImmutableSkillContext
import com.gitee.planners.api.job.target.LeastType
import com.gitee.planners.api.job.target.Target
import com.gitee.planners.api.job.target.TargetBukkitEntity
import com.gitee.planners.module.kether.commandEnum
import com.gitee.planners.module.kether.commandInt
import com.gitee.planners.module.kether.commandObjective
import org.bukkit.entity.Player

@CombinationKetherParser.Used
object ActionSkill : MultipleKetherParser("skill") {

    @KetherEditor.Document("skill cast <id> [type: relative] [level: 1] [at objective:TargetContainer(sender)]")
    val cast0 = KetherHelper.combinedKetherParser {
        it.group(
            text(),
            commandEnum<PlayupType>("type", PlayupType.RELATIVE),
            commandInt("level", 1),
            commandObjective(type = LeastType.SENDER)
        ).apply(it) { id, type, level, sender ->
            now {
                if (type == PlayupType.FORCE) {
                    val skill = RegistryBuiltin.SKILL.get(id)
                    sender.forEach { ImmutableSkillContext(it, skill, level).run() }
                } else if (type == PlayupType.RELATIVE) {
                    sender.filterIsInstance<TargetBukkitEntity>().forEach { entity ->
                        val player = entity.instance as? Player

                    }
                } else if (type == PlayupType.INVOKE) {
                    val skill = RegistryBuiltin.SKILL.get(id)
                    sender.filterIsInstance<TargetBukkitEntity>().forEach { entity ->
                        val player = entity.instance as? Player ?: return@forEach
                        val i = player.plannersProfile.getRegistriedSkillOrNull(id)?.level ?: return@forEach
                        ImmutableSkillContext(entity, skill, i).run()
                    }
                }
            }
        }
    }

    val cast = object : MultipleKetherParser() {

        // 强制释放 不计入冷却，可传入等级，不传入等级相对自身技能等级释放
        val force = KetherHelper.combinedKetherParser {
            it.group(text(), commandInt("level", -1), commandObjective(type = LeastType.SENDER)).apply(it) { id, level, objective ->
                now {
                    val skill = RegistryBuiltin.SKILL.get(id)
                    objective.forEach {
                        val i = if (level == -1) getSkillLevelWithTarget(it, id) ?: 1 else 1
                        ImmutableSkillContext(it, skill, i).run()
                    }
                }
            }
        }

        // 调用释放，技能必须释放者存在，会计入冷却（跟随等级参数），可传入等级，不传入等级相对自身技能等级释放
        val invoke = KetherHelper.combinedKetherParser {
            it.group(text(), commandInt("level", -1), commandObjective(type = LeastType.SENDER)).apply(it) { id, level, objective ->
                now {
                    val skill = RegistryBuiltin.SKILL.get(id)
                    objective.forEach {
                        val i = if (level == -1) getSkillLevelWithTarget(it, id) ?: 1 else 1
                        ImmutableSkillContext(it, skill, i).run()
                    }
                }
            }
        }

    }

    fun getSkillLevelWithTarget(target: Target<*>, id: String): Int? {
        return ((target as? TargetBukkitEntity)?.instance as? Player)?.plannersProfile?.getRegistriedSkillOrNull(id)?.level
    }


    enum class PlayupType {

        FORCE, RELATIVE, INVOKE

    }

}
