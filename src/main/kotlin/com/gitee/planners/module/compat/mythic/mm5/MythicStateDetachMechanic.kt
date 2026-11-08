package com.gitee.planners.module.compat.mythic.mm5

import com.gitee.planners.api.job.target.asTarget
import com.gitee.planners.api.job.target.detachState
import com.gitee.planners.api.job.target.removeState
import com.gitee.planners.module.compat.mythic.MythicMobsLoader
import io.lumine.mythic.api.adapters.AbstractEntity
import io.lumine.mythic.api.config.MythicLineConfig
import io.lumine.mythic.api.skills.ITargetedEntitySkill
import io.lumine.mythic.api.skills.SkillMetadata
import io.lumine.mythic.api.skills.SkillResult
import io.lumine.mythic.api.skills.placeholders.PlaceholderString
import io.lumine.mythic.core.skills.SkillMechanic
import io.lumine.mythic.core.skills.mechanics.CustomMechanic
import taboolib.common.platform.Ghost
import taboolib.common.platform.function.warning

/**
 * MythicMobs 5 的 `plstatedetach`：移除目标身上的 Planners 状态层数。
 *
 * `layer` 不低于 999 时清空全部层数，与 MythicMobs 4 版本一致。
 */
@Ghost
class MythicStateDetachMechanic(holder: CustomMechanic, line: String, config: MythicLineConfig) :
    SkillMechanic(holder.manager, line, config), ITargetedEntitySkill {

    /** 状态 ID，支持 MythicMobs 占位符。 */
    private val stateId: PlaceholderString = config.getPlaceholderString(arrayOf("state", "id"), "")

    /** 移除层数，默认 999 表示清空。 */
    private val layer: Int = config.getInteger(arrayOf("layer"), 999)

    override fun castAtEntity(data: SkillMetadata, target: AbstractEntity): SkillResult {
        if (!target.isLiving) {
            return SkillResult.INVALID_TARGET
        }
        val id = stateId.get(data, target).trim()
        if (id.isEmpty()) {
            warning("MythicStateDetachMechanic: state parameter is empty.")
            return SkillResult.INVALID_CONFIG
        }
        val state = MythicMobsLoader.resolveStateOrWarn(id) ?: return SkillResult.INVALID_CONFIG
        val targetEntity = target.bukkitEntity.asTarget()
        if (layer >= 999) {
            targetEntity.removeState(state)
        } else {
            targetEntity.detachState(state, layer)
        }
        return SkillResult.SUCCESS
    }
}
