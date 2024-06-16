package com.gitee.planners.core.config.level

import com.gitee.planners.api.common.Unique
import com.gitee.planners.api.common.script.KetherScriptOptions
import com.gitee.planners.api.common.script.SingletonKetherScript
import org.bukkit.entity.Player
import taboolib.common5.cint
import taboolib.library.configuration.ConfigurationSection
import taboolib.module.kether.runKether
import java.util.concurrent.CompletableFuture

interface Algorithm {

    val maxLevel: Int

    val minLevel: Int

    fun getExp(player: Player, level: Int): CompletableFuture<Int>


    class Kether(val root: ConfigurationSection) : Algorithm, Unique {

        override val id: String = root.name

        override val minLevel = root.getInt("min")

        override val maxLevel = root.getInt("max")

        private val action = SingletonKetherScript(root.getString("experience"))

        override fun getExp(player: Player, level: Int): CompletableFuture<Int> {
            val options = KetherScriptOptions.create {
                this.sender(player)
                this.vars("level" to level)
            }
            return runKether(CompletableFuture.completedFuture(Int.MAX_VALUE)) {
                action.run(options).thenApply { it.cint }
            }!!
        }

    }

    companion object {

        fun parseKether(root: ConfigurationSection?): Algorithm? {
            if (root == null) {
                return null
            }
            return Kether(root)
        }

    }

}
