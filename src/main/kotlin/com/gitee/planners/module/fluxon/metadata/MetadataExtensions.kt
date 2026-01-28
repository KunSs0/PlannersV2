package com.gitee.planners.module.fluxon.metadata

import com.gitee.planners.api.PlayerTemplateAPI.plannersTemplate
import com.gitee.planners.api.common.entity.ProxyBukkitEntity
import com.gitee.planners.api.common.metadata.EntityMetadataManager
import com.gitee.planners.api.common.metadata.MetadataTypeToken
import com.gitee.planners.api.common.metadata.metadataValue
import com.gitee.planners.module.fluxon.FluxonScriptCache
import com.gitee.planners.module.fluxon.registerFunction
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake

/**
 * Metadata 元数据操作扩展
 */
object MetadataExtensions {

    @Awake(LifeCycle.LOAD)
    fun init() {
        val runtime = FluxonScriptCache.runtime

        runtime.registerFunction("hasMeta", listOf(2)) { ctx ->
            val key = ctx.getAsString(0) ?: return@registerFunction false
            val entity = ctx.getRef(1) as? Entity ?: return@registerFunction false
            getContainer(entity)[key] != null
        }

        runtime.registerFunction("getMeta", listOf(2)) { ctx ->
            val key = ctx.getAsString(0) ?: return@registerFunction null
            val entity = ctx.getRef(1) as? Entity ?: return@registerFunction null
            getContainer(entity)[key]?.any()
        }

        runtime.registerFunction("setMeta", listOf(3)) { ctx ->
            val key = ctx.getAsString(0) ?: return@registerFunction null
            val value = ctx.getRef(1) ?: return@registerFunction null
            val entity = ctx.getRef(2) as? Entity ?: return@registerFunction null
            getContainer(entity)[key] = metadataValue(value)
            null
        }

        runtime.registerFunction("setMetaTimeout", listOf(4)) { ctx ->
            val key = ctx.getAsString(0) ?: return@registerFunction null
            val value = ctx.getRef(1) ?: return@registerFunction null
            val timeout = ctx.getAsLong(2)
            val entity = ctx.getRef(3) as? Entity ?: return@registerFunction null
            getContainer(entity)[key] = metadataValue(value, timeout)
            null
        }

        runtime.registerFunction("removeMeta", listOf(2)) { ctx ->
            val key = ctx.getAsString(0) ?: return@registerFunction null
            val entity = ctx.getRef(1) as? Entity ?: return@registerFunction null
            getContainer(entity)[key] = MetadataTypeToken.Void()
            null
        }
    }

    private fun getContainer(entity: Entity) = if (entity is Player) {
        entity.plannersTemplate
    } else {
        EntityMetadataManager[ProxyBukkitEntity(entity)]
    }
}
