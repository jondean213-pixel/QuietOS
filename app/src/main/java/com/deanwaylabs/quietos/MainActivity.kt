package com.deanwaylabs.quietos

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.deanwaylabs.quietos.ai.LiteRtQwenModel
import com.deanwaylabs.quietos.ai.ModelState
import com.deanwaylabs.quietos.attention.AttentionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

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

    private val picker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            lifecycleScope.launch { importAndLoad(uri) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "QuietOS Alpha 0.1"
        setContentView(buildUi())
        lifecycleScope.launch { autoLoadExistingModel() }
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
                prefs.edit().putBoolean("interception_enabled", next).apply()
                updateInterceptionButton()
                refreshAttentionLog()
            }
        }
        updateInterceptionButton()

        val refreshAttention = Button(this).apply {
            text = "Refresh Attention Log"
            setOnClickListener { refreshAttentionLog() }
        }
        attentionLog = TextView(this).apply {
            text = "No captured notifications yet."
            textSize = 14f
            setPadding(0, pad / 2, 0, pad / 2)
        }
        column.addView(attentionStatus)
        column.addView(notificationAccess)
        column.addView(interceptionButton)
        column.addView(refreshAttention)
        column.addView(attentionLog)

        column.addView(
            transcriptScroll,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )
        column.addView(input)
        column.addView(send)
        return column
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

    private fun updateInterceptionButton() {
        val enabled = getSharedPreferences("quietos_attention", MODE_PRIVATE)
            .getBoolean("interception_enabled", false)
        interceptionButton.text = if (enabled) {
            "Interception Test Mode: ON"
        } else {
            "Interception Test Mode: OFF"
        }
    }

    private fun refreshAttentionLog() {
        val records = attentionRepository.readAll()
        if (records.isEmpty()) {
            attentionStatus.text = "Attention Engine: no captured notifications yet"
            attentionLog.text = "No captured notifications yet."
            return
        }

        val counts = records.groupingBy { it.classification }.eachCount()
        val cancelled = records.count { it.originalCancelled }
        attentionStatus.text = "Attention Engine: captured ${records.size} | NOW ${counts[com.deanwaylabs.quietos.attention.AttentionClass.NOW] ?: 0} | SOON ${counts[com.deanwaylabs.quietos.attention.AttentionClass.SOON] ?: 0} | DIGEST ${counts[com.deanwaylabs.quietos.attention.AttentionClass.DIGEST] ?: 0} | QUIET ${counts[com.deanwaylabs.quietos.attention.AttentionClass.QUIET] ?: 0} | cancelled $cancelled"

        attentionLog.text = records.take(5).joinToString("\n\n") { record ->
            val action = if (record.originalCancelled) "Original cancelled after Android posted it" else "Original left in Android notification flow"
            "[${record.classification}] ${record.title.ifBlank { record.packageName }}\n${record.text.take(180)}\nWhy: ${record.reason}\nAction: $action"
        }
    }

    override fun onResume() {
        super.onResume()
        if (::attentionStatus.isInitialized) refreshAttentionLog()
    }

    private fun queryDisplayName(uri: Uri): String? {
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) return cursor.getString(0)
        }
        return null
    }

    override fun onDestroy() {
        model.close()
        super.onDestroy()
    }
}
