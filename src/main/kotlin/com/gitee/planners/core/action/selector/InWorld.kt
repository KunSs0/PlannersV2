package com.gitee.planners.core.action.selector

import com.gitee.planners.api.common.script.kether.CombinationKetherParser
import com.gitee.planners.api.common.script.kether.KetherHelper
import com.gitee.planners.api.job.selector.Selector
import com.gitee.planners.api.job.target.adaptTarget
import com.gitee.planners.core.action.*
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.entity.EntityType
import taboolib.library.kether.Parser
import taboolib.library.kether.QuestActionParser
import taboolib.module.kether.*
import taboolib.module.kether.ParserHolder.option

object InWorld : Selector {

    override fun namespace(): Array<String> {
        return arrayOf("inworld", "inWorld")
    }

    override fun action(): QuestActionParser {
        return combinationParser {
            it.group(bukkitWorldListOf(), commandEnumListOf<EntityType>("type")).apply(it) { worlds, types ->
                now {
                    val entities = worlds.flatMap { it.entities }.filter { it.isDead && (types.isEmpty() || it.type in types) }
                    getTargetContainer() += entities.map { it.adaptTarget() }
                }
            }
        }
    }

    fun ParserHolder.bukkitWorldListOf(): Parser<List<World>> {
        return tokenListOf { Bukkit.getWorld(it) ?: error("World $it not found.") }
    }


}
