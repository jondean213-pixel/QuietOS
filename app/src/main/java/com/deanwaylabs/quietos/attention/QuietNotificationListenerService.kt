package com.deanwaylabs.quietos.attention

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class QuietNotificationListenerService : NotificationListenerService() {
    private val engine by lazy { AttentionEngine() }
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

        repository.add(
            AttentionRecord(
                timestampMs = posted.postTime,
                packageName = posted.packageName,
                title = title,
                text = text,
                classification = decision.classification,
                reason = decision.reason
            )
        )

        // Alpha safety boundary:
        // capture + classify only. QuietOS does not cancel, delay, or suppress the
        // original Android notification until physical classification evidence exists.
    }
}
