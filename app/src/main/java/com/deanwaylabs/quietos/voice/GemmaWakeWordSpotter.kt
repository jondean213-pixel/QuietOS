package com.deanwaylabs.quietos.voice

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import com.k2fsa.sherpa.onnx.KeywordSpotter
import com.k2fsa.sherpa.onnx.KeywordSpotterConfig
import com.k2fsa.sherpa.onnx.OnlineStream
import com.k2fsa.sherpa.onnx.getFeatureConfig
import com.k2fsa.sherpa.onnx.getKwsModelConfig
import kotlin.concurrent.thread

/**
 * Lightweight local wake-word listener. It never starts Android SpeechRecognizer.
 * SpeechRecognizer is reserved for the command AFTER "Gemma" is detected.
 */
class GemmaWakeWordSpotter(
    private val context: Context,
    private val onWake: () -> Unit,
    private val onError: (String) -> Unit
) {
    private val sampleRate = 16000
    @Volatile private var running = false
    private var recorder: AudioRecord? = null
    private var worker: Thread? = null
    private var kws: KeywordSpotter? = null
    private var stream: OnlineStream? = null

    fun start() {
        if (running) return
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            onError("Microphone permission required")
            return
        }
        try {
            if (kws == null) {
                val model = getKwsModelConfig(type = 1) ?: error("English wake model unavailable")
                val config = KeywordSpotterConfig(
                    featConfig = getFeatureConfig(sampleRate = sampleRate, featureDim = 80),
                    modelConfig = model,
                    keywordsFile = "sherpa-onnx-kws-zipformer-gigaspeech-3.3M-2024-01-01/keywords.txt",
                    keywordsScore = 2.0f,
                    keywordsThreshold = 0.20f,
                    numTrailingBlanks = 1
                )
                kws = KeywordSpotter(context.assets, config)
            }
            stream = kws!!.createStream()
            val minBytes = AudioRecord.getMinBufferSize(
                sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
            )
            recorder = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                maxOf(minBytes * 2, 3200)
            )
            recorder!!.startRecording()
            running = true
            worker = thread(name = "quietos-gemma-wake", start = true) { loop() }
        } catch (t: Throwable) {
            stop()
            onError(t.message ?: t.javaClass.simpleName)
        }
    }

    private fun loop() {
        val buffer = ShortArray(1600)
        while (running) {
            val n = recorder?.read(buffer, 0, buffer.size) ?: break
            if (n <= 0) continue
            val samples = FloatArray(n) { buffer[it] / 32768.0f }
            val current = stream ?: break
            current.acceptWaveform(samples, sampleRate)
            val spotter = kws ?: break
            while (spotter.isReady(current)) {
                spotter.decode(current)
                if (spotter.getResult(current).keyword.isNotBlank()) {
                    spotter.reset(current)
                    running = false
                    context.mainExecutor.execute { onWake() }
                    break
                }
            }
        }
        releaseRecorder()
    }

    fun stop() {
        running = false
        releaseRecorder()
        worker = null
        stream?.release()
        stream = null
    }

    private fun releaseRecorder() {
        val r = recorder ?: return
        runCatching { r.stop() }
        r.release()
        recorder = null
    }

    fun release() {
        stop()
        kws?.release()
        kws = null
    }
}
