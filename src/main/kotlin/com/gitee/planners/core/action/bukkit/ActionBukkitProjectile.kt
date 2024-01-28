package com.gitee.planners.core.action.bukkit

import com.gitee.planners.api.common.entity.animated.Animated
import com.gitee.planners.api.common.entity.animated.AnimatedListener
import com.gitee.planners.api.common.script.KetherEditor
import com.gitee.planners.api.common.script.kether.CombinationKetherParser
import com.gitee.planners.api.common.script.kether.KetherHelper
import com.gitee.planners.api.common.script.kether.MultipleKetherParser
import com.gitee.planners.core.action.context.AbstractComplexScriptContext
import com.gitee.planners.api.job.target.LeastType
import com.gitee.planners.api.job.target.TargetContainer
import com.gitee.planners.api.job.target.adaptTarget
import com.gitee.planners.core.action.bukkit.ActionBukkitEntity.getAnimated
import com.gitee.planners.core.action.bukkit.ActionBukkitEntity.getCasterContext
import com.gitee.planners.core.action.bukkit.ActionBukkitEntity.getCasterTarget
import com.gitee.planners.core.action.commandFloat
import com.gitee.planners.core.action.commandObjective
import com.gitee.planners.core.action.enum
import com.gitee.planners.module.entity.animated.AbstractBukkitEntityAnimated
import com.gitee.planners.module.entity.animated.BukkitProjectile
import com.gitee.planners.module.entity.animated.event.AnimatedEntityEvent
import org.bukkit.event.entity.ProjectileHitEvent
import taboolib.common.platform.event.SubscribeEvent

@CombinationKetherParser.Used
object ActionBukkitProjectile : MultipleKetherParser("projectile") {

    @KetherEditor.Document("projectile create arrow [step: float(0.4)]")
    val create = KetherHelper.combinedKetherParser {
        it.group(enum<BukkitProjectile.Type>(), commandFloat("step", 0.4f)).apply(it) { type, step ->
            now {
                BukkitProjectile(type, step)
            }
        }
    }

    @KetherEditor.Document("projectile spawn <animated> [at objective:TargetContainer(sender)]")
    val spawn = ActionBukkitEntity.spawn

    @KetherEditor.Document("projectile listen <animated> on <event> then <function>")
    val listen = ActionBukkitEntity.listen

    @SubscribeEvent
    fun e(e: ProjectileHitEvent) {
        val projectile = e.entity
        val context = projectile.getCasterContext() as? AbstractComplexScriptContext ?: return
        val animated = projectile.getAnimated() as? BukkitProjectile ?: return
        if (animated.isAutoRemove.asBoolean()) {
            projectile.remove()
        }

        if (!animated.isFreedom.asBoolean() && (e.hitEntity == projectile.getCasterTarget()?.getInstance())) {
            return
        }
        // emit event
        animated.emit(AnimatedEntityEvent.Hit(animated,projectile,e.hitEntity,e.hitBlock),context)
    }


}
