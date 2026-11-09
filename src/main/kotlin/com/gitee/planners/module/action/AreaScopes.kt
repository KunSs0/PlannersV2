package com.gitee.planners.module.action

import com.gitee.planners.module.action.query.QueryScope
import org.bukkit.entity.LivingEntity

/**
 * 为区域回调构造作用域。
 *
 * 周期回调：施法者是取数主体，作用点被替换成区域锚点。
 * 受伤回调：施法者仍是取数主体（数值与技能等级取区域主），作用点是受伤成员，
 * 攻击者、承伤值与来源通过载荷进入 `<skill.var.*>`。
 */
object AreaScopes {

    /**
     * 构造一次周期回调使用的作用域。
     *
     * @param captured 启动区域时捕获的作用域。
     * @param instance 本次触发对应的区域实例。
     * @return 回调作用域；锚点不可用时返回 null。
     */
    fun tick(captured: QueryScope, instance: AreaInstance): QueryScope? {
        val center = instance.center()
        if (center == null) {
            return null
        }
        return QueryScope(
            sender = captured.sender,
            caster = captured.caster,
            target = center,
            skill = captured.skill,
            profile = captured.profile,
            variables = captured.variables,
            skillFieldReader = captured.skillFieldReader,
            profileFieldReader = captured.profileFieldReader,
            dataReader = captured.dataReader,
            attrReader = captured.attrReader,
            naturalReader = captured.naturalReader,
            nodeReader = captured.nodeReader,
            label = captured.label + ":area:" + instance.id,
            payload = captured.payload,
            anchoredReader = captured.anchoredReader,
            playbackReader = captured.playbackReader,
            timerCache = captured.timerCache,
            area = instance,
        )
    }

    /**
     * 构造一次受伤回调使用的作用域。
     *
     * @param captured 启动区域时捕获的作用域。
     * @param instance 触发回调的区域实例。
     * @param victim 受伤成员。
     * @param attacker 施加者；环境伤害为 null。
     * @param damage 本次实际承伤。
     * @param cause 业务来源；原版环境伤害为 null。
     * @return 回调作用域。
     */
    fun damaged(
        captured: QueryScope,
        instance: AreaInstance,
        victim: LivingEntity,
        attacker: LivingEntity?,
        damage: Double,
        cause: String?,
    ): QueryScope {
        val variables = LinkedHashMap<String, Any?>(captured.variables)
        variables["victim"] = victim
        variables["attacker"] = attacker
        variables["damage"] = damage
        variables["cause"] = cause
        return QueryScope(
            sender = captured.sender,
            caster = captured.caster,
            target = victim,
            skill = captured.skill,
            profile = captured.profile,
            variables = variables,
            skillFieldReader = captured.skillFieldReader,
            profileFieldReader = captured.profileFieldReader,
            dataReader = captured.dataReader,
            attrReader = captured.attrReader,
            naturalReader = captured.naturalReader,
            nodeReader = captured.nodeReader,
            label = captured.label + ":area:" + instance.id + ":damaged",
            payload = captured.payload,
            anchoredReader = captured.anchoredReader,
            playbackReader = captured.playbackReader,
            timerCache = captured.timerCache,
            area = instance,
        )
    }
}
