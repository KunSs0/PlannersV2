package com.gitee.planners.core.action.selector

import com.gitee.planners.api.job.selector.Selector
import com.gitee.planners.api.job.target.TargetEntity
import com.gitee.planners.core.action.getTargetContainer
import org.bukkit.entity.EntityType
import taboolib.common.util.asList
import taboolib.library.kether.QuestActionParser
import taboolib.module.kether.combinationParser

object EntityType : Selector {

    override fun namespace(): Array<String> {
        return arrayOf("type", "entity-type")
    }

    override fun action(): QuestActionParser {
        return combinationParser { instance ->
            instance.group(any()).apply(instance) { types ->
                val entityTypes = types!!.asList().map { EntityType.valueOf(it.uppercase().replace(".", "_").trim()) }
                now {
                    getTargetContainer().removeIf { it !is TargetEntity<*> || it.getEntityType() !in entityTypes }
                }
            }
        }
    }
}
