package com.gitee.planners.api.common.metadata

import com.gitee.planners.api.common.AbstractRegistry
import com.gitee.planners.api.common.Registry
import com.gitee.planners.api.common.entity.ProxyBukkitEntity
import com.gitee.planners.api.common.entity.ProxyEntity
import org.bukkit.entity.Entity
import org.bukkit.event.entity.EntityDeathEvent
import taboolib.common.platform.event.EventPriority
import taboolib.common.platform.function.registerBukkitListener

object EntityMetadataManager : AbstractRegistry<ProxyEntity<*>, MetadataContainer>() {

    override fun get(id: ProxyEntity<*>): MetadataContainer {
        if (id.isDead()) {
            error("Entity $id is dead")
        }

        return table.computeIfAbsent(id) { EntityMetadataContainer(it) }
    }

    fun delete(id: ProxyEntity<*>) {
        this.table.remove(id)
    }

    class EntityMetadataContainer(val entity: ProxyEntity<*>) : MetadataContainer(emptyMap()) {

        init {
            // 注册销毁监听器
            if (entity is ProxyBukkitEntity) {
                registerBukkitListener(EntityDeathEvent::class.java, EventPriority.NORMAL, false) {
                    EntityMetadataManager.delete(entity)
                    this.close()
                }
            }
        }

    }

}