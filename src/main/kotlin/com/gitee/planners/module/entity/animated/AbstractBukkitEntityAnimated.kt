package com.gitee.planners.module.entity.animated

import com.gitee.planners.api.common.entity.animated.AbstractAnimated
import com.gitee.planners.api.common.entity.animated.AnimatedMeta
import com.gitee.planners.api.job.target.Target
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import taboolib.module.ai.clearGoalAi
import taboolib.module.ai.clearTargetAi
import taboolib.module.nms.getI18nName
import taboolib.platform.util.setMeta

abstract class AbstractBukkitEntityAnimated<E : Entity> : AbstractAnimated() {

    /** 临时实例 */
    lateinit var instance: E

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
    val isFreedom = bool("is-freedom",false) {

    }

    fun invokeSpawn(target: Target<*>): E {
        instance = create(target)
        instance.setMeta("@animated", this)
        this.getImmutableRegistry().getValues().filterIsInstance<AnimatedMeta<Any>>().forEach {
            it.onUpdate(this, it.any())
        }
        return instance
    }



    // 禁止原生更新逻辑
    override fun handleUpdate(metadata: AnimatedMeta.CoerceMeta<Any>) {

    }

    protected abstract fun create(target: Target<*>): E

}
