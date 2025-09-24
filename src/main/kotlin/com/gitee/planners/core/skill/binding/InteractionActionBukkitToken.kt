package com.gitee.planners.core.skill.binding

class InteractionActionBukkitToken(type: InteractionActionBukkitType) : InteractionAction {

    override val code: String = type.id

}
