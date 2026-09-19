package com.deanwaylabs.quietos.voice

import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceTextNormalizerTest {
    private val normalizer = VoiceTextNormalizer()

    @Test fun fixesJonDean() =
        assertEquals("who is Jon Dean", normalizer.normalize("who is John Dean"))

    @Test fun fixesDeanWayLabsSpacing() =
        assertEquals("what is DeanWay Labs", normalizer.normalize("what is Dean way labs"))

    @Test fun fixesGreenwayLabsMisrecognition() =
        assertEquals("what is DeanWay Labs", normalizer.normalize("what is Greenway labs"))

    @Test fun fixesDeanWayTravels() =
        assertEquals("tell me about DeanWay Travels", normalizer.normalize("tell me about Dean way travels"))

    @Test fun fixesQuietOS() =
        assertEquals("what is QuietOS", normalizer.normalize("what is Quiet OS"))

    @Test fun fixesGhostMode() =
        assertEquals("what is GhostMode", normalizer.normalize("what is Ghost mode"))

    @Test fun leavesOrdinarySpeechAlone() =
        assertEquals("what is the weather today", normalizer.normalize("what is the weather today"))
}
