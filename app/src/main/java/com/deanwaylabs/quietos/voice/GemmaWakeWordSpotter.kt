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
 *
 * Restart safety matters because model/voice pickers temporarily release the microphone.
 * Each worker owns its own AudioRecord and stream so an old worker can never tear down
 * a newly restarted wake session.
 */
class GemmaWakeWordSpotter(
    private val context: Context,
    private val onWake: () -> Unit,
    private val onError: (String) -> Unit
) {
    private val sampleRate = 16000
    private val stateLock = Any()
    @Volatile private var running = false
    @Volatile private var generation = 0L
    private var recorder: AudioRecord? = null
    private var worker: Thread? = null
    private var kws: KeywordSpotter? = null

    fun start() {
        synchronized(stateLock) {
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

                val localStream = kws!!.createStream()
                val minBytes = AudioRecord.getMinBufferSize(
                    sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
                )
                val localRecorder = AudioRecord(
                    MediaRecorder.AudioSource.VOICE_RECOGNITION,
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    maxOf(minBytes * 2, 3200)
                )
                localRecorder.startRecording()

                generation += 1L
                val token = generation
                recorder = localRecorder
                running = true
                worker = thread(name = "quietos-gemma-wake", start = true) {
                    loop(token, localRecorder, localStream)
                }
            } catch (t: Throwable) {
                running = false
                recorder = null
                worker = null
                onError(t.message ?: t.javaClass.simpleName)
            }
        }
    }

    private fun loop(token: Long, localRecorder: AudioRecord, localStream: OnlineStream) {
        val buffer = ShortArray(1600)
        try {
            while (running && generation == token) {
                val n = localRecorder.read(buffer, 0, buffer.size)
                if (n <= 0) continue
                val samples = FloatArray(n) { buffer[it] / 32768.0f }
                localStream.acceptWaveform(samples, sampleRate)
                val spotter = kws ?: break
                while (running && generation == token && spotter.isReady(localStream)) {
                    spotter.decode(localStream)
                    if (spotter.getResult(localStream).keyword.isNotBlank()) {
                        spotter.reset(localStream)
                        synchronized(stateLock) {
                            if (generation == token) running = false
                        }
                        context.mainExecutor.execute { onWake() }
                        return
                    }
                }
            }
        } catch (_: Throwable) {
            if (running && generation == token) {
                context.mainExecutor.execute { onError("Wake microphone session ended unexpectedly") }
            }
        } finally {
            runCatching { localRecorder.stop() }
            localRecorder.release()
            localStream.release()
            synchronized(stateLock) {
                if (generation == token) {
                    if (recorder === localRecorder) recorder = null
                    worker = null
                    running = false
                }
            }
        }
    }

    fun stop() {
        val localRecorder: AudioRecord?
        synchronized(stateLock) {
            generation += 1L
            running = false
            localRecorder = recorder
            recorder = null
            worker = null
        }
        localRecorder?.let { r ->
            runCatching { r.stop() }
        }
    }

    fun release() {
        stop()
        synchronized(stateLock) {
            kws?.release()
            kws = null
        }
    }
}
