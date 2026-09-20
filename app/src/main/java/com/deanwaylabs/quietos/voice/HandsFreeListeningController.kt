package com.deanwaylabs.quietos.voice

/**
 * Run #92 hands-free voice foundation.
 *
 * This controller deliberately owns only listening state. It does not change
 * command routing, Gemma routing, or Attention Engine behavior.
 */
class HandsFreeListeningController {
    enum class State {
        OFF,
        READY,
        LISTENING,
        PROCESSING,
        ERROR
    }

    private var currentState: State = State.OFF

    val state: State
        get() = currentState

    fun enable() {
        currentState = State.READY
    }

    fun disable() {
        currentState = State.OFF
    }

    fun listeningStarted() {
        if (currentState != State.OFF) currentState = State.LISTENING
    }

    fun speechReceived() {
        if (currentState != State.OFF) currentState = State.PROCESSING
    }

    fun readyForNextUtterance() {
        if (currentState != State.OFF) currentState = State.READY
    }

    fun failed() {
        if (currentState != State.OFF) currentState = State.ERROR
    }
}
