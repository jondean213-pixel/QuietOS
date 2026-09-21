package com.deanwaylabs.quietos.calls

import android.telecom.Call
import android.telecom.CallScreeningService

/**
 * QuietOS call-screening boundary.
 *
 * Milestone 1 is intentionally conservative: it records the screening path
 * but allows calls until the local trust/risk rules have been physically
 * verified. QuietOS, not Gemma, owns the real-time call decision.
 */
class QuietCallScreeningService : CallScreeningService() {
    override fun onScreenCall(callDetails: Call.Details) {
        val response = CallResponse.Builder()
            .setDisallowCall(false)
            .setRejectCall(false)
            .setSilenceCall(false)
            .setSkipCallLog(false)
            .setSkipNotification(false)
            .build()
        respondToCall(callDetails, response)
    }
}
