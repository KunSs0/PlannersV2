package com.gitee.planners.module.currency

import com.gitee.planners.api.Registries

object Currencies {

    fun getInstance(id: String): OpenConvertibleCurrencyImpl? {
        return Registries.CURRENCY.getOrNull(id)
    }

}
