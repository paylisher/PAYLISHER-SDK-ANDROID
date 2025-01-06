package com.paylisher.android.notification.helpers

import com.google.gson.*
import com.paylisher.android.notification.InAppLayoutBlock
import java.lang.reflect.Type

class InAppLayoutBlockDeserializer : JsonDeserializer<InAppLayoutBlock> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): InAppLayoutBlock {
        val jsonObject = json.asJsonObject
        return when (val blockType = jsonObject["type"].asString) {
            "spacer" -> context.deserialize(jsonObject, InAppLayoutBlock.SpacerBlock::class.java)
            "image" -> context.deserialize(jsonObject, InAppLayoutBlock.ImageBlock::class.java)
            "text" -> context.deserialize(jsonObject, InAppLayoutBlock.TextBlock::class.java)
            "buttonGroup" -> context.deserialize(jsonObject, InAppLayoutBlock.ButtonGroupBlock::class.java)
            else -> throw JsonParseException("Unknown block type: $blockType")
        }
    }
}
