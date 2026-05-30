package com.gitee.planners.core.condition

import com.gitee.planners.Planners
import com.gitee.planners.api.event.PluginReloadEvents
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.info

/**
 * 条件注册表。
 * 加载 config.yml → settings.condition，按 key 索引。
 */
object ConditionRegistry {

    private val conditions = linkedMapOf<String, ConditionConfig>()

    /** 初始化，由 Planners.onEnable() 调用 */
    fun init() {
        load()
    }

    /** 重新加载配置 */
    fun reload() {
        conditions.clear()
        load()
    }

    fun get(key: String): ConditionConfig {
        return conditions[key]
            ?: throw IllegalArgumentException("Unknown condition key: $key")
    }

    fun getOrNull(key: String): ConditionConfig? {
        return conditions[key]
    }

    fun contains(key: String): Boolean {
        return conditions.containsKey(key)
    }

    fun keys(): Set<String> = conditions.keys

    fun size(): Int = conditions.size

    @SubscribeEvent
    @Suppress("UNUSED_PARAMETER")
    fun e(e: PluginReloadEvents.Post) {
        reload()
    }

    // ---- 内部 ----

    private fun load() {
        val section = Planners.config.getConfigurationSection("settings.condition") ?: return
        for (key in section.getKeys(false)) {
            val cfg = section.getConfigurationSection(key) ?: continue
            val exper = cfg.getString("exper") ?: continue
            val hint = cfg.getString("hint") ?: continue
            val props: Map<String, Any> = cfg.getConfigurationSection("props")
                ?.getValues(false)?.mapValues { it.value ?: "" } ?: emptyMap()
            val consume = cfg.getString("consume")?.takeIf { it.isNotEmpty() }

            conditions[key] = ConditionConfig(key, exper, props, hint, consume)
        }
        info("[Condition] 已加载 ${conditions.size} 条条件定义")
    }
}
