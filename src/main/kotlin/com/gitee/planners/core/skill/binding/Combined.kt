package com.gitee.planners.core.skill.binding

import com.gitee.planners.api.common.Unique
import taboolib.library.configuration.ConfigurationSection

abstract class Combined(val config: ConfigurationSection) : Unique {

    final override val id = config.name

    val name = config.getString("name",id)!!

    /** 键位对照 Localized Id */
    val mapping = if (config.isString("mapping")) {
        listOf(config.getString("mapping")!!)
    } else {
        config.getStringList("mapping")
    }

    val requestTick = config.getInt("request-tick")

    val matchingType = config.getEnum("matching-type", MatchingType::class.java) ?: MatchingType.FUZZY

    enum class MatchingType {

        STRICT, FUZZY, NONE

    }

}
