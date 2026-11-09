package com.gitee.planners.module.action

import com.gitee.planners.api.common.facing.EntityFacingProviders
import com.gitee.planners.module.action.query.QueryScope
import org.bukkit.Location
import org.bukkit.entity.LivingEntity
import kotlin.math.cos
import kotlin.math.sin

/**
 * `@forward`：产出施法者身前的一段坐标，可选安全落点解析。
 *
 * 距离按**身体朝向**计算，用于"角色朝向前方 N 格召唤"这类落点需求。
 * `safe=true` 时在该列上下搜索一个可站立点（脚下方块实心、身体两格可通行），
 * 找不到时退回原始水平坐标。
 */
object ForwardTargeter : ActionTargeter {

    override val id = "forward"

    override val aliases = listOf("forward", "position.forward")

    override val scope = ActionScope.SKILL

    override fun validate(arguments: ActionArguments) {
        arguments.require("range")
        arguments.rejectUnknown("range", "search", "safe")
    }

    override fun resolve(scope: QueryScope, arguments: ActionArguments, input: List<ActionTarget>): List<ActionTarget> {
        val source = scope.caster
        if (source !is LivingEntity) {
            return emptyList()
        }
        val range = arguments.requireNumber(scope, "range")
        if (range <= 0.0) {
            return emptyList()
        }
        val search = arguments.number(scope, "search") ?: DEFAULT_SEARCH
        if (search < 0.0) {
            throw IllegalArgumentException("${arguments.path} search 不能为负数。")
        }
        val safe = arguments.boolean(scope, "safe") ?: true
        val origin = source.location
        val world = origin.world
        if (world == null) {
            return emptyList()
        }
        val yaw = Math.toRadians(EntityFacingProviders.getFacingYaw(source).toDouble())
        // Minecraft 朝向：yaw=0 指向 +Z，x 方向取负号。
        val targetX = origin.x - sin(yaw) * range
        val targetZ = origin.z + cos(yaw) * range
        val targetY = if (safe) {
            resolveSafeY(world, targetX, origin.y, targetZ, search) ?: origin.y
        } else {
            origin.y
        }
        val location = Location(world, targetX, targetY, targetZ, origin.yaw, origin.pitch)
        return listOf(ActionTarget.LocationTarget(location))
    }

    /**
     * 在该列上下搜索可站立点。
     *
     * 判定口径：脚下方块实心，且脚部与头部两格可通行。
     *
     * @param world 世界。
     * @param x 目标 X。
     * @param centerY 搜索中心 Y。
     * @param z 目标 Z。
     * @param search 上下搜索格数。
     * @return 可站立点 Y；找不到时返回 null。
     */
    private fun resolveSafeY(world: org.bukkit.World, x: Double, centerY: Double, z: Double, search: Double): Double? {
        val startY = centerY.toInt()
        val limit = search.toInt()
        var offset = 0
        while (offset <= limit) {
            val up = resolveAt(world, x, startY + offset, z)
            if (up != null) {
                return up.toDouble()
            }
            if (offset > 0) {
                val down = resolveAt(world, x, startY - offset, z)
                if (down != null) {
                    return down.toDouble()
                }
            }
            offset += 1
        }
        return null
    }

    /**
     * 判定某一层的三格结构。
     *
     * @param world 世界。
     * @param x 目标 X。
     * @param y 待判定层 Y。
     * @param z 目标 Z。
     * @return 该层可站立时返回其 Y。
     */
    private fun resolveAt(world: org.bukkit.World, x: Double, y: Int, z: Double): Int? {
        val floor = world.getBlockAt(x.toInt(), y - 1, z.toInt())
        if (!floor.type.isSolid) {
            return null
        }
        val feet = world.getBlockAt(x.toInt(), y, z.toInt())
        if (!feet.isPassable) {
            return null
        }
        val head = world.getBlockAt(x.toInt(), y + 1, z.toInt())
        if (!head.isPassable) {
            return null
        }
        return y
    }

    /**
     * 默认上下搜索格数。
     */
    private const val DEFAULT_SEARCH = 3.0
}
