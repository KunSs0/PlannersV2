package com.gitee.planners.module.compat.placeholder

import org.bukkit.entity.Player
import taboolib.module.configuration.ConfigNode
import taboolib.module.configuration.conversion
import taboolib.platform.compat.PlaceholderExpansion

object PlaceholderHooked : PlaceholderExpansion {

    @ConfigNode("settings.placeholder.use")
    private val useType = conversion<String, UseType> {
        UseType.valueOf(this.trim().uppercase())
    }


    override val identifier: String
        get() = "planners"


    override fun onPlaceholderRequest(player: Player?, args: String): String {
        return when (useType.get()) {
            UseType.SCRIPT -> {
                PlaceholderScript.parse(player!!, args)
            }

            UseType.LITERAL -> {
                PlaceholderLiteral.parse(player!!, args)
            }
        }
    }

    enum class UseType {
        SCRIPT, LITERAL
    }

}