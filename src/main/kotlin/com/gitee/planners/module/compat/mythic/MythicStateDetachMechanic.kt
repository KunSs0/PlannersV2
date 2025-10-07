package com.gitee.planners.module.compat.mythic

import com.gitee.planners.api.job.target.adaptTarget
import io.lumine.xikage.mythicmobs.adapters.AbstractEntity
import io.lumine.xikage.mythicmobs.io.MythicLineConfig
import io.lumine.xikage.mythicmobs.skills.ITargetedEntitySkill
import io.lumine.xikage.mythicmobs.skills.SkillMechanic
import io.lumine.xikage.mythicmobs.skills.SkillMetadata
import io.lumine.xikage.mythicmobs.skills.placeholders.parsers.PlaceholderString
import taboolib.common.platform.function.warning

class MythicStateDetachMechanic(config: MythicLineConfig) : SkillMechanic(config.line, config), ITargetedEntitySkill {

    private val stateId: PlaceholderString = config.getPlaceholderString(arrayOf("state", "id"), "")

    override fun castAtEntity(data: SkillMetadata, target: AbstractEntity?): Boolean {
        // Only applies to living entities
        if (target == null || !target.isLiving) {
            return false
        }

        val id = stateId.get(data, target).trim()
        // Placeholder may be empty, guard explicitly
        if (id.isEmpty()) {
            warning("MythicStateDetachMechanic: state parameter is empty.")
            return false
        }

        val state = MythicMobsLoader.resolveStateOrWarn(id) ?: return false
        val targetEntity = target.bukkitEntity?.adaptTarget() ?: return false
        targetEntity.removeState(state)
        return true
    }
}
