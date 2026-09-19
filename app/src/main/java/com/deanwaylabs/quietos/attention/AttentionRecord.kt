package com.deanwaylabs.quietos.attention

data class AttentionRecord(
    val timestampMs: Long,
    val packageName: String,
    val title: String,
    val text: String,
    val classification: AttentionClass,
    val reason: String
)
