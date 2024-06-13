package com.gitee.planners.module.kether.germengine

import com.germ.germplugin.api.GermSrcManager
import com.germ.germplugin.api.RootType
import com.germ.germplugin.api.dynamic.DynamicBase
import com.germ.germplugin.api.dynamic.effect.GermEffectPart
import com.germ.germplugin.api.dynamic.effect.GermEffectParticle
import org.bukkit.configuration.ConfigurationSection
import java.util.*

object GermEffectManager {

    val cache = Collections.synchronizedMap(mutableMapOf<String, GermEffectPart<*>>())

    fun get(path: String, type: RootType): GermEffectPart<*> {
        val effect = cache.computeIfAbsent(path) { create(path, type) }
        return mixture(UUID.randomUUID().toString(), effect)
    }

    fun create(path: String, type: RootType): GermEffectPart<*> {
        val split = path.split(".")
        return create(split[0], split[1], type)
    }

    fun create(file: String, name: String, type: RootType): GermEffectPart<*> {
        val src = GermSrcManager.getGermSrcManager().getSrc(file, type)
        if (src == null) {
            error("GermPlugin effect file '$file' not found.")
        }
        val node = src.getConfigurationSection(name)
        if (node == null) {
            error("GermPlugin effect '$name' not found of '$file'.")
        }
        return create(name, node)
    }

    fun create(id: String, node: ConfigurationSection): GermEffectPart<*> {
        return GermEffectParticle.getGermEffectPart(id, node)
    }

    fun mixture(id: String, effect: GermEffectPart<*>): GermEffectPart<*> {
        val clone = effect.clone()
        clone.setIndexName(id)
        return clone
    }

}
