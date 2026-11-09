package com.gitee.planners.module.action

import org.bukkit.Location
import org.bukkit.entity.LivingEntity
import java.util.UUID
import kotlin.math.abs

/**
 * 一个区域实例：几何、归属与寿命。
 *
 * 区域不承载任何业务数值，也不参与实例之间的合并或替换；同一 `id` 可以同时存在多份实例，
 * 业务想结束旧的实例时自行调用 `area.stop`。
 *
 * @property instance 实例 ID，创建时生成且唯一。
 * @property id 业务分类名，用于查询时按类聚合。
 * @property ownerId 建立者唯一 ID，用于 `area.stop` 与失效清理。
 * @property radius 圆形半径。
 * @property height 上下容差。
 * @property fixedCenter 固定圆心；跟随型实例为 null。
 * @property followSource 跟随实体；固定型实例为 null。
 * @property expiresAt 过期时刻（毫秒时间戳）。
 */
class AreaInstance(
    val instance: UUID,
    val id: String,
    val ownerId: UUID,
    val radius: Double,
    val height: Double,
    private val fixedCenter: Location?,
    private val followSource: LivingEntity?,
    val expiresAt: Long,
) {

    /** 周期回调的任务句柄；没有回调时为 null。 */
    var task: ActionTask? = null

    /** 受伤回调流程名；未配置时为 null。 */
    var damagedFlow: String? = null

    /** 启动时捕获的作用域，供回调构造子作用域。 */
    var captured: com.gitee.planners.module.action.query.QueryScope? = null

    /** 流程跳转入口，供受伤回调使用。 */
    var runner: ((String, com.gitee.planners.module.action.query.QueryScope) -> Unit)? = null

    /**
     * 取当前圆心。
     *
     * 固定型返回创建时克隆的坐标；跟随型返回实体的实时坐标，实体失效时返回 null。
     *
     * @return 圆心；跟随实体失效时返回 null。
     */
    fun center(): Location? {
        if (fixedCenter != null) {
            return fixedCenter.clone()
        }
        if (followSource != null) {
            if (!followSource.isValid) {
                return null
            }
            return followSource.location
        }
        return null
    }

    /**
     * 判断实例是否已过期。
     *
     * @param now 当前毫秒时间戳。
     * @return 已到期时返回 true。
     */
    fun isExpired(now: Long): Boolean {
        return now >= expiresAt
    }

    /**
     * 判断坐标是否落在本实例内。
     *
     * 圆柱判定：同一世界、垂直差不超过 [height]、水平距离不超过 [radius]。
     *
     * @param position 待判定坐标。
     * @return 在区域内时返回 true。
     */
    fun contains(position: Location): Boolean {
        val center = center()
        if (center == null) {
            return false
        }
        val world = center.world
        if (world == null) {
            return false
        }
        if (position.world !== world) {
            return false
        }
        val deltaY = abs(position.y - center.y)
        if (deltaY > height) {
            return false
        }
        val deltaX = position.x - center.x
        val deltaZ = position.z - center.z
        return deltaX * deltaX + deltaZ * deltaZ <= radius * radius
    }

    /**
     * 取消周期回调任务。
     */
    fun cancelTask() {
        val task = this.task
        if (task != null) {
            task.cancel()
            this.task = null
        }
    }
}
