package com.gitee.planners.module.action

import com.gitee.planners.module.action.query.QueryScope

/**
 * 为区域周期回调构造作用域。
 *
 * 区域主仍是取数主体与施法者，作用点被替换成区域锚点（固定坐标或跟随实体的实时坐标），
 * 并把当前实例注入 `area.self`，让回调能读到"自己这一份"的半径、圆心与归属。
 */
object AreaScopes {

    /**
     * 构造一次回调使用的作用域。
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
}
