package com.deanwaylabs.quietos

import org.junit.Assert.assertEquals
import org.junit.Test

class AssistantProfileTest {
    @Test fun personalAssistantIsGemma() = assertEquals("Gemma", AssistantProfile.personal().displayName)
    @Test fun productionCanBeRenamed() = assertEquals("Nova", AssistantProfile.production("Nova").displayName)
    @Test fun blankProductionNameFallsBack() = assertEquals("QuietOS", AssistantProfile.production(" ").displayName)
}
