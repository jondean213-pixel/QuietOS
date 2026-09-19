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
                maxNumTokens = 512
            ))
            created.initialize()

            val warmupStarted = System.nanoTime()
            val createdConversation = created.createConversation(
                ConversationConfig(
                    systemInstruction = Contents.of(
                        "You are Gemma, Jon's local QuietOS assistant. Talk naturally and directly. Keep answers concise unless more detail is requested. Respond to what Jon just said instead of restating his prompt. Avoid generic praise, filler, and brochure-style language. Ask at most one useful question when appropriate. Finish complete thoughts."
                    ),
                    prefillPrefaceOnInit = true,
                    maxOutputToken = 96,
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
        val activeConversation = checkNotNull(conversation) { "Local model conversation is not ready." }
        val output = StringBuilder()
        var chunks = 0
        val startedNs = System.nanoTime()
        var firstChunkNs: Long? = null
        withTimeout(180_000L) {
            activeConversation.sendMessageAsync(message).collect { chunk ->
                if (firstChunkNs == null) firstChunkNs = System.nanoTime()
                chunks++
                output.append(chunk)
            }
        }
        val finishedNs = System.nanoTime()
        check(chunks > 0) { "LiteRT-LM completed without producing response chunks." }
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
