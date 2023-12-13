package com.gitee.planners.api.job.selector

import com.gitee.planners.api.job.target.adaptTarget
import com.gitee.planners.core.action.enum
import org.bukkit.Bukkit
import org.bukkit.entity.EntityType
import taboolib.library.kether.QuestActionParser
import taboolib.module.kether.combinationParser

object InWorld : Selector {

    override fun namespace(): Array<String> {
        return arrayOf("inworld", "inWorld")
    }

    override fun action(): QuestActionParser {
        return combinationParser {
            it.group(text(), command("text", then = enum<EntityType>(EntityType.PLAYER))).apply(it) { name, type ->
                now {
                    Bukkit.getWorld(name)?.entities?.filter { it.type == type }?.map { it.adaptTarget() } ?: emptyList()
                }
            }
        }
    }

}
