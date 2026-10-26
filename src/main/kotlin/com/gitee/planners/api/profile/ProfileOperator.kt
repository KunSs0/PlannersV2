package com.gitee.planners.api.template

import com.gitee.planners.core.config.ImmutableRoute
import com.gitee.planners.core.player.PlayerTemplate
import com.gitee.planners.core.player.PlayerRoute
import java.util.concurrent.CompletableFuture

/** 职业变更的业务入口，完成阶段只表示主线程内存与通知已经提交。 */
interface ProfileOperator {

    /**
     * 创建并发布当前职业阶段，数据库独立保存。
     * @param template 当前在线档案。
     * @param route 目标职业阶段。
     * @return 内存变更结果，不代表数据库提交结果。
     */
    fun createPlayerRoute(template: PlayerTemplate, route: ImmutableRoute) : CompletableFuture<PlayerRoute>

}
