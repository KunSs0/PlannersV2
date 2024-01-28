package com.gitee.planners.module.binding

class InteractionActionBukkitToken(type: InteractionActionBukkitType) : InteractionAction {

    override val code: String = type.id

}
