package com.deanwaylabs.quietos.ai

import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

class LiteRtQwenModel : LocalModel {
    @Volatile
    override var state: ModelState = ModelState.UNLOADED
        private set

    private var engine: Engine? = null

    override suspend fun load(modelPath: String): LoadMetrics = withContext(Dispatchers.IO) {
        close()
        state = ModelState.LOADING
        val started = System.nanoTime()
        try {
            val created = Engine(
                EngineConfig(
                    modelPath = modelPath,
                    backend = Backend.CPU()
                )
            )
            created.initialize()
            engine = created
            state = ModelState.READY
            LoadMetrics(
                loadTimeMs = (System.nanoTime() - started) / 1_000_000,
                modelPath = modelPath
            )
        } catch (t: Throwable) {
            state = ModelState.ERROR
            throw t
        }
    }

    override suspend fun send(message: String): String {
        val active = checkNotNull(engine) { "Local model is not loaded." }
        val output = StringBuilder()
        var chunks = 0
        withTimeout(180_000L) {
            active.createConversation().use { conversation ->
                conversation.sendMessageAsync(message).collect { chunk ->
                    chunks++
                    output.append(chunk)
                }
            }
        }
        check(chunks > 0) { "LiteRT-LM completed without producing response chunks." }
        return output.toString()
    }

    override fun close() {
        engine?.close()
        engine = null
        state = ModelState.UNLOADED
    }
}
