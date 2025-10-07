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

    // 与 ActionState 的 state 参数保持对齐
    private val stateId: PlaceholderString = config.getPlaceholderString(arrayOf("state", "id"), "")

    override fun castAtEntity(data: SkillMetadata, target: AbstractEntity?): Boolean {
        // 仅对存活实体生效
        if (target == null || !target.isLiving) {
            return false
        }

        val id = stateId.get(data, target).trim()
        // 占位符结果可能为空，需要显式校验
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
