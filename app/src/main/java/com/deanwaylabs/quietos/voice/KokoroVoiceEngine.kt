package com.deanwaylabs.quietos.voice

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.k2fsa.sherpa.onnx.GenerationConfig
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsKokoroModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import java.io.File
import kotlin.math.roundToInt
import kotlin.math.min

/**
 * Isolated neural voice layer for the #146 keeper.
 *
 * Kokoro model files are intentionally NOT committed to the repository.
 * Expected app-private directory:
 * files/voices/kokoro/
 *   model.onnx
 *   voices.bin
 *   tokens.txt
 *   espeak-ng-data/
 *
 * Android system TTS remains the fallback until Motorola physical testing
 * promotes this layer to keeper status.
 */
class KokoroVoiceEngine(private val context: Context) {
    data class Metrics(
        val loadMs: Long,
        val synthesisMs: Long,
        val pcmMs: Long,
        val audioSetupMs: Long,
        val timeToPlaybackMs: Long,
        val audioDurationMs: Long,
        val speakerId: Int,
        val sampleRate: Int,
        val sampleCount: Int
    )

    private var tts: OfflineTts? = null
    private var track: AudioTrack? = null
    private var lastLoadMs: Long = 0
    private var playbackGeneration: Long = 0

    fun isInstalled(): Boolean {
        val d = modelDir()
        return File(d, "model.onnx").isFile &&
            File(d, "voices.bin").isFile &&
            File(d, "tokens.txt").isFile &&
            File(d, "espeak-ng-data").isDirectory
    }

    @Synchronized
    fun load(numThreads: Int = 4): Long {
        if (tts != null) return lastLoadMs
        require(isInstalled()) { "Kokoro voice files are not installed." }
        val d = modelDir()
        val config = OfflineTtsConfig(
            model = OfflineTtsModelConfig(
                kokoro = OfflineTtsKokoroModelConfig(
                    model = File(d, "model.onnx").absolutePath,
                    voices = File(d, "voices.bin").absolutePath,
                    tokens = File(d, "tokens.txt").absolutePath,
                    dataDir = File(d, "espeak-ng-data").absolutePath,
                ),
                numThreads = numThreads,
                debug = false,
                provider = "cpu",
            ),
            maxNumSentences = 2,
            silenceScale = 0.2f,
        )
        val started = System.nanoTime()
        tts = OfflineTts(config = config)
        lastLoadMs = (System.nanoTime() - started) / 1_000_000
        return lastLoadMs
    }

    @Synchronized
    fun numSpeakers(): Int {
        val engine = tts ?: run {
            load()
            requireNotNull(tts)
        }
        return engine.numSpeakers()
    }

    @Synchronized
    fun speak(text: String, speakerId: Int = 0, speed: Float = 1.0f): Metrics {
        require(text.isNotBlank()) { "Cannot synthesize blank text." }
        val speakStarted = System.nanoTime()
        val engine = tts ?: run {
            load()
            requireNotNull(tts)
        }
        stopPlayback()

        // Start Emma with the first natural sentence instead of forcing Jon to wait
        // for the entire reply to synthesize before hearing anything.
        val firstSentenceEnd = text.indexOfFirst { it == '.' || it == '!' || it == '?' }
        val firstSegment = if (firstSentenceEnd in 0 until text.lastIndex) {
            text.substring(0, firstSentenceEnd + 1).trim()
        } else {
            text.trim()
        }
        val remaining = if (firstSegment.length < text.length) {
            text.substring(firstSegment.length).trim()
        } else ""

        val started = System.nanoTime()
        val audio = engine.generateWithConfig(
            text = firstSegment,
            config = GenerationConfig(
                sid = speakerId,
                speed = speed,
                silenceScale = 0.2f,
            )
        )
        val synthesisMs = (System.nanoTime() - started) / 1_000_000
        require(audio.samples.isNotEmpty()) { "Kokoro returned no audio samples." }

        val pcmStarted = System.nanoTime()
        val pcm = ShortArray(audio.samples.size) { i ->
            (audio.samples[i].coerceIn(-1f, 1f) * Short.MAX_VALUE)
                .roundToInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                .toShort()
        }
        val pcmMs = (System.nanoTime() - pcmStarted) / 1_000_000
        val audioSetupStarted = System.nanoTime()
        val minBuffer = AudioTrack.getMinBufferSize(
            audio.sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(pcm.size * 2)

        track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(audio.sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(minBuffer)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
            .also { output ->
                output.write(pcm, 0, pcm.size)
                output.play()
            }

        val audioSetupMs = (System.nanoTime() - audioSetupStarted) / 1_000_000
        val timeToPlaybackMs = (System.nanoTime() - speakStarted) / 1_000_000

        // Continue the remainder after first audio is already playing. This keeps
        // the first-audio latency low without changing Gemma's generated answer.
        if (remaining.isNotBlank()) {
            val generation = ++playbackGeneration
            Thread {
                runCatching {
                    val rest = engine.generateWithConfig(
                        text = remaining,
                        config = GenerationConfig(
                            sid = speakerId,
                            speed = speed,
                            silenceScale = 0.2f,
                        )
                    )
                    if (rest.samples.isNotEmpty() && generation == playbackGeneration) {
                        val restPcm = ShortArray(rest.samples.size) { i ->
                            (rest.samples[i].coerceIn(-1f, 1f) * Short.MAX_VALUE)
                                .roundToInt()
                                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                                .toShort()
                        }
                        val firstDurationMs = (audio.samples.size * 1000L) / audio.sampleRate
                        Thread.sleep(firstDurationMs)
                        if (generation == playbackGeneration) {
                            val restBuffer = AudioTrack.getMinBufferSize(
                                rest.sampleRate,
                                AudioFormat.CHANNEL_OUT_MONO,
                                AudioFormat.ENCODING_PCM_16BIT
                            ).coerceAtLeast(restPcm.size * 2)
                            val nextTrack = AudioTrack.Builder()
                                .setAudioAttributes(
                                    AudioAttributes.Builder()
                                        .setUsage(AudioAttributes.USAGE_ASSISTANT)
                                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                                        .build()
                                )
                                .setAudioFormat(
                                    AudioFormat.Builder()
                                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                        .setSampleRate(rest.sampleRate)
                                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                        .build()
                                )
                                .setBufferSizeInBytes(restBuffer)
                                .setTransferMode(AudioTrack.MODE_STATIC)
                                .build()
                            nextTrack.write(restPcm, 0, restPcm.size)
                            nextTrack.play()
                            synchronized(this@KokoroVoiceEngine) {
                                track?.release()
                                track = nextTrack
                            }
                        }
                    }
                }
            }.start()
        }

        return Metrics(
            loadMs = lastLoadMs,
            synthesisMs = synthesisMs,
            pcmMs = pcmMs,
            audioSetupMs = audioSetupMs,
            timeToPlaybackMs = timeToPlaybackMs,
            audioDurationMs = (audio.samples.size * 1000L) / audio.sampleRate,
            speakerId = speakerId,
            sampleRate = audio.sampleRate,
            sampleCount = audio.samples.size,
        )
    }

    @Synchronized
    fun stopPlayback() {
        track?.let { output ->
            runCatching { output.stop() }
            output.release()
        }
        track = null
    }

    @Synchronized
    fun release() {
        stopPlayback()
        tts?.release()
        tts = null
    }

    private fun modelDir(): File = File(context.filesDir, "voices/kokoro")
}
