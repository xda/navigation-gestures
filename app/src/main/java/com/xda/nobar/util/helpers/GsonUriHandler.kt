package com.xda.nobar.util.helpers

import android.content.Intent
import android.net.Uri
import com.google.gson.*
import java.lang.reflect.Type

class GsonUriHandler : JsonSerializer<Uri>, JsonDeserializer<Uri> {
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

class GsonIntentHandler : JsonSerializer<Intent>, JsonDeserializer<Intent> {
    override fun serialize(src: Intent, typeOfSrc: Type, context: JsonSerializationContext?): JsonElement {
        return JsonPrimitive(src.toUri(0))
    }

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Intent? {
        return try {
            Intent.parseUri(json.asString, 0)
        } catch (e: Exception) {
            null
        }
    }
}