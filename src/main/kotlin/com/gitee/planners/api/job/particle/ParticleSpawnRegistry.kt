package com.gitee.planners.api.job.particle

import com.gitee.planners.api.common.registry.AbstractRegistry
import com.gitee.planners.api.common.registry.RunningClassRegistriesVisitor
import com.gitee.planners.module.particle.particle.ParticleSpawner
import taboolib.common.platform.Awake

object ParticleSpawnRegistry : AbstractRegistry<String, ParticleSpawner>() {

    fun getDefault(): ParticleSpawner {
        return this["minecraft"]
    }

    @Awake
    class Visitor : RunningClassRegistriesVisitor<ParticleSpawner>(ParticleSpawner::class.java, ParticleSpawnRegistry) {

        override fun visit(instance: ParticleSpawner) {
            instance.namespace.forEach { id ->
                this.registry[id] = instance
            }
        }

        override fun getId(instance: ParticleSpawner): String {
            return ""
        }

    }

}
