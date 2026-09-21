package com.deanwaylabs.quietos.messages

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class QuietMmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // MMS role boundary is declared so Android can recognize QuietOS as an SMS app.
        // Full MMS ingestion/quarantine is intentionally deferred until SMS behavior is physically proven.
    }
}
