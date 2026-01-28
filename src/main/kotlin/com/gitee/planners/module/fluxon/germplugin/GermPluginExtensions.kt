package com.gitee.planners.module.fluxon.germplugin

import com.gitee.planners.module.fluxon.FluxonScriptCache
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.tabooproject.fluxon.runtime.FunctionSignature
import org.tabooproject.fluxon.runtime.Type
import org.tabooproject.fluxon.runtime.java.Export
import org.tabooproject.fluxon.runtime.java.Optional
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.Requires

/**
 * GermPlugin 集成扩展
 * 使用方式: import germ; germ.playModel(player, "model_id")
 */
@Requires(classes = ["com.germ.germplugin.GermPlugin"])
object GermPluginExtensions {

    @Awake(LifeCycle.LOAD)
    private fun init() {
        val runtime = FluxonScriptCache.runtime
        runtime.registerFunction("pl:germ", "germ", FunctionSignature.returns(Type.OBJECT).noParams()) { ctx ->
            ctx.setReturnRef(GermObject)
        }
        runtime.exportRegistry.registerClass(GermObject::class.java, "pl:germ")
    }


    object GermObject {

        @JvmField
        val TYPE: Type = Type.fromClass(GermObject::class.java)

        @Export
        fun playModel(model: String, @Optional player: Player) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "gp model cast ${player.name} $model")
        }

        @Export
        fun stopModel(model: String, @Optional player: Player) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "gp model stop ${player.name} $model")
        }

        @Export
        fun playEffect(effect: String, @Optional player: Player) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "gp effect spawn ${player.name} $effect")
        }

        @Export
        fun stopEffect(effect: String, @Optional player: Player) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "gp effect stop ${player.name} $effect")
        }

        @Export
        fun playSound(sound: String, @Optional player: Player) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "gp sound play ${player.name} $sound player")
        }
    }

}
