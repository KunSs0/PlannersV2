package com.gitee.planners.module.compat.worldguard

import com.gitee.planners.util.checkPluginClass
import org.bukkit.entity.Player
import taboolib.common.platform.function.info
import taboolib.common.util.unsafeLazy

interface WorldGuardAccess {

    fun getRegions(player: Player): List<String>

    companion object {

        val INSTANCE: WorldGuardAccess? by unsafeLazy {
            if (checkPluginClass("com.sk89q.worldguard.bukkit.WorldGuardPlugin")) {
                info("Loading WorldGuard 6.x")
                WorldGuardAccessImpl6()
            } else if (checkPluginClass("com.sk89q.worldguard.WorldGuard")) {
                info("Loading WorldGuard 7.x")
                WorldGuardAccessImpl7()
            } else {
                null
            }
        }

    }
}