package com.deanwaylabs.quietos.messages

enum class MessageSpamRisk {
    CLEAN,
    SUSPICIOUS,
    BLOCKED
}

data class MessageSpamDecision(
    val risk: MessageSpamRisk,
    val score: Int,
    val reason: String,
    val suppressNotification: Boolean
)

class MessageSpamRiskEngine {
    fun classify(sender: String, text: String): MessageSpamDecision {
        val combined = (sender + " " + text).lowercase()
        var score = 0
        val reasons = mutableListOf<String>()

        fun hit(points: Int, reason: String, vararg terms: String) {
            if (terms.any { combined.contains(it) }) {
                score += points
                reasons += reason
            }
        }

        if (URL_REGEX.containsMatchIn(combined)) {
            score += 1
            reasons += "contains a link"
        }

        hit(1, "uses urgency language",
            "urgent", "act now", "immediately", "final notice", "last warning", "suspended")
        hit(2, "asks for account or payment action",
            "verify your account", "confirm your identity", "payment failed", "unpaid toll",
            "gift card", "bank account", "crypto wallet", "update payment")
        hit(2, "uses prize or reward bait",
            "you won", "winner", "claim your prize", "claim reward", "free gift")
        hit(1, "uses threat or penalty language",
            "legal action", "penalty", "late fee", "service will be disconnected")
        hit(1, "asks for a reply or code action",
            "reply yes", "send the code", "verification code", "one-time code")

        val risk = when {
            score >= 6 -> MessageSpamRisk.BLOCKED
            score >= 4 -> MessageSpamRisk.SUSPICIOUS
            else -> MessageSpamRisk.CLEAN
        }

        val reason = if (reasons.isEmpty()) {
            "no strong spam signals"
        } else {
            reasons.distinct().joinToString(", ")
        }

        return MessageSpamDecision(
            risk = risk,
            score = score,
            reason = reason,
            suppressNotification = risk != MessageSpamRisk.CLEAN
        )
    }

    companion object {
        private val URL_REGEX = Regex("""https?://|www\.|\b[a-z0-9-]+\.(com|net|org|info|xyz|top|click|link)\b""")
    }
}
