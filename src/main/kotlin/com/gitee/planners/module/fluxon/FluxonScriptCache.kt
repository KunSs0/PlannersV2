package com.gitee.planners.module.fluxon

import com.gitee.planners.api.event.PluginReloadEvents
import org.tabooproject.fluxon.Fluxon
import org.tabooproject.fluxon.parser.ParsedScript
import org.tabooproject.fluxon.runtime.FluxonRuntime
import taboolib.common.platform.event.SubscribeEvent
import java.util.concurrent.ConcurrentHashMap

/**
 * Fluxon 脚本缓存
 * 管理脚本解析和缓存，避免重复解析
 */
object FluxonScriptCache {

    private val cache = ConcurrentHashMap<String, ParsedScript>()

    /**
     * 获取全局运行时实例
     */
    val runtime: FluxonRuntime
        get() = FluxonRuntime.getInstance()

    /**
     * 获取或解析脚本
     * @param source 脚本源代码
     * @return 解析后的脚本
     */
    fun getOrParse(source: String): ParsedScript {
        return cache.computeIfAbsent(source) {
            try {
                Fluxon.parse(it)
            } catch (e: Exception) {
                throw RuntimeException("Failed to parse script: $it", e)
            }
        }
    }

    /**
     * 清空缓存
     */
    fun clear() {
        cache.clear()
    }

    /**
     * 获取缓存大小
     */
    fun size(): Int = cache.size

    /**
     * 配置重载时清空缓存
     */
    @SubscribeEvent
    private fun onReload(e: PluginReloadEvents.Pre) {
        clear()
    }
}
