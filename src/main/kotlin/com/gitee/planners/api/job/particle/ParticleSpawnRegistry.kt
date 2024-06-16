package com.gitee.planners.api.job.particle

import com.gitee.planners.module.particle.particle.ParticleSpawner
import com.gitee.planners.util.RunningClassRegistriesVisitor
import com.gitee.planners.util.builtin.MutableRegistryInMap
import taboolib.common.platform.Awake

object ParticleSpawnRegistry : MutableRegistryInMap<String, ParticleSpawner>() {

    fun getDefault(): ParticleSpawner {
        return this["minecraft"]
    }

    @Awake
    class Visitor : RunningClassRegistriesVisitor<ParticleSpawner>(ParticleSpawner::class.java, ParticleSpawnRegistry) {

        override fun visit(instance: ParticleSpawner) {
            instance.namespace.forEach { id ->
                this.builtin[id] = instance
            }
        }

        override fun getId(instance: ParticleSpawner): String {
            return ""
        }

    }

}
