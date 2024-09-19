package com.gitee.planners.module.kether.bukkit.damageable

import com.gitee.planners.api.common.script.KetherEditor
import com.gitee.planners.api.common.script.kether.CombinationKetherParser
import com.gitee.planners.api.common.script.kether.KetherHelper
import com.gitee.planners.api.common.script.kether.MultipleKetherParser
import com.gitee.planners.api.job.target.TargetBukkitEntity
import com.gitee.planners.module.kether.bukkit.ActionBukkitDamage.setKiller
import com.gitee.planners.module.kether.commandObjectiveOrEmpty
import com.gitee.planners.module.kether.commandObjectiveOrSender
import org.bukkit.Bukkit
import org.bukkit.entity.LivingEntity
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.serverct.ersha.api.AttributeAPI
import org.serverct.ersha.attribute.AttributeHandle
import org.serverct.ersha.attribute.data.AttributeSource
import taboolib.common.platform.function.warning
import java.util.UUID

@CombinationKetherParser.Used
object ActionAttack : MultipleKetherParser("attack") {

    @Suppress("NAME_SHADOWING")
    @KetherEditor.Document("attack --ap <attr: array|string> [at objective:TargetContainer(empty)] [source objective:TargetContainer(sender)]")
    val attributeplus = KetherHelper.combinedKetherParser("attributeplus", "--ap") {
        it.group(text(), commandObjectiveOrEmpty(), commandObjectiveOrSender("source"))
            .apply(it) { attr, objective, killer ->
                val template = attr.split(",")
                val killer = killer.filterIsInstance<TargetBukkitEntity>().firstOrNull()?.instance as? LivingEntity
                now {
                    if (killer == null) {
                        warning("No killer found.")
                        return@now
                    }
                    val trackId = UUID.randomUUID().toString()
                    val source = AttributeAPI.getAttributeSource(attr.split(","))

                    val data = AttributeAPI.getAttrData(killer)
                        .operationAttribute(source, AttributeSource.OperationType.ADD, "@planners_skill")


                    objective.filterIsInstance<TargetBukkitEntity>().filter { it.instance != killer }
                        .forEach { target ->
                            val entity = target.instance as? LivingEntity ?: return@forEach
                            val event =
                                EntityDamageByEntityEvent(
                                    killer,
                                    entity,
                                    EntityDamageEvent.DamageCause.CUSTOM,
                                    0.0
                                )

                            val handle = AttributeHandle(data, AttributeAPI.getAttrData(entity))
                                .init(event, isProjectile = false, isSkillDamage = true)
                                .handleAttackOrDefenseAttribute()
                            if (event.isCancelled || handle.isCancelled) {
                                return@forEach
                            }
                            if (handle.getDamage(killer) > entity.health) {
                                entity.setKiller(killer)
                            }
                            handle.sendAttributeMessage()
                            entity.damage(handle.getDamage(killer))
                            if (handle.getDamage(entity) > 0.0) {
                                killer.damage(handle.getDamage(entity))
                            }
                            // 清空属性
                            data.takeApiAttribute(trackId)
                        }
                }
            }
    }

    @KetherEditor.Document("attack <value: number> [at objective:TargetContainer(empty)] [source objective:TargetContainer(sender)]")
    val main = KetherHelper.combinedKetherParser {
        it.group(double(), commandObjectiveOrEmpty(), commandObjectiveOrSender("source"))
            .apply(it) { value, objective, source ->
                val killer = source.filterIsInstance<TargetBukkitEntity>().firstOrNull()?.instance as? LivingEntity
                now {
                    if (killer == null) {
                        warning("No killer found.")
                        return@now
                    }
                    objective.filterIsInstance<TargetBukkitEntity>().map { it.instance }
                        .filter { it != killer && !it.isDead }.forEach { entity ->
                            if (entity is LivingEntity) {
                                val event = EntityDamageByEntityEvent(
                                    killer,
                                    entity,
                                    EntityDamageEvent.DamageCause.ENTITY_ATTACK,
                                    value
                                )
                                Bukkit.getPluginManager().callEvent(event)
                                if (event.isCancelled) {
                                    return@forEach
                                }
                                if (event.damage > entity.health) {
                                    entity.setKiller(killer)
                                }
                                entity.damage(event.damage)
                            }
                        }
                }
            }
    }


}
