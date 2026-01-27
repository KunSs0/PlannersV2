package com.gitee.planners.module.compat.mythic

import com.gitee.planners.api.job.target.asTarget
import com.gitee.planners.core.skill.entity.state.States
import io.lumine.xikage.mythicmobs.adapters.AbstractEntity
import io.lumine.xikage.mythicmobs.io.MythicLineConfig
import io.lumine.xikage.mythicmobs.skills.ITargetedEntitySkill
import io.lumine.xikage.mythicmobs.skills.SkillMechanic
import io.lumine.xikage.mythicmobs.skills.SkillMetadata
import io.lumine.xikage.mythicmobs.skills.placeholders.parsers.PlaceholderString
import taboolib.common.platform.Ghost
import taboolib.common.platform.function.warning

@Ghost
class MythicStateCustomTriggerMechanic(config: MythicLineConfig) : SkillMechanic(config.line, config), ITargetedEntitySkill {

    private val triggerName: PlaceholderString = config.getPlaceholderString(arrayOf("name", "trigger", "id"), "")

    override fun castAtEntity(data: SkillMetadata, target: AbstractEntity?): Boolean {
        if (target == null || !target.isLiving) {
            return false
        }

        val name = triggerName.get(data, target).trim()
        if (name.isEmpty()) {
            warning("MythicStateCustomTriggerMechanic: name parameter is empty.")
            return false
        }

        val targetEntity = target.bukkitEntity?.asTarget() ?: return false
        States.trigger(targetEntity, name)
        return true
    }
}
