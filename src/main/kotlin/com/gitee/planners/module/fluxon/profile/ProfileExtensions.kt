package com.gitee.planners.module.fluxon.profile

import com.gitee.planners.api.PlayerTemplateAPI.plannersTemplate
import com.gitee.planners.api.common.metadata.metadataValue
import com.gitee.planners.core.player.magic.MagicPointProvider.Companion.magicPoint
import com.gitee.planners.module.fluxon.FluxonScriptCache
import org.bukkit.entity.Player
import org.tabooproject.fluxon.runtime.FunctionSignature
import org.tabooproject.fluxon.runtime.Type
import org.tabooproject.fluxon.runtime.java.Export
import org.tabooproject.fluxon.runtime.java.Optional
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake

/**
 * Profile 玩家资料操作扩展
 */
object ProfileExtensions {

    @Awake(LifeCycle.LOAD)
    private fun init() {
        val runtime = FluxonScriptCache.runtime
        runtime.registerFunction("pl:profile", "profile", FunctionSignature.returns(Type.OBJECT).noParams()) { ctx ->
            ctx.setReturnRef(ProfileObject)
        }
        runtime.exportRegistry.registerClass(ProfileObject::class.java, "pl:profile")
    }

    object ProfileObject {

        @JvmField
        val TYPE: Type = Type.fromClass(ProfileObject::class.java)

        @Export
        fun getMagicPoint(@Optional player: Player): Int {
            return player.plannersTemplate.magicPoint
        }

        @Export
        fun setMagicPoint(value: Int, @Optional player: Player) {
            player.plannersTemplate.magicPoint = value
        }

        @Export
        fun takeMagicPoint(amount: Int, @Optional player: Player) {
            player.plannersTemplate.magicPoint -= amount
        }

        @Export
        fun giveMagicPoint(amount: Int, @Optional player: Player) {
            player.plannersTemplate.magicPoint += amount
        }

        @Export
        fun getMaxMagicPoint(@Optional player: Player): Int {
            return player.plannersTemplate["@magic.point.max"]?.asInt() ?: 0
        }

        @Export
        fun setMaxMagicPoint(value: Int, @Optional player: Player) {
            player.plannersTemplate["@magic.point.max"] = metadataValue(value, -1)
        }
    }
}
