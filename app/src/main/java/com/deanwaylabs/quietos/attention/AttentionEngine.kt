package com.deanwaylabs.quietos.attention

enum class AttentionClass { NOW, SOON, DIGEST, QUIET }

data class AttentionInput(
    val text: String,
    val isEmergencySource: Boolean = false,
    val isDirectRequest: Boolean = false,
    val isPromotional: Boolean = false
)

data class AttentionDecision(val classification: AttentionClass, val reason: String)

class AttentionEngine {
    private val emergencyTerms = setOf("emergency", "911", "hospital", "urgent help")

    fun classify(input: AttentionInput): AttentionDecision {
        val normalized = input.text.lowercase()
        if (input.isEmergencySource || emergencyTerms.any(normalized::contains))
            return AttentionDecision(AttentionClass.NOW, "deterministic emergency rule")
        if (input.isDirectRequest)
            return AttentionDecision(AttentionClass.SOON, "direct request")
        if (input.isPromotional)
            return AttentionDecision(AttentionClass.QUIET, "promotional content")
        return AttentionDecision(AttentionClass.DIGEST, "default useful event")
    }
}
