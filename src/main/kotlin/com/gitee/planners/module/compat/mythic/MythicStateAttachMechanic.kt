package com.gitee.planners.module.compat.mythic

import com.gitee.planners.api.job.target.adaptTarget
import io.lumine.xikage.mythicmobs.adapters.AbstractEntity
import io.lumine.xikage.mythicmobs.io.MythicLineConfig
import io.lumine.xikage.mythicmobs.skills.ITargetedEntitySkill
import io.lumine.xikage.mythicmobs.skills.SkillMechanic
import io.lumine.xikage.mythicmobs.skills.SkillMetadata
import io.lumine.xikage.mythicmobs.skills.placeholders.parsers.PlaceholderDouble
import io.lumine.xikage.mythicmobs.skills.placeholders.parsers.PlaceholderString
import taboolib.common.platform.Ghost
import taboolib.common.platform.function.warning
import kotlin.math.roundToLong

@Ghost
class MythicStateAttachMechanic(config: MythicLineConfig) : SkillMechanic(config.line, config), ITargetedEntitySkill {

    private val stateId: PlaceholderString = config.getPlaceholderString(arrayOf("state", "id"), "")
    private val duration: PlaceholderDouble = config.getPlaceholderDouble(arrayOf("duration", "time", "t"), "-1")
    private val cover: Boolean = config.getBoolean(arrayOf("cover"), true)

    override fun castAtEntity(data: SkillMetadata, target: AbstractEntity?): Boolean {
        // Only applies to living entities
        if (target == null || !target.isLiving) {
            return false
        }

        val id = stateId.get(data, target).trim()
        // Placeholder may resolve to blank, validate before use
        if (id.isEmpty()) {
            warning("MythicStateAttachMechanic: state parameter is empty.")
            return false
        }

        val state = MythicMobsLoader.resolveStateOrWarn(id) ?: return false
        val targetEntity = target.bukkitEntity?.adaptTarget() ?: return false

        val durationMs = runCatching { duration.get(data, target) }.getOrElse { -1.0 }.roundToLong()
        targetEntity.addState(state, durationMs, cover)
        return true
    }
}
