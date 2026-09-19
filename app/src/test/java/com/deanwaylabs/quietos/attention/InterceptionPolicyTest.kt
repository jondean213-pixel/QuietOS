package com.deanwaylabs.quietos.attention

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InterceptionPolicyTest {
    private val policy = InterceptionPolicy()

    @Test fun offNeverCancels() {
        AttentionClass.entries.forEach { classification ->
            assertFalse(policy.shouldCancelOriginal(classification, interceptionEnabled = false))
        }
    }

    @Test fun nowNeverCancels() =
        assertFalse(policy.shouldCancelOriginal(AttentionClass.NOW, interceptionEnabled = true))

    @Test fun soonNeverCancels() =
        assertFalse(policy.shouldCancelOriginal(AttentionClass.SOON, interceptionEnabled = true))

    @Test fun digestCancelsInTestMode() =
        assertTrue(policy.shouldCancelOriginal(AttentionClass.DIGEST, interceptionEnabled = true))

    @Test fun quietCancelsInTestMode() =
        assertTrue(policy.shouldCancelOriginal(AttentionClass.QUIET, interceptionEnabled = true))
}
