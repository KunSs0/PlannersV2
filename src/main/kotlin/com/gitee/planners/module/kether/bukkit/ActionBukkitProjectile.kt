package com.gitee.planners.module.kether.bukkit

import com.gitee.planners.api.common.script.KetherEditor
import com.gitee.planners.api.common.script.kether.CombinationKetherParser
import com.gitee.planners.api.common.script.kether.KetherHelper
import com.gitee.planners.api.common.script.kether.MultipleKetherParser
import com.gitee.planners.api.job.target.TargetContainer
import com.gitee.planners.core.skill.entity.animated.BukkitEntityBuilder
import com.gitee.planners.core.skill.entity.animated.BukkitProjectileBuilder
import com.gitee.planners.module.kether.context.AbstractComplexScriptContext
import com.gitee.planners.module.kether.bukkit.ActionBukkitEntityBuilder.getAnimated
import com.gitee.planners.module.kether.bukkit.ActionBukkitEntityBuilder.getCasterContext
import com.gitee.planners.module.kether.bukkit.ActionBukkitEntityBuilder.getCasterTarget
import com.gitee.planners.module.kether.commandFloat
import com.gitee.planners.module.kether.enum
import com.gitee.planners.core.skill.entity.animated.event.AnimatedEntityEvent
import org.bukkit.event.entity.ProjectileHitEvent
import taboolib.common.platform.event.SubscribeEvent

@CombinationKetherParser.Used
object ActionBukkitProjectile : MultipleKetherParser("projectile") {

    @KetherEditor.Document("projectile create arrow [step: float(0.4)]")
    val create = KetherHelper.combinedKetherParser {
        it.group(enum<BukkitProjectileBuilder.Type>(), commandFloat("step", 0.4f)).apply(it) { type, step ->
            now {
                BukkitProjectileBuilder(type, step)
            }
        }
    }

    @KetherEditor.Document("projectile spawn <animated> [at objective:TargetContainer(sender)]")
    val spawn = ActionBukkitEntityBuilder.spawn

    @KetherEditor.Document("projectile listen <animated> on <event> then <function>")
    val listen = ActionBukkitEntityBuilder.listen

    @SubscribeEvent
    fun e(e: ProjectileHitEvent) {
        val projectile = e.entity
        val context = projectile.getCasterContext() as? AbstractComplexScriptContext ?: return
        val animated = projectile.getAnimated() as? BukkitProjectileBuilder ?: return
        if (animated.isAutoRemove.asBoolean()) {
            projectile.remove()
        }

        if (!animated.isFreedom.asBoolean() && (e.hitEntity == projectile.getCasterTarget()?.instance)) {
            return
        }
        // emit event
        animated.emit(AnimatedEntityEvent.Hit(animated,projectile,e.hitEntity,e.hitBlock),context)
    }


}
