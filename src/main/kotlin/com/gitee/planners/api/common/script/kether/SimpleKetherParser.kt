package com.gitee.planners.api.common.script.kether



abstract class SimpleKetherParser(vararg id : String) : CombinationKetherParser {

    override val id = arrayOf(*id)

    override val namespace = KetherHelper.NAMESPACE_COMMON

    override fun toString(): String {
        return "SimpleKetherParser(id=${id.contentToString()}, namespace=${namespace})"
    }

}
