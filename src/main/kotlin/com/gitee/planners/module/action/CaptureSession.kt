package com.gitee.planners.module.action

import com.gitee.planners.module.action.query.QueryScope
import org.bukkit.Location
import java.util.UUID

/**
 * 一次会话目标捕捉：几何参数、已捕捉目标与寿命。
 *
 * 捕捉按周期把“上一采样点到当前采样点”的移动包络盒投到世界里，命中目标去重后累积；
 * 结束时由业务流程一次结算。目标以 [ActionTarget] 快照保存，读取时按活跃状态过滤。
 *
 * @property instance 实例 ID，创建时生成且唯一。
 * @property id 业务分类名。
 * @property ownerId 捕捉者唯一 ID。
 * @property dimensions 盒体 full 尺寸：宽、高、长。
 * @property limit 累积上限；0 表示不限。
 * @property ticks 会话总采样次数。
 * @property period 采样周期（tick）。
 * @property expiresAt 过期时刻（毫秒时间戳）。
 */
class CaptureSession(
    val instance: UUID,
    val id: String,
    val ownerId: UUID,
    val dimensions: DoubleArray,
    val limit: Int,
    val ticks: Int,
    val period: Int,
    val expiresAt: Long,
) {

    /** 周期任务句柄；未启用采样时为 null。 */
    var task: ActionTask? = null

    /** 启动时捕获的作用域。 */
    var captured: QueryScope? = null

    /** 流程跳转入口。 */
    var runner: ((String, QueryScope) -> Unit)? = null

    /** 每次采样后回调的流程名。 */
    var tickFlow: String? = null

    /** 捕捉到新目标时回调的流程名。 */
    var hitFlow: String? = null

    /** 结束时回调一次的流程名。 */
    var endFlow: String? = null

    /** 上一次采样时的锚点坐标。 */
    var previous: Location? = null

    /** 上一次采样时的中心偏移。 */
    var previousOffset: DoubleArray = doubleArrayOf(0.0, 0.0, 0.0)

    /** 已完成的采样次数。 */
    var elapsed: Int = 0

    /** 是否已因达到上限而结束。 */
    var closed: Boolean = false

    private val capturedTargets = LinkedHashMap<UUID, ActionTarget>()

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
     * 判断是否已达到累积上限。
     *
     * @return 达到上限时返回 true。
     */
    fun reachedLimit(): Boolean {
        return limit > 0 && capturedTargets.size >= limit
    }

    /**
     * 累积一个目标，重复目标只保留第一次。
     *
     * @param target 目标快照。
     * @return 首次纳入时返回 true。
     */
    fun add(target: ActionTarget): Boolean {
        val entity = target.bukkitEntity()
        if (entity == null) {
            return false
        }
        if (capturedTargets.containsKey(entity.uniqueId)) {
            return false
        }
        capturedTargets[entity.uniqueId] = target
        return true
    }

    /**
     * 取已捕捉目标，按首次捕捉顺序返回并过滤已失效目标。
     *
     * @return 目标列表。
     */
    fun targets(): List<ActionTarget> {
        val result = ArrayList<ActionTarget>(capturedTargets.size)
        for (target in capturedTargets.values) {
            if (target.isActive()) {
                result.add(target)
            }
        }
        return result
    }

    /**
     * 取已捕捉目标数量。
     *
     * @return 数量。
     */
    fun count(): Int {
        return capturedTargets.size
    }

    /**
     * 取消周期任务。
     */
    fun cancelTask() {
        val current = task
        if (current == null) {
            return
        }
        current.cancel()
        task = null
    }
}
