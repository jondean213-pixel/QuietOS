package com.deanwaylabs.quietos.voice

import org.junit.Assert.assertEquals
import org.junit.Test

class HandsFreeListeningControllerTest {
    @Test
    fun enableMovesControllerToReady() {
        val controller = HandsFreeListeningController()
        controller.enable()
        assertEquals(HandsFreeListeningController.State.READY, controller.state)
    }

    @Test
    fun listeningLifecycleReturnsToReady() {
        val controller = HandsFreeListeningController()
        controller.enable()
        controller.listeningStarted()
        assertEquals(HandsFreeListeningController.State.LISTENING, controller.state)
        controller.speechReceived()
        assertEquals(HandsFreeListeningController.State.PROCESSING, controller.state)
        controller.readyForNextUtterance()
        assertEquals(HandsFreeListeningController.State.READY, controller.state)
    }

    @Test
    fun disabledControllerIgnoresListeningEvents() {
        val controller = HandsFreeListeningController()
        controller.listeningStarted()
        controller.speechReceived()
        assertEquals(HandsFreeListeningController.State.OFF, controller.state)
    }

    @Test
    fun disableAlwaysReturnsToOff() {
        val controller = HandsFreeListeningController()
        controller.enable()
        controller.listeningStarted()
        controller.disable()
        assertEquals(HandsFreeListeningController.State.OFF, controller.state)
    }
}
