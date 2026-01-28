package com.gitee.planners.module.fluxon.cooldown

import com.gitee.planners.api.Registries
import com.gitee.planners.api.job.Skill
import com.gitee.planners.core.skill.cooler.Cooler
import com.gitee.planners.module.fluxon.FluxonScriptCache
import org.bukkit.entity.Player
import org.tabooproject.fluxon.runtime.FunctionSignature
import org.tabooproject.fluxon.runtime.Type
import org.tabooproject.fluxon.runtime.java.Export
import org.tabooproject.fluxon.runtime.java.Optional
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake

/**
 * Cooldown 冷却系统扩展
 */
object CooldownExtensions {

    @Awake(LifeCycle.LOAD)
    private fun init() {
        val runtime = FluxonScriptCache.runtime
        runtime.registerFunction("pl:cooldown", "cooldown", FunctionSignature.returns(Type.OBJECT).noParams()) { ctx ->
            ctx.setReturnRef(CooldownObject)
        }
        runtime.exportRegistry.registerClass(CooldownObject::class.java, "pl:cooldown")
    }

    object CooldownObject {

        @JvmField
        val TYPE: Type = Type.fromClass(CooldownObject::class.java)

        @Export
        fun get(skillIdOrSkill: Any, @Optional player: Player): Long {
            val skill = resolveSkill(skillIdOrSkill) ?: return 0L
            return Cooler.INSTANCE.get(player, skill)
        }

        @Export
        fun set(skillIdOrSkill: Any, ticks: Int, @Optional player: Player) {
            val skill = resolveSkill(skillIdOrSkill) ?: return
            Cooler.INSTANCE.set(player, skill, ticks)
        }

        @Export
        fun reset(skillIdOrSkill: Any, @Optional player: Player) {
            val skill = resolveSkill(skillIdOrSkill) ?: return
            Cooler.INSTANCE.set(player, skill, 0)
        }

        @Export
        fun has(skillIdOrSkill: Any, @Optional player: Player): Boolean {
            val skill = resolveSkill(skillIdOrSkill) ?: return false
            return Cooler.INSTANCE.get(player, skill) > 0
        }

        private fun resolveSkill(arg: Any?): Skill? {
            return when (arg) {
                is String -> Registries.SKILL.get(arg)
                is Skill -> arg
                else -> null
            }
        }
    }
}
