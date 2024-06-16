package com.gitee.planners.module.kether.germengine

import com.germ.germplugin.api.dynamic.animation.GermAnimationPart

interface ProxyGermAnimation {

    fun create() :GermAnimationPart<*>

}
