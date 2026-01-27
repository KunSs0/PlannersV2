package com.gitee.planners.api.job.target

import org.bukkit.Bukkit


/**
 * 空容器填充方案
 */
enum class LeastType {

    ONLINE_PLAYERS {
        override fun getTargetContainer(sender: Any?): TargetContainer {
            return TargetContainer.of(*Bukkit.getOnlinePlayers().map { it.adaptTarget() }.toTypedArray())
        }
    },

    // 释放者方案
    SENDER {
        override fun getTargetContainer(sender: Any?): TargetContainer {
            return if (sender != null) TargetContainer.of(adaptTarget<Target<*>>(sender)) else TargetContainer()
        }
    },

    // 控制台方案
    CONSOLE {
        override fun getTargetContainer(sender: Any?): TargetContainer {
            return TargetContainer.of(Bukkit.getConsoleSender().adaptTarget())
        }
    },

    // 以技能坐标为原点的方案
    ORIGIN {
        override fun getTargetContainer(sender: Any?): TargetContainer {
            // Origin needs context, return sender as fallback
            return if (sender != null) TargetContainer.of(adaptTarget<Target<*>>(sender)) else TargetContainer()
        }
    },

    // 空方案
    EMPTY {
        override fun getTargetContainer(sender: Any?): TargetContainer {
            return TargetContainer()
        }
    };

    abstract fun getTargetContainer(sender: Any?): TargetContainer

}
