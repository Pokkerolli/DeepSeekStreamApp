package com.example.deepseekstream.data.parser

import com.example.deepseekstream.domain.model.TokenUsage
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull

data class ParsedSseData(
    val text: String? = null,
    val usage: TokenUsage? = null,
    val isDone: Boolean = false
)

class StreamingParser(
    private val json: Json
) {
    fun parseSseDataLine(line: String): ParsedSseData? {
        if (!line.startsWith("data:")) return null
        val payload = line.removePrefix("data:").trim()
        if (payload.isEmpty()) return null
        if (payload == "[DONE]") return ParsedSseData(isDone = true)

        return runCatching {
            val element = json.parseToJsonElement(payload)
            ParsedSseData(
                text = extractContent(element),
                usage = extractUsage(element)
            )
        }.getOrNull()
    }

    fun extractTextFromSseDataLine(line: String): String? {
        return parseSseDataLine(line)?.text?.takeIf { it.isNotBlank() }
    }

    private fun extractContent(element: JsonElement): String? {
        val root = element as? JsonObject ?: return null
        val choices = root["choices"] as? JsonArray ?: return null
        val first = choices.firstOrNull() as? JsonObject ?: return null

        val delta = first["delta"] as? JsonObject
        val deltaCombined = mergeNonBlank(
            extractTextValue(delta?.get("reasoning_content")),
            extractTextValue(delta?.get("content"))
        )
        if (!deltaCombined.isNullOrBlank()) return deltaCombined

        val message = first["message"] as? JsonObject
        val messageCombined = mergeNonBlank(
            extractTextValue(message?.get("reasoning_content")),
            extractTextValue(message?.get("content"))
        )
        if (!messageCombined.isNullOrBlank()) return messageCombined

        return null
    }

    private fun extractTextValue(element: JsonElement?): String? {
        return when (element) {
            null -> null
            is JsonPrimitive -> element.contentOrNull
            is JsonArray -> element.joinToString(separator = "") { item ->
                extractTextValue(item).orEmpty()
            }.ifBlank { null }
            is JsonObject -> {
                val directText = (element["text"] as? JsonPrimitive)?.contentOrNull
                if (!directText.isNullOrBlank()) return directText

                val nestedText = extractTextValue(element["content"])
                if (!nestedText.isNullOrBlank()) return nestedText

                null
            }
            else -> null
        }
    }

    private fun mergeNonBlank(first: String?, second: String?): String? {
        val a = first?.takeIf { it.isNotBlank() }
        val b = second?.takeIf { it.isNotBlank() }
        return when {
            a == null && b == null -> null
            a != null && b != null -> a + b
            else -> a ?: b
        }
    }

    private fun extractUsage(element: JsonElement): TokenUsage? {
        val root = element as? JsonObject ?: return null
        val usage = root["usage"] as? JsonObject ?: return null

        val prompt = (usage["prompt_tokens"] as? JsonPrimitive)?.intOrNull
        val completion = (usage["completion_tokens"] as? JsonPrimitive)?.intOrNull
        val total = (usage["total_tokens"] as? JsonPrimitive)?.intOrNull

        if (prompt == null && completion == null && total == null) return null
        return TokenUsage(
            promptTokens = prompt,
            completionTokens = completion,
            totalTokens = total
        )
    }
}
