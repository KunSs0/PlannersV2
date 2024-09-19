package com.gitee.planners.module.kether.compat.germengine

import com.germ.germplugin.api.dynamic.animation.GermAnimationPart

interface ProxyGermAnimation {

    fun create() :GermAnimationPart<*>

}
