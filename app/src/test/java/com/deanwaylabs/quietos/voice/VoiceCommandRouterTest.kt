package com.deanwaylabs.quietos.voice

import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceCommandRouterTest {
    private val router = VoiceCommandRouter()

    @Test fun summarizeDigest() =
        assertEquals(VoiceCommand.SUMMARIZE_DIGEST, router.route("Gemma, summarize my digest").command)

    @Test fun attentionOn() =
        assertEquals(VoiceCommand.ATTENTION_ON, router.route("Gemma turn Attention Mode on").command)

    @Test fun attentionOff() =
        assertEquals(VoiceCommand.ATTENTION_OFF, router.route("disable attention mode").command)

    @Test fun clearLog() =
        assertEquals(VoiceCommand.CLEAR_ATTENTION_LOG, router.route("clear the attention log").command)

    @Test fun refresh() =
        assertEquals(VoiceCommand.REFRESH_ATTENTION, router.route("refresh attention").command)

    @Test fun status() =
        assertEquals(VoiceCommand.ATTENTION_STATUS, router.route("Gemma what did QuietOS catch?").command)

    @Test fun ordinarySpeechFallsThroughToConversation() =
        assertEquals(VoiceCommand.CONVERSATION, router.route("What do you think about my day?").command)
}
