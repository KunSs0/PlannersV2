package com.gitee.planners.module.fluxon.metadata

import com.gitee.planners.api.PlayerTemplateAPI.plannersTemplate
import com.gitee.planners.api.common.entity.ProxyBukkitEntity
import com.gitee.planners.api.common.metadata.EntityMetadataManager
import com.gitee.planners.api.common.metadata.metadataValue
import com.gitee.planners.module.fluxon.FluxonScriptCache
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.tabooproject.fluxon.runtime.FunctionSignature
import org.tabooproject.fluxon.runtime.Type

/**
 * Metadata 元数据操作扩展
 */
object MetadataExtensions {

    fun register() {
        val runtime = FluxonScriptCache.runtime

        // Entity 元数据扩展
        runtime.registerExtension(Entity::class.java)
            .function("hasMetadata", FunctionSignature.returns(Type.Z).params(Type.OBJECT)) { ctx ->
                val entity = ctx.target ?: return@function
                val key = ctx.getRef(0)?.toString() ?: return@function

                val container = if (entity is Player) {
                    entity.plannersTemplate
                } else {
                    EntityMetadataManager[ProxyBukkitEntity(entity)]
                }

                ctx.setReturnBool(container[key] != null)
            }
            .function("getMetadata", FunctionSignature.returns(Type.OBJECT).params(Type.OBJECT)) { ctx ->
                val entity = ctx.target ?: return@function
                val key = ctx.getRef(0)?.toString() ?: return@function

                val container = if (entity is Player) {
                    entity.plannersTemplate
                } else {
                    EntityMetadataManager[ProxyBukkitEntity(entity)]
                }

                val metadata = container[key]
                ctx.setReturnRef(metadata?.any())
            }
            .function("setMetadata", FunctionSignature.returns(Type.VOID).params(Type.OBJECT, Type.OBJECT)) { ctx ->
                val entity = ctx.target ?: return@function
                val key = ctx.getRef(0)?.toString() ?: return@function
                val value = ctx.getRef(1) ?: return@function

                val container = if (entity is Player) {
                    entity.plannersTemplate
                } else {
                    EntityMetadataManager[ProxyBukkitEntity(entity)]
                }

                container[key] = metadataValue(value)
            }
            .function("setMetadata", FunctionSignature.returns(Type.VOID).params(Type.OBJECT, Type.OBJECT, Type.J)) { ctx ->
                val entity = ctx.target ?: return@function
                val key = ctx.getRef(0)?.toString() ?: return@function
                val value = ctx.getRef(1) ?: return@function
                val timeout = ctx.getAsLong(2)

                val container = if (entity is Player) {
                    entity.plannersTemplate
                } else {
                    EntityMetadataManager[ProxyBukkitEntity(entity)]
                }

                container[key] = metadataValue(value, timeout)
            }
            .function("removeMetadata", FunctionSignature.returns(Type.VOID).params(Type.OBJECT)) { ctx ->
                val entity = ctx.target ?: return@function
                val key = ctx.getRef(0)?.toString() ?: return@function

                val container = if (entity is Player) {
                    entity.plannersTemplate
                } else {
                    EntityMetadataManager[ProxyBukkitEntity(entity)]
                }

                container[key] = com.gitee.planners.api.common.metadata.MetadataTypeToken.Void()
            }
            .function("metadataContains", FunctionSignature.returns(Type.Z).params(Type.OBJECT, Type.OBJECT)) { ctx ->
                val entity = ctx.target ?: return@function
                val key = ctx.getRef(0)?.toString() ?: return@function
                val searchValue = ctx.getRef(1)?.toString() ?: return@function

                val container = if (entity is Player) {
                    entity.plannersTemplate
                } else {
                    EntityMetadataManager[ProxyBukkitEntity(entity)]
                }

                val metadata = container[key]
                val contains = metadata?.asString()?.contains(searchValue) ?: false
                ctx.setReturnBool(contains)
            }
    }
}
