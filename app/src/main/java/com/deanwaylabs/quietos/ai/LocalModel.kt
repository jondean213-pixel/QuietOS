package com.deanwaylabs.quietos.ai

interface LocalModel : AutoCloseable {
    val state: ModelState
    suspend fun load(modelPath: String): LoadMetrics
    suspend fun send(message: String): String
    override fun close()
}

enum class ModelState { UNLOADED, LOADING, READY, ERROR }

data class LoadMetrics(
    val loadTimeMs: Long,
    val modelPath: String
)
