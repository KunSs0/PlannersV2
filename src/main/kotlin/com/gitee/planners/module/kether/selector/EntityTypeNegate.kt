package com.gitee.planners.module.kether.selector

import com.gitee.planners.api.common.script.kether.KetherHelper
import com.gitee.planners.api.job.target.TargetEntity
import com.gitee.planners.module.kether.getTargetContainer
import org.bukkit.entity.EntityType
import taboolib.common.util.asList

object EntityTypeNegate : AbstractSelector("f-type", "not-type") {

    override fun select()= KetherHelper.combinedKetherParser { instance ->
        instance.group(any()).apply(instance) { types ->
            val entityTypes = types!!.asList().map { EntityType.valueOf(it.uppercase().replace(".", "_").trim()) }
            now {
                getTargetContainer().removeIf { it !is TargetEntity<*> || it.getEntityType() in entityTypes }
            }
        }
    }


}