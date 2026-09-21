package com.deanwaylabs.quietos.calls

import android.telecom.Call
import android.telecom.CallScreeningService

/** QuietOS owns real-time screening. Gemma is not on the call-decision path. */
class QuietCallScreeningService : CallScreeningService() {
    override fun onScreenCall(callDetails: Call.Details) {
        val number = callDetails.handle?.schemeSpecificPart
        val decision = CallRiskEngine(this).classify(number)
        CallScreeningLog(this).record(number.orEmpty(), decision)
        val response = CallResponse.Builder().setDisallowCall(decision.block).setRejectCall(decision.block).setSilenceCall(decision.silence && !decision.block).setSkipCallLog(false).setSkipNotification(false).build()
        respondToCall(callDetails, response)
    }
}
