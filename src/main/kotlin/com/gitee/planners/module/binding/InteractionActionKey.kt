package com.gitee.planners.module.binding

class InteractionActionKey(val localized: String, val type: Type) : InteractionAction {

    override val code = "$localized.${type.name.lowercase()}"

    enum class Type {

        PRESS, RELEASE, PRESSING

    }

}
