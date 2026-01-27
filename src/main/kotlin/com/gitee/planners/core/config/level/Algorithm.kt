package com.gitee.planners.core.config.level

import com.gitee.planners.api.common.Unique
import com.gitee.planners.module.fluxon.FluxonScriptOptions
import com.gitee.planners.module.fluxon.SingletonFluxonScript
import org.bukkit.entity.Player
import taboolib.common5.cint
import taboolib.library.configuration.ConfigurationSection
import java.util.concurrent.CompletableFuture

interface Algorithm {

    val maxLevel: Int

    val minLevel: Int

    fun getExp(player: Player, level: Int): CompletableFuture<Int>


    class Fluxon(val root: ConfigurationSection) : Algorithm, Unique {

        override val id: String = root.name

        override val minLevel = root.getInt("min")

        override val maxLevel = root.getInt("max")

        private val action = SingletonFluxonScript(root.getString("experience"))

        override fun getExp(player: Player, level: Int): CompletableFuture<Int> {
            val options = FluxonScriptOptions.create {
                set("sender", player)
                set("level", level)
            }
            return action.run(options).thenApply { it?.cint ?: Int.MAX_VALUE }
        }

    }

    companion object {

        fun parse(root: ConfigurationSection?): Algorithm? {
            if (root == null) {
                return null
            }
            return Fluxon(root)
        }

    }

}
