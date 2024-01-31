package com.gitee.planners.module.kether.selector

import com.gitee.planners.api.common.script.kether.KetherHelper
import com.gitee.planners.api.common.script.kether.SimpleKetherParser
import com.gitee.planners.api.job.selector.Selector
import com.gitee.planners.api.job.target.TargetEntity
import com.gitee.planners.module.kether.getTargetContainer
import org.bukkit.entity.EntityType
import taboolib.common.util.asList
import taboolib.module.kether.combinationParser

object EntityType : Selector {

    override fun namespace(): Array<String> {
        return arrayOf("type", "entity-type")
    }

    override fun action() = KetherHelper.combinedKetherParser {
        it.group(any()).apply(it) { types ->
            val entityTypes = types!!.asList().map { EntityType.valueOf(it.uppercase().replace(".", "_").trim()) }
            now {
                getTargetContainer().removeIf { it !is TargetEntity<*> || it.getEntityType() !in entityTypes }
            }
        }
    }
}
