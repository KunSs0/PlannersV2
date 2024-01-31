package com.gitee.planners.module.kether.bukkit

import com.gitee.planners.api.common.script.KetherEditor
import com.gitee.planners.api.common.script.kether.CombinationKetherParser
import com.gitee.planners.api.common.script.kether.SimpleKetherParser
import com.gitee.planners.api.job.target.TargetBukkitEntity
import com.gitee.planners.module.kether.*
import org.bukkit.Bukkit
import org.bukkit.entity.Damageable
import org.bukkit.entity.LivingEntity
import org.bukkit.event.entity.EntityDeathEvent
import taboolib.library.kether.QuestActionParser
import taboolib.library.reflex.Reflex.Companion.getProperty
import taboolib.library.reflex.Reflex.Companion.setProperty
import taboolib.module.kether.combinationParser
import taboolib.module.nms.MinecraftVersion
import taboolib.platform.util.setMeta

@KetherEditor.Document("damage <value: number> [at objective:TargetContainer(empty)] [source objective:TargetContainer(sender)]")
@CombinationKetherParser.Used
object ActionBukkitDamage : SimpleKetherParser("damage") {

    override fun run(): QuestActionParser {
        return combinationParser {
            it.group(
                actionDouble(),
                commandObjectiveOrEmpty(),
                commandObjectiveOrSender("source")
            ).apply(it) { value, objective, source ->
                val killer = source.filterIsInstance<TargetBukkitEntity>().firstOrNull()?.getInstance() as? LivingEntity
                now {
                    objective.filterIsInstance<TargetBukkitEntity>().map { it.getInstance() }.filter { !it.isDead }.forEach { entity ->
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
    }

    fun LivingEntity.setKiller(source: LivingEntity) {
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
