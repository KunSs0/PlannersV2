package com.gitee.planners.module.fluxon.profile

import com.gitee.planners.api.PlayerTemplateAPI.plannersTemplate
import com.gitee.planners.api.common.metadata.metadataValue
import com.gitee.planners.core.player.magic.MagicPoint.magicPoint
import com.gitee.planners.module.fluxon.FluxonScriptCache
import org.bukkit.entity.Player
import org.tabooproject.fluxon.runtime.FunctionSignature
import org.tabooproject.fluxon.runtime.Type

/**
 * Profile 玩家资料操作扩展
 */
object ProfileExtensions {

    fun register() {
        val runtime = FluxonScriptCache.runtime

        // Player Profile 扩展
        runtime.registerExtension(Player::class.java)
            .function("getMagicPoint", FunctionSignature.returns(Type.I).noParams()) { ctx ->
                val player = ctx.target ?: return@function
                ctx.setReturnInt(player.plannersTemplate.magicPoint)
            }
            .function("setMagicPoint", FunctionSignature.returns(Type.VOID).params(Type.I)) { ctx ->
                val player = ctx.target ?: return@function
                val value = ctx.getAsInt(0)
                player.plannersTemplate.magicPoint = value
            }
            .function("takeMagicPoint", FunctionSignature.returns(Type.VOID).params(Type.I)) { ctx ->
                val player = ctx.target ?: return@function
                val amount = ctx.getAsInt(0)
                player.plannersTemplate.magicPoint -= amount
            }
            .function("giveMagicPoint", FunctionSignature.returns(Type.VOID).params(Type.I)) { ctx ->
                val player = ctx.target ?: return@function
                val amount = ctx.getAsInt(0)
                player.plannersTemplate.magicPoint += amount
            }
            .function("getMaxMagicPoint", FunctionSignature.returns(Type.I).noParams()) { ctx ->
                val player = ctx.target ?: return@function
                ctx.setReturnInt(player.plannersTemplate["@magic.point.max"]?.asInt() ?: 0)
            }
            .function("setMaxMagicPoint", FunctionSignature.returns(Type.VOID).params(Type.I)) { ctx ->
                val player = ctx.target ?: return@function
                val value = ctx.getAsInt(0)
                player.plannersTemplate["@magic.point.max"] = metadataValue(value, -1)
            }
    }
}
