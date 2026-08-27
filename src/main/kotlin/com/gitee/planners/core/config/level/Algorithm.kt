package com.gitee.planners.core.config.level

import com.gitee.planners.api.common.Unique
import com.gitee.planners.module.script.ScriptOptions
import com.gitee.planners.module.script.SingletonScript
import org.bukkit.entity.Player
import taboolib.common.platform.function.warning
import taboolib.common5.cint
import taboolib.library.configuration.ConfigurationSection
import java.util.concurrent.CompletableFuture

interface Algorithm {

    val maxLevel: Int

    val minLevel: Int

    fun getExp(player: Player, level: Int): CompletableFuture<Int>

    fun getCallbacks(level: Int): List<LevelCallback> = emptyList()


    /**
     * 使用预编译 Nova 表达式计算等级经验与回调的算法实现。
     *
     * @property root 等级算法配置节。
     */
    class Nova(val root: ConfigurationSection) : Algorithm, Unique {

        override val id: String = root.name

        override val minLevel = root.getInt("min")

        override val maxLevel = root.getInt("max")

        private val action: SingletonScript
        init {
            val experience = root.getString("experience")
            if (experience == null || experience.isBlank()) {
                throw IllegalArgumentException("Level algorithm '$id' requires an experience expression")
            }
            action = SingletonScript(experience, "level:$id:experience")
        }

        private val callbacks = parseCallbacks(root.getConfigurationSection("callbacks"))

        override fun getExp(player: Player, level: Int): CompletableFuture<Int> {
            val options = ScriptOptions.create {
                it.set("sender", player)
                it.set("level", level)
            }
            return action.run(options).thenApply { result ->
                if (result == null) {
                    throw IllegalStateException("Level algorithm '$id' returned null")
                }
                result.cint
            }
        }

        override fun getCallbacks(level: Int): List<LevelCallback> {
            return callbacks[level] ?: emptyList()
        }

        private fun parseCallbacks(section: ConfigurationSection?): Map<Int, List<LevelCallback>> {
            if (section == null) {
                return emptyMap()
            }
            val result = LinkedHashMap<Int, List<LevelCallback>>()
            for (key in section.getKeys(false)) {
                val level = key.toIntOrNull()
                if (level == null) {
                    warning("Unknown level callback key: $key")
                    continue
                }
                val sources = ArrayList<String>()
                if (section.isList(key)) {
                    sources.addAll(section.getStringList(key))
                } else if (section.isString(key)) {
                    val source = section.getString(key)
                    if (source != null) {
                        sources.add(source)
                    }
                }
                val callbacks = ArrayList<LevelCallback>()
                for (source in sources) {
                    val callback = LevelCallback.parse("level:$id:callback:$key", source)
                    if (callback != null) {
                        callbacks.add(callback)
                    }
                }
                result[level] = callbacks
            }
            return result
        }

    }

    companion object {

        fun parse(root: ConfigurationSection?): Algorithm? {
            if (root == null) {
                return null
            }
            return Nova(root)
        }

    }

}
