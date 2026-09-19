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
        val text = listOfNotNull(
            extras.getCharSequence(Notification.EXTRA_TEXT)?.toString(),
            extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString(),
            extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()
        )
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .joinToString(" | ")

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

    companion object {
        private const val KEY_INTERCEPTION_ENABLED = "interception_enabled"
    }
}
