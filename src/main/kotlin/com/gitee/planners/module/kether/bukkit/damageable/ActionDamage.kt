package com.gitee.planners.module.kether.bukkit.damageable

import com.gitee.planners.api.common.script.KetherEditor
import com.gitee.planners.api.common.script.kether.CombinationKetherParser
import com.gitee.planners.api.common.script.kether.KetherHelper
import com.gitee.planners.api.common.script.kether.MultipleKetherParser
import com.gitee.planners.api.job.target.TargetBukkitEntity
import com.gitee.planners.module.kether.*
import org.bukkit.Bukkit
import org.bukkit.entity.Damageable
import org.bukkit.entity.LivingEntity
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.serverct.ersha.api.AttributeAPI
import org.serverct.ersha.attribute.AttributeHandle
import org.serverct.ersha.attribute.data.AttributeData
import org.serverct.ersha.attribute.data.AttributeSource
import taboolib.common.platform.function.warning
import taboolib.library.reflex.Reflex.Companion.getProperty
import taboolib.library.reflex.Reflex.Companion.setProperty
import taboolib.module.nms.MinecraftVersion
import taboolib.platform.util.setMeta


@CombinationKetherParser.Used
object ActionDamage : MultipleKetherParser("damage") {

    @KetherEditor.Document("damage --ap <attr: array|string> [at objective:TargetContainer(empty)] [source objective:TargetContainer(sender)]")
    val attributeplus = KetherHelper.combinedKetherParser("attributeplus", "--ap") {
        it.group(text(),commandObjectiveOrEmpty(),commandObjectiveOrSender("source")).apply(it) { attr, objective, killer ->
            val template = attr.split(",")
            val killer = killer.filterIsInstance<TargetBukkitEntity>().firstOrNull()?.instance as? LivingEntity

            now {
                var alldamage = 0.0
                if (killer == null) {
                    warning("No killer found.")
                    return@now alldamage
                }
                val source = AttributeAPI.getAttributeSource(template)

                val data = AttributeData.create(killer)
                    .operationAttribute(source, AttributeSource.OperationType.ADD, "@planners_skill")
                objective.filterIsInstance<TargetBukkitEntity>().filter { it.instance != killer }.forEach { target ->
                    val entity = target.instance as? LivingEntity ?: return@forEach
                    val event =
                        EntityDamageByEntityEvent(killer, entity, EntityDamageEvent.DamageCause.ENTITY_ATTACK, 0.0)
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
                    alldamage += handle.getDamage(killer)
                    if (handle.getDamage(entity) > 0.0) {
                        killer.damage(handle.getDamage(entity))
                    }
                }
                AttributeAPI.takeSourceAttribute(data,"@planners_skill")
                return@now alldamage
            }
        }
    }

    @KetherEditor.Document("damage <value: number> [at objective:TargetContainer(empty)] [source objective:TargetContainer(sender)]")
    val main = KetherHelper.combinedKetherParser {
            it.group(
                actionDouble(),
                commandObjectiveOrEmpty(),
                commandObjectiveOrSender("source")
            ).apply(it) { value, objective, source ->
                val killer = source.filterIsInstance<TargetBukkitEntity>().firstOrNull()?.instance as? LivingEntity
                now {
                    objective.filterIsInstance<TargetBukkitEntity>().map { it.instance }.filter { !it.isDead }.forEach { entity ->
                        // 忽略 自己攻击自己
                        if (killer == entity) return@forEach

                        if (entity is Damageable) {
                            // 如果即将死亡 设置死亡
                            if (entity.health <= value && killer != null) {
                                // 如果是 living entity 唤起 bukkit event
                                if (entity is LivingEntity) {
                                    (entity as? LivingEntity)?.setKiller(killer)
                                    Bukkit.getPluginManager().callEvent(EntityDeathEvent(entity, emptyList()))
                                }
                                // 通过 meta data 设置击杀者
                                entity.setMeta("@killer",killer)
                            }
                            entity.damage(value)
                        }
                    }
                }
            }
        }

    fun LivingEntity.setKiller(source: LivingEntity) {
        this.setMeta("@killer",source)
        when (MinecraftVersion.major) {
            // 1.12.* 1.16.*
            4, 8 -> setProperty("entity/killer", source.getProperty("entity"))
            // 1.15.* 1.17.* bc
            7, 9 -> setProperty("entity/bc", source.getProperty("entity"))
            // 1.18.2 bc 1.18.1 bd
            10 -> if (MinecraftVersion.minecraftVersion == "v1_18_R2") {
                setProperty("entity/bc", source.getProperty("entity"))
            } else {
                setProperty("entity/bd", source.getProperty("entity"))
            }
            // 1.18.* 1.19.* bd
            11 -> setProperty("entity/bd", source.getProperty("entity"))

        }
    }

}
