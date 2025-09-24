package com.gitee.planners.core.skill.binding

import com.gitee.planners.api.common.Unique

enum class InteractionActionBukkitType(override val id: String) : Unique {

    MISC_SNEAK("sneak"),
    MISC_STAND_UP("stand-up"),
    MISC_SPRING("spring"),
    MISC_WALK("walk"),
    MISC_JUMP("jump"),

    INTERACT_LEFT("interact-left"),
    INTERACT_RIGHT("interact-right"),

}
