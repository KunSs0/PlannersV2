package com.gitee.planners.module.action

import com.gitee.planners.module.action.query.QueryScope
import org.bukkit.entity.LivingEntity

/**
 * 为会话捕捉的回调构造作用域。
 *
 * 每次回调都以捕捉者作为取数主体，作用点分别是锚点（周期回调）或新捕捉到的目标（命中回调）；
 * 目标缓存与一次性标记按次新建，避免跨 tick 复用。
 */
object CaptureScopes {

    /**
     * 构造一次周期回调使用的作用域。
     *
     * @param captured 启动捕捉时捕获的作用域。
     * @param session 本次触发对应的捕捉实例。
     * @param anchor 本次采样的锚点。
     * @return 回调作用域。
     */
    fun tick(captured: QueryScope, session: CaptureSession, anchor: org.bukkit.Location): QueryScope {
        return build(captured, session, anchor, captured.label + ":capture:" + session.id + ":tick")
    }

    /**
     * 构造一次命中回调使用的作用域。
     *
     * @param captured 启动捕捉时捕获的作用域。
     * @param session 本次触发对应的捕捉实例。
     * @param target 新捕捉到的目标。
     * @return 回调作用域。
     */
    fun hit(captured: QueryScope, session: CaptureSession, target: LivingEntity): QueryScope {
        return build(captured, session, target, captured.label + ":capture:" + session.id + ":hit")
    }

    /**
     * 构造一次结束回调使用的作用域。
     *
     * @param captured 启动捕捉时捕获的作用域。
     * @param session 已结束的捕捉实例。
     * @param anchor 最后一帧的锚点。
     * @return 回调作用域。
     */
    fun end(captured: QueryScope, session: CaptureSession, anchor: org.bukkit.Location): QueryScope {
        return build(captured, session, anchor, captured.label + ":capture:" + session.id + ":end")
    }

    /**
     * 组装回调作用域。
     *
     * @param captured 捕获的作用域。
     * @param session 捕捉实例。
     * @param target 本次作用点。
     * @param label 作用域标签。
     * @return 作用域。
     */
    private fun build(captured: QueryScope, session: CaptureSession, target: Any?, label: String): QueryScope {
        return QueryScope(
            sender = captured.sender,
            caster = captured.caster,
            target = target,
            skill = captured.skill,
            profile = captured.profile,
            variables = captured.variables,
            skillFieldReader = captured.skillFieldReader,
            profileFieldReader = captured.profileFieldReader,
            dataReader = captured.dataReader,
            attrReader = captured.attrReader,
            nodeReader = captured.nodeReader,
            label = label,
            payload = captured.payload,
            anchoredReader = captured.anchoredReader,
            timerCache = captured.timerCache,
        )
    }
}
