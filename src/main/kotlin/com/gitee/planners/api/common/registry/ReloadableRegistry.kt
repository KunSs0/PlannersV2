package com.gitee.planners.api.common.registry

interface ReloadableRegistry {

    fun onLoad()

    fun onReload()

    companion object {

        private val registry = mutableListOf<ReloadableRegistry>()

        fun visit(registry: ReloadableRegistry) {
            registry.onLoad()
            Companion.registry += registry
        }

        fun onReload() {
            registry.forEach { it.onReload() }
        }


    }

}
