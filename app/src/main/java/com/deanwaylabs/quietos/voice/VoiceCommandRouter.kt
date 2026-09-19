package com.deanwaylabs.quietos.voice

enum class VoiceCommand {
    SUMMARIZE_DIGEST,
    ATTENTION_ON,
    ATTENTION_OFF,
    CLEAR_ATTENTION_LOG,
    REFRESH_ATTENTION,
    ATTENTION_STATUS,
    CONVERSATION
}

data class VoiceRoute(
    val command: VoiceCommand,
    val originalText: String
)

class VoiceCommandRouter {
    fun route(raw: String): VoiceRoute {
        val text = raw.trim()
        val normalized = text
            .lowercase()
            .replace(Regex("[^a-z0-9% ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        fun hasAll(vararg words: String) = words.all { normalized.contains(it) }

        val command = when {
            normalized.contains("summarize") && normalized.contains("digest") ->
                VoiceCommand.SUMMARIZE_DIGEST

            normalized.contains("attention mode") &&
                (
                    normalized.endsWith(" on") ||
                    normalized.contains("enable attention mode") ||
                    normalized.contains("start attention mode") ||
                    normalized.contains("switch attention mode on") ||
                    normalized.contains("turn attention mode on")
                ) ->
                VoiceCommand.ATTENTION_ON

            normalized.contains("attention mode") &&
                (
                    normalized.endsWith(" off") ||
                    normalized.contains("disable attention mode") ||
                    normalized.contains("stop attention mode") ||
                    normalized.contains("switch attention mode off") ||
                    normalized.contains("turn attention mode off")
                ) ->
                VoiceCommand.ATTENTION_OFF

            normalized.contains("clear") &&
                (normalized.contains("attention log") || normalized.contains("notification log")) ->
                VoiceCommand.CLEAR_ATTENTION_LOG

            normalized.contains("refresh") &&
                (normalized.contains("attention") || normalized.contains("notifications")) ->
                VoiceCommand.REFRESH_ATTENTION

            (normalized.contains("what did quietos catch") ||
                normalized.contains("what did you catch") ||
                normalized.contains("attention status") ||
                hasAll("what", "attention", "status")) ->
                VoiceCommand.ATTENTION_STATUS

            else -> VoiceCommand.CONVERSATION
        }

        return VoiceRoute(command, text)
    }
}
