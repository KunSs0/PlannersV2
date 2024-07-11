package com.gitee.planners.core.database

import com.gitee.planners.api.common.metadata.Metadata
import com.gitee.planners.api.event.DatabaseInitEvent
import com.gitee.planners.api.job.Skill
import com.gitee.planners.core.config.ImmutableRoute
import com.gitee.planners.core.player.PlayerTemplate
import com.gitee.planners.core.player.PlayerRoute
import com.gitee.planners.core.player.PlayerSkill
import com.gitee.planners.util.configNodeTo
import org.bukkit.entity.Player
import taboolib.module.configuration.ConfigNode
import java.util.concurrent.CompletableFuture

interface Database {

    companion object {

        @ConfigNode("database")
        val option = configNodeTo { DatabaseOption(this) }

        val INSTANCE: Database by lazy {
            when (val type = option.get().use.uppercase()) {
                "LOCAL" -> TODO("Not implemented")
                "SQL" -> DatabaseSQL()
                else -> {
                    val event = DatabaseInitEvent(type)
                    event.call()
                    event.instance ?: error("Unsupported database type: $type")
                }
            }
        }

    }

    fun getPlayerProfile(player: Player): PlayerTemplate

    fun updateRoute(template: PlayerTemplate)

    fun updateMetadata(template: PlayerTemplate, id: String, metadata: Metadata)

    fun deleteSkill(vararg skill: PlayerSkill)

    fun updateSkill(skill: PlayerSkill)

    fun createPlayerSkill(template: PlayerTemplate, skill: Skill): CompletableFuture<PlayerSkill>

    fun createPlayerJob(template: PlayerTemplate, parentId: Long, route: ImmutableRoute): CompletableFuture<PlayerRoute>

    fun createPlayerJob(template: PlayerTemplate, route: ImmutableRoute): CompletableFuture<PlayerRoute>

}
