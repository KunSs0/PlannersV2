package com.gitee.planners.core.skill.script.animated

import com.gitee.planners.api.common.entity.animated.AbstractAnimated
import org.bukkit.event.Event

abstract class AbstractEventModifier<E : Event>(val original: E) : AbstractAnimated() {


}
