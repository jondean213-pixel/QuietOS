package com.deanwaylabs.quietos

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.os.SystemClock
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
import android.widget.Toast
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
import com.deanwaylabs.quietos.messages.MessageSpamLog
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
    private lateinit var messageSpamStatus: TextView
    private lateinit var messageSpamButton: Button
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

    // Diagnostic-only QuietOS latency trace. No routing or model behavior changes.
    private data class QuietOsLatencyTrace(
        val wakeMs: Long,
        var recognizerReadyMs: Long? = null,
        var speechBeginMs: Long? = null,
        var speechEndMs: Long? = null,
        var recognitionResultMs: Long? = null,
        var routeCompleteMs: Long? = null,
        var gemmaStartMs: Long? = null,
        var gemmaCompleteMs: Long? = null,
        var emmaStartMs: Long? = null,
        var emmaCompleteMs: Long? = null
    )
    private var latencyTrace: QuietOsLatencyTrace? = null

    private fun markLatency(label: String) {
        val trace = latencyTrace ?: return
        val elapsed = SystemClock.elapsedRealtime() - trace.wakeMs
        transcript.append("\n[QuietOS latency: " + label + " +" + elapsed + " ms]\n")
    }

    private fun appendLatencySummary() {
        val t = latencyTrace ?: return
        fun delta(a: Long?, b: Long?): String = if (a != null && b != null) (b - a).toString() + " ms" else "n/a"
        transcript.append("\n[QuietOS E2E: wake→recognizer " + delta(t.wakeMs, t.recognizerReadyMs) +
            " | speech " + delta(t.speechBeginMs, t.speechEndMs) +
            " | recognition " + delta(t.speechEndMs, t.recognitionResultMs) +
            " | route " + delta(t.recognitionResultMs, t.routeCompleteMs) +
            " | Gemma " + delta(t.gemmaStartMs, t.gemmaCompleteMs) +
            " | Emma synth " + delta(t.emmaStartMs, t.emmaCompleteMs) +
            " | wake→audio " + delta(t.wakeMs, t.emmaCompleteMs) + "]\n")
    }

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

    private val smsRoleLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        updateSmsRoleStatus()
    }

    private val kokoroFolderPicker = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            runCatching {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }
            lifecycleScope.launch { importKokoroFolder(uri) }
        } else {
            restoreHandsFreeAfterExternalAction()
        }
    }

    private val picker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            lifecycleScope.launch { importAndLoad(uri) }
        } else {
            restoreHandsFreeAfterExternalAction()
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
        val pad = (16 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
            setBackgroundColor(android.graphics.Color.rgb(2, 7, 22))
        }
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(pad, pad + bars.top, pad, pad + bars.bottom)
            insets
        }

        root.addView(TextView(this).apply {
            text = "QuietOS"
            textSize = 38f
            gravity = android.view.Gravity.CENTER
            setTextColor(android.graphics.Color.rgb(190, 205, 255))
        })
        root.addView(TextView(this).apply {
            text = "BY DEANWAY LABS    •    FOCUS  •  CONTROL  •  PEACE"
            textSize = 12f
            gravity = android.view.Gravity.CENTER
            setTextColor(android.graphics.Color.rgb(110, 180, 255))
        })

        status = TextView(this).apply { text = "Gemma status: not loaded"; visibility = View.GONE }
        choose = Button(this).apply { text = "Choose Gemma model"; visibility = View.GONE; setOnClickListener { launchGemmaModelPicker() } }
        transcript = TextView(this).apply { text = "Conversation will appear here.\n"; visibility = View.GONE }
        input = EditText(this).apply { hint = "Message Gemma"; visibility = View.GONE }
        send = Button(this).apply { text = "Send"; isEnabled = false; visibility = View.GONE; setOnClickListener { sendMessage() } }
        handsFreeStatus = TextView(this).apply {
            text = "QuietOS: starting..."
            textSize = 13f
            gravity = android.view.Gravity.CENTER
            setTextColor(android.graphics.Color.LTGRAY)
        }
        neuralVoiceStatus = TextView(this).apply { text = if (kokoroVoice.isInstalled()) "Neural voice: Kokoro installed" else "Neural voice: Kokoro not installed"; visibility = View.GONE }
        attentionStatus = TextView(this).apply { text = "Attention Engine: ready"; visibility = View.GONE }
        attentionLog = TextView(this).apply { text = ""; visibility = View.GONE }
        interceptionButton = Button(this).apply {
            visibility = View.GONE
            setOnClickListener {
                val prefs = getSharedPreferences("quietos_attention", MODE_PRIVATE)
                setAttentionMode(!prefs.getBoolean("interception_enabled", false))
            }
        }
        messageSpamStatus = TextView(this).apply { visibility = View.GONE }
        messageSpamButton = Button(this).apply {
            visibility = View.GONE
            setOnClickListener {
                val prefs = getSharedPreferences("quietos_message_spam", MODE_PRIVATE)
                prefs.edit().putBoolean("enabled", !prefs.getBoolean("enabled", false)).apply()
                updateMessageSpamStatus()
            }
        }

        val orbGrid = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
        }
        fun orb(label: String, subtitle: String, action: () -> Unit): Button = Button(this).apply {
            text = "$label\n$subtitle"
            textSize = 17f
            isAllCaps = false
            minHeight = (92 * resources.displayMetrics.density).toInt()
            setTextColor(android.graphics.Color.rgb(220, 228, 255))
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = 28f * resources.displayMetrics.density
                setColor(android.graphics.Color.rgb(8, 14, 38))
                setStroke((2 * resources.displayMetrics.density).toInt(), android.graphics.Color.rgb(96, 76, 210))
            }
            setPadding(pad / 2, pad / 2, pad / 2, pad / 2)
            setOnClickListener {
                animate().scaleX(0.97f).scaleY(0.97f).setDuration(80L).withEndAction {
                    animate().scaleX(1f).scaleY(1f).setDuration(120L).start()
                    action()
                }.start()
            }
        }
        fun row(left: View, right: View): LinearLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER
            addView(left, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = pad / 2 })
            addView(right, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = pad / 2 })
        }

        orbGrid.addView(row(
            orb("⚡ NOW", "HANDLE IT NOW") { showAttentionBucket("NOW") },
            orb("◷ SOON", "FOR LATER") { showAttentionBucket("SOON") }
        ))

        val gemmaOrb = Button(this).apply {
            text = "GEMMA\nYOUR AI ASSISTANT\n\n“I’ve got you.”"
            textSize = 20f
            isAllCaps = false
            minHeight = (190 * resources.displayMetrics.density).toInt()
            setTextColor(android.graphics.Color.rgb(235, 230, 255))
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(android.graphics.Color.rgb(11, 13, 42))
                setStroke((3 * resources.displayMetrics.density).toInt(), android.graphics.Color.rgb(126, 86, 235))
            }
            setOnClickListener {
                if (model.state != ModelState.READY || !kokoroVoice.isInstalled()) {
                    showGemmaSetupWorkspace()
                } else {
                    restoreHandsFreeAfterExternalAction()
                    handsFreeStatus.text = "QuietOS: hands-free active, say \"Gemma\""
                }
            }
            setOnLongClickListener {
                showGemmaSetupWorkspace()
                true
            }
        }
        orbGrid.addView(gemmaOrb, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            topMargin = pad / 2; bottomMargin = pad / 2
        })

        orbGrid.addView(row(
            orb("☷ DIGEST", "BATCH & REVIEW") { showAttentionBucket("DIGEST") },
            orb("☾ QUIET", "KEEP IT SILENT") { showAttentionBucket("QUIET") }
        ))

        val spamOrb = orb("⬡ SPAM SHIELD", "BLOCK • LOG • PROTECT") { showSpamShieldWorkspace() }
        orbGrid.addView(spamOrb, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            topMargin = pad / 2
        })

        root.addView(orbGrid, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        root.addView(handsFreeStatus)

        val utilityRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER
            addView(Button(this@MainActivity).apply {
                text = "GEMMA SETUP"
                isAllCaps = false
                setOnClickListener { showGemmaSetupWorkspace() }
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = pad / 2 })
            addView(Button(this@MainActivity).apply {
                text = "MESSAGES"
                isAllCaps = false
                setOnClickListener {
                    startActivity(Intent(this@MainActivity, com.deanwaylabs.quietos.messages.MessagesActivity::class.java))
                }
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = pad / 2 })
        }
        root.addView(utilityRow)

        root.addView(TextView(this).apply {
            text = "DEANWAY LABS    •    BUILDING A QUIETER TOMORROW"
            textSize = 12f
            gravity = android.view.Gravity.CENTER
            setTextColor(android.graphics.Color.rgb(110, 180, 255))
        })

        updateInterceptionButton()
        updateMessageSpamStatus()
        return root
    }

    private fun showGemmaSetupWorkspace() {
        val pad = (16 * resources.displayMetrics.density).toInt()
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }
        val modelState = TextView(this).apply {
            text = if (model.state == ModelState.READY) "Gemma model: READY" else "Gemma model: not loaded"
            textSize = 17f
        }
        val voiceState = TextView(this).apply {
            text = if (kokoroVoice.isInstalled()) "Emma voice: Kokoro installed" else "Emma voice: not installed"
            textSize = 17f
        }
        content.addView(modelState)
        content.addView(Button(this).apply {
            text = "Connect Gemma Model"
            setOnClickListener { launchGemmaModelPicker() }
        })
        content.addView(voiceState)
        content.addView(Button(this).apply {
            text = "Install Emma / Kokoro Voice Folder"
            setOnClickListener { launchEmmaFolderPicker() }
        })
        content.addView(Button(this).apply {
            text = "Audition Emma"
            setOnClickListener { auditionKokoroEmma() }
        })
        content.addView(TextView(this).apply {
            text = "Import guide: Gemma selects the .litertlm FILE. Emma selects the extracted Kokoro FOLDER containing model.onnx, voices.bin, tokens.txt, and espeak-ng-data. You may also select the parent folder that contains that Kokoro folder. QuietOS keeps Android read access after selection. Once both are installed, say Gemma for hands-free use. GEMMA SETUP stays available on the home screen."
            textSize = 14f
            setPadding(0, pad, 0, 0)
        })
        AlertDialog.Builder(this)
            .setTitle("GEMMA Setup Workspace")
            .setView(ScrollView(this).apply { addView(content) })
            .setPositiveButton("FOLD INTO ORB") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun showSpamShieldWorkspace() {
        val pad = (16 * resources.displayMetrics.density).toInt()
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }
        val callStatus = TextView(this).apply {
            tag = "call_screening_status"
            text = "Spam Call Protection: checking..."
            textSize = 16f
        }
        val smsStatus = TextView(this).apply {
            tag = "sms_role_status"
            text = "SMS Handler: checking..."
            textSize = 16f
        }
        content.addView(callStatus)
        content.addView(Button(this).apply {
            text = "Enable Spam Call Protection"
            setOnClickListener { requestCallScreeningRole() }
        })
        content.addView(messageSpamStatus)
        content.addView(messageSpamButton)
        content.addView(Button(this).apply {
            text = "View Message Quarantine"
            setOnClickListener { showMessageSpamLog() }
        })
        content.addView(smsStatus)
        content.addView(Button(this).apply {
            text = "Make QuietOS Default SMS App"
            setOnClickListener { requestSmsRole() }
        })
        content.addView(Button(this).apply {
            text = "Open QuietOS Messages"
            setOnClickListener { startActivity(Intent(this@MainActivity, com.deanwaylabs.quietos.messages.MessagesActivity::class.java)) }
        })
        content.addView(Button(this).apply {
            text = "New Message"
            setOnClickListener { startActivity(Intent(this@MainActivity, com.deanwaylabs.quietos.messages.SmsComposeActivity::class.java)) }
        })
        content.addView(Button(this).apply {
            text = "Notification Access"
            setOnClickListener { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
        })
        AlertDialog.Builder(this)
            .setTitle("SPAM SHIELD Workspace")
            .setView(ScrollView(this).apply { addView(content) })
            .setPositiveButton("FOLD INTO ORB") { dialog, _ -> dialog.dismiss() }
            .setOnDismissListener {
                updateCallScreeningStatus()
                updateSmsRoleStatus()
                updateMessageSpamStatus()
            }
            .show()
        callStatus.post { updateCallScreeningStatus() }
        smsStatus.post { updateSmsRoleStatus() }
    }

    private fun launchGemmaModelPicker() {
        status.text = "Gemma status: choose the .litertlm model file..."
        pauseHandsFreeForExternalAction()
        runCatching { picker.launch(arrayOf("application/octet-stream", "*/*")) }
            .onFailure {
                status.text = "Gemma picker FAILED: " + (it.message ?: it.javaClass.simpleName)
                restoreHandsFreeAfterExternalAction()
            }
    }

    private fun launchEmmaFolderPicker() {
        neuralVoiceStatus.text = "Neural voice: choose the extracted Kokoro folder..."
        pauseHandsFreeForExternalAction()
        runCatching { kokoroFolderPicker.launch(null) }
            .onFailure {
                neuralVoiceStatus.text = "Emma folder picker FAILED: " + (it.message ?: it.javaClass.simpleName)
                Toast.makeText(this, neuralVoiceStatus.text, Toast.LENGTH_LONG).show()
                restoreHandsFreeAfterExternalAction()
            }
    }

    private suspend fun importKokoroFolder(uri: Uri) {
        pauseHandsFreeForExternalAction()
        neuralVoiceStatus.text = "Neural voice: importing Kokoro..."
        val staging = File(filesDir, "voices/kokoro-import")
        try {
            withContext(Dispatchers.IO) {
                val selected = DocumentFile.fromTreeUri(this@MainActivity, uri)
                    ?: error("Could not open selected Kokoro folder.")
                val source = findKokoroBundle(selected)
                    ?: error("Could not find model.onnx, voices.bin, tokens.txt, and espeak-ng-data in the selected folder.")
                if (staging.exists()) staging.deleteRecursively()
                staging.mkdirs()
                copyDocumentTree(source, staging)
                require(fileHasKokoroBundle(staging)) {
                    "Kokoro files were found but could not be copied completely."
                }

                kokoroVoice.release()
                val destination = File(filesDir, "voices/kokoro")
                if (destination.exists()) destination.deleteRecursively()
                if (!staging.renameTo(destination)) {
                    staging.copyRecursively(destination, overwrite = true)
                    staging.deleteRecursively()
                }
                require(fileHasKokoroBundle(destination)) { "Kokoro install did not complete." }
            }
            val loadMs = withContext(Dispatchers.Default) { kokoroVoice.load() }
            val speakers = withContext(Dispatchers.Default) { kokoroVoice.numSpeakers() }
            neuralVoiceStatus.text = "Neural voice: READY | load $loadMs ms | speakers $speakers"
            transcript.append("\nQuietOS: Kokoro neural voice installed.\n")
            Toast.makeText(this, "Emma voice installed and ready.", Toast.LENGTH_LONG).show()
        } catch (t: Throwable) {
            staging.deleteRecursively()
            neuralVoiceStatus.text = "Neural voice FAILED: ${t.message ?: t.javaClass.simpleName}"
            transcript.append("\nQuietOS: Kokoro import failed: ${t.message ?: t.javaClass.simpleName}\n")
            Toast.makeText(this, neuralVoiceStatus.text, Toast.LENGTH_LONG).show()
        } finally {
            restoreHandsFreeAfterExternalAction()
        }
    }

    private fun findKokoroBundle(root: DocumentFile, depth: Int = 0): DocumentFile? {
        if (documentHasKokoroBundle(root)) return root
        if (depth >= 3) return null
        root.listFiles().filter { it.isDirectory }.forEach { child ->
            findKokoroBundle(child, depth + 1)?.let { return it }
        }
        return null
    }

    private fun documentHasKokoroBundle(folder: DocumentFile): Boolean {
        val children = folder.listFiles()
        fun hasFile(name: String) = children.any { it.isFile && it.name.equals(name, ignoreCase = true) }
        fun hasDir(name: String) = children.any { it.isDirectory && it.name.equals(name, ignoreCase = true) }
        return hasFile("model.onnx") &&
            hasFile("voices.bin") &&
            hasFile("tokens.txt") &&
            hasDir("espeak-ng-data")
    }

    private fun fileHasKokoroBundle(folder: File): Boolean =
        File(folder, "model.onnx").isFile &&
            File(folder, "voices.bin").isFile &&
            File(folder, "tokens.txt").isFile &&
            File(folder, "espeak-ng-data").isDirectory

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
        pauseHandsFreeForExternalAction()
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
            restoreHandsFreeAfterExternalAction()
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
                                awaitingGemmaCommand = false
                                returnToWakeIdle()
                            }
                        }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        runOnUiThread {
                            if (utteranceId == activeTtsUtteranceId) {
                                activeTtsUtteranceId = null
                                awaitingGemmaCommand = false
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
                    latencyTrace?.emmaStartMs = SystemClock.elapsedRealtime()
                    markLatency("QuietOS handed response to Emma")
                    val metrics = withContext(Dispatchers.Default) { kokoroVoice.speak(text, 7, 1.0f) }
                    latencyTrace?.emmaCompleteMs = SystemClock.elapsedRealtime()
                    markLatency("Emma synthesis complete / playback started")
                    appendLatencySummary()
                    neuralVoiceStatus.text = "Gemma voice: Emma | load " + metrics.loadMs + " ms | synth " + metrics.synthesisMs + " ms | PCM " + metrics.pcmMs + " ms | setup " + metrics.audioSetupMs + " ms | to-play " + metrics.timeToPlaybackMs + " ms | audio " + metrics.audioDurationMs + " ms"
                    transcript.append("[Emma wiring: load " + metrics.loadMs + " ms | synth " + metrics.synthesisMs + " ms | PCM " + metrics.pcmMs + " ms | setup " + metrics.audioSetupMs + " ms | to-play " + metrics.timeToPlaybackMs + " ms]\n")
                    handsFreeStatus.postDelayed({
                        awaitingGemmaCommand = false
                        returnToWakeIdle()
                    }, metrics.audioDurationMs + 250L)
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
            awaitingGemmaCommand = false
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
        awaitingGemmaCommand = true
        wakeWordSpotter?.stop()
        lifecycleScope.launch {
            try {
                latencyTrace?.gemmaStartMs = SystemClock.elapsedRealtime()
                markLatency("QuietOS handed request to Gemma")
                val started = System.nanoTime()
                val result = model.send(message)
                latencyTrace?.gemmaCompleteMs = SystemClock.elapsedRealtime()
                markLatency("Gemma returned to QuietOS")
                val reply = result.text
                val elapsed = (System.nanoTime() - started) / 1_000_000
                if (reply.isBlank()) {
                    transcript.append("[EMPTY RESPONSE]\n")
                    status.text = "Gemma generation FAILED: empty response after ${elapsed} ms"
                    awaitingGemmaCommand = false
                    returnToWakeIdle()
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
                awaitingGemmaCommand = false
                returnToWakeIdle()
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
        if (!handsFreeEnabled) {
            handsFreeEnabled = true
            handsFreeController.enable()
            ensureCommandRecognizer()
        }

        if (wakeWordSpotter == null) {
            wakeWordSpotter = GemmaWakeWordSpotter(
                context = this,
                onWake = {
                    if (!handsFreeEnabled) return@GemmaWakeWordSpotter
                    latencyTrace = QuietOsLatencyTrace(SystemClock.elapsedRealtime())
                    transcript.append("\nQuietOS wake: Gemma\n")
                    markLatency("wake detected")
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

    private fun pauseHandsFreeForExternalAction() {
        handsFreeEnabled = false
        handsFreeController.disable()
        recognitionSessionActive = false
        wakeWordSpotter?.stop()
        speechRecognizer?.cancel()
    }

    private fun restoreHandsFreeAfterExternalAction() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            handsFreeStatus.text = "QuietOS: speech recognition unavailable"
            return
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            enableHandsFreeListening()
        } else {
            handsFreeStatus.text = "QuietOS: microphone permission required"
        }
    }

    private fun ensureCommandRecognizer() {
        if (speechRecognizer != null) return
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    latencyTrace?.recognizerReadyMs = SystemClock.elapsedRealtime()
                    markLatency("command recognizer ready")
                    recognitionSessionActive = true
                    handsFreeController.listeningStarted()
                    handsFreeStatus.text = "QuietOS: awake, listening for request..."
                }
                override fun onBeginningOfSpeech() {
                    latencyTrace?.speechBeginMs = SystemClock.elapsedRealtime()
                    markLatency("speech began")
                }
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() {
                    latencyTrace?.speechEndMs = SystemClock.elapsedRealtime()
                    markLatency("speech ended")
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
                    latencyTrace?.recognitionResultMs = SystemClock.elapsedRealtime()
                    markLatency("speech result")
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
        if (!handsFreeEnabled || awaitingGemmaCommand) return
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
        val route = voiceRouter.route(normalizedSpeech)
        latencyTrace?.routeCompleteMs = SystemClock.elapsedRealtime()
        markLatency("QuietOS route complete")
        when (route) {
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
        updateSmsRoleStatus()
        if (::messageSpamStatus.isInitialized) updateMessageSpamStatus()
        if (::handsFreeStatus.isInitialized) restoreHandsFreeAfterExternalAction()
    }

    private fun updateMessageSpamStatus() {
        val enabled = getSharedPreferences("quietos_message_spam", MODE_PRIVATE)
            .getBoolean("enabled", false)
        val count = MessageSpamLog(this).readAll().size
        messageSpamStatus.text = "Spam Message Protection: " + if (enabled) "ACTIVE | logged $count" else "INACTIVE | logged $count"
        messageSpamButton.text = if (enabled) "Disable Spam Message Protection" else "Enable Spam Message Protection"
    }

    private fun showMessageSpamLog() {
        val records = MessageSpamLog(this).readAll()
        val body = if (records.isEmpty()) {
            "No SMS spam decisions logged yet."
        } else {
            records.take(40).joinToString("\n\n")
        }
        val view = TextView(this).apply {
            text = body
            textSize = 16f
            val space = (20 * resources.displayMetrics.density).toInt()
            setPadding(space, space, space, space)
        }
        AlertDialog.Builder(this)
            .setTitle("Spam Message Log • ${records.size}")
            .setView(ScrollView(this).apply { addView(view) })
            .setPositiveButton("BACK") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun requestSmsRole() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            transcript.append("\nQuietOS: Android 10 or newer is required for the SMS role.\n")
            return
        }
        val roleManager = getSystemService(RoleManager::class.java)
        if (!roleManager.isRoleAvailable(RoleManager.ROLE_SMS)) {
            transcript.append("\nQuietOS: the SMS role is not available on this device.\n")
            return
        }
        if (roleManager.isRoleHeld(RoleManager.ROLE_SMS)) {
            updateSmsRoleStatus()
            return
        }
        smsRoleLauncher.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS))
    }

    private fun updateSmsRoleStatus() {
        val root = findViewById<View>(android.R.id.content)
        val statusView = findTaggedTextView(root, "sms_role_status") ?: return
        val active = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java)
            roleManager.isRoleAvailable(RoleManager.ROLE_SMS) && roleManager.isRoleHeld(RoleManager.ROLE_SMS)
        } else false
        statusView.text = if (active) {
            "SMS Handler: QuietOS ACTIVE"
        } else {
            "SMS Handler: system messaging app"
        }
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
