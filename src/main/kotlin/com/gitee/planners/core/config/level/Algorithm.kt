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


    class Js(val root: ConfigurationSection) : Algorithm, Unique {

        override val id: String = root.name

        override val minLevel = root.getInt("min")

        override val maxLevel = root.getInt("max")

        private val action = SingletonScript(root.getString("experience"))

        private val callbacks = parseCallbacks(root.getConfigurationSection("callbacks"))

        override fun getExp(player: Player, level: Int): CompletableFuture<Int> {
            val options = ScriptOptions.create {
                it.set("sender", player)
                it.set("level", level)
            }
            return action.run(options).thenApply { it?.cint ?: Int.MAX_VALUE }
        }

        override fun getCallbacks(level: Int): List<LevelCallback> {
            return callbacks[level] ?: emptyList()
        }

        private fun parseCallbacks(section: ConfigurationSection?): Map<Int, List<LevelCallback>> {
            if (section == null) {
                return emptyMap()
            }
            return section.getKeys(false).mapNotNull { key ->
                val level = key.toIntOrNull()
                if (level == null) {
                    warning("Unknown level callback key: $key")
                    return@mapNotNull null
                }
                val callbacks = when {
                    section.isList(key) -> section.getStringList(key)
                    section.isString(key) -> listOfNotNull(section.getString(key))
                    else -> emptyList()
                }.mapNotNull { LevelCallback.parse(it) }
                level to callbacks
            }.toMap()
        }

    }

    companion object {

        fun parse(root: ConfigurationSection?): Algorithm? {
            if (root == null) {
                return null
            }
            return Js(root)
        }

    }

}

class LevelCallback private constructor(
    val command: String?,
    val script: SingletonScript?
) {

    companion object {

        fun parse(source: String): LevelCallback? {
            val value = source.trim()
            if (value.isEmpty()) {
                return null
            }
            val normalized = value.trimStart()
            return if (normalized.startsWith("js:")) {
                val script = normalized.removePrefix("js:").trimStart()
                if (script.isEmpty()) {
                    null
                } else {
                    LevelCallback(null, SingletonScript(script))
                }
            } else {
                LevelCallback(value, null)
            }
        }
    }
}
