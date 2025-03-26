package com.gitee.planners.module.compat.worldguard

import com.sk89q.worldguard.bukkit.WorldGuardPlugin
import org.bukkit.entity.Player

class WorldGuardAccessImpl6 : WorldGuardAccess {

    override fun getRegions(player: Player): List<String> {
        val manager = WorldGuardPlugin.inst().regionContainer
            .get(player.world)
        if (manager == null) {
            return emptyList()
        }


        val applicableRegionSet = manager.getApplicableRegions(player.location)

        return applicableRegionSet.regions.map { it.id }
    }


}