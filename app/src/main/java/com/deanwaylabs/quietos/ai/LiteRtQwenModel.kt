package com.deanwaylabs.quietos.ai

import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ThinkingConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

class LiteRtQwenModel : LocalModel {
    @Volatile
    override var state: ModelState = ModelState.UNLOADED
        private set

    private var engine: Engine? = null
    private var conversation: Conversation? = null

    override suspend fun load(modelPath: String): LoadMetrics = withContext(Dispatchers.IO) {
        close()
        state = ModelState.LOADING
        val started = System.nanoTime()
        try {
            val created = Engine(EngineConfig(
                modelPath = modelPath,
                backend = Backend.CPU(threadCount = 4),
                maxNumTokens = 1024
            ))
            created.initialize()

            val warmupStarted = System.nanoTime()
            val createdConversation = created.createConversation(
                ConversationConfig(
                    systemInstruction = Contents.of(
                        """You are Gemma, Jon's local QuietOS assistant. Talk naturally and directly. Keep answers concise unless more detail is requested. Respond to what Jon just said instead of restating his prompt. Avoid generic praise, filler, and brochure-style language. Ask at most one useful question when appropriate. Finish complete thoughts. Never stop in the middle of a sentence. If you need to be brief, finish the current sentence cleanly before ending.

Personal memory supplied by QuietOS:
${GemmaMemory.personalCore}

Authoritative QuietOS core reference:
- QuietOS is a local-first Android attention-intelligence and personal-assistant project by DeanWay Labs.
- Its Alpha 0.1 goal is to handle notification attention intelligently: capture, analyze, classify, decide delivery timing, and keep an explainable record.
- Attention classes are Emergency -> Now, Important -> Soon, Useful -> Digest, and Noise -> Quiet.
- Emergency handling must keep a deterministic safety path and must not rely only on generative AI.
- Jon Dean is the owner and builder behind DeanWay Labs and QuietOS. In this personal build, Jon is the primary user.
- Gemma is Jon Dean's personal assistant identity inside QuietOS.
- Gemma 3 1B IT INT4 is the current local model candidate running through LiteRT-LM.
- QuietOS owns permissions, tools, notification handling, state, policy, and execution. Gemma handles conversation, language, context, summarization, and reasoning.
- Future service or phone actions must go through explicit QuietOS tools and permissions. Gemma does not directly control Android or connected accounts.
- QuietOS is not a Linux distribution and is not a generic privacy-mode operating system.

Grounding rules:
- Treat the QuietOS core reference above as authoritative.
- Do not invent facts about QuietOS, DeanWay, Jon, your training, memory, reading, tool access, or external sources.
- Only claim knowledge that comes from the current conversation or context QuietOS explicitly provides.
- If you do not know something, say so plainly.
- Never imply you have read, remembered, trained on, or accessed information unless QuietOS actually supplied it.
"""
                    ),
                    prefillPrefaceOnInit = true,
                    maxOutputToken = 128,
                    thinkingConfig = ThinkingConfig(enableThinking = false)
                )
            )
            val warmupTimeMs = (System.nanoTime() - warmupStarted) / 1_000_000
            engine = created
            conversation = createdConversation
            state = ModelState.READY
            LoadMetrics(
                loadTimeMs = (System.nanoTime() - started) / 1_000_000,
                modelPath = modelPath,
                warmupTimeMs = warmupTimeMs
            )
        } catch (t: Throwable) {
            state = ModelState.ERROR
            throw t
        }
    }

    override suspend fun send(message: String): GenerationResult {
        val startedNs = System.nanoTime()

        GemmaMemory.answerKnownQuestion(message)?.let { known ->
            val finishedNs = System.nanoTime()
            return GenerationResult(
                text = known,
                metrics = GenerationMetrics(
                    timeToFirstChunkMs = 0,
                    totalTimeMs = (finishedNs - startedNs) / 1_000_000,
                    generationAfterFirstChunkMs = 0,
                    chunkCount = 0,
                    outputChars = known.length
                )
            )
        }
        val output = StringBuilder()
        var chunks = 0
        var firstChunkNs: Long? = null

        suspend fun collectFrom(activeConversation: Conversation, prompt: String) {
            withTimeout(180_000L) {
                activeConversation.sendMessageAsync(prompt).collect { chunk ->
                    if (firstChunkNs == null) firstChunkNs = System.nanoTime()
                    chunks++
                    output.append(chunk)
                }
            }
        }

        fun needsCompletion(text: String): Boolean {
            val trimmed = text.trimEnd()
            if (trimmed.isEmpty()) return false
            return trimmed.last() !in listOf('.', '!', '?', '…', '”', '"', '\'', ')', ']', '}')
        }

        val firstConversation = checkNotNull(conversation) { "Local model conversation is not ready." }
        try {
            collectFrom(firstConversation, message)
        } catch (t: Throwable) {
            val overflow = t.message?.contains("Input token ids are too long", ignoreCase = true) == true
            if (!overflow) throw t

            firstConversation.close()
            val activeEngine = checkNotNull(engine) { "Local model engine is not ready." }
            val recovered = activeEngine.createConversation(
                ConversationConfig(
                    systemInstruction = Contents.of(
                        """You are Gemma, Jon's local QuietOS assistant. Talk naturally and directly. Keep answers concise unless more detail is requested. Respond to what Jon just said instead of restating his prompt. Avoid generic praise, filler, and brochure-style language. Ask at most one useful question when appropriate. Finish complete thoughts. Never stop in the middle of a sentence. If you need to be brief, finish the current sentence cleanly before ending.

Personal memory supplied by QuietOS:
${GemmaMemory.personalCore}

Authoritative QuietOS core reference:
- QuietOS is a local-first Android attention-intelligence and personal-assistant project by DeanWay Labs.
- Its Alpha 0.1 goal is to handle notification attention intelligently: capture, analyze, classify, decide delivery timing, and keep an explainable record.
- Attention classes are Emergency -> Now, Important -> Soon, Useful -> Digest, and Noise -> Quiet.
- Emergency handling must keep a deterministic safety path and must not rely only on generative AI.
- Jon Dean is the owner and builder behind DeanWay Labs and QuietOS. In this personal build, Jon is the primary user.
- Gemma is Jon Dean's personal assistant identity inside QuietOS.
- Gemma 3 1B IT INT4 is the current local model candidate running through LiteRT-LM.
- QuietOS owns permissions, tools, notification handling, state, policy, and execution. Gemma handles conversation, language, context, summarization, and reasoning.
- Future service or phone actions must go through explicit QuietOS tools and permissions. Gemma does not directly control Android or connected accounts.
- QuietOS is not a Linux distribution and is not a generic privacy-mode operating system.

Grounding rules:
- Treat the QuietOS core reference above as authoritative.
- Do not invent facts about QuietOS, DeanWay, Jon, your training, memory, reading, tool access, or external sources.
- Only claim knowledge that comes from the current conversation or context QuietOS explicitly provides.
- If you do not know something, say so plainly.
- Never imply you have read, remembered, trained on, or accessed information unless QuietOS actually supplied it.
"""
                    ),
                    prefillPrefaceOnInit = true,
                    maxOutputToken = 128,
                    thinkingConfig = ThinkingConfig(enableThinking = false)
                )
            )
            conversation = recovered
            output.clear()
            chunks = 0
            firstChunkNs = null
            collectFrom(recovered, message)
        }

        if (chunks == 0) {
            firstConversation.close()
            val activeEngine = checkNotNull(engine) { "Local model engine is not ready." }
            val recovered = activeEngine.createConversation(
                ConversationConfig(
                    systemInstruction = Contents.of(
                        """You are Gemma, Jon's local QuietOS assistant. Talk naturally and directly. Keep answers concise unless more detail is requested. Respond to what Jon just said instead of restating his prompt. Avoid generic praise, filler, and brochure-style language. Ask at most one useful question when appropriate. Finish complete thoughts. Never stop in the middle of a sentence. If you need to be brief, finish the current sentence cleanly before ending.

Personal memory supplied by QuietOS:
${GemmaMemory.personalCore}

Authoritative QuietOS core reference:
- QuietOS is a local-first Android attention-intelligence and personal-assistant project by DeanWay Labs.
- Its Alpha 0.1 goal is to handle notification attention intelligently: capture, analyze, classify, decide delivery timing, and keep an explainable record.
- Attention classes are Emergency -> Now, Important -> Soon, Useful -> Digest, and Noise -> Quiet.
- Emergency handling must keep a deterministic safety path and must not rely only on generative AI.
- Jon Dean is the owner and builder behind DeanWay Labs and QuietOS. In this personal build, Jon is the primary user.
- Gemma is Jon Dean's personal assistant identity inside QuietOS.
- Gemma 3 1B IT INT4 is the current local model candidate running through LiteRT-LM.
- QuietOS owns permissions, tools, notification handling, state, policy, and execution. Gemma handles conversation, language, context, summarization, and reasoning.
- Future service or phone actions must go through explicit QuietOS tools and permissions. Gemma does not directly control Android or connected accounts.
- QuietOS is not a Linux distribution and is not a generic privacy-mode operating system.

Grounding rules:
- Treat the QuietOS core reference above as authoritative.
- Do not invent facts about QuietOS, DeanWay, Jon, your training, memory, reading, tool access, or external sources.
- Only claim knowledge that comes from the current conversation or context QuietOS explicitly provides.
- If you do not know something, say so plainly.
- Never imply you have read, remembered, trained on, or accessed information unless QuietOS actually supplied it.
"""
                    ),
                    prefillPrefaceOnInit = true,
                    maxOutputToken = 128,
                    thinkingConfig = ThinkingConfig(enableThinking = false)
                )
            )
            conversation = recovered
            collectFrom(recovered, message)
        }

        if (chunks > 0 && needsCompletion(output.toString())) {
            val activeConversation = checkNotNull(conversation) { "Local model conversation is not ready." }
            output.append(" ")
            collectFrom(
                activeConversation,
                "Finish only the incomplete final sentence from your previous answer. Continue naturally from where you stopped, use one short clause or sentence, and do not repeat earlier text."
            )
        }

        val finishedNs = System.nanoTime()
        if (chunks == 0) {
            state = ModelState.READY
            val fallback = "I hit a local generation error and reset my conversation. Please try that again."
            return GenerationResult(
                text = fallback,
                metrics = GenerationMetrics(
                    timeToFirstChunkMs = 0,
                    totalTimeMs = (finishedNs - startedNs) / 1_000_000,
                    generationAfterFirstChunkMs = 0,
                    chunkCount = 0,
                    outputChars = fallback.length
                )
            )
        }

        val firstNs = checkNotNull(firstChunkNs)
        val ttfc = (firstNs - startedNs) / 1_000_000
        val total = (finishedNs - startedNs) / 1_000_000
        return GenerationResult(
            text = output.toString(),
            metrics = GenerationMetrics(
                timeToFirstChunkMs = ttfc,
                totalTimeMs = total,
                generationAfterFirstChunkMs = (finishedNs - firstNs) / 1_000_000,
                chunkCount = chunks,
                outputChars = output.length
            )
        )
    }

    override fun close() {
        conversation?.close()
        conversation = null
        engine?.close()
        engine = null
        state = ModelState.UNLOADED
    }
}
