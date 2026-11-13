package com.gitee.planners.module.compat.faction

import org.bukkit.entity.LivingEntity

/**
 * 默认友方策略：只把实体自身视为友方。
 *
 * `@enemy` 会先排除自身，因此默认下等价于"全部敌对"；`@ally` 默认只保留自身。
 */
object DefaultEntityFriendlyPolicy : EntityFriendlyPolicy {

    override fun checkEnable(): Boolean {
        return true
    }

    override fun isFriendly(source: LivingEntity, target: LivingEntity): Boolean {
        return source.uniqueId == target.uniqueId
    }
}
