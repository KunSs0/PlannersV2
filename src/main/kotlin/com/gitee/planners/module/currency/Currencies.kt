package com.gitee.planners.module.currency

import com.gitee.planners.api.Registries
import com.gitee.planners.api.common.script.ComplexScriptPlatform

object Currencies : ComplexScriptPlatform.DefaultComplexScriptPlatform(){


    fun getInstance(id: String): OpenConvertibleCurrencyImpl? {
        return Registries.CURRENCY.getOrNull(id)
    }

}
