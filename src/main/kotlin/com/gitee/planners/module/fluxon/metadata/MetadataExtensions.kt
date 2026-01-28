package com.gitee.planners.module.fluxon.metadata

import com.gitee.planners.api.PlayerTemplateAPI.plannersTemplate
import com.gitee.planners.api.common.entity.ProxyBukkitEntity
import com.gitee.planners.api.common.metadata.EntityMetadataManager
import com.gitee.planners.api.common.metadata.MetadataTypeToken
import com.gitee.planners.api.common.metadata.metadataValue
import com.gitee.planners.module.fluxon.FluxonScriptCache
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.tabooproject.fluxon.runtime.FunctionSignature
import org.tabooproject.fluxon.runtime.Type
import org.tabooproject.fluxon.runtime.java.Export
import org.tabooproject.fluxon.runtime.java.Optional
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake

/**
 * Metadata 元数据操作扩展
 */
object MetadataExtensions {

    @Awake(LifeCycle.LOAD)
    private fun init() {
        val runtime = FluxonScriptCache.runtime
        runtime.registerFunction("pl:metadata", "metadata", FunctionSignature.returns(Type.OBJECT).noParams()) { ctx ->
            ctx.setReturnRef(MetadataObject)
        }
        runtime.exportRegistry.registerClass(MetadataObject::class.java, "pl:metadata")
    }

    object MetadataObject {

        @JvmField
        val TYPE: Type = Type.fromClass(MetadataObject::class.java)

        @Export
        fun has(key: String, @Optional entity: Entity): Boolean {
            return getContainer(entity)[key] != null
        }

        @Export
        fun get(key: String, @Optional entity: Entity): Any? {
            return getContainer(entity)[key]?.any()
        }

        @Export
        fun set(key: String, value: Any, @Optional entity: Entity) {
            getContainer(entity)[key] = metadataValue(value)
        }

        @Export
        fun setWithTimeout(key: String, value: Any, timeout: Long, @Optional entity: Entity) {
            getContainer(entity)[key] = metadataValue(value, timeout)
        }

        @Export
        fun remove(key: String, @Optional entity: Entity) {
            getContainer(entity)[key] = MetadataTypeToken.Void()
        }

        @Export
        fun contains(key: String, searchValue: String, @Optional entity: Entity): Boolean {
            return getContainer(entity)[key]?.asString()?.contains(searchValue) ?: false
        }

        private fun getContainer(entity: Entity) = if (entity is Player) {
            entity.plannersTemplate
        } else {
            EntityMetadataManager[ProxyBukkitEntity(entity)]
        }
    }
}
