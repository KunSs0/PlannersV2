package com.gitee.planners.core.config.level

/**
 * 等级变化时执行的命令回调。
 *
 * 历史上支持 `nova:` 前缀的脚本回调，脚本运行时已移除；现在只接受控制台命令，
 * 需要复杂逻辑时应改为技能或职业配置里的动作流程。
 *
 * @property command 控制台命令。
 */
class LevelCallback private constructor(
    val command: String,
) {

    companion object {

        /**
         * 解析等级回调配置。
         *
         * @param source 控制台命令文本。
         * @return 已解析回调；空文本返回 null。
         * @throws IllegalArgumentException 使用已移除的 `nova:` 前缀时抛出。
         */
        fun parse(source: String): LevelCallback? {
            val value = source.trim()
            if (value.isEmpty()) {
                return null
            }
            if (value.startsWith("nova:")) {
                throw IllegalArgumentException(
                    "等级回调不再支持 'nova:' 脚本写法，请改用控制台命令或动作流程：$value",
                )
            }
            return LevelCallback(value)
        }
    }
}
