package com.gitee.planners.core.database

import com.gitee.planners.api.event.DatabaseInitEvent
import com.gitee.planners.core.player.PlayerTemplate
import com.gitee.planners.util.configNodeTo
import org.bukkit.entity.Player
import taboolib.module.configuration.ConfigNode

/** 数据库后端只负责后台加载及字段快照写入，不承载在线业务成功与回滚。 */
interface Database {

    companion object {

        @ConfigNode("database")
        val option = configNodeTo { DatabaseOption(this) }

        val INSTANCE: Database by lazy {
            when (val type = option.get().use.uppercase()) {
                "LOCAL" -> DatabaseLocal()
                "SQL" -> DatabaseSQL()
                else -> {
                    val event = DatabaseInitEvent(type)
                    event.call()
                    event.instance ?: error("Unsupported database type: $type")
                }
            }
        }

    }

    /**
     * 阻塞读取完整档案，只能由 ProfilePersistence 的后台队列调用。
     * @param player 目标玩家，发布时必须再次验证其登录会话。
     * @return 尚未发布到在线注册表的档案。
     */
    fun getPlayerProfile(player: Player): PlayerTemplate

    /** 后台 JDBC 写入入口，调用方必须通过 ProfilePersistence 提交。 */
    val store: ProfileStore
}
