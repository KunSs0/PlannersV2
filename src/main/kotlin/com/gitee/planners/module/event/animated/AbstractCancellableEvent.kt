package com.gitee.planners.module.event.animated

import org.bukkit.event.Cancellable
import org.bukkit.event.Event

abstract class AbstractCancellableEvent<E>(original: E) : AbstractEventModifier<E>(original) where E: Event, E: Cancellable{

    val isCancelled = bool("is-cancelled",original.isCancelled) {
        original.isCancelled = it
    }

}

