package com.gitee.planners.core.skill.pick

import com.gitee.planners.api.pick.DirectionPickDefinition
import com.gitee.planners.api.pick.EntityPickDefinition
import com.gitee.planners.api.pick.PickCloseReason
import com.gitee.planners.api.pick.PickContext
import com.gitee.planners.api.pick.PickDirectionInput
import com.gitee.planners.api.pick.PickDirectionResult
import com.gitee.planners.api.pick.PickEntityInput
import com.gitee.planners.api.pick.PickEntityResult
import com.gitee.planners.api.pick.PickInput
import com.gitee.planners.api.pick.PickLocResult
import com.gitee.planners.api.pick.PickPositionInput
import com.gitee.planners.api.pick.PickPresentation
import com.gitee.planners.api.pick.PickResult
import com.gitee.planners.api.pick.PickSession
import com.gitee.planners.api.pick.PositionPickDefinition
import org.bukkit.Bukkit
import org.bukkit.FluidCollisionMode
import org.bukkit.Location
import org.bukkit.util.Vector
import kotlin.math.abs
import kotlin.math.acos

/**
 * Planners 自建的指向会话。
 *
 * 会话只做两件事：把开始/结束交给表现后端，以及在确认时按 type 重新校验客户端上报的
 * 快照。几何、地面、视线与夹角全部在服务端复核，客户端上报只作为候选。
 *
 * @property context 会话固定上下文。
 * @property presentation 表现后端。
 */
class PlannersPickSession(
    private val context: PickContext,
    private val presentation: PickPresentation,
) : PickSession {

    /** 视线校验时允许命中点与落点的偏差。 */
    private val lineOfSightTolerance = 0.5

    override fun begin() {
        presentation.open(context)
    }

    override fun update(input: PickInput) {
        // 瞄准期快照只用于表现层预览，服务端不累积状态；确认时重新校验。
    }

    override fun confirm(input: PickInput): PickResult? {
        val definition = context.definition
        if (definition is PositionPickDefinition) {
            return confirmPosition(definition, input)
        }
        if (definition is EntityPickDefinition) {
            return confirmEntity(definition, input)
        }
        if (definition is DirectionPickDefinition) {
            return confirmDirection(definition, input)
        }
        return null
    }

    override fun close(reason: PickCloseReason) {
        presentation.close(context, reason)
    }

    /**
     * 校验位置指向。
     *
     * @param definition 位置定义。
     * @param input 客户端快照。
     * @return 确认结果；校验失败时返回 null。
     */
    private fun confirmPosition(definition: PositionPickDefinition, input: PickInput): PickResult? {
        if (input !is PickPositionInput) {
            return null
        }
        val player = context.player
        val world = player.world
        val location = Location(world, input.x, input.y, input.z)
        val eye = player.eyeLocation
        if (eye.distance(location) > definition.range) {
            return null
        }
        if (!world.getBlockAt(location.clone().add(0.0, -0.05, 0.0)).type.isSolid) {
            return null
        }
        if (!world.getBlockAt(location).isPassable) {
            return null
        }
        if (!world.getBlockAt(location.clone().add(0.0, 1.0, 0.0)).isPassable) {
            return null
        }
        if (definition.requireLineOfSight && !hasLineOfSight(location)) {
            return null
        }
        return PickLocResult(location)
    }

    /**
     * 校验实体指向。
     *
     * 上报没有实体 ID 时不做几何校验，直接按“未选中”返回，由会话层按定义的兜底策略处理。
     *
     * @param definition 实体定义。
     * @param input 客户端快照。
     * @return 确认结果；校验失败或未选中时返回 null。
     */
    private fun confirmEntity(definition: EntityPickDefinition, input: PickInput): PickResult? {
        if (input !is PickEntityInput) {
            return null
        }
        val entityId = input.entityId
        if (entityId == null) {
            return null
        }
        val entity = Bukkit.getEntity(entityId)
        if (entity == null) {
            return null
        }
        if (!entity.isValid || entity.isDead) {
            return null
        }
        val player = context.player
        val eye = player.eyeLocation
        if (eye.distance(entity.location) > definition.range) {
            return null
        }
        val target = entity.location.clone().add(0.0, entity.height / 2.0, 0.0).toVector().subtract(eye.toVector())
        val aim = Vector(input.aimX, input.aimY, input.aimZ)
        if (target.lengthSquared() > 0.0 && aim.lengthSquared() > 0.0) {
            val dot = aim.normalize().dot(target.normalize())
            val angle = Math.toDegrees(acos(dot.coerceIn(-1.0, 1.0)))
            if (angle > definition.maxAngleDegrees) {
                return null
            }
        }
        if (definition.requireLineOfSight && !player.hasLineOfSight(entity)) {
            return null
        }
        val filter = context.filter
        if (filter != null && !filter.accept(entity)) {
            return null
        }
        return PickEntityResult(entity)
    }

    /**
     * 校验方向指向。
     *
     * @param definition 方向定义。
     * @param input 客户端快照。
     * @return 确认结果；校验失败时返回 null。
     */
    private fun confirmDirection(definition: DirectionPickDefinition, input: PickInput): PickResult? {
        if (input !is PickDirectionInput) {
            return null
        }
        val aim = Vector(input.aimX, input.aimY, input.aimZ)
        if (aim.lengthSquared() <= 0.0) {
            return null
        }
        val normalized = aim.normalize()
        // 与 Minecraft 一致：pitch 为负表示向上，这里只校验绝对值上限。
        val pitch = Math.toDegrees(kotlin.math.asin(-normalized.y.coerceIn(-1.0, 1.0)))
        if (abs(pitch) > definition.maxPitchDegrees) {
            return null
        }
        return PickDirectionResult(normalized.x, normalized.y, normalized.z)
    }

    /**
     * 判断施法者眼睛到落点之间是否没有遮挡。
     *
     * @param location 落点。
     * @return 无遮挡时返回 true。
     */
    private fun hasLineOfSight(location: Location): Boolean {
        val eye = context.player.eyeLocation
        val world = context.player.world
        val target = location.clone().add(0.0, 0.1, 0.0)
        val delta = target.toVector().subtract(eye.toVector())
        val distance = delta.length()
        if (distance <= 0.0) {
            return true
        }
        val hit = world.rayTraceBlocks(eye, delta.normalize(), distance, FluidCollisionMode.NEVER, true)
        if (hit == null) {
            return true
        }
        val hitPosition = hit.hitPosition
        if (hitPosition == null) {
            return true
        }
        return hitPosition.distance(location.toVector()) <= lineOfSightTolerance
    }
}