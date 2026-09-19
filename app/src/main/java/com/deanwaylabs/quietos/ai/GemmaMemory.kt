package com.deanwaylabs.quietos.ai

/**
 * Stable DeanWay memory supplied by QuietOS to Gemma.
 *
 * This is application-owned memory, not model training data.
 * Keep only grounded, durable facts here. Dynamic memory will later move
 * behind a local store/retrieval layer.
 */
object GemmaMemory {
    private fun normalize(message: String): String =
        message.trim().lowercase()
            .replace(Regex("[^a-z0-9' ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

    fun answerKnownQuestion(message: String): String? {
        val q = normalize(message)

        return when {
            q in setOf("who am i", "who am i jon", "what do you know about me") ->
                "You are Jon Dean, owner of DeanWay LLC and DeanWay Labs, builder and product owner of QuietOS, and the primary user of this personal QuietOS build."

            q in setOf("who is jon", "who is jon dean") ->
                "Jon Dean is the owner of DeanWay LLC and DeanWay Labs, the builder and product owner of QuietOS, and the primary user of this personal QuietOS build."

            q in setOf("what is my role in quietos", "what's my role in quietos", "what is my role") ->
                "Your role in QuietOS is owner, builder, product owner, primary user, and physical-device tester."

            q in setOf("who are you", "what are you", "who is gemma") ->
                "I am Gemma, Jon Dean's personal assistant inside QuietOS."

            q.contains("deanway absolutes") ||
            q.contains("what are the absolutes") ||
            q.contains("what are the 5 absolutes") ||
            q.contains("what are five absolutes") ||
            q.contains("list the 5 absolutes") ||
            q.contains("list five absolutes") ||
            q.contains("tell me the 5 absolutes") ||
            q.contains("tell me the five absolutes") ->
                "The DeanWay Absolutes are: 1. Evidence before claims. 2. Every system is built as though it may become a product. 3. DeanWay must never depend entirely on Jon being there to explain it. 4. Build for continuity across generations, not merely chats. 5. Consult core principles, architecture, current state, and continuity before decisions."

            q.contains("quietos pricing") ||
            q.contains("how much is quietos") ||
            q.contains("quietos network pricing") ||
            q.contains("what does quietos cost") ->
                "The current locked baseline is QuietOS Basic free, QuietOS Personal at $4.99 per month or $39.99 per year, and QuietOS Network at $8.99 per month or $69.99 per year. There is no lifetime license in the initial plan."

            q.contains("what is deanway labs") ||
            q.contains("tell me about deanway labs") ->
                "DeanWay Labs is Jon Dean's product and software brand. It builds and documents systems such as GhostMode, QuietOS, QuietOS Network, and future DeanWayOS products."

            q.contains("what is deanway travels") ||
            q.contains("tell me about deanway travels") ->
                "DeanWay Travels is Jon Dean's travel agency under DeanWay LLC. It is separate from DeanWay Labs and also serves as a real-world user and proving ground for DeanWay systems."

            (q.contains("deanway travels") && q.contains("deanway labs")) ||
            q.contains("everything you know about deanway") ->
                "DeanWay LLC is Jon Dean's umbrella business. DeanWay Labs is the product and software side that builds systems including GhostMode, QuietOS, QuietOS Network, and future DeanWayOS products. DeanWay Travels is Jon's travel agency under DeanWay LLC and serves as a real-world user and proving ground for DeanWay systems."

            q.contains("what is deanway") ||
            q.contains("tell me about deanway") ->
                "DeanWay LLC is Jon Dean's umbrella business. DeanWay Labs is the product and software side, and DeanWay Travels is the travel-agency side."

            q.contains("what is quietos network") ||
            q.contains("tell me about quietos network") ->
                "QuietOS Network is the planned connected tier above QuietOS. The local model remains on-device while QuietOS mediates authorized access to network services and tools."

            q.contains("what is ghostmode") ||
            q.contains("what was ghostmode") ||
            q.contains("tell me about ghostmode") ->
                "GhostMode was Jon Dean's first DeanWay app and the proven predecessor to QuietOS attention handling. It demonstrated notification capture, calm digest behavior, emergency bypass, local processing, and physical-device testing before QuietOS became a separate product."

            q.contains("who owns permissions") ||
            q.contains("does gemma control") ||
            q.contains("gemma permissions") ||
            q.contains("who controls permissions") ->
                "QuietOS owns permissions, tools, notification handling, state, policy, and execution. Gemma handles conversation, language, context, summarization, and reasoning. Gemma does not directly control Android or connected accounts."

            q.contains("what is quietos") ||
            q.contains("purpose of quietos") ||
            q.contains("quietos purpose") ->
                "QuietOS is a local-first Android attention-intelligence and personal-assistant system by DeanWay Labs. It captures, analyzes, classifies, and times notification delivery while keeping an explainable record. Its attention classes are Emergency to Now, Important to Soon, Useful to Digest, and Noise to Quiet."

            else -> null
        }
    }

    val personalCore: String = """
- The human user is Jon Dean.
- Jon owns DeanWay LLC and DeanWay Labs and is the builder/product owner of QuietOS.
- Gemma is Jon's personal assistant inside QuietOS.
- In Jon's messages, I/me/my refer to Jon unless he explicitly asks about Gemma.
- Do not invent DeanWay or Jon facts not supplied by QuietOS memory or the current conversation.
""".trimIndent()
}
