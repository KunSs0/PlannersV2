package com.gitee.planners.module.compat.mythic.mm5

import com.gitee.planners.api.job.target.asTarget
import com.gitee.planners.api.job.target.attachState
import com.gitee.planners.module.compat.mythic.MythicMobsLoader
import io.lumine.mythic.api.adapters.AbstractEntity
import io.lumine.mythic.api.config.MythicLineConfig
import io.lumine.mythic.api.skills.ITargetedEntitySkill
import io.lumine.mythic.api.skills.SkillMetadata
import io.lumine.mythic.api.skills.SkillResult
import io.lumine.mythic.api.skills.placeholders.PlaceholderDouble
import io.lumine.mythic.api.skills.placeholders.PlaceholderString
import io.lumine.mythic.core.skills.SkillMechanic
import io.lumine.mythic.core.skills.mechanics.CustomMechanic
import taboolib.common.platform.Ghost
import taboolib.common.platform.function.warning
import kotlin.math.roundToLong

/**
 * MythicMobs 5 的 `plstateattach`：给目标附加 Planners 状态。
 *
 * 参数与 MythicMobs 4 版本一致：`state`/`id`、`duration`/`time`/`t`、`cover`/`refresh`。
 */
@Ghost
class MythicStateAttachMechanic(holder: CustomMechanic, line: String, config: MythicLineConfig) :
    SkillMechanic(holder.manager, line, config), ITargetedEntitySkill {

    /** 状态 ID，支持 MythicMobs 占位符。 */
    private val stateId: PlaceholderString = config.getPlaceholderString(arrayOf("state", "id"), "")

    /** 持续 tick，默认 -1 表示常驻。 */
    private val duration: PlaceholderDouble = config.getPlaceholderDouble(arrayOf("duration", "time", "t"), -1.0)

    /** 已有该状态时是否刷新剩余时间。 */
    private val refresh: Boolean = config.getBoolean(arrayOf("cover", "refresh"), true)

    override fun castAtEntity(data: SkillMetadata, target: AbstractEntity): SkillResult {
        if (!target.isLiving) {
            return SkillResult.INVALID_TARGET
        }
        val id = stateId.get(data, target).trim()
        if (id.isEmpty()) {
            warning("MythicStateAttachMechanic: state parameter is empty.")
            return SkillResult.INVALID_CONFIG
        }
        val state = MythicMobsLoader.resolveStateOrWarn(id) ?: return SkillResult.INVALID_CONFIG
        val targetEntity = target.bukkitEntity.asTarget()
        val durationTicks = runCatching { duration.get(data, target) }.getOrElse { -1.0 }.roundToLong()
        targetEntity.attachState(state, durationTicks, refresh)
        return SkillResult.SUCCESS
    }
}
