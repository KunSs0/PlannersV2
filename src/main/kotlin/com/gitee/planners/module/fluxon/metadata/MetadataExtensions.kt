package com.gitee.planners.module.fluxon.metadata

import com.gitee.planners.api.PlayerTemplateAPI.plannersTemplate
import com.gitee.planners.api.common.entity.ProxyBukkitEntity
import com.gitee.planners.api.common.metadata.EntityMetadataManager
import com.gitee.planners.api.common.metadata.MetadataTypeToken
import com.gitee.planners.api.common.metadata.metadataValue
import com.gitee.planners.module.fluxon.FluxonScriptCache
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.tabooproject.fluxon.runtime.FunctionSignature.returns
import org.tabooproject.fluxon.runtime.Type
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake

/**
 * Metadata 元数据操作扩展
 */
object MetadataExtensions {

    @Awake(LifeCycle.LOAD)
    fun init() {
        val runtime = FluxonScriptCache.runtime

        // hasMeta(key, entity)
        runtime.registerFunction("hasMeta", returns(Type.BOOLEAN).params(Type.STRING, Type.OBJECT)) { ctx ->
            val key = ctx.getString(0) ?: return@registerFunction
            val entity = ctx.getRef(1) as? Entity ?: return@registerFunction
            ctx.setReturnBool(getContainer(entity)[key] != null)
        }

        // getMeta(key, entity)
        runtime.registerFunction("getMeta", returns(Type.OBJECT).params(Type.STRING, Type.OBJECT)) { ctx ->
            val key = ctx.getString(0) ?: return@registerFunction
            val entity = ctx.getRef(1) as? Entity ?: return@registerFunction
            ctx.setReturnRef(getContainer(entity)[key]?.any())
        }

        // setMeta(key, value, entity)
        runtime.registerFunction("setMeta", returns(Type.VOID).params(Type.STRING, Type.OBJECT, Type.OBJECT)) { ctx ->
            val key = ctx.getString(0) ?: return@registerFunction
            val value = ctx.getRef(1) ?: return@registerFunction
            val entity = ctx.getRef(2) as? Entity ?: return@registerFunction
            getContainer(entity)[key] = metadataValue(value)
        }

        // setMetaTimeout(key, value, timeout, entity)
        runtime.registerFunction("setMetaTimeout", returns(Type.VOID).params(Type.STRING, Type.OBJECT, Type.NUMBER, Type.OBJECT)) { ctx ->
            val key = ctx.getString(0) ?: return@registerFunction
            val value = ctx.getRef(1) ?: return@registerFunction
            val timeout = ctx.getAsLong(2)
            val entity = ctx.getRef(3) as? Entity ?: return@registerFunction
            getContainer(entity)[key] = metadataValue(value, timeout)
        }

        // removeMeta(key, entity)
        runtime.registerFunction("removeMeta", returns(Type.VOID).params(Type.STRING, Type.OBJECT)) { ctx ->
            val key = ctx.getString(0) ?: return@registerFunction
            val entity = ctx.getRef(1) as? Entity ?: return@registerFunction
            getContainer(entity)[key] = MetadataTypeToken.Void()
        }
    }

    private fun getContainer(entity: Entity) = if (entity is Player) {
        entity.plannersTemplate
    } else {
        EntityMetadataManager[ProxyBukkitEntity(entity)]
    }
}
