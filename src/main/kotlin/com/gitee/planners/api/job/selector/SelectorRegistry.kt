package com.gitee.planners.api.job.selector

import com.gitee.planners.api.common.AbstractRegistry
import com.gitee.planners.api.common.RunningClassRegistredVisitor
import taboolib.common.platform.Awake

object SelectorRegistry : AbstractRegistry<String, Selector>() {



    @Awake
    class Visitor : RunningClassRegistredVisitor<Selector>(Selector::class.java, SelectorRegistry) {

        override fun visit(instance: Selector) {
            instance.namespace().forEach { id ->
                this.registry[id] = instance
            }
        }

        override fun getId(instance: Selector): String {
            return ""
        }

    }

}
