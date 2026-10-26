package com.gitee.planners.core.database

import com.gitee.planners.api.common.metadata.Metadata
import com.gitee.planners.api.common.metadata.MetadataTypeToken
import com.gitee.planners.core.player.PlayerRouter
import com.gitee.planners.core.player.PlayerRoute
import com.gitee.planners.core.player.PlayerSkill
import com.gitee.planners.core.player.PlayerSkillTreeNodeState
import org.bukkit.Bukkit
import taboolib.common.platform.function.submitAsync
import java.util.concurrent.CompletableFuture
import java.util.logging.Level

/**
 * 在线业务与持久化之间的唯一提交边界。
 *
 * 调用方先完成内存变更，本入口只捕获字段并提交；数据库初始化、连接池、SQL、
 * 生成主键和事务全部位于后台消费者，失败只记录，不修改在线状态。
 */
object ProfilePersistence {
    /** 同一事件发布期间按业务顺序暂存写入，防止监听器保存抢在创建之前。 */
    private val publication = ThreadLocal<MutableList<Write>>()

    /** 一次已经捕获字段的写入，不包含在线对象。 */
    private class Write(val context: String, val operation: () -> Unit)

    private val queue = AsyncDatabaseQueue(
        { task -> submitAsync { task.run() } },
        { context, failure -> Bukkit.getLogger().log(Level.SEVERE, "[Planners] 持久化失败: $context", failure) }
    )

    /**
     * 将读档案排在此前所有保存之后，返回异步结果。
     * @param context 日志上下文。
     * @param read 后台查询，完成后调用方异步调度到主线程发布。
     */
    fun <T> read(context: String, read: () -> T): CompletableFuture<T> {
        // 不在主线程读取 Database.INSTANCE，避免触发同步建表。
        return queue.submit(context, read)
    }

    /**
     * 捕获职业线状态，当前阶段缺省时仅用于初始插入。
     * @param router 业务线程拥有的职业线。
     * @return 不引用可变职业字段的快照。
     */
    fun routerSnapshot(router: PlayerRouter): ProfileSnapshot.Router {
        var current: RecordIdentity? = null
        for (route in router.getRoutes()) {
            if (route.bindingId == router.currentRouteId) {
                current = route.persistenceId
                break
            }
        }
        return ProfileSnapshot.Router(router.persistenceId, router.userId, router.routerId,
            router.level, router.experience, current, router.skillPointsCurrent, router.skillPointsUsed)
    }

    /**
     * 捕获完整技能状态，后续装备变更不会改变已提交快照。
     * @param skill 已提交内存的技能。
     * @return 固定字段与独立持久化身份。
     */
    fun skillSnapshot(skill: PlayerSkill): ProfileSnapshot.Skill {
        return ProfileSnapshot.Skill(skill.persistenceId, skill.id, skill.level, skill.equipped, skill.backpackPage, skill.backpackSlot)
    }

    /**
     * 保存新选职或转职，创建完成前也允许后续业务继续提交。
     * @param router 已提交的职业线。
     * @param route 已提交的新阶段。
     * @param initial 是否需要首次创建职业线。
     * @param notify 主线程完成通知，执行后才提交 SQL。
     */
    fun createRoute(router: PlayerRouter, route: PlayerRoute, initial: Boolean, notify: () -> Unit) {
        val routerData = routerSnapshot(router)
        var parent: RecordIdentity? = null
        for (candidate in router.getRoutes()) {
            if (candidate.bindingId == route.parentId) {
                parent = candidate.persistenceId
                break
            }
        }
        val routeData = ProfileSnapshot.Route(route.persistenceId, router.userId, router.routerId, parent, route.jobId)
        publish("选职 user=${routerData.user}, router=${routerData.identity.runtimeId}, route=${routeData.identity.runtimeId}", {
            Database.INSTANCE.store.createRoute(routerData, routeData, initial)
        }, notify)
    }

    /**
     * 保存主线程已注册的新技能。
     * @param route 技能所属阶段。
     * @param skill 已发布的技能对象。
     */
    fun createSkill(route: PlayerRoute, skill: PlayerSkill) {
        val routeId = route.persistenceId
        val snapshot = skillSnapshot(skill)
        enqueue("创建技能 route=${routeId.runtimeId}, skill=${snapshot.skill}") {
            Database.INSTANCE.store.createSkill(routeId, snapshot)
        }
    }

    /**
     * 将职业线最终字段排队保存，不持有在线对象或对象锁。
     * @param router 需要捕获字段的职业线。
     */
    fun saveRouter(router: PlayerRouter) {
        val snapshot = routerSnapshot(router)
        enqueue("保存职业 user=${snapshot.user}, router=${snapshot.identity.runtimeId}") {
            Database.INSTANCE.store.saveRouter(snapshot)
        }
    }

    /**
     * 将技能最终字段排队保存，装备和等级写入使用同一顺序。
     * @param skill 需要捕获字段的技能。
     */
    fun saveSkill(skill: PlayerSkill) {
        val snapshot = skillSnapshot(skill)
        enqueue("保存技能 skill=${snapshot.identity.runtimeId}") {
            Database.INSTANCE.store.saveSkill(snapshot)
        }
    }

    /**
     * 保存已完成扣费及节点发布的学习结果，不向数据库传递外部资源回调。
     * @param router 完成学习的职业线。
     * @param route 节点所属阶段。
     * @param node 已发布的新节点等级。
     * @param skill 本次新建技能。
     * @param notify 主线程学习完成通知。
     */
    fun saveLearning(router: PlayerRouter, route: PlayerRoute, node: PlayerSkillTreeNodeState, skill: PlayerSkill?, notify: () -> Unit) {
        val routerData = routerSnapshot(router)
        val routeId = route.persistenceId
        val nodeData = ProfileSnapshot.Node(node.persistenceId, node.treeId, node.nodeId, node.level)
        var skillData: ProfileSnapshot.Skill? = null
        if (skill != null) {
            skillData = skillSnapshot(skill)
        }
        val capturedSkill = skillData
        publish("学习 user=${routerData.user}, route=${routeId.runtimeId}, tree=${nodeData.tree}, node=${nodeData.node}, level=${nodeData.level}", {
            Database.INSTANCE.store.saveLearning(routerData, routeId, nodeData, capturedSkill)
        }, notify)
    }

    /**
     * 捕获旧路由身份并异步删除，后台不得遍历当前职业。
     * @param router 已从档案断开的旧职业线。
     * @param notify 主线程清空完成通知。
     */
    fun deleteRouter(router: PlayerRouter, notify: () -> Unit) {
        val routes = ArrayList<RecordIdentity>()
        for (route in router.getRoutes()) {
            routes.add(route.persistenceId)
        }
        val snapshot = ProfileSnapshot.Deletion(router.persistenceId, routes)
        publish("清空职业 user=${router.userId}, router=${snapshot.router.runtimeId}", {
            Database.INSTANCE.store.deleteRouter(snapshot)
        }, notify)
    }

    /**
     * 捕获元数据值并异步保存，避免后台释放主线程的变更集合。
     * @param user 玩家数据库身份。
     * @param key 元数据字段。
     * @param metadata 当前字段值。
     */
    fun saveMetadata(user: Long, key: String, metadata: Metadata) {
        val snapshot: ProfileSnapshot.Metadata
        if (metadata is MetadataTypeToken.Void || metadata.isTimeout()) {
            snapshot = ProfileSnapshot.Metadata(user, key, null, null, 0)
        } else {
            snapshot = ProfileSnapshot.Metadata(user, key, metadata.clazz.name, Metadata.Loader.toJson(metadata), metadata.timeoutTick)
        }
        enqueue("元数据 user=$user, key=$key") {
            Database.INSTANCE.store.saveMetadata(snapshot)
        }
    }

    /** 事件内产生的后继保存先暂存；其他写入立即非阻塞入队。 */
    private fun enqueue(context: String, operation: () -> Unit) {
        val batch = publication.get()
        if (batch == null) {
            queue.submit(context, operation)
        } else {
            batch.add(Write(context, operation))
        }
    }

    /**
     * 先记录当前变更的顺序，发布完成事件后才提交数据库队列。
     * 监听器重入所产生的新变更排在本次变更之后，无需等待任何任务或锁。
     * @param context 当前持久化上下文。
     * @param operation 已捕获快照的数据库操作。
     * @param notify 主线程完成通知。
     */
    private fun publish(context: String, operation: () -> Unit, notify: () -> Unit) {
        val existing = publication.get()
        if (existing != null) {
            existing.add(Write(context, operation))
            notify()
            return
        }
        val batch = ArrayList<Write>()
        publication.set(batch)
        batch.add(Write(context, operation))
        try {
            notify()
        } finally {
            publication.remove()
            for (write in batch) {
                queue.submit(write.context, write.operation)
            }
        }
    }
}
