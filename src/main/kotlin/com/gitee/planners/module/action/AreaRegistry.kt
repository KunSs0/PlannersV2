package com.gitee.planners.module.action

import org.bukkit.Bukkit
import org.bukkit.Location
import java.util.UUID

/**
 * 全局区域注册表。
 *
 * 负责实例的登记、销毁与几何查询；实例之间互相独立，注册表不做任何合并或择优，
 * `count` 只回答"有几个实例覆盖该点"，取舍由业务配置决定。
 *
 * 过期与失效采用惰性清理：每次访问时清理已到期、跟随实体失效或区域主离线的实例。
 */
object AreaRegistry {

    /** 实例表，按实例 ID 索引。 */
    private val instances = LinkedHashMap<UUID, AreaInstance>()

    /**
     * 判断区域主是否仍然有效的入口；单元测试可替换。
     *
     * 默认实现查在线玩家，避免下线玩家的区域继续占用回调。
     */
    var ownerOnline: (UUID) -> Boolean = { ownerId -> isPlayerOnline(ownerId) }

    /**
     * 登记一个实例。
     *
     * @param instance 新实例。
     */
    fun register(instance: AreaInstance) {
        purge()
        instances[instance.instance] = instance
    }

    /**
     * 按实例 ID 移除实例。
     *
     * @param instanceId 实例 ID。
     * @return 移除成功时返回 true。
     */
    fun remove(instanceId: UUID): Boolean {
        val instance = instances.remove(instanceId)
        if (instance == null) {
            return false
        }
        instance.cancelTask()
        return true
    }

    /**
     * 移除某个区域主名下指定分类的全部实例。
     *
     * 只影响该区域主自己的实例，其它玩家的同 id 实例不受影响。
     *
     * @param ownerId 区域主唯一 ID。
     * @param id 业务分类名。
     * @return 移除的实例数量。
     */
    fun stopOwned(ownerId: UUID, id: String): Int {
        purge()
        var removed = 0
        val iterator = instances.values.iterator()
        while (iterator.hasNext()) {
            val instance = iterator.next()
            if (instance.ownerId != ownerId) {
                continue
            }
            if (instance.id != id) {
                continue
            }
            instance.cancelTask()
            iterator.remove()
            removed += 1
        }
        return removed
    }

    /**
     * 清空全部实例，用于插件重载。
     */
    fun clear() {
        for (instance in instances.values) {
            instance.cancelTask()
        }
        instances.clear()
    }

    /**
     * 判断坐标是否落在指定分类的任一实例内。
     *
     * @param position 待判定坐标。
     * @param id 业务分类名。
     * @return 命中任一实例时返回 true。
     */
    fun contains(position: Location, id: String): Boolean {
        purge()
        for (instance in instances.values) {
            if (instance.id != id) {
                continue
            }
            if (instance.contains(position)) {
                return true
            }
        }
        return false
    }

    /**
     * 统计覆盖该坐标的实例数量。
     *
     * @param position 待判定坐标。
     * @param id 业务分类名。
     * @return 覆盖的实例数量。
     */
    fun count(position: Location, id: String): Int {
        purge()
        var total = 0
        for (instance in instances.values) {
            if (instance.id != id) {
                continue
            }
            if (instance.contains(position)) {
                total += 1
            }
        }
        return total
    }

    /**
     * 取覆盖该坐标的实例主。
     *
     * 多个实例同时覆盖时返回最近建立的那个，只用于展示或调试；业务取值应走各自的标记。
     *
     * @param position 待判定坐标。
     * @param id 业务分类名。
     * @return 区域主唯一 ID；没有覆盖时返回 null。
     */
    fun ownerOf(position: Location, id: String): UUID? {
        purge()
        var found: AreaInstance? = null
        for (instance in instances.values) {
            if (instance.id != id) {
                continue
            }
            if (!instance.contains(position)) {
                continue
            }
            found = instance
        }
        if (found == null) {
            return null
        }
        return found.ownerId
    }

    /**
     * 当前实例数量，供测试与诊断使用。
     *
     * @return 实例总数。
     */
    fun size(): Int {
        return instances.size
    }

    /**
     * 清理已到期、跟随实体失效或区域主离线的实例。
     */
    fun purge() {
        val now = System.currentTimeMillis()
        val iterator = instances.values.iterator()
        while (iterator.hasNext()) {
            val instance = iterator.next()
            val expired = instance.isExpired(now)
            val missingCenter = instance.center() == null
            val ownerGone = !ownerOnline(instance.ownerId)
            if (!expired && !missingCenter && !ownerGone) {
                continue
            }
            instance.cancelTask()
            iterator.remove()
        }
    }

    /**
     * 默认的区域主在线判定。
     *
     * @param ownerId 区域主唯一 ID。
     * @return 在线时返回 true。
     */
    private fun isPlayerOnline(ownerId: UUID): Boolean {
        val player = Bukkit.getPlayer(ownerId)
        if (player == null) {
            return false
        }
        return player.isOnline
    }
}
