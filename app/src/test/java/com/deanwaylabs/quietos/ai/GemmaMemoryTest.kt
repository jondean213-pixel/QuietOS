package com.deanwaylabs.quietos.ai

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GemmaMemoryTest {
    @Test
    fun whoIsQuietOSRoutesToDeterministicMemory() {
        val answer = GemmaMemory.answerKnownQuestion("who is QuietOS")
        assertNotNull(answer)
        assertTrue(answer!!.contains("DeanWay Labs"))
        assertTrue(answer.contains("local-first Android attention-intelligence"))
    }

    @Test
    fun tellMeAboutQuietOSRoutesToDeterministicMemory() {
        val answer = GemmaMemory.answerKnownQuestion("tell me about QuietOS")
        assertNotNull(answer)
        assertTrue(answer!!.contains("DeanWay Labs"))
    }
}
