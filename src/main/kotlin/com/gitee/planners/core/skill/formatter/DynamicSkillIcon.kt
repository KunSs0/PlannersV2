package com.gitee.planners.core.skill.formatter

import com.gitee.planners.api.common.script.SingletonKetherScript
import com.gitee.planners.api.job.target.Target
import com.gitee.planners.api.job.target.adaptTarget
import com.gitee.planners.module.kether.context.ImmutableSkillContext
import com.gitee.planners.core.config.ImmutableSkill
import com.gitee.planners.core.player.PlayerSkill
import org.bukkit.entity.Player
import taboolib.common.util.unsafeLazy
import taboolib.module.kether.KetherFunction

class DynamicSkillIcon(sender: Target<*>, skill: ImmutableSkill, level: Int = 1) :
    AbstractSkillIcon(sender, skill, level) {

    val context by unsafeLazy {
        ImmutableSkillContext(sender, skill, level)
    }

    override fun parse(text: String?): String {
        if (text == null) {
            return ""
        }

        // parse the text
        return KetherFunction.reader.replaceNested(text.trim()) {
            SingletonKetherScript(this).run(context.createOptions())
                .getNow(null)
                .toString()
        }
    }

    companion object {

        fun build(player: Player, skill: PlayerSkill) = build(player, skill.immutable, skill.level)

        fun build(player: Player, skill: ImmutableSkill, level: Int = 1) =
            DynamicSkillIcon(player.adaptTarget(), skill, level).build()

    }


}
