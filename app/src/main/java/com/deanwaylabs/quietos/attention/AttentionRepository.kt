package com.deanwaylabs.quietos.attention

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class AttentionRepository(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("quietos_attention", Context.MODE_PRIVATE)

    fun add(record: AttentionRecord) {
        val current = readAll().toMutableList()

        val withoutDuplicate = current.filterNot { existing ->
            isDuplicate(existing, record)
        }.toMutableList()

        withoutDuplicate.add(0, record)
        val limited = withoutDuplicate.take(MAX_RECORDS)

        val array = JSONArray()
        limited.forEach { item ->
            array.put(
                JSONObject()
                    .put("timestampMs", item.timestampMs)
                    .put("packageName", item.packageName)
                    .put("title", item.title)
                    .put("text", item.text)
                    .put("classification", item.classification.name)
                    .put("reason", item.reason)
                    .put("originalCancelled", item.originalCancelled)
            )
        }
        prefs.edit().putString(KEY_RECORDS, array.toString()).apply()
    }

    fun clear() {
        prefs.edit().remove(KEY_RECORDS).apply()
    }

    fun readAll(): List<AttentionRecord> {
        val raw = prefs.getString(KEY_RECORDS, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    val classification = runCatching {
                        AttentionClass.valueOf(item.optString("classification"))
                    }.getOrDefault(AttentionClass.DIGEST)

                    add(
                        AttentionRecord(
                            timestampMs = item.optLong("timestampMs"),
                            packageName = item.optString("packageName"),
                            title = item.optString("title"),
                            text = item.optString("text"),
                            classification = classification,
                            reason = item.optString("reason"),
                            originalCancelled = item.optBoolean("originalCancelled", false)
                        )
                    )
                }
            }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    private fun isDuplicate(existing: AttentionRecord, incoming: AttentionRecord): Boolean {
        if (existing.packageName != incoming.packageName) return false

        val existingTitle = existing.title.trim()
        val incomingTitle = incoming.title.trim()
        val existingText = existing.text.trim()
        val incomingText = incoming.text.trim()

        val sameContent = existingTitle == incomingTitle && existingText == incomingText
        if (!sameContent) return false

        val delta = kotlin.math.abs(existing.timestampMs - incoming.timestampMs)
        return delta <= DUPLICATE_WINDOW_MS
    }

    companion object {
        private const val KEY_RECORDS = "records"
        private const val MAX_RECORDS = 100
        private const val DUPLICATE_WINDOW_MS = 5 * 60 * 1000L
    }
}
