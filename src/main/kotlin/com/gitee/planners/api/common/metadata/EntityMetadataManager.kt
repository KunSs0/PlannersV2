package com.gitee.planners.api.common.metadata

import com.gitee.planners.api.common.entity.ProxyBukkitEntity
import com.gitee.planners.api.common.entity.ProxyEntity
import com.gitee.planners.util.builtin.MutableRegistryInMap
import org.bukkit.event.entity.EntityDeathEvent
import taboolib.common.platform.event.EventPriority
import taboolib.common.platform.function.registerBukkitListener

object EntityMetadataManager : MutableRegistryInMap<ProxyEntity<*>, MetadataContainer>() {

    override fun get(key: ProxyEntity<*>): MetadataContainer {
        if (key.isDead()) {
            error("Entity $key is dead")
        }
        if (!this.containsKey(key)) {
            this[key] = EntityMetadataContainer(key)
        }

        return super.get(key)
    }

    fun delete(id: ProxyEntity<*>) {
        this.remove(id)
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
