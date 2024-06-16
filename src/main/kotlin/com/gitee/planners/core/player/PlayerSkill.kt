package com.gitee.planners.core.player

import com.gitee.planners.api.Registries
import com.gitee.planners.api.job.KeyBinding
import com.gitee.planners.api.job.Skill
import com.gitee.planners.api.job.Variable
import com.gitee.planners.core.config.ImmutableSkill
import com.gitee.planners.core.database.Database
import taboolib.common.platform.function.submitAsync

class PlayerSkill(var index: Long, private val skillId: String, level: Int, private var bindingId: String?) : Skill {

    val immutable: ImmutableSkill
        get() = Registries.SKILL.getOrNull(skillId) ?: error("Couldn't find skill with id $skillId'")

    var level = level
        set(value) {
            field = value
            submitAsync { Database.INSTANCE.updateSkill(this@PlayerSkill) }
        }

    var binding: KeyBinding?
        get() = if (bindingId != null) Registries.KEYBINDING.getOrNull(bindingId!!) else null
        set(value) {
            bindingId = value?.id
            submitAsync { Database.INSTANCE.updateSkill(this@PlayerSkill) }
        }

    override val id: String
        get() = immutable.id

    override fun getVariables(): Map<String, Variable> {
        return immutable.getVariables()
    }

    override fun getVariableOrNull(id: String): Variable? {
        return immutable.getVariableOrNull(id)
    }


}
