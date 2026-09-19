package com.deanwaylabs.quietos.attention

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class QuietNotificationListenerService : NotificationListenerService() {
    private val engine by lazy { AttentionEngine() }
    private val interceptionPolicy by lazy { InterceptionPolicy() }
    private val repository by lazy { AttentionRepository(this) }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val posted = sbn ?: return
        if (posted.packageName == packageName) return

        val extras = posted.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty().trim()
        val text = selectBestText(
            extras.getCharSequence(Notification.EXTRA_TEXT)?.toString(),
            extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString(),
            extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()
        )

        val combined = listOf(title, text)
            .filter { it.isNotBlank() }
            .joinToString(": ")
            .ifBlank { "(notification without readable text)" }

        val decision = engine.classify(AttentionInput(text = combined))
        val prefs = applicationContext.getSharedPreferences("quietos_attention", MODE_PRIVATE)
        val interceptionEnabled = prefs.getBoolean(KEY_INTERCEPTION_ENABLED, false)
        val shouldCancel = interceptionPolicy.shouldCancelOriginal(decision.classification, interceptionEnabled)

        if (shouldCancel) {
            // Android delivers onNotificationPosted only after the notification is posted.
            // This is therefore the fastest standard-app cancellation path, not true pre-delivery interception.
            cancelNotification(posted.key)
        }

        repository.add(
            AttentionRecord(
                timestampMs = posted.postTime,
                packageName = posted.packageName,
                title = title,
                text = text,
                classification = decision.classification,
                reason = decision.reason,
                originalCancelled = shouldCancel
            )
        )

    }

    private fun selectBestText(vararg rawParts: String?): String {
        val parts = rawParts
            .mapNotNull { it?.trim()?.takeIf(String::isNotEmpty) }
            .distinct()

        if (parts.isEmpty()) return ""

        val richest = parts.maxByOrNull { it.length }.orEmpty()
        val extras = parts.filter { part ->
            part != richest &&
                !richest.contains(part, ignoreCase = true) &&
                !part.contains(richest, ignoreCase = true)
        }

        return (listOf(richest) + extras).distinct().joinToString(" | ")
    }

    companion object {
        private const val KEY_INTERCEPTION_ENABLED = "interception_enabled"
    }
}
