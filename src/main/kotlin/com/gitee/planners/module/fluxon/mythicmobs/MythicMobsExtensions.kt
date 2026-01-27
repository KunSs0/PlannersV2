package com.gitee.planners.module.fluxon.mythicmobs

import com.gitee.planners.module.fluxon.FluxonScriptCache
import io.lumine.mythic.bukkit.BukkitAdapter
import io.lumine.mythic.bukkit.MythicBukkit
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.tabooproject.fluxon.runtime.FunctionSignature
import org.tabooproject.fluxon.runtime.Type
import org.tabooproject.fluxon.runtime.java.Export
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake

/**
 * MythicMobs 集成扩展
 * 使用方式: import mythic; mythic.spawnMob("SkeletonKing", location)
 */
object MythicMobsExtensions {

    @Awake(LifeCycle.LOAD)
    private fun init() {
        val runtime = FluxonScriptCache.runtime
        runtime.registerFunction("pl:mythic", "mythic", FunctionSignature.returns(Type.OBJECT).noParams()) { ctx ->
            ctx.setReturnRef(MythicObject)
        }
        runtime.exportRegistry.registerClass(MythicObject::class.java, "pl:mythic")
    }
}

object MythicObject {
    @JvmField
    val TYPE: Type = Type.fromClass(MythicObject::class.java)

    private val mythicMobs by lazy {
        try {
            MythicBukkit.inst()
        } catch (e: Exception) {
            null
        }
    }

    @Export
    fun spawnMob(mobType: String, location: Location): Entity? {
        val mm = mythicMobs ?: return null
        val mob = mm.mobManager.getMythicMob(mobType).orElse(null) ?: return null
        val spawnedMob = mob.spawn(BukkitAdapter.adapt(location), 1.0)
        return spawnedMob?.entity?.bukkitEntity
    }

    @Export
    fun spawnMob(mobType: String, location: Location, level: Double): Entity? {
        val mm = mythicMobs ?: return null
        val mob = mm.mobManager.getMythicMob(mobType).orElse(null) ?: return null
        val spawnedMob = mob.spawn(BukkitAdapter.adapt(location), level)
        return spawnedMob?.entity?.bukkitEntity
    }

    @Export
    fun isMythicMob(entity: Entity): Boolean {
        val mm = mythicMobs ?: return false
        return try {
            mm.apiHelper.isMythicMob(entity)
        } catch (e: Exception) {
            false
        }
    }
}
