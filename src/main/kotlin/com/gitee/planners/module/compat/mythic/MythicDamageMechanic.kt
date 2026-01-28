package com.gitee.planners.module.compat.mythic

import com.gitee.planners.api.damage.DamageCause
import com.gitee.planners.api.damage.ProxyDamage
import io.lumine.xikage.mythicmobs.adapters.AbstractEntity
import io.lumine.xikage.mythicmobs.io.MythicLineConfig
import io.lumine.xikage.mythicmobs.skills.ITargetedEntitySkill
import io.lumine.xikage.mythicmobs.skills.SkillMechanic
import io.lumine.xikage.mythicmobs.skills.SkillMetadata
import io.lumine.xikage.mythicmobs.skills.placeholders.parsers.PlaceholderDouble
import io.lumine.xikage.mythicmobs.skills.placeholders.parsers.PlaceholderString
import org.bukkit.entity.LivingEntity
import taboolib.common.platform.Ghost
import taboolib.common.platform.function.warning

/**
 * MythicMobs 伤害机制 - 使用 ProxyDamage 和自定义 DamageCause
 *
 * 用法: pl-damage{amount=10;cause=SKILL}
 */
@Ghost
class MythicDamageMechanic(config: MythicLineConfig) : SkillMechanic(config.line, config), ITargetedEntitySkill {

    private val amount: PlaceholderDouble = config.getPlaceholderDouble(arrayOf("amount", "a", "damage", "d"), "1")
    private val causeName: PlaceholderString = config.getPlaceholderString(arrayOf("cause", "c", "type", "t"), "SKILL")

    override fun castAtEntity(data: SkillMetadata, target: AbstractEntity?): Boolean {
        if (target == null || !target.isLiving) {
            return false
        }

        val targetEntity = target.bukkitEntity as? LivingEntity ?: return false
        val damageAmount = runCatching { amount.get(data, target) }.getOrElse { 1.0 }
        val cause = causeName.get(data, target).trim()

        // 验证 DamageCause
        val damageCause = DamageCause.ofOrNull(cause)
        if (damageCause == null) {
            warning("MythicDamageMechanic: unknown damage cause '$cause'.")
            return false
        }

        // 获取攻击者
        val source = data.caster?.entity?.bukkitEntity as? LivingEntity

        ProxyDamage.builder()
            .source(source)
            .target(targetEntity)
            .damage(damageAmount)
            .cause(damageCause)
            .build()
            .execute()

        return true
    }
}
