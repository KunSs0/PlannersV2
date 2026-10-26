package com.gitee.planners.api.template

import com.gitee.planners.api.PlayerTemplateAPI
import com.gitee.planners.api.Registries
import com.gitee.planners.api.event.player.PlayerSetRouteEvent
import com.gitee.planners.core.attribute.AttributeProxy
import com.gitee.planners.core.condition.ConditionMainThread
import com.gitee.planners.core.config.ImmutableRoute
import com.gitee.planners.core.config.level.AlgorithmLevel
import com.gitee.planners.core.database.ProfilePersistence
import com.gitee.planners.core.database.RecordIdentity
import com.gitee.planners.core.player.PlayerRouter
import com.gitee.planners.core.player.PlayerTemplate
import com.gitee.planners.core.player.PlayerRoute
import java.util.concurrent.CompletableFuture

/** 选职、转职只在主线程提交内存，存储不参与业务成功的判定。 */
class ProfileOperatorImpl : ProfileOperator {
    /**
     * 主线程校验职业线、创建稳定身份并发布最终状态，然后独立提交持久化。
     * @param template 当前在线档案。
     * @param route 目标职业阶段配置。
     * @return 内存和完成事件已经发布的阶段，不等待数据库。
     */
    override fun createPlayerRoute(template: PlayerTemplate, route: ImmutableRoute): CompletableFuture<PlayerRoute> {
        return CompletableFuture.supplyAsync({
            val player = template.onlinePlayer
            check(player.isOnline && PlayerTemplateAPI.getOrNull(player.uniqueId) === template) { "玩家档案已经失效" }
            var router = template.playerRouter
            val initial = router == null
            var parentId = -1L
            if (router == null) {
                val definition = Registries.ROUTER.getOrNull(route.routerId)
                val algorithm = definition?.algorithmLevel ?: AlgorithmLevel.default
                val initialLevel = algorithm?.minLevel ?: 1
                router = PlayerRouter(RecordIdentity.next(), template.id, route.routerId, initialLevel, 0, -1L, 0, 0, emptyList())
            } else {
                require(router.routerId == route.routerId) { "玩家已选择 Router '${router.routerId}'" }
                var allowed = false
                for (candidate in router.getNextRoutes()) {
                    if (candidate.id == route.id) {
                        allowed = true
                        break
                    }
                }
                require(allowed) { "Job '${route.id}' 不是当前 Job 的可转职目标" }
                parentId = router.currentRouteId
            }
            // 内存身份立即可用，后续学习与装备不等待 INSERT 生成主键。
            val playerRoute = PlayerRoute(RecordIdentity.next(), route.routerId, parentId, route.id, emptyList(), emptyList())
            router.registerRoute(playerRoute)
            router.setCurrentRoute(playerRoute)
            template.playerRouter = router
            ProfilePersistence.createRoute(router, playerRoute, initial) {
                AttributeProxy.sync(player)
                PlayerSetRouteEvent.Post(template, playerRoute).call()
            }
            playerRoute
        }, ConditionMainThread)
    }
}
