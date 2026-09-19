package com.deanwaylabs.quietos

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.deanwaylabs.quietos.ai.LiteRtQwenModel
import com.deanwaylabs.quietos.ai.ModelState
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
                ScrollView.LayoutParams(
                    ScrollView.LayoutParams.MATCH_PARENT,
                    ScrollView.LayoutParams.WRAP_CONTENT
                )
            )
        }
        column.addView(status)
        column.addView(choose)
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
