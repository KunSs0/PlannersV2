package com.gitee.planners.api.job.target

import org.bukkit.Bukkit


/**
 * 空容器填充方案
 */
enum class LeastType {

    ONLINE_PLAYERS {
        override fun getTargetContainer(sender: Any?): ProxyTargetContainer {
            return ProxyTargetContainer.of(*Bukkit.getOnlinePlayers().map { it.asTarget() }.toTypedArray())
        }
    },

    // 释放者方案
    SENDER {
        override fun getTargetContainer(sender: Any?): ProxyTargetContainer {
            return if (sender != null) ProxyTargetContainer.of(ProxyTarget.of(sender)) else ProxyTargetContainer()
        }
    },

    // 控制台方案
    CONSOLE {
        override fun getTargetContainer(sender: Any?): ProxyTargetContainer {
            return ProxyTargetContainer.of(Bukkit.getConsoleSender().asTarget())
        }
    },

    // 以技能坐标为原点的方案
    ORIGIN {
        override fun getTargetContainer(sender: Any?): ProxyTargetContainer {
            // Origin needs context, return sender as fallback
            return if (sender != null) ProxyTargetContainer.of(ProxyTarget.of(sender)) else ProxyTargetContainer()
        }
    },

    // 空方案
    EMPTY {
        override fun getTargetContainer(sender: Any?): ProxyTargetContainer {
            return ProxyTargetContainer()
        }
    };

    abstract fun getTargetContainer(sender: Any?): ProxyTargetContainer

}
