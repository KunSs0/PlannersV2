package com.gitee.planners.module.action

import com.gitee.action.api.model.ActionTarget as ApiTarget
import org.bukkit.Location
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player

/**
 * 目标类型双向映射。
 *
 * Action 的目标容器只有玩家、实体、坐标三种形态；技能侧要求实体是 LivingEntity（需要血量与仇恨），
 * 因此映射时非 LivingEntity 的实体目标会被丢弃，而不是降级成其他形态。
 */
object PlannersActionTargets {

    /**
     * 把技能侧的目标对象转换为 Action 目标。
     *
     * @param value 施法者/目标对象，可为 Player、LivingEntity、Location 或其他值
     * @return Action 目标；无法映射时返回 null
     */
    fun toAction(value: Any?): ApiTarget? {
        if (value is Player) {
            return ApiTarget.of(value)
        }
        if (value is LivingEntity) {
            return ApiTarget.of(value)
        }
        if (value is Location) {
            return ApiTarget.of(value)
        }
        return null
    }

    /**
     * 把 Action 目标转换为技能侧目标。
     *
     * @param target Action 目标
     * @return 技能侧目标；实体不是 LivingEntity 时返回 null
     */
    fun toPlanners(target: ApiTarget): ActionTarget? {
        val player = target.getPlayer()
        if (player != null) {
            return ActionTarget.PlayerTarget(player)
        }
        val entity = target.getEntity()
        if (entity != null) {
            if (entity is LivingEntity) {
                return ActionTarget.EntityTarget(entity)
            }
            return null
        }
        val location = target.getLocation()
        if (location != null) {
            return ActionTarget.LocationTarget(location)
        }
        return null
    }

    /**
     * 批量转换为技能侧目标。
     *
     * @param targets Action 目标容器
     * @return 技能侧目标容器，无法映射的项被丢弃
     */
    fun toPlannersAll(targets: List<ApiTarget>): List<ActionTarget> {
        val result = ArrayList<ActionTarget>()
        for (target in targets) {
            val converted = toPlanners(target)
            if (converted == null) {
                continue
            }
            result.add(converted)
        }
        return result
    }

    /**
     * 批量转换为 Action 目标。
     *
     * @param targets 技能侧目标容器
     * @return Action 目标容器
     */
    fun toActionAll(targets: List<ActionTarget>): List<ApiTarget> {
        val result = ArrayList<ApiTarget>()
        for (target in targets) {
            if (target is ActionTarget.PlayerTarget) {
                result.add(ApiTarget.of(target.handle))
                continue
            }
            if (target is ActionTarget.EntityTarget) {
                result.add(ApiTarget.of(target.handle))
                continue
            }
            if (target is ActionTarget.LocationTarget) {
                result.add(ApiTarget.of(target.location))
            }
        }
        return result
    }
}
