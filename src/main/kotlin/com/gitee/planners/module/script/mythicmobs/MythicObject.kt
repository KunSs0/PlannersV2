package com.gitee.planners.module.script.mythicmobs

import io.lumine.mythic.bukkit.BukkitAdapter
import io.lumine.mythic.bukkit.MythicBukkit
import org.bukkit.Location
import org.bukkit.entity.Entity

/**
 * MythicMobs 集成对象
 * 供 JS 脚本通过 mythic() 函数调用
 */
object MythicObject {

    private val mythicMobs by lazy {
        try {
            MythicBukkit.inst()
        } catch (e: Exception) {
            null
        }
    }

    fun spawnMob(mobType: String, location: Location): Entity? {
        val mm = mythicMobs ?: return null
        val mob = mm.mobManager.getMythicMob(mobType).orElse(null) ?: return null
        val spawnedMob = mob.spawn(BukkitAdapter.adapt(location), 1.0)
        return spawnedMob?.entity?.bukkitEntity
    }

    fun spawnMob(mobType: String, location: Location, level: Double): Entity? {
        val mm = mythicMobs ?: return null
        val mob = mm.mobManager.getMythicMob(mobType).orElse(null) ?: return null
        val spawnedMob = mob.spawn(BukkitAdapter.adapt(location), level)
        return spawnedMob?.entity?.bukkitEntity
    }

    fun isMythicMob(entity: Entity): Boolean {
        val mm = mythicMobs ?: return false
        return try {
            mm.apiHelper.isMythicMob(entity)
        } catch (e: Exception) {
            false
        }
    }
}
