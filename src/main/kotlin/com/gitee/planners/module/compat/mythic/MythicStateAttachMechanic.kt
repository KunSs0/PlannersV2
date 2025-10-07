package com.gitee.planners.module.compat.mythic

import com.gitee.planners.api.job.target.adaptTarget
import io.lumine.xikage.mythicmobs.adapters.AbstractEntity
import io.lumine.xikage.mythicmobs.io.MythicLineConfig
import io.lumine.xikage.mythicmobs.skills.ITargetedEntitySkill
import io.lumine.xikage.mythicmobs.skills.SkillMechanic
import io.lumine.xikage.mythicmobs.skills.SkillMetadata
import io.lumine.xikage.mythicmobs.skills.placeholders.parsers.PlaceholderDouble
import io.lumine.xikage.mythicmobs.skills.placeholders.parsers.PlaceholderString
import taboolib.common.platform.function.warning
import kotlin.math.roundToLong

class MythicStateAttachMechanic(config: MythicLineConfig) : SkillMechanic(config.line, config), ITargetedEntitySkill {

    // 参数解析与 ActionState 保持一致，兼容多个键位与占位符
    private val stateId: PlaceholderString = config.getPlaceholderString(arrayOf("state", "id"), "")
    private val duration: PlaceholderDouble = config.getPlaceholderDouble(arrayOf("duration", "time", "t"), "-1")
    private val cover: Boolean = config.getBoolean(arrayOf("cover"), true)

    override fun castAtEntity(data: SkillMetadata, target: AbstractEntity?): Boolean {
        // 仅对存活实体生效
        if (target == null || !target.isLiving) {
            return false
        }

        val id = stateId.get(data, target).trim()
        // 占位符可能解析为空字符串，仍需兜底
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
