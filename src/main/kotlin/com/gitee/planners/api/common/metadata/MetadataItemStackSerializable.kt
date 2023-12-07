package com.gitee.planners.api.common.metadata

import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import org.bukkit.inventory.ItemStack
import taboolib.common5.util.decodeBase64
import taboolib.common5.util.encodeBase64
import taboolib.platform.util.deserializeToItemStack
import taboolib.platform.util.serializeToByteArray

class MetadataItemStackSerializable : Metadata.Serializable<ItemStack> {

    override fun type(): Class<ItemStack> {
        return ItemStack::class.java
    }

    override fun decode(element: JsonElement): ItemStack {
        return element.asString.decodeBase64().deserializeToItemStack(zipped = true)
    }

    override fun encode(src: ItemStack): JsonElement {
        return JsonPrimitive(src.serializeToByteArray(zipped = true).encodeBase64())
    }


}
