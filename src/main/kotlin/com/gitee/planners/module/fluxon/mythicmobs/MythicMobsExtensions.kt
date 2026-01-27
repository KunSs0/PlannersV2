package com.gitee.planners.module.fluxon.mythicmobs

import com.gitee.planners.module.fluxon.FluxonScriptCache
import io.lumine.mythic.bukkit.BukkitAdapter
import io.lumine.mythic.bukkit.MythicBukkit
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.tabooproject.fluxon.runtime.FunctionSignature
import org.tabooproject.fluxon.runtime.Type

/**
 * MythicMobs 集成扩展
 * 通过 Location 和 Entity 扩展实现 MythicMobs 功能
 */
object MythicMobsExtensions {

    private val mythicMobs by lazy {
        try {
            MythicBukkit.inst()
        } catch (e: Exception) {
            null
        }
    }

    fun register() {
        val runtime = FluxonScriptCache.runtime

        // Location 生成 MythicMob 扩展
        runtime.registerExtension(Location::class.java)
            .function("spawnMythicMob", FunctionSignature.returns(Type.OBJECT).params(Type.OBJECT)) { ctx ->
                val location = ctx.target ?: return@function
                val mobType = ctx.getRef(0)?.toString() ?: return@function

                val mm = mythicMobs ?: return@function
                val mob = mm.mobManager.getMythicMob(mobType).orElse(null) ?: return@function

                val spawnedMob = mob.spawn(BukkitAdapter.adapt(location), 1.0)
                ctx.setReturnRef(spawnedMob?.entity?.bukkitEntity)
            }

        // Entity 检查是否为 MythicMob 扩展
        runtime.registerExtension(Entity::class.java)
            .function("isMythicMob", FunctionSignature.returns(Type.Z).noParams()) { ctx ->
                val entity = ctx.target ?: return@function
                val mm = mythicMobs ?: return@function
                try {
                    val isMythic = mm.apiHelper.isMythicMob(entity)
                    ctx.setReturnBool(isMythic)
                } catch (e: Exception) {
                    ctx.setReturnBool(false)
                }
            }
    }
}
