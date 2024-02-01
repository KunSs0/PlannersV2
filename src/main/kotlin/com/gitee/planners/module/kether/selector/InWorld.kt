package com.gitee.planners.module.kether.selector

import com.gitee.planners.api.common.script.kether.KetherHelper
import com.gitee.planners.api.common.script.kether.SimpleKetherParser
import com.gitee.planners.api.job.selector.Selector
import com.gitee.planners.api.job.target.adaptTarget
import com.gitee.planners.module.kether.*
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.entity.EntityType
import taboolib.library.kether.Parser
import taboolib.module.kether.*

object InWorld : AbstractSelector("inworld","inWorld","in-world") {

    override fun select() = KetherHelper.combinedKetherParser {
        it.group(bukkitWorldListOf(), commandEnumListOf<EntityType>("type")).apply(it) { worlds, types ->
            now {
                val entities = worlds.flatMap { it.entities }.filter { it.isDead && (types.isEmpty() || it.type in types) }
                getTargetContainer() += entities.map { it.adaptTarget() }
            }
        }
    }

    fun ParserHolder.bukkitWorldListOf(): Parser<List<World>> {
        return tokenListOf { Bukkit.getWorld(it) ?: error("World $it not found.") }
    }


}
