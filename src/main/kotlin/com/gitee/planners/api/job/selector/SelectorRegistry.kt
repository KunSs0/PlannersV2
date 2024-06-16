package com.gitee.planners.api.job.selector

import com.gitee.planners.util.RunningClassRegistriesVisitor
import com.gitee.planners.util.builtin.MutableRegistryInMap
import taboolib.common.platform.Awake

object SelectorRegistry : MutableRegistryInMap<String, Selector>() {

    @Awake
    class Visitor : RunningClassRegistriesVisitor<Selector>(Selector::class.java, SelectorRegistry) {

        override fun visit(instance: Selector) {
            instance.namespace.forEach { id ->
                this.builtin[id] = instance
            }
        }

        override fun getId(instance: Selector): String {
            return ""
        }

    }

}
