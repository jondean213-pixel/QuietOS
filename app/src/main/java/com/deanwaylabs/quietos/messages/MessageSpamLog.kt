package com.deanwaylabs.quietos.messages

import android.content.Context

class MessageSpamLog(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun add(
        timestampMs: Long,
        sender: String,
        text: String,
        risk: String,
        reason: String,
        suppressed: Boolean
    ) {
        val entry = buildString {
            append("[").append(risk).append("] ")
            append(sender.ifBlank { "Unknown sender" }.singleLine().take(80))
            append("\n")
            append(text.singleLine().take(240).ifBlank { "(no readable message text)" })
            append("\nWhy: ").append(reason.singleLine().take(180))
            append("\nAction: ").append(if (suppressed) "notification suppressed; message kept in SMS app" else "allowed")
            append("\nTime: ").append(timestampMs)
        }

        val updated = (listOf(entry) + readAll()).take(MAX_ENTRIES)
        prefs.edit().putStringSet(KEY_ENTRIES, updated.mapIndexed { index, value -> "%03d|%s".format(index, value) }.toSet()).apply()
    }

    fun readAll(): List<String> {
        return prefs.getStringSet(KEY_ENTRIES, emptySet())
            .orEmpty()
            .sorted()
            .map { it.substringAfter('|') }
    }

    fun clear() {
        prefs.edit().remove(KEY_ENTRIES).apply()
    }

    private fun String.singleLine(): String = replace(Regex("\\s+"), " ").trim()

    companion object {
        private const val PREFS = "quietos_message_spam_log"
        private const val KEY_ENTRIES = "entries"
        private const val MAX_ENTRIES = 100
    }
}
