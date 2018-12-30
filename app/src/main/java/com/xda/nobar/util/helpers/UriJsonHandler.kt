package com.xda.nobar.util.helpers

import android.net.Uri
import com.google.gson.*
import java.lang.reflect.Type

class UriJsonHandler : JsonSerializer<Uri>, JsonDeserializer<Uri> {
    override fun serialize(src: Uri, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        return JsonPrimitive(src.toString())
    }

    override fun deserialize(
            src: JsonElement, srcType: Type,
            context: JsonDeserializationContext
    ): Uri? {
        return try {
            Uri.parse(src.asString)
        } catch (e: Exception) {
            null
        }
    }
}