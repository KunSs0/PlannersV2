package com.gitee.planners.module.event.animated

import com.gitee.planners.api.common.entity.animated.AbstractAnimated
import com.gitee.planners.api.common.entity.animated.Animated
import com.gitee.planners.api.common.entity.animated.AnimatedMeta
import org.bukkit.event.Event

abstract class AbstractEventModifier<E : Event>(val original: E) : AbstractAnimated() {


}
