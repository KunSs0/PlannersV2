package com.gitee.planners.util

import com.gitee.planners.api.skill.Variable
import org.bukkit.entity.Player
import taboolib.module.kether.runKether

/**
 * 安全转换
 */
inline fun <reified T> Variable.castSafely(player: Player, defaultValue: T? = null): T? {
    return runKether(defaultValue) {
        this.run(player).getNow(defaultValue)
    }!! as T
}
