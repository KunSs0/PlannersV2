package com.gitee.planners.module.fluxon.germplugin

import com.gitee.planners.module.fluxon.FluxonScriptCache
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.tabooproject.fluxon.runtime.FunctionSignature
import org.tabooproject.fluxon.runtime.Type
import org.tabooproject.fluxon.runtime.java.Export
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake

/**
 * GermPlugin 集成扩展
 * 使用方式: import germ; germ.playModel(player, "model_id")
 */
object GermPluginExtensions {

    @Awake(LifeCycle.LOAD)
    private fun init() {
        val runtime = FluxonScriptCache.runtime
        runtime.registerFunction("pl:germ", "germ", FunctionSignature.returns(Type.OBJECT).noParams()) { ctx ->
            ctx.setReturnRef(GermObject)
        }
        runtime.exportRegistry.registerClass(GermObject::class.java, "pl:germ")
    }
}

object GermObject {
    @JvmField
    val TYPE: Type = Type.fromClass(GermObject::class.java)

    @Export
    fun playModel(player: Player, model: String) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "gp model cast ${player.name} $model")
    }

    @Export
    fun stopModel(player: Player, model: String) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "gp model stop ${player.name} $model")
    }

    @Export
    fun playEffect(player: Player, effect: String) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "gp effect spawn ${player.name} $effect")
    }

    @Export
    fun stopEffect(player: Player, effect: String) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "gp effect stop ${player.name} $effect")
    }

    @Export
    fun playSound(player: Player, sound: String) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "gp sound play ${player.name} $sound player")
    }
}
