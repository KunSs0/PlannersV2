package com.gitee.planners.module.action

import com.gitee.planners.module.action.query.QueryScope

/**
 * 为定时任务的每次触发构造独立作用域。
 *
 * 复用捕获时的施法者、目标、技能变量与各读取器，但目标缓存与一次性标记按次新建，
 * 避免 `once` 条件跨 tick 误判、目标缓存跨 tick 复用。
 */
object TimerScopes {

    /**
     * 构造一次触发使用的作用域。
     *
     * @param captured 启动定时任务时捕获的作用域。
     * @return 本次触发的作用域。
     */
    fun fresh(captured: QueryScope): QueryScope {
        return QueryScope(
            sender = captured.sender,
            target = captured.target,
            skill = captured.skill,
            profile = captured.profile,
            variables = captured.variables,
            skillFieldReader = captured.skillFieldReader,
            profileFieldReader = captured.profileFieldReader,
            dataReader = captured.dataReader,
            attrReader = captured.attrReader,
            naturalReader = captured.naturalReader,
            nodeReader = captured.nodeReader,
            label = captured.label + ":timer",
            payload = captured.payload,
            anchoredReader = captured.anchoredReader,
            playbackReader = captured.playbackReader,
            timerCache = captured.timerCache,
        )
    }
}
