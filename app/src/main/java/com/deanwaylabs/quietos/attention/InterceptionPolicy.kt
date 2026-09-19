package com.deanwaylabs.quietos.attention

class InterceptionPolicy {
    fun shouldCancelOriginal(classification: AttentionClass, interceptionEnabled: Boolean): Boolean {
        if (!interceptionEnabled) return false
        return classification == AttentionClass.DIGEST || classification == AttentionClass.QUIET
    }
}
