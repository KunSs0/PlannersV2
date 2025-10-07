package com.gitee.planners.module.compat.mythic

import com.gitee.planners.api.Registries
import com.gitee.planners.core.config.State
import org.bukkit.Bukkit
import taboolib.common.platform.function.warning
import taboolib.common.util.unsafeLazy
import taboolib.common5.cint

object MythicMobsLoader {

    val isEnable by unsafeLazy {
        Bukkit.getPluginManager().getPlugin("MythicMobs") != null
    }

    val version by unsafeLazy {
        Bukkit.getPluginManager().getPlugin("MythicMobs")!!.description.version.split(".").map { it.cint }
    }

    fun resolveStateOrWarn(id: String): State? {
        val state = Registries.STATE.getOrNull(id)
        if (state == null) {
            warning("MythicStateMechanic: state '$id' is missing in registry.")
        }
        return state
    }
}
