package com.deanwaylabs.quietos.messages

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.telephony.SmsManager

class RespondViaMessageService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val destination = intent?.data?.schemeSpecificPart.orEmpty()
        val message = intent?.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
        if (destination.isNotBlank() && message.isNotBlank()) {
            runCatching { SmsManager.getDefault().sendTextMessage(destination, null, message, null, null) }
        }
        stopSelf(startId)
        return START_NOT_STICKY
    }
}
