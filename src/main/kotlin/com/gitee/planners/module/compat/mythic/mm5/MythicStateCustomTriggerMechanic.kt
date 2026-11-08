package com.gitee.planners.module.compat.mythic.mm5

import com.gitee.planners.api.job.target.asTarget
import com.gitee.planners.core.skill.entity.state.States
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
 * MythicMobs 5 的 `plstatecustomtrigger`：广播自定义触发事件。
 *
 * 只广播事件，不会自动调用状态的生命周期流程。
 */
@Ghost
class MythicStateCustomTriggerMechanic(holder: CustomMechanic, line: String, config: MythicLineConfig) :
    SkillMechanic(holder.manager, line, config), ITargetedEntitySkill {

    /** 触发名，支持 MythicMobs 占位符。 */
    private val triggerName: PlaceholderString = config.getPlaceholderString(arrayOf("name", "trigger", "id"), "")

    override fun castAtEntity(data: SkillMetadata, target: AbstractEntity): SkillResult {
        if (!target.isLiving) {
            return SkillResult.INVALID_TARGET
        }
        val name = triggerName.get(data, target).trim()
        if (name.isEmpty()) {
            warning("MythicStateCustomTriggerMechanic: name parameter is empty.")
            return SkillResult.INVALID_CONFIG
        }
        States.trigger(target.bukkitEntity.asTarget(), name)
        return SkillResult.SUCCESS
    }
}
