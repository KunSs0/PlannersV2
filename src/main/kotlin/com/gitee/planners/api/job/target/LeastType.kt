package com.gitee.planners.api.job.target

import com.gitee.planners.module.kether.getEnvironmentContext
import org.bukkit.Bukkit
import taboolib.module.kether.ScriptFrame


/**
 * 空容器填充方案
 */
enum class LeastType {

    ONLINE_PLAYERS {
        override fun getTargetContainer(frame: ScriptFrame): TargetContainer {
            return TargetContainer.of(*Bukkit.getOnlinePlayers().map { it.adaptTarget() }.toTypedArray())
        }
    },

    // 释放者方案
    SENDER {
        override fun getTargetContainer(frame: ScriptFrame): TargetContainer {
            return TargetContainer.of(frame.getEnvironmentContext().sender)
        }
    },

    // 控制台方案
    CONSOLE {
        override fun getTargetContainer(frame: ScriptFrame): TargetContainer {
            return TargetContainer.of(Bukkit.getConsoleSender().adaptTarget())
        }
    },

    // 以技能坐标为原点的方案
    ORIGIN {
        override fun getTargetContainer(frame: ScriptFrame): TargetContainer {
            return TargetContainer.of(frame.getEnvironmentContext().origin)
        }
    },

    // 空方案
    EMPTY {
        override fun getTargetContainer(frame: ScriptFrame): TargetContainer {
            return TargetContainer()
        }
    };

    abstract fun getTargetContainer(frame: ScriptFrame): TargetContainer

}
