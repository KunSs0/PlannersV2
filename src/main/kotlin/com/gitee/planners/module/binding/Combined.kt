package com.gitee.planners.module.binding

import com.gitee.planners.api.common.registry.Unique
import taboolib.library.configuration.ConfigurationSection

abstract class Combined(val config: ConfigurationSection) : Unique {

    final override val id = config.name

    val name = config.getString("name",id)!!

    val mapping = config.getStringList("mapping")

    val requestTick = config.getInt("request-tick")

    val matchingType = config.getEnum("matching-type", MatchingType::class.java) ?: MatchingType.FUZZY

    enum class MatchingType {

        STRICT, FUZZY

    }

}
