package com.deanwaylabs.quietos.attention

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.provider.Telephony
import android.os.Handler
import android.os.Looper
import com.deanwaylabs.quietos.messages.MessageSpamLog
import com.deanwaylabs.quietos.messages.MessageSpamRiskEngine

class QuietNotificationListenerService : NotificationListenerService() {
    private val engine by lazy { AttentionEngine() }
    private val interceptionPolicy by lazy { InterceptionPolicy() }
    private val repository by lazy { AttentionRepository(this) }
    private val messageSpamEngine by lazy { MessageSpamRiskEngine() }
    private val messageSpamLog by lazy { MessageSpamLog(this) }
    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }

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

        val spamPrefs = applicationContext.getSharedPreferences("quietos_message_spam", MODE_PRIVATE)
        val spamEnabled = spamPrefs.getBoolean("enabled", false)
        val defaultSmsPackage = runCatching { Telephony.Sms.getDefaultSmsPackage(this) }.getOrNull()
        val looksLikeSmsMessage = defaultSmsPackage != null &&
            posted.packageName == defaultSmsPackage &&
            (posted.notification.category == Notification.CATEGORY_MESSAGE ||
                extras.containsKey(Notification.EXTRA_MESSAGES))

        var spamSuppressed = false
        var spamReason: String? = null
        if (spamEnabled && looksLikeSmsMessage) {
            val spamDecision = messageSpamEngine.classify(title, text)
            spamSuppressed = spamDecision.suppressNotification
            spamReason = spamDecision.reason
            if (spamSuppressed) {
                // Some messaging apps update/re-post the same notification immediately after delivery.
                // Cancel now and repeat twice so the final updated notification is also removed.
                cancelNotification(posted.key)
                mainHandler.postDelayed({ runCatching { cancelNotification(posted.key) } }, 200L)
                mainHandler.postDelayed({ runCatching { cancelNotification(posted.key) } }, 700L)
            }
            messageSpamLog.add(
                timestampMs = posted.postTime,
                sender = title,
                text = text,
                risk = spamDecision.risk.name,
                reason = spamDecision.reason,
                suppressed = spamSuppressed
            )
        }

        val decision = engine.classify(AttentionInput(text = combined))
        val prefs = applicationContext.getSharedPreferences("quietos_attention", MODE_PRIVATE)
        val interceptionEnabled = prefs.getBoolean(KEY_INTERCEPTION_ENABLED, false)
        val attentionCancel = interceptionPolicy.shouldCancelOriginal(decision.classification, interceptionEnabled)
        val shouldCancel = spamSuppressed || attentionCancel

        if (attentionCancel && !spamSuppressed) {
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
                classification = if (spamSuppressed) AttentionClass.QUIET else decision.classification,
                reason = spamReason?.let { "Spam Shield: $it" } ?: decision.reason,
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
