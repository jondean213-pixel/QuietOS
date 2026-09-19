package com.deanwaylabs.quietos.ai

/**
 * Stable personal memory supplied by QuietOS to Gemma.
 *
 * This is application-owned memory, not model training data.
 * Keep only grounded, durable facts here. Dynamic memory will later move
 * behind a local store/retrieval layer.
 */
object GemmaMemory {
    fun answerKnownQuestion(message: String): String? {
        val q = message.trim().lowercase()
            .replace(Regex("[^a-z0-9' ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        return when (q) {
            "who am i",
            "who am i jon",
            "what do you know about me" ->
                "You are Jon Dean, owner of DeanWay LLC and DeanWay Labs, builder and product owner of QuietOS, and the primary user of this personal QuietOS build."

            "who is jon",
            "who is jon dean" ->
                "Jon Dean is the owner of DeanWay LLC and DeanWay Labs, the builder and product owner of QuietOS, and the primary user of this personal QuietOS build."

            "what is my role in quietos",
            "what's my role in quietos",
            "what is my role" ->
                "Your role in QuietOS is owner, builder, product owner, primary user, and physical-device tester."

            "who are you",
            "what are you" ->
                "I am Gemma, Jon Dean's personal assistant inside QuietOS."

            else -> null
        }
    }

    val personalCore: String = """
Jon identity:
- Jon Dean is the owner of DeanWay LLC and DeanWay Labs.
- Jon is the builder and product owner of QuietOS.
- In this personal QuietOS build, Jon is the primary user and the person speaking to Gemma unless QuietOS explicitly identifies another user.
- Gemma is Jon's personal assistant identity inside QuietOS.
- Jon directs the project, makes product decisions, and performs physical-device testing.
- Conversation perspective rule: the human user is Jon. In Jon's messages, "I", "me", "my", and "am I" refer to Jon, not Gemma.
- When Jon asks "who am I", answer about Jon Dean, not about Gemma.
- When Jon asks "what is my role in QuietOS", "my" means Jon's role: owner, builder, product owner, primary user, and physical-device tester.
- When Jon asks "who is Jon" or "what do you know about me", answer from these grounded facts rather than merely repeating his name.
- Only describe Gemma's role when Jon explicitly asks about Gemma, the assistant, or "your role".
- Do not invent biographical details that are not present in QuietOS memory or current conversation.
""".trimIndent()
}
