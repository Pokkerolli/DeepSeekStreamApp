package com.example.deepseekstream.data.parser

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

class StreamingParser(
    private val json: Json
) {
    fun extractTextFromSseDataLine(line: String): String? {
        if (!line.startsWith("data:")) return null
        val payload = line.removePrefix("data:").trim()
        if (payload.isEmpty() || payload == "[DONE]") return null

        return runCatching {
            val element = json.parseToJsonElement(payload)
            extractContent(element)
        }.getOrNull()?.takeIf { it.isNotBlank() }
    }

    private fun extractContent(element: JsonElement): String? {
        val root = element as? JsonObject ?: return null
        val choices = root["choices"] as? JsonArray ?: return null
        val first = choices.firstOrNull() as? JsonObject ?: return null

        val deltaContent = ((first["delta"] as? JsonObject)?.get("content") as? JsonPrimitive)
            ?.contentOrNull
        if (!deltaContent.isNullOrBlank()) return deltaContent

        val messageContent = ((first["message"] as? JsonObject)?.get("content") as? JsonPrimitive)
            ?.contentOrNull
        if (!messageContent.isNullOrBlank()) return messageContent

        return null
    }
}
