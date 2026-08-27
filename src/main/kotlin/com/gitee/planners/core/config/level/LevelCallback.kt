package com.gitee.planners.core.config.level

import com.gitee.planners.module.script.SingletonScript

/**
 * 等级变化时执行的命令或预编译 Nova 回调。
 *
 * @property command 控制台命令；Nova 回调存在时为 null。
 * @property script 预编译 Nova 入口；命令回调存在时为 null。
 */
class LevelCallback private constructor(
    val command: String?,
    val script: SingletonScript?
) {

    companion object {

        /**
         * 解析等级回调配置。
         *
         * @param sourceId 稳定的虚拟源码标识。
         * @param source `nova:` 回调或控制台命令文本。
         * @return 已解析回调；空文本返回 null。
         */
        fun parse(sourceId: String, source: String): LevelCallback? {
            val value = source.trim()
            if (value.isEmpty()) {
                return null
            }
            val normalized = value.trimStart()
            if (normalized.startsWith("nova:")) {
                val script = normalized.removePrefix("nova:").trimStart()
                if (script.isEmpty()) {
                    return null
                }
                return LevelCallback(null, SingletonScript(script, sourceId))
            }
            return LevelCallback(value, null)
        }
    }
}
