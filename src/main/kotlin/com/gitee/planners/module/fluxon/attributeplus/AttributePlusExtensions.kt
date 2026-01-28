package com.gitee.planners.module.fluxon.attributeplus

import com.gitee.planners.api.event.player.PlayerDamageEntityEvent
import com.gitee.planners.api.job.target.LeastType
import com.gitee.planners.api.job.target.ProxyTarget
import com.gitee.planners.module.fluxon.FluxonScriptCache
import com.gitee.planners.module.fluxon.getTargetsArg
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.serverct.ersha.api.AttributeAPI
import org.serverct.ersha.attribute.AttributeHandle
import org.serverct.ersha.attribute.data.AttributeData
import org.serverct.ersha.attribute.data.AttributeSource
import org.tabooproject.fluxon.runtime.FunctionSignature.returns
import org.tabooproject.fluxon.runtime.Type
import taboolib.common.LifeCycle
import taboolib.common.Requires
import taboolib.common.platform.Awake
import taboolib.common.platform.function.submit
import taboolib.common.platform.function.warning
import taboolib.common.util.runSync
import taboolib.platform.util.setMeta

/**
 * AttributePlus 属性攻击扩展
 */
@Requires(classes = ["org.serverct.ersha.AttributePlus"])
object AttributePlusExtensions {

    @Awake(LifeCycle.LOAD)
    private fun init() {
        val runtime = FluxonScriptCache.runtime

        /**
         * 使用临时属性对目标进行属性攻击
         * @param attributes 临时属性字符串（逗号分隔，如 "物理攻击: 100, 暴击几率: 50"）
         * @return 总伤害值
         */
        runtime.registerFunction("apAttack", returns(Type.D).params(Type.STRING)) { ctx ->
            val attributes = ctx.getString(0) ?: return@registerFunction
            val targetsArg = ctx.getTargetsArg(-1, LeastType.EMPTY)
            val sourceArg = ctx.getTargetsArg(-1, LeastType.SENDER)
            ctx.setReturnDouble(executeApAttack(attributes, false, targetsArg, sourceArg))
        }

        /**
         * 属性攻击，可选是否使用隔离数据
         * @param attributes 临时属性字符串
         * @param isolation 是否使用隔离数据（true=创建独立副本，不影响实体原属性）
         * @return 总伤害值
         */
        runtime.registerFunction("apAttack", returns(Type.D).params(Type.STRING, Type.BOOLEAN)) { ctx ->
            val attributes = ctx.getString(0) ?: return@registerFunction
            val isolation = ctx.getBool(1)
            val targetsArg = ctx.getTargetsArg(-1, LeastType.EMPTY)
            val sourceArg = ctx.getTargetsArg(-1, LeastType.SENDER)
            ctx.setReturnDouble(executeApAttack(attributes, isolation, targetsArg, sourceArg))
        }

        /**
         * 对指定目标进行属性攻击
         * @param attributes 临时属性字符串
         * @param isolation 是否使用隔离数据
         * @param targets 攻击目标（支持 Entity/ProxyTarget/容器）
         * @return 总伤害值
         */
        runtime.registerFunction("apAttack", returns(Type.D).params(Type.STRING, Type.BOOLEAN, Type.OBJECT)) { ctx ->
            val attributes = ctx.getString(0) ?: return@registerFunction
            val isolation = ctx.getBool(1)
            val targetsArg = ctx.getTargetsArg(2, LeastType.EMPTY)
            val sourceArg = ctx.getTargetsArg(-1, LeastType.SENDER)
            ctx.setReturnDouble(executeApAttack(attributes, isolation, targetsArg, sourceArg))
        }

        /**
         * 指定攻击来源进行属性攻击
         * @param attributes 临时属性字符串
         * @param isolation 是否使用隔离数据
         * @param targets 攻击目标
         * @param source 攻击来源实体
         * @return 总伤害值
         */
        runtime.registerFunction("apAttack", returns(Type.D).params(Type.STRING, Type.BOOLEAN, Type.OBJECT, Type.OBJECT)) { ctx ->
            val attributes = ctx.getString(0) ?: return@registerFunction
            val isolation = ctx.getBool(1)
            val targetsArg = ctx.getTargetsArg(2, LeastType.EMPTY)
            val sourceArg = ctx.getTargetsArg(3, LeastType.SENDER)
            ctx.setReturnDouble(executeApAttack(attributes, isolation, targetsArg, sourceArg))
        }
    }

    /**
     * 执行 AttributePlus 属性攻击
     * @param attributes 临时属性字符串（逗号分隔，如 "物理攻击: 100, 暴击几率: 50"）
     * @param isolation 是否使用隔离数据（true=创建独立副本，不影响实体原属性）
     * @param targetsArg 攻击目标容器
     * @param sourceArg 攻击来源容器
     * @return 总伤害值
     */
    private fun executeApAttack(
        attributes: String,
        isolation: Boolean,
        targetsArg: com.gitee.planners.api.job.target.ProxyTargetContainer,
        sourceArg: com.gitee.planners.api.job.target.ProxyTargetContainer
    ): Double {
        val sender = sourceArg.filterIsInstance<ProxyTarget.BukkitEntity>()
            .firstOrNull()?.instance as? LivingEntity

        if (sender == null) {
            warning("apAttack: source not correctly defined")
            return 0.0
        }

        val data: AttributeData = if (isolation) {
            AttributeData.create(sender)
        } else {
            AttributeAPI.getAttrData(sender)
        }

        data.operationAttribute(
            AttributeAPI.getAttributeSource(attributes.split(",")),
            AttributeSource.OperationType.ADD,
            "planners_skill"
        )

        var totalDamage = 0.0

        targetsArg
            .filterIsInstance<ProxyTarget.BukkitEntity>()
            .map { it.instance }
            .filterIsInstance<LivingEntity>()
            .filter { it != sender }
            .forEach { entity ->
                val env = EntityDamageByEntityEvent(
                    sender, entity,
                    EntityDamageEvent.DamageCause.CUSTOM, 0.0
                )

                val handle = runSync {
                    AttributeHandle(data, AttributeAPI.getAttrData(entity))
                }.init(env, false, true).handleAttackOrDefenseAttribute()

                if (!env.isCancelled && !handle.isCancelled) {
                    val finalDamage = handle.getDamage(sender)

                    if (finalDamage > entity.health) {
                        entity.setMeta("killer", sender)
                    }

                    handle.sendAttributeMessage()
                    runSync { entity.damage(finalDamage) }

                    if (sender is Player) {
                        PlayerDamageEntityEvent(
                            sender, entity, finalDamage,
                            EntityDamageEvent.DamageCause.CUSTOM
                        ).call()
                    }

                    totalDamage += finalDamage

                    val reflectDamage = handle.getDamage(entity)
                    if (reflectDamage > 0.0) {
                        submit { sender.damage(reflectDamage) }
                    }
                }
            }

        data.takeApiAttribute("planners_skill")
        return totalDamage
    }
}
