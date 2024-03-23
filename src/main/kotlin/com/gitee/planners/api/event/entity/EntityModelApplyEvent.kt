package com.gitee.planners.api.event.entity

import org.bukkit.entity.Entity
import taboolib.platform.type.BukkitProxyEvent

class EntityModelApplyEvent(val entity: Entity,val model: String) : BukkitProxyEvent() {



}
