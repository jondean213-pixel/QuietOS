package com.deanwaylabs.quietos

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.app.role.RoleManager
import android.provider.OpenableColumns
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.app.AlertDialog
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.documentfile.provider.DocumentFile
import com.deanwaylabs.quietos.voice.KokoroVoiceEngine
import com.deanwaylabs.quietos.ai.LiteRtQwenModel
import com.deanwaylabs.quietos.ai.ModelState
import com.deanwaylabs.quietos.attention.AttentionRepository
import com.deanwaylabs.quietos.voice.HandsFreeListeningController
import com.deanwaylabs.quietos.voice.GemmaWakeWordSpotter
import com.deanwaylabs.quietos.voice.VoiceCommand
import com.deanwaylabs.quietos.voice.VoiceCommandRouter
import com.deanwaylabs.quietos.voice.VoiceTextNormalizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private val model = LiteRtQwenModel()
    private lateinit var status: TextView
    private lateinit var transcript: TextView
    private lateinit var input: EditText
    private lateinit var send: Button
    private lateinit var choose: Button
    private lateinit var attentionStatus: TextView
    private lateinit var attentionLog: TextView
    private lateinit var interceptionButton: Button
    private val attentionRepository by lazy { AttentionRepository(this) }
    private val voiceRouter = VoiceCommandRouter()
    private val voiceTextNormalizer = VoiceTextNormalizer()
    private val handsFreeController = HandsFreeListeningController()
    private var speechRecognizer: SpeechRecognizer? = null
    private var handsFreeEnabled = false
    private var awaitingGemmaCommand = false
    private var speechRestartPending = false
    private var recognitionSessionActive = false
    private var handsFreeWakeArmed = true
    private var wakeSessionConsumed = false
    private var wakeDetectedInSession = false
    private var wakeWordSpotter: GemmaWakeWordSpotter? = null
    private lateinit var handsFreeStatus: TextView
    private var textToSpeech: TextToSpeech? = null
    private var ttsReady = false
    private var activeTtsUtteranceId: String? = null
    private val kokoroVoice by lazy { KokoroVoiceEngine(this) }
    private lateinit var neuralVoiceStatus: TextView

    private val voiceLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult
        val spoken = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
            ?.trim()
            .orEmpty()

        if (spoken.isNotEmpty()) handleVoiceInput(spoken)
    }

    private val microphonePermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) enableHandsFreeListening() else {
            handsFreeStatus.text = "Hands-free: microphone permission required"
            transcript.append("\nQuietOS: microphone permission is required for hands-free listening.\n")
        }
    }

    private val callScreeningRoleLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        updateCallScreeningStatus()
    }

    private val kokoroFolderPicker = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) lifecycleScope.launch { importKokoroFolder(uri) }
    }

    private val picker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            lifecycleScope.launch { importAndLoad(uri) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "QuietOS Alpha 0.1"
        setContentView(buildUi())
        initializeTextToSpeech()
        lifecycleScope.launch { autoLoadExistingModel() }
        startHandsFreeWithPermission()
    }

    private fun buildUi(): View {
        val pad = (20 * resources.displayMetrics.density).toInt()
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }
        ViewCompat.setOnApplyWindowInsetsListener(column) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            view.setPadding(
                pad,
                pad,
                pad,
                pad + maxOf(systemBars.bottom, ime.bottom)
            )
            insets
        }
        column.addView(TextView(this).apply {
            text = "QuietOS Alpha 0.1\nAssistant: Gemma"
            textSize = 22f
        })
        status = TextView(this).apply { text = "Gemma status: not loaded" }
        choose = Button(this).apply {
            text = "Choose Gemma model"
            setOnClickListener { picker.launch(arrayOf("*/*")) }
        }
        transcript = TextView(this).apply { text = "Conversation will appear here.\n" }
        input = EditText(this).apply { hint = "Message Gemma" }
        send = Button(this).apply {
            text = "Send"
            isEnabled = false
            setOnClickListener { sendMessage() }
        }
        handsFreeStatus = TextView(this).apply {
            text = "Hands-free: starting..."
            textSize = 14f
        }
        val talk = Button(this).apply {
            text = "Talk to Gemma"
            setOnClickListener { launchVoiceInput() }
        }
        val transcriptScroll = ScrollView(this).apply {
            isFillViewport = true
            addView(
                transcript,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )
            )
        }
        column.addView(status)
        column.addView(choose)
        column.addView(handsFreeStatus)
        neuralVoiceStatus = TextView(this).apply {
            text = if (kokoroVoice.isInstalled()) "Neural voice: Kokoro installed" else "Neural voice: Kokoro not installed"
            textSize = 14f
        }
        val importNeuralVoice = Button(this).apply {
            text = "Install Kokoro Voice Folder"
            setOnClickListener { kokoroFolderPicker.launch(null) }
        }
        val auditionEmma = Button(this).apply {
            text = "Audition Emma (British)"
            setOnClickListener { auditionKokoroEmma() }
        }
        column.addView(neuralVoiceStatus)
        column.addView(importNeuralVoice)
        column.addView(auditionEmma)

        attentionStatus = TextView(this).apply {
            text = "Attention Engine: capture not yet verified"
            textSize = 16f
        }
        val notificationAccess = Button(this).apply {
            text = "Enable Notification Access"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
        }
        interceptionButton = Button(this).apply {
            setOnClickListener {
                val prefs = getSharedPreferences("quietos_attention", MODE_PRIVATE)
                val next = !prefs.getBoolean("interception_enabled", false)
                setAttentionMode(next)
            }
        }
        updateInterceptionButton()

        val bucketRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf("NOW", "SOON", "DIGEST", "QUIET").forEach { bucket ->
            bucketRow.addView(Button(this).apply {
                text = bucket
                setOnClickListener { showAttentionBucket(bucket) }
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }
        val refreshAttention = Button(this).apply {
            text = "Refresh Attention Log"
            setOnClickListener { refreshAttentionLog() }
        }
        val summarizeDigest = Button(this).apply {
            text = "Gemma: Summarize Digest"
            setOnClickListener { summarizeDigestWithGemma() }
        }
        val clearAttention = Button(this).apply {
            text = "Clear Attention Log"
            setOnClickListener {
                attentionRepository.clear()
                refreshAttentionLog()
                transcript.append("\nQuietOS: attention log cleared.\n")
            }
        }
        attentionLog = TextView(this).apply {
            text = "No captured notifications yet."
            textSize = 14f
            setPadding(0, pad / 2, 0, pad / 2)
        }
        column.addView(attentionStatus)
        column.addView(notificationAccess)
        column.addView(interceptionButton)
        column.addView(bucketRow)
        column.addView(refreshAttention)
        column.addView(summarizeDigest)
        column.addView(clearAttention)
        // Keep captured call/notification details out of the main screen; bucket buttons expose them on demand.

        val callScreeningStatus = TextView(this).apply {
            id = View.generateViewId()
            tag = "call_screening_status"
            text = "Spam Call Protection: checking..."
            textSize = 16f
        }
        val enableCallScreening = Button(this).apply {
            text = "Enable Spam Call Protection"
            setOnClickListener { requestCallScreeningRole() }
        }
        column.addView(callScreeningStatus)
        column.addView(enableCallScreening)
        callScreeningStatus.post { updateCallScreeningStatus() }

        column.addView(
            transcriptScroll,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )
        column.addView(input)
        column.addView(talk)
        column.addView(send)
        return column
    }

    private suspend fun importKokoroFolder(uri: Uri) {
        neuralVoiceStatus.text = "Neural voice: importing Kokoro..."
        try {
            withContext(Dispatchers.IO) {
                val source = DocumentFile.fromTreeUri(this@MainActivity, uri)
                    ?: error("Could not open selected Kokoro folder.")
                val destination = File(filesDir, "voices/kokoro")
                if (destination.exists()) destination.deleteRecursively()
                destination.mkdirs()
                copyDocumentTree(source, destination)
            }
            require(kokoroVoice.isInstalled()) {
                "Folder must contain model.onnx, voices.bin, tokens.txt, and espeak-ng-data."
            }
            val loadMs = withContext(Dispatchers.Default) { kokoroVoice.load() }
            val speakers = withContext(Dispatchers.Default) { kokoroVoice.numSpeakers() }
            neuralVoiceStatus.text = "Neural voice: READY | load $loadMs ms | speakers $speakers"
            transcript.append("\nQuietOS: Kokoro neural voice installed.\n")
        } catch (t: Throwable) {
            neuralVoiceStatus.text = "Neural voice FAILED: ${t.message ?: t.javaClass.simpleName}"
            transcript.append("\nQuietOS: Kokoro import failed: ${t.message ?: t.javaClass.simpleName}\n")
        }
    }

    private fun copyDocumentTree(source: DocumentFile, destination: File) {
        source.listFiles().forEach { child ->
            val name = child.name ?: return@forEach
            val target = File(destination, name)
            if (child.isDirectory) {
                target.mkdirs()
                copyDocumentTree(child, target)
            } else if (child.isFile) {
                contentResolver.openInputStream(child.uri).use { inputStream ->
                    requireNotNull(inputStream) { "Could not read $name" }
                    FileOutputStream(target).use { output -> inputStream.copyTo(output, 1024 * 1024) }
                }
            }
        }
    }

    private fun auditionKokoroEmma() {
        if (!kokoroVoice.isInstalled()) {
            neuralVoiceStatus.text = "Neural voice: install Kokoro first"
            return
        }
        wakeWordSpotter?.stop()
        textToSpeech?.stop()
        handsFreeStatus.text = "Gemma: neural voice audition..."
        neuralVoiceStatus.text = "Neural voice: synthesizing Emma..."
        lifecycleScope.launch {
            try {
                val metrics = withContext(Dispatchers.Default) {
                    kokoroVoice.speak(
                        text = "Hello Jon. I am Gemma. I think this voice suits me rather well.",
                        speakerId = 7,
                        speed = 1.0f
                    )
                }
                neuralVoiceStatus.text = "Emma | synth ${metrics.synthesisMs} ms | audio ${metrics.audioDurationMs} ms | ${metrics.sampleRate} Hz"
                transcript.append("\n[neural voice: Emma sid 7 | synth ${metrics.synthesisMs} ms | audio ${metrics.audioDurationMs} ms]\n")
                handsFreeStatus.postDelayed({ returnToWakeIdle() }, metrics.audioDurationMs + 250L)
            } catch (t: Throwable) {
                neuralVoiceStatus.text = "Neural voice FAILED: ${t.message ?: t.javaClass.simpleName}"
                returnToWakeIdle()
            }
        }
    }

    private suspend fun autoLoadExistingModel() {
        val existing = withContext(Dispatchers.IO) {
            File(filesDir, "models").listFiles()?.filter { it.isFile && it.extension.equals("litertlm", true) }?.maxByOrNull { it.lastModified() }
        } ?: return
        choose.isEnabled = false
        send.isEnabled = false
        status.text = "Gemma status: loading saved ${existing.name}..."
        try {
            val metrics = model.load(existing.absolutePath)
            status.text = "Gemma READY | load ${metrics.loadTimeMs} ms | warmup ${metrics.warmupTimeMs} ms"
            transcript.append("\nQuietOS: saved Gemma model loaded automatically.\n")
            send.isEnabled = true
        } catch (t: Throwable) {
            status.text = "Saved Gemma load FAILED: ${t.message ?: t.javaClass.simpleName}"
            transcript.append("\nQuietOS: saved model failed to load. Choose Gemma model to replace it.\n")
        } finally {
            choose.isEnabled = true
        }
    }

    private suspend fun importAndLoad(uri: Uri) {
        choose.isEnabled = false
        send.isEnabled = false
        status.text = "Gemma status: importing model..."
        try {
            val displayName = queryDisplayName(uri) ?: "gemma-model.litertlm"
            require(displayName.endsWith(".litertlm", ignoreCase = true)) { "Select a .litertlm model file." }
            val local = withContext(Dispatchers.IO) {
                val modelDir = File(filesDir, "models").apply { mkdirs() }
                val destination = File(modelDir, displayName)
                contentResolver.openInputStream(uri).use { source ->
                    requireNotNull(source) { "Could not open selected model." }
                    FileOutputStream(destination).use { target -> source.copyTo(target, 1024 * 1024) }
                }
                require(destination.length() > 0L) { "Imported model is empty." }
                destination
            }
            status.text = "Gemma status: loading "+local.name+"..."
            val metrics = model.load(local.absolutePath)
            status.text = "Gemma READY | load "+metrics.loadTimeMs+" ms | warmup "+metrics.warmupTimeMs+" ms"
            transcript.append("\nQuietOS: Gemma loaded locally.\n")
            send.isEnabled = true
        } catch (t: Throwable) {
            status.text = "Gemma load FAILED: "+(t.message ?: t.javaClass.simpleName)
            transcript.append("\nQuietOS: model load failed.\n")
        } finally {
            choose.isEnabled = true
        }
    }

    private fun initializeTextToSpeech() {
        textToSpeech = TextToSpeech(this) { statusCode ->
            if (statusCode == TextToSpeech.SUCCESS) {
                val engine = textToSpeech ?: return@TextToSpeech
                // Restore the original #135 system-default TTS behavior. Physical tests
                // preferred it over the later forced UK/local voice experiments.
                val languageResult = engine.setLanguage(Locale.US)
                ttsReady = languageResult != TextToSpeech.LANG_MISSING_DATA &&
                    languageResult != TextToSpeech.LANG_NOT_SUPPORTED
                engine.setSpeechRate(1.0f)
                engine.setPitch(1.0f)
                engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        runOnUiThread {
                            wakeWordSpotter?.stop()
                            handsFreeStatus.text = "Gemma: speaking..."
                        }
                    }

                    override fun onDone(utteranceId: String?) {
                        runOnUiThread {
                            if (utteranceId == activeTtsUtteranceId) {
                                activeTtsUtteranceId = null
                                returnToWakeIdle()
                            }
                        }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        runOnUiThread {
                            if (utteranceId == activeTtsUtteranceId) {
                                activeTtsUtteranceId = null
                                returnToWakeIdle()
                            }
                        }
                    }
                })
                transcript.append(if (ttsReady) "\nQuietOS: Gemma voice ready (system default).\n" else "\nQuietOS: TTS voice data unavailable.\n")
            } else {
                ttsReady = false
                transcript.append("\nQuietOS: TTS initialization failed.\n")
            }
        }
    }

    private fun speakGemma(text: String) {
        if (text.isBlank()) return
        wakeWordSpotter?.stop()
        textToSpeech?.stop()
        if (kokoroVoice.isInstalled()) {
            handsFreeStatus.text = "Gemma: speaking..."
            lifecycleScope.launch {
                try {
                    val metrics = withContext(Dispatchers.Default) { kokoroVoice.speak(text, 7, 1.0f) }
                    neuralVoiceStatus.text = "Gemma voice: Emma | synth " + metrics.synthesisMs + " ms | audio " + metrics.audioDurationMs + " ms"
                    handsFreeStatus.postDelayed({ returnToWakeIdle() }, metrics.audioDurationMs + 250L)
                } catch (t: Throwable) {
                    neuralVoiceStatus.text = "Neural voice FAILED: " + (t.message ?: t.javaClass.simpleName)
                    speakGemmaWithSystemFallback(text)
                }
            }
            return
        }
        speakGemmaWithSystemFallback(text)
    }

    private fun speakGemmaWithSystemFallback(text: String) {
        if (!ttsReady || text.isBlank()) {
            returnToWakeIdle()
            return
        }
        val utteranceId = "gemma-" + System.nanoTime()
        activeTtsUtteranceId = utteranceId
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    private fun sendMessage() {
        val message = input.text.toString().trim()
        if (message.isEmpty()) return
        input.text.clear()
        send.isEnabled = false
        transcript.append("\nJon: "+message+"\nGemma: ")
        status.text = "Gemma status: generating..."
        lifecycleScope.launch {
            try {
                val started = System.nanoTime()
                val result = model.send(message)
                val reply = result.text
                val elapsed = (System.nanoTime() - started) / 1_000_000
                if (reply.isBlank()) {
                    transcript.append("[EMPTY RESPONSE]\n")
                    status.text = "Gemma generation FAILED: empty response after ${elapsed} ms"
                } else {
                    transcript.append(reply+"\n[response "+elapsed+" ms]\n")
                    speakGemma(reply)
                    val m = result.metrics
                    transcript.append("[wiring: first chunk ${m.timeToFirstChunkMs} ms | after first ${m.generationAfterFirstChunkMs} ms | total ${m.totalTimeMs} ms | chunks ${m.chunkCount} | chars ${m.outputChars}]\n")
                    status.text = "Gemma READY | first ${m.timeToFirstChunkMs} ms | total ${m.totalTimeMs} ms"
                }
            } catch (t: Throwable) {
                transcript.append("[FAILED: "+t.javaClass.simpleName+": "+(t.message ?: "no message")+"]\n")
                status.text = "Gemma generation FAILED: "+t.javaClass.simpleName
            } finally {
                send.isEnabled = model.state == ModelState.READY
            }
        }
    }

    private fun startHandsFreeWithPermission() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            handsFreeStatus.text = "Hands-free: speech recognition unavailable"
            return
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            enableHandsFreeListening()
        } else {
            microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun enableHandsFreeListening() {
        if (handsFreeEnabled) return
        handsFreeEnabled = true
        handsFreeController.enable()
        ensureCommandRecognizer()

        if (wakeWordSpotter == null) {
            wakeWordSpotter = GemmaWakeWordSpotter(
                context = this,
                onWake = {
                    if (!handsFreeEnabled) return@GemmaWakeWordSpotter
                    transcript.append("\nQuietOS wake: Gemma\n")
                    handsFreeStatus.text = "QuietOS: awake, listening for request..."
                    wakeWordSpotter?.stop()
                    startCommandRecognition()
                },
                onError = { message ->
                    handsFreeStatus.text = "QuietOS: wake detector error"
                    transcript.append("\nQuietOS wake detector error: $message\n")
                }
            )
        }

        handsFreeStatus.text = "QuietOS: idle, say \"Gemma\""
        transcript.append("\nQuietOS: local Gemma wake detector armed.\n")
        wakeWordSpotter?.start()
    }

    private fun ensureCommandRecognizer() {
        if (speechRecognizer != null) return
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    recognitionSessionActive = true
                    handsFreeController.listeningStarted()
                    handsFreeStatus.text = "QuietOS: awake, listening for request..."
                }
                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() {
                    handsFreeStatus.text = "QuietOS: processing..."
                }
                override fun onError(error: Int) {
                    recognitionSessionActive = false
                    if (!handsFreeEnabled) return
                    transcript.append("\nQuietOS: command recognition ended without a usable request (error $error).\n")
                    returnToWakeIdle()
                }
                override fun onResults(results: Bundle?) {
                    recognitionSessionActive = false
                    if (!handsFreeEnabled) return
                    handsFreeController.speechReceived()
                    val heard = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        ?.trim()
                        .orEmpty()
                    if (heard.isNotEmpty()) handleVoiceInput(heard)
                    else transcript.append("\nQuietOS: no command was recognized.\n")
                    returnToWakeIdle()
                }
                override fun onPartialResults(partialResults: Bundle?) = Unit
                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })
        }
    }

    private fun startCommandRecognition() {
        if (!handsFreeEnabled || recognitionSessionActive) return
        ensureCommandRecognizer()
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            if (android.os.Build.VERSION.SDK_INT >= 33) {
                putStringArrayListExtra(
                    RecognizerIntent.EXTRA_BIASING_STRINGS,
                    arrayListOf("Attention Mode", "QuietOS", "DeanWay", "DeanWay Labs", "DeanWay Travels")
                )
            }
        }
        runCatching {
            recognitionSessionActive = true
            speechRecognizer?.startListening(intent)
        }.onFailure {
            recognitionSessionActive = false
            transcript.append("\nQuietOS: command recognizer failed to start.\n")
            returnToWakeIdle()
        }
    }

    private fun returnToWakeIdle() {
        if (!handsFreeEnabled) return
        handsFreeController.readyForNextUtterance()
        handsFreeStatus.text = "QuietOS: idle, say \"Gemma\""
        handsFreeStatus.postDelayed({
            if (handsFreeEnabled && !recognitionSessionActive) wakeWordSpotter?.start()
        }, 300L)
    }

    private fun launchVoiceInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Talk to Gemma")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }

        runCatching { voiceLauncher.launch(intent) }
            .onFailure {
                transcript.append("\nQuietOS: speech recognition is not available on this device.\n")
            }
    }

    private fun handleVoiceInput(spoken: String) {
        val normalizedSpeech = voiceTextNormalizer.normalize(spoken)
        transcript.append("\nJon (voice): $spoken\n")
        if (normalizedSpeech != spoken) {
            transcript.append("QuietOS heard/corrected: $normalizedSpeech\n")
        }
        when (val route = voiceRouter.route(normalizedSpeech)) {
            is com.deanwaylabs.quietos.voice.VoiceRoute -> when (route.command) {
                VoiceCommand.SUMMARIZE_DIGEST -> {
                    transcript.append("QuietOS: voice command accepted — summarize digest.\n")
                    summarizeDigestWithGemma()
                }

                VoiceCommand.ATTENTION_ON -> {
                    setAttentionMode(true)
                    transcript.append("QuietOS: Attention Mode turned ON by voice.\n")
                }

                VoiceCommand.ATTENTION_OFF -> {
                    setAttentionMode(false)
                    transcript.append("QuietOS: Attention Mode turned OFF by voice.\n")
                }

                VoiceCommand.CLEAR_ATTENTION_LOG -> {
                    attentionRepository.clear()
                    refreshAttentionLog()
                    transcript.append("QuietOS: attention log cleared by voice.\n")
                }

                VoiceCommand.REFRESH_ATTENTION,
                VoiceCommand.ATTENTION_STATUS -> {
                    refreshAttentionLog()
                    transcript.append("QuietOS: attention status refreshed by voice.\n")
                }

                VoiceCommand.CONVERSATION -> {
                    input.setText(route.originalText)
                    sendMessage()
                }
            }
        }
    }

    private fun setAttentionMode(enabled: Boolean) {
        getSharedPreferences("quietos_attention", MODE_PRIVATE)
            .edit()
            .putBoolean("interception_enabled", enabled)
            .apply()
        updateInterceptionButton()
        refreshAttentionLog()
    }

    private fun summarizeDigestWithGemma() {
        val digest = attentionRepository.readAll()
            .filter { it.classification == com.deanwaylabs.quietos.attention.AttentionClass.DIGEST }
            .take(5)

        if (digest.isEmpty()) {
            transcript.append("\nQuietOS: no DIGEST notifications are waiting.\n")
            return
        }

        if (model.state != ModelState.READY) {
            transcript.append("\nQuietOS: Gemma is not ready yet, so I kept the digest locally.\n")
            return
        }

        val digestContext = digest.mapIndexed { index, record ->
            val source = record.title.ifBlank { record.packageName }
            "${index + 1}. ${source}: ${record.text.take(120)}"
        }.joinToString("\n")

        val prompt = """QuietOS collected these DIGEST notifications. Summarize only the useful information for Jon in a calm, concise way. Do not invent details. If several items are repetitive, combine them.

$digestContext"""

        transcript.append("\nQuietOS: sending ${digest.size} DIGEST items to Gemma for a local summary.\nGemma: ")
        status.text = "Gemma status: summarizing digest..."
        send.isEnabled = false

        lifecycleScope.launch {
            try {
                val result = model.send(prompt)
                transcript.append(result.text + "\n")
                speakGemma(result.text)
                val m = result.metrics
                transcript.append("[digest wiring: first ${m.timeToFirstChunkMs} ms | total ${m.totalTimeMs} ms | chunks ${m.chunkCount}]\n")
                status.text = "Gemma READY | digest total ${m.totalTimeMs} ms"
            } catch (t: Throwable) {
                transcript.append("[DIGEST FAILED: ${t.javaClass.simpleName}: ${t.message ?: "no message"}]\n")
                status.text = "Gemma digest FAILED: ${t.javaClass.simpleName}"
            } finally {
                send.isEnabled = model.state == ModelState.READY
            }
        }
    }

    private fun updateInterceptionButton() {
        val enabled = getSharedPreferences("quietos_attention", MODE_PRIVATE)
            .getBoolean("interception_enabled", false)
        interceptionButton.text = if (enabled) {
            "QuietOS Attention Mode: ON"
        } else {
            "QuietOS Attention Mode: OFF"
        }
    }

    private fun showAttentionBucket(bucket: String) {
        val classification = runCatching {
            com.deanwaylabs.quietos.attention.AttentionClass.valueOf(bucket)
        }.getOrNull() ?: return
        val records = attentionRepository.readAll().filter { it.classification == classification }

        val body = if (records.isEmpty()) {
            "Nothing waiting in $bucket."
        } else {
            records.take(25).joinToString("\n\n") { record ->
                val action = if (record.originalCancelled) {
                    "QuietOS intercepted the original"
                } else {
                    "Original remains in Android notification flow"
                }
                "${record.title.ifBlank { record.packageName }}\n${record.text.take(240)}\nWhy: ${record.reason}\nAction: $action"
            }
        }

        val workspace = TextView(this).apply {
            text = body
            textSize = 18f
            val space = (20 * resources.displayMetrics.density).toInt()
            setPadding(space, space, space, space)
        }
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            addView(workspace)
        }

        AlertDialog.Builder(this)
            .setTitle("$bucket Workspace • ${records.size} item(s)")
            .setView(scroll)
            .setPositiveButton("BACK") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun refreshAttentionLog() {
        val records = attentionRepository.readAll()
        if (records.isEmpty()) {
            attentionStatus.text = "Attention Engine: no captured notifications yet"
            if (::attentionLog.isInitialized) attentionLog.text = "No captured notifications yet."
            return
        }

        val counts = records.groupingBy { it.classification }.eachCount()
        val cancelled = records.count { it.originalCancelled }
        attentionStatus.text = "Attention Engine: captured ${records.size} | NOW ${counts[com.deanwaylabs.quietos.attention.AttentionClass.NOW] ?: 0} | SOON ${counts[com.deanwaylabs.quietos.attention.AttentionClass.SOON] ?: 0} | DIGEST ${counts[com.deanwaylabs.quietos.attention.AttentionClass.DIGEST] ?: 0} | QUIET ${counts[com.deanwaylabs.quietos.attention.AttentionClass.QUIET] ?: 0} | cancelled $cancelled"

        if (::attentionLog.isInitialized) attentionLog.text = records.take(5).joinToString("\n\n") { record ->
            val action = if (record.originalCancelled) "QuietOS intercepted and cancelled the original" else "Original left in Android notification flow"
            "[${record.classification}] ${record.title.ifBlank { record.packageName }}\n${record.text.take(180)}\nWhy: ${record.reason}\nAction: $action"
        }
    }

    private fun requestCallScreeningRole() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            transcript.append("\nQuietOS: Android 10 or newer is required for the call-screening role.\n")
            return
        }
        val roleManager = getSystemService(RoleManager::class.java)
        if (!roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
            transcript.append("\nQuietOS: call screening is not available on this device.\n")
            return
        }
        if (roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)) {
            transcript.append("\nQuietOS: Spam Call Protection is already active.\n")
            updateCallScreeningStatus()
            return
        }
        callScreeningRoleLauncher.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING))
    }

    private fun updateCallScreeningStatus() {
        val root = findViewById<View>(android.R.id.content)
        val statusView = findTaggedTextView(root, "call_screening_status") ?: return
        val active = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java)
            roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) && roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
        } else false
        statusView.text = if (active) "Spam Call Protection: ACTIVE" else "Spam Call Protection: INACTIVE"
    }

    private fun findTaggedTextView(view: View, wantedTag: String): TextView? {
        if (view is TextView && view.tag == wantedTag) return view
        if (view is android.view.ViewGroup) for (i in 0 until view.childCount) {
            findTaggedTextView(view.getChildAt(i), wantedTag)?.let { return it }
        }
        return null
    }

    override fun onResume() {
        super.onResume()
        if (::attentionStatus.isInitialized) refreshAttentionLog()
        updateCallScreeningStatus()
    }

    private fun queryDisplayName(uri: Uri): String? {
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) return cursor.getString(0)
        }
        return null
    }

    override fun onDestroy() {
        handsFreeEnabled = false
        speechRestartPending = false
        recognitionSessionActive = false
        handsFreeController.disable()
        wakeWordSpotter?.release()
        wakeWordSpotter = null
        speechRecognizer?.cancel()
        speechRecognizer?.destroy()
        speechRecognizer = null
        kokoroVoice.release()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        ttsReady = false
        model.close()
        super.onDestroy()
    }
}
