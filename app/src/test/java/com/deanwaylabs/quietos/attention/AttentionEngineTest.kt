package com.deanwaylabs.quietos.attention

import org.junit.Assert.assertEquals
import org.junit.Test

class AttentionEngineTest {
    private val engine = AttentionEngine()

    @Test fun emergencyIsNow() =
        assertEquals(AttentionClass.NOW, engine.classify(AttentionInput("hospital emergency")).classification)

    @Test fun directRequestIsSoon() =
        assertEquals(AttentionClass.SOON, engine.classify(AttentionInput("Need a cruise quote", isDirectRequest = true)).classification)

    @Test fun promotionIsQuiet() =
        assertEquals(AttentionClass.QUIET, engine.classify(AttentionInput("sale", isPromotional = true)).classification)

    @Test fun ordinaryEventIsDigest() =
        assertEquals(AttentionClass.DIGEST, engine.classify(AttentionInput("package delivered")).classification)
}
