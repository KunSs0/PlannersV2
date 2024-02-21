package com.gitee.planners.api.job.particle

import com.gitee.planners.api.common.registry.AbstractRegistry
import com.gitee.planners.api.common.registry.RunningClassRegistriesVisitor
import com.gitee.planners.api.job.selector.Selector
import com.gitee.planners.api.job.selector.SelectorRegistry
import com.gitee.planners.module.particle.particle.Particle
import taboolib.common.platform.Awake

object ParticleRegistry : AbstractRegistry<String, Particle>() {

    fun getDefault(): Particle {
        return this["minecraft"]
    }

    @Awake
    class Visitor : RunningClassRegistriesVisitor<Particle>(Particle::class.java, ParticleRegistry) {

        override fun visit(instance: Particle) {
            instance.namespace.forEach { id ->
                this.registry[id] = instance
            }
        }

        override fun getId(instance: Particle): String {
            return ""
        }

    }

}