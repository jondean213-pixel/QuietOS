package com.deanwaylabs.quietos.attention

import org.junit.Assert.assertEquals
import org.junit.Test

class AttentionEngineTest {
    private val engine = AttentionEngine()

    @Test fun emergencyIsNow() =
        assertEquals(AttentionClass.NOW, engine.classify(AttentionInput("hospital emergency")).classification)

    @Test fun emergencyBeatsPromotion() =
        assertEquals(
            AttentionClass.NOW,
            engine.classify(AttentionInput("Emergency hospital alert - limited time notice", isPromotional = true)).classification
        )

    @Test fun explicitDirectRequestIsSoon() =
        assertEquals(AttentionClass.SOON, engine.classify(AttentionInput("Need a cruise quote", isDirectRequest = true)).classification)

    @Test fun inferredDirectRequestIsSoon() =
        assertEquals(AttentionClass.SOON, engine.classify(AttentionInput("Can you call me when you get this?")).classification)

    @Test fun explicitPromotionIsQuiet() =
        assertEquals(AttentionClass.QUIET, engine.classify(AttentionInput("member offer", isPromotional = true)).classification)

    @Test fun inferredPromotionIsQuiet() =
        assertEquals(AttentionClass.QUIET, engine.classify(AttentionInput("Limited time sale - 25% off")).classification)

    @Test fun ordinaryEventIsDigest() =
        assertEquals(AttentionClass.DIGEST, engine.classify(AttentionInput("package delivered")).classification)
}
