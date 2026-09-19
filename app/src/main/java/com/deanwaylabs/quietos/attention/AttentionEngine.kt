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
    private val emergencyTerms = setOf(
        "emergency", "911", "hospital", "urgent help", "call 911",
        "medical emergency", "amber alert", "tornado warning"
    )
    private val directTerms = setOf(
        "can you", "could you", "please", "need you", "call me",
        "text me", "reply", "respond", "are you available"
    )
    private val promotionalTerms = setOf(
        "sale", "discount", "% off", "limited time", "shop now",
        "promo", "promotion", "coupon", "clearance"
    )

    fun classify(input: AttentionInput): AttentionDecision {
        val normalized = input.text.lowercase()

        if (input.isEmergencySource || emergencyTerms.any(normalized::contains)) {
            return AttentionDecision(AttentionClass.NOW, "deterministic emergency rule")
        }

        if (input.isDirectRequest || directTerms.any(normalized::contains)) {
            return AttentionDecision(AttentionClass.SOON, "direct-request rule")
        }

        if (input.isPromotional || promotionalTerms.any(normalized::contains)) {
            return AttentionDecision(AttentionClass.QUIET, "promotional-content rule")
        }

        return AttentionDecision(AttentionClass.DIGEST, "default useful event")
    }
}
