package com.gitee.planners.api.common.metadata

import com.google.gson.JsonElement
import taboolib.common.util.Location
import taboolib.common5.cdouble
import taboolib.common5.cint

class MetadataLocationSerializable : Metadata.Serializable<Location> {
    override fun type(): Class<Location> {
        return Location::class.java
    }

    override fun decode(element: JsonElement): Location {
        val split = element.asJsonPrimitive.asString.split(" ")
        return Location(split[0], split[1].cdouble, split[2].cdouble, split[3].cdouble)
    }


}
