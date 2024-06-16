package com.gitee.planners.module.entity.animated

import com.gitee.planners.api.common.entity.animated.AbstractAnimated
import com.gitee.planners.api.common.entity.animated.AnimatedMeta
import com.gitee.planners.api.event.entity.EntityModelApplyEvent
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import taboolib.module.ai.clearGoalAi
import taboolib.module.ai.clearTargetAi
import taboolib.module.nms.getI18nName

abstract class AbstractBukkitEntityAnimated<E : Entity> : AbstractAnimated() {

    /** 临时实例 */
    open lateinit var instance: E

    val name = text("name", "") {
        instance.customName = if (it == "") instance.getI18nName() else it
    }

    val nameVisible = bool("is-name-visible", false) {
        instance.isCustomNameVisible = it
    }

    val invulnerable = bool("is-invulnerable", true) {
        instance.isInvulnerable = it
    }

    val ai = bool("is-ai", false) {
        val entity = instance as? LivingEntity ?: return@bool
        entity.setAI(it)
        if (!it) {
            entity.clearGoalAi()
            entity.clearTargetAi()
        }
    }

    /** 自由节点 不会hit到发射者 */
    val isFreedom = bool("is-freedom", false) {

    }

    /** 重力 */
    val isGravity = bool("is-gravity", true) {
        instance.setGravity(it)
    }

    /**
     * Model Engine v2.5.4
     */
    val model = text("model", "") {
        // post event
        EntityModelApplyEvent(this@AbstractBukkitEntityAnimated.instance, it).call()
    }

    // 禁止原生更新逻辑
    override fun handleUpdate(metadata: AnimatedMeta.CoerceMeta<Any>) {

    }

}
