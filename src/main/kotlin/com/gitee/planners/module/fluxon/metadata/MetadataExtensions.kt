package com.gitee.planners.module.fluxon.metadata

import com.gitee.planners.api.PlayerTemplateAPI.plannersTemplate
import com.gitee.planners.api.common.entity.ProxyBukkitEntity
import com.gitee.planners.api.common.metadata.EntityMetadataManager
import com.gitee.planners.api.common.metadata.MetadataTypeToken
import com.gitee.planners.api.common.metadata.metadataValue
import com.gitee.planners.module.fluxon.FluxonFunctionContext
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
    private fun init() {
        val runtime = FluxonScriptCache.runtime

        // hasMetadata(key, [entity]) -> boolean
        runtime.registerFunction("hasMetadata", listOf(1, 2)) { ctx ->
            val key = ctx.arguments[0]?.toString() ?: return@registerFunction false
            val entity = ctx.getEntityArg(1)
            getContainer(entity)[key] != null
        }

        // getMetadata(key, [entity]) -> object
        runtime.registerFunction("getMetadata", listOf(1, 2)) { ctx ->
            val key = ctx.arguments[0]?.toString() ?: return@registerFunction null
            val entity = ctx.getEntityArg(1)
            getContainer(entity)[key]?.any()
        }

        // setMetadata(key, value, [entity]) -> void
        runtime.registerFunction("setMetadata", listOf(2, 3)) { ctx ->
            val key = ctx.arguments[0]?.toString() ?: return@registerFunction null
            val value = ctx.arguments[1] ?: return@registerFunction null
            val entity = ctx.getEntityArg(2)
            getContainer(entity)[key] = metadataValue(value)
            null
        }

        // setMetadataWithTimeout(key, value, timeout, [entity]) -> void
        runtime.registerFunction("setMetadataWithTimeout", listOf(3, 4)) { ctx ->
            val key = ctx.arguments[0]?.toString() ?: return@registerFunction null
            val value = ctx.arguments[1] ?: return@registerFunction null
            val timeout = (ctx.arguments[2] as Number).toLong()
            val entity = ctx.getEntityArg(3)
            getContainer(entity)[key] = metadataValue(value, timeout)
            null
        }

        // removeMetadata(key, [entity]) -> void
        runtime.registerFunction("removeMetadata", listOf(1, 2)) { ctx ->
            val key = ctx.arguments[0]?.toString() ?: return@registerFunction null
            val entity = ctx.getEntityArg(1)
            getContainer(entity)[key] = MetadataTypeToken.Void()
            null
        }

        // metadataContains(key, searchValue, [entity]) -> boolean
        runtime.registerFunction("metadataContains", listOf(2, 3)) { ctx ->
            val key = ctx.arguments[0]?.toString() ?: return@registerFunction false
            val searchValue = ctx.arguments[1]?.toString() ?: return@registerFunction false
            val entity = ctx.getEntityArg(2)
            getContainer(entity)[key]?.asString()?.contains(searchValue) ?: false
        }
    }

    private fun getContainer(entity: Entity) = if (entity is Player) {
        entity.plannersTemplate
    } else {
        EntityMetadataManager[ProxyBukkitEntity(entity)]
    }

    private fun FluxonFunctionContext.getEntityArg(index: Int): Entity {
        if (arguments.size > index) {
            return arguments[index] as? Entity
                ?: throw IllegalStateException("Argument at $index is not an entity")
        }
        return environment.rootVariables["player"] as? Entity
            ?: throw IllegalStateException("No entity found in environment")
    }
}
