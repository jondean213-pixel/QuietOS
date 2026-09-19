package com.deanwaylabs.quietos.ai

interface LocalModel : AutoCloseable {
    val state: ModelState
    suspend fun load(modelPath: String): LoadMetrics
    suspend fun send(message: String): GenerationResult
    override fun close()
}

enum class ModelState { UNLOADED, LOADING, READY, ERROR }

data class LoadMetrics(
    val loadTimeMs: Long,
    val modelPath: String,
    val warmupTimeMs: Long = 0L
)

data class GenerationMetrics(
    val timeToFirstChunkMs: Long,
    val totalTimeMs: Long,
    val generationAfterFirstChunkMs: Long,
    val chunkCount: Int,
    val outputChars: Int
)

data class GenerationResult(
    val text: String,
    val metrics: GenerationMetrics
)
