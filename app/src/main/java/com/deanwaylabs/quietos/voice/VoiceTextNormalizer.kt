package com.deanwaylabs.quietos.voice

class VoiceTextNormalizer {
    private val replacements = listOf(
        Regex("\\bjohn dean\\b", RegexOption.IGNORE_CASE) to "Jon Dean",
        Regex("\\bdean\\s+way\\s+labs\\b", RegexOption.IGNORE_CASE) to "DeanWay Labs",
        Regex("\\bgreenway\\s+labs\\b", RegexOption.IGNORE_CASE) to "DeanWay Labs",
        Regex("\\bdean\\s+way\\s+travels\\b", RegexOption.IGNORE_CASE) to "DeanWay Travels",
        Regex("\\bgreenway\\s+travels\\b", RegexOption.IGNORE_CASE) to "DeanWay Travels",
        Regex("\\bdean\\s+way\\b", RegexOption.IGNORE_CASE) to "DeanWay",
        Regex("\\bquiet\\s+os\\b", RegexOption.IGNORE_CASE) to "QuietOS",
        Regex("\\bghost\\s+mode\\b", RegexOption.IGNORE_CASE) to "GhostMode"
    )

    fun normalize(raw: String): String {
        var result = raw.trim()
        replacements.forEach { (pattern, replacement) ->
            result = pattern.replace(result, replacement)
        }
        return result.replace(Regex("\\s+"), " ").trim()
    }
}
