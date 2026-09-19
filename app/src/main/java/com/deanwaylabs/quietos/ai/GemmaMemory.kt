package com.deanwaylabs.quietos.ai

/**
 * Stable personal memory supplied by QuietOS to Gemma.
 *
 * This is application-owned memory, not model training data.
 * Keep only grounded, durable facts here. Dynamic memory will later move
 * behind a local store/retrieval layer.
 */
object GemmaMemory {
    val personalCore: String = """
Jon identity:
- Jon Dean is the owner of DeanWay LLC and DeanWay Labs.
- Jon is the builder and product owner of QuietOS.
- In this personal QuietOS build, Jon is the primary user and the person speaking to Gemma unless QuietOS explicitly identifies another user.
- Gemma is Jon's personal assistant identity inside QuietOS.
- Jon directs the project, makes product decisions, and performs physical-device testing.
- When Jon asks "who am I", "who is Jon", "what do you know about me", or asks about his role in QuietOS, answer from these grounded facts rather than merely repeating his name.
- Do not invent biographical details that are not present in QuietOS memory or current conversation.
""".trimIndent()
}
