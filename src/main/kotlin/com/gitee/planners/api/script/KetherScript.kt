package com.gitee.planners.api.script

import org.bukkit.entity.Player
import taboolib.module.kether.ScriptOptions
import java.util.concurrent.CompletableFuture

interface KetherScript : Script {

    fun run(block: ScriptOptions.ScriptOptionsBuilder.() -> Unit): CompletableFuture<Any?>

    fun run(player: Player, block: ScriptOptions.ScriptOptionsBuilder.() -> Unit = {}): CompletableFuture<Any?> {
        return this.run {
            this.sender(player)
            block(this)
        }
    }

}
