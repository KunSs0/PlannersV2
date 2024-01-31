package com.gitee.planners.module.kether.common

import com.gitee.planners.api.common.script.KetherEditor
import com.gitee.planners.api.common.script.kether.CombinationKetherParser
import com.gitee.planners.api.common.script.kether.KetherHelper
import taboolib.common5.Coerce

@KetherEditor.Document(value = "int <value:Any>", result = Int::class)
@CombinationKetherParser.Used
private fun actionInt() = coerce("int") { Coerce.toInteger(this) }

@KetherEditor.Document(value = "double <value:Any>", result = Int::class)
@CombinationKetherParser.Used
private fun actionDouble() = coerce("double") { Coerce.toDouble(this) }

@KetherEditor.Document(value = "long <value:Any>", result = Int::class)
@CombinationKetherParser.Used
private fun actionLong() = coerce("long") { Coerce.toLong(this) }

@KetherEditor.Document(value = "float <value:Any>", result = Int::class)
@CombinationKetherParser.Used
private fun actionFloat() = coerce("float") { Coerce.toFloat(this) }

private fun <T> coerce(id: String, convert: Any?.() -> T) = KetherHelper.combinedKetherParser(id) {
    it.group(any()).apply(it) { value ->
        now { convert(value) }
    }
}
