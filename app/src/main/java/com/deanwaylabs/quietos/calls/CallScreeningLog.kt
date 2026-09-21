package com.deanwaylabs.quietos.calls

import android.content.Context

class CallScreeningLog(context: Context) {
    private val prefs = context.getSharedPreferences("quietos_call_screening", Context.MODE_PRIVATE)
    fun record(number: String, decision: CallDecision) {
        val old = prefs.getString("history", "").orEmpty()
        val safe = if (number.length > 4) "***" + number.takeLast(4) else number
        val line = java.lang.System.currentTimeMillis().toString() + "|" + safe + "|" + decision.risk + "|" + decision.reason
        prefs.edit().putString("history", (line + "\n" + old).lineSequence().take(100).joinToString("\n")).apply()
    }
}
