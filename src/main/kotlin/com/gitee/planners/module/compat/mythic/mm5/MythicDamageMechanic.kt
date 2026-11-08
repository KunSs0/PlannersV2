package com.gitee.planners.module.compat.mythic.mm5

import com.gitee.planners.api.damage.DamageCause
import com.gitee.planners.api.damage.ProxyDamage
import io.lumine.mythic.api.adapters.AbstractEntity
import io.lumine.mythic.api.config.MythicLineConfig
import io.lumine.mythic.api.skills.ITargetedEntitySkill
import io.lumine.mythic.api.skills.SkillMetadata
import io.lumine.mythic.api.skills.SkillResult
import io.lumine.mythic.api.skills.placeholders.PlaceholderDouble
import io.lumine.mythic.api.skills.placeholders.PlaceholderString
import io.lumine.mythic.core.skills.SkillMechanic
import io.lumine.mythic.core.skills.mechanics.CustomMechanic
import org.bukkit.entity.LivingEntity
import taboolib.common.platform.Ghost
import taboolib.common.platform.function.warning

/**
 * MythicMobs 5 的 `pldamage`：按 Planners 伤害链提交一次伤害。
 *
 * 用法与 MythicMobs 4 版本一致：`pldamage{amount=10;cause=SKILL}`。
 */
@Ghost
class MythicDamageMechanic(holder: CustomMechanic, line: String, config: MythicLineConfig) :
    SkillMechanic(holder.manager, line, config), ITargetedEntitySkill {

    /** 伤害值，支持 MythicMobs 占位符。 */
    private val amount: PlaceholderDouble = config.getPlaceholderDouble(arrayOf("amount", "a", "damage", "d"), 1.0)

    /** 伤害来源 ID，需在 settings.damage-causes 声明。 */
    private val causeName: PlaceholderString = config.getPlaceholderString(arrayOf("cause", "c", "type", "t"), "SKILL")

    override fun castAtEntity(data: SkillMetadata, target: AbstractEntity): SkillResult {
        if (!target.isLiving) {
            return SkillResult.INVALID_TARGET
        }
        val targetEntity = target.bukkitEntity as? LivingEntity ?: return SkillResult.INVALID_TARGET
        val damageAmount = runCatching { amount.get(data, target) }.getOrElse { 1.0 }
        val cause = causeName.get(data, target).trim()
        val damageCause = DamageCause.ofOrNull(cause)
        if (damageCause == null) {
            warning("MythicDamageMechanic: unknown damage cause '$cause'.")
            return SkillResult.INVALID_CONFIG
        }
        val source = data.caster?.entity?.bukkitEntity as? LivingEntity
        ProxyDamage.builder()
            .source(source)
            .target(targetEntity)
            .damage(damageAmount)
            .cause(damageCause)
            .build()
            .execute()
        return SkillResult.SUCCESS
    }
}
