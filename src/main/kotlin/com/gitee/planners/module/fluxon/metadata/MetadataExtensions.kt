package com.gitee.planners.module.fluxon.metadata

import com.gitee.planners.api.PlayerTemplateAPI.plannersTemplate
import com.gitee.planners.api.common.entity.ProxyBukkitEntity
import com.gitee.planners.api.common.metadata.EntityMetadataManager
import com.gitee.planners.api.common.metadata.MetadataTypeToken
import com.gitee.planners.api.common.metadata.metadataValue
import com.gitee.planners.module.fluxon.FluxonScriptCache
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.tabooproject.fluxon.runtime.FunctionContext
import org.tabooproject.fluxon.runtime.FunctionSignature
import org.tabooproject.fluxon.runtime.Type
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake

/**
 * Metadata 元数据操作扩展
 */
object MetadataExtensions {

    @Awake(LifeCycle.LOAD)
    private fun init() {
        val runtime = FluxonScriptCache.runtime

        // hasMetadata(key) -> boolean (从环境获取entity)
        runtime.registerFunction("hasMetadata", FunctionSignature.returns(Type.Z).params(Type.OBJECT)) { ctx ->
            val key = ctx.getRef(0)?.toString() ?: return@registerFunction
            val entity = getEntityFromEnv(ctx)
            val container = getContainer(entity)
            ctx.setReturnBool(container[key] != null)
        }

        // hasMetadata(key, entity) -> boolean
        runtime.registerFunction("hasMetadata", FunctionSignature.returns(Type.Z).params(Type.OBJECT, Type.OBJECT)) { ctx ->
            val key = ctx.getRef(0)?.toString() ?: return@registerFunction
            val entity = ctx.getRef(1) as? Entity ?: return@registerFunction
            val container = getContainer(entity)
            ctx.setReturnBool(container[key] != null)
        }

        // getMetadata(key) -> object (从环境获取entity)
        runtime.registerFunction("getMetadata", FunctionSignature.returns(Type.OBJECT).params(Type.OBJECT)) { ctx ->
            val key = ctx.getRef(0)?.toString() ?: return@registerFunction
            val entity = getEntityFromEnv(ctx)
            val container = getContainer(entity)
            ctx.setReturnRef(container[key]?.any())
        }

        // getMetadata(key, entity) -> object
        runtime.registerFunction("getMetadata", FunctionSignature.returns(Type.OBJECT).params(Type.OBJECT, Type.OBJECT)) { ctx ->
            val key = ctx.getRef(0)?.toString() ?: return@registerFunction
            val entity = ctx.getRef(1) as? Entity ?: return@registerFunction
            val container = getContainer(entity)
            ctx.setReturnRef(container[key]?.any())
        }

        // setMetadata(key, value) -> void (从环境获取entity)
        runtime.registerFunction("setMetadata", FunctionSignature.returns(Type.VOID).params(Type.OBJECT, Type.OBJECT)) { ctx ->
            val key = ctx.getRef(0)?.toString() ?: return@registerFunction
            val value = ctx.getRef(1) ?: return@registerFunction
            val entity = getEntityFromEnv(ctx)
            val container = getContainer(entity)
            container[key] = metadataValue(value)
        }

        // setMetadata(key, value, entity) -> void
        runtime.registerFunction("setMetadata", FunctionSignature.returns(Type.VOID).params(Type.OBJECT, Type.OBJECT, Type.OBJECT)) { ctx ->
            val key = ctx.getRef(0)?.toString() ?: return@registerFunction
            val value = ctx.getRef(1) ?: return@registerFunction
            val entity = ctx.getRef(2) as? Entity ?: return@registerFunction
            val container = getContainer(entity)
            container[key] = metadataValue(value)
        }

        // setMetadataWithTimeout(key, value, timeout) -> void (从环境获取entity)
        runtime.registerFunction("setMetadataWithTimeout", FunctionSignature.returns(Type.VOID).params(Type.OBJECT, Type.OBJECT, Type.J)) { ctx ->
            val key = ctx.getRef(0)?.toString() ?: return@registerFunction
            val value = ctx.getRef(1) ?: return@registerFunction
            val timeout = ctx.getAsLong(2)
            val entity = getEntityFromEnv(ctx)
            val container = getContainer(entity)
            container[key] = metadataValue(value, timeout)
        }

        // setMetadataWithTimeout(key, value, timeout, entity) -> void
        runtime.registerFunction("setMetadataWithTimeout", FunctionSignature.returns(Type.VOID).params(Type.OBJECT, Type.OBJECT, Type.J, Type.OBJECT)) { ctx ->
            val key = ctx.getRef(0)?.toString() ?: return@registerFunction
            val value = ctx.getRef(1) ?: return@registerFunction
            val timeout = ctx.getAsLong(2)
            val entity = ctx.getRef(3) as? Entity ?: return@registerFunction
            val container = getContainer(entity)
            container[key] = metadataValue(value, timeout)
        }

        // removeMetadata(key) -> void (从环境获取entity)
        runtime.registerFunction("removeMetadata", FunctionSignature.returns(Type.VOID).params(Type.OBJECT)) { ctx ->
            val key = ctx.getRef(0)?.toString() ?: return@registerFunction
            val entity = getEntityFromEnv(ctx)
            val container = getContainer(entity)
            container[key] = MetadataTypeToken.Void()
        }

        // removeMetadata(key, entity) -> void
        runtime.registerFunction("removeMetadata", FunctionSignature.returns(Type.VOID).params(Type.OBJECT, Type.OBJECT)) { ctx ->
            val key = ctx.getRef(0)?.toString() ?: return@registerFunction
            val entity = ctx.getRef(1) as? Entity ?: return@registerFunction
            val container = getContainer(entity)
            container[key] = MetadataTypeToken.Void()
        }

        // metadataContains(key, searchValue) -> boolean (从环境获取entity)
        runtime.registerFunction("metadataContains", FunctionSignature.returns(Type.Z).params(Type.OBJECT, Type.OBJECT)) { ctx ->
            val key = ctx.getRef(0)?.toString() ?: return@registerFunction
            val searchValue = ctx.getRef(1)?.toString() ?: return@registerFunction
            val entity = getEntityFromEnv(ctx)
            val container = getContainer(entity)
            val contains = container[key]?.asString()?.contains(searchValue) ?: false
            ctx.setReturnBool(contains)
        }

        // metadataContains(key, searchValue, entity) -> boolean
        runtime.registerFunction("metadataContains", FunctionSignature.returns(Type.Z).params(Type.OBJECT, Type.OBJECT, Type.OBJECT)) { ctx ->
            val key = ctx.getRef(0)?.toString() ?: return@registerFunction
            val searchValue = ctx.getRef(1)?.toString() ?: return@registerFunction
            val entity = ctx.getRef(2) as? Entity ?: return@registerFunction
            val container = getContainer(entity)
            val contains = container[key]?.asString()?.contains(searchValue) ?: false
            ctx.setReturnBool(contains)
        }
    }

    private fun getContainer(entity: Entity) = if (entity is Player) {
        entity.plannersTemplate
    } else {
        EntityMetadataManager[ProxyBukkitEntity(entity)]
    }

    private fun getEntityFromEnv(ctx: FunctionContext<*>): Entity {
        val find = ctx.environment.rootVariables["player"]
        if (find is Entity) {
            return find
        }
        throw IllegalStateException("No entity found in environment")
    }
}
