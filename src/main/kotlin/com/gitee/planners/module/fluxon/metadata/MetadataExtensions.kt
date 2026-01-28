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

        /**
         * 检查实体是否拥有指定键的元数据
         * @param key 元数据键名
         * @param entity 目标实体（Player 使用 plannersTemplate，其他使用 EntityMetadataManager）
         * @return 是否存在该键
         */
        runtime.registerFunction("hasMeta", returns(Type.BOOLEAN).params(Type.STRING, Type.OBJECT)) { ctx ->
            val key = ctx.getString(0) ?: return@registerFunction
            val entity = ctx.getRef(1) as? Entity ?: return@registerFunction
            ctx.setReturnBool(getContainer(entity)[key] != null)
        }

        /**
         * 获取实体的元数据值
         * @param key 元数据键名
         * @param entity 目标实体
         * @return 元数据值，不存在则返回 null
         */
        runtime.registerFunction("getMeta", returns(Type.OBJECT).params(Type.STRING, Type.OBJECT)) { ctx ->
            val key = ctx.getString(0) ?: return@registerFunction
            val entity = ctx.getRef(1) as? Entity ?: return@registerFunction
            ctx.setReturnRef(getContainer(entity)[key]?.any())
        }

        /**
         * 设置实体的元数据（永久有效）
         * @param key 元数据键名
         * @param value 元数据值
         * @param entity 目标实体
         */
        runtime.registerFunction("setMeta", returns(Type.VOID).params(Type.STRING, Type.OBJECT, Type.OBJECT)) { ctx ->
            val key = ctx.getString(0) ?: return@registerFunction
            val value = ctx.getRef(1) ?: return@registerFunction
            val entity = ctx.getRef(2) as? Entity ?: return@registerFunction
            getContainer(entity)[key] = metadataValue(value)
        }

        /**
         * 设置实体的元数据（带超时自动删除）
         * @param key 元数据键名
         * @param value 元数据值
         * @param timeout 超时时间（tick），到期后自动删除
         * @param entity 目标实体
         */
        runtime.registerFunction("setMetaTimeout", returns(Type.VOID).params(Type.STRING, Type.OBJECT, Type.NUMBER, Type.OBJECT)) { ctx ->
            val key = ctx.getString(0) ?: return@registerFunction
            val value = ctx.getRef(1) ?: return@registerFunction
            val timeout = ctx.getAsLong(2)
            val entity = ctx.getRef(3) as? Entity ?: return@registerFunction
            getContainer(entity)[key] = metadataValue(value, timeout)
        }

        /**
         * 删除实体的元数据
         * @param key 元数据键名
         * @param entity 目标实体
         */
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
