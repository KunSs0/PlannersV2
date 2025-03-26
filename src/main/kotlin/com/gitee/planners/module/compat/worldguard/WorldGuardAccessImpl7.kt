package com.gitee.planners.module.compat.worldguard

import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldguard.WorldGuard
import com.sk89q.worldguard.protection.ApplicableRegionSet
import org.bukkit.entity.Player
import taboolib.library.reflex.Reflex.Companion.invokeMethod

class WorldGuardAccessImpl7 : WorldGuardAccess {

    override fun getRegions(player: Player): List<String> {
        val manager = WorldGuard.getInstance()
            .platform
            .regionContainer
            .get(BukkitAdapter.adapt(player.world))
        if (manager == null) {
            return emptyList()
        }

        val blockVector3 = BukkitAdapter.asBlockVector(player.location)

        val applicableRegionSet = manager
            .invokeMethod<ApplicableRegionSet>("getApplicableRegions", blockVector3)!!


        return applicableRegionSet.regions.map { it.id }
    }


}