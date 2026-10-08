package com.deanwaylabs.quietos

import android.app.Activity
import android.app.AlertDialog
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
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
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import com.deanwaylabs.quietos.ai.LiteRtGemmaModel
import com.deanwaylabs.quietos.ai.ModelState
import com.deanwaylabs.quietos.attention.AttentionClass
import com.deanwaylabs.quietos.attention.AttentionRepository
import com.deanwaylabs.quietos.messages.MessageSpamLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * QuietOS stripped build: basic interface.
 * Attention engine, no-spam message/call protection, local model chat.
 * No voice, no branding, no showpiece. A quiet tool.
 */
class MainActivity : AppCompatActivity() {
    private val model = LiteRtGemmaModel()
    private lateinit var status: TextView
    private lateinit var transcript: TextView
    private lateinit var input: EditText
    private lateinit var send: Button
    private lateinit var choose: Button
    private lateinit var attentionStatus: TextView
    private lateinit var interceptionButton: Button
    private lateinit var messageSpamStatus: TextView
    private lateinit var messageSpamButton: Button
    private val attentionRepository by lazy { AttentionRepository(this) }

    private val callScreeningRoleLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            updateCallScreeningStatus()
        }

    private val smsRoleLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            updateSmsRoleStatus()
        }

    private val picker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            lifecycleScope.launch { importAndLoad(uri) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "QuietOS"
        setContentView(buildUi())
        lifecycleScope.launch { autoLoadExistingModel() }
    }

    // ---------- UI ----------

    private fun buildUi(): View {
        val pad = (16 * resources.displayMetrics.density).toInt()
        val bg = android.graphics.Color.rgb(2, 7, 22)
        val gold = android.graphics.Color.rgb(212, 175, 55)
        val textColor = android.graphics.Color.rgb(235, 240, 250)

        fun label(text: String, size: Float = 14f, color: Int = gold): TextView =
            TextView(this).apply {
                this.text = text
                textSize = size
                setTextColor(color)
                setPadding(0, pad / 2, 0, pad / 4)
            }

        fun button(text: String, action: () -> Unit): Button =
            Button(this).apply {
                this.text = text
                setOnClickListener { action() }
            }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
            setBackgroundColor(bg)
        }
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(pad, bars.top, pad, bars.bottom)
            insets
        }

        // Header
        root.addView(label("QuietOS", 22f, textColor))
        status = TextView(this).apply {
            text = "Model: not loaded"
            textSize = 14f
            setTextColor(textColor)
        }
        root.addView(status)

        // Model section
        root.addView(label("Local model"))
        choose = button("Choose model file (.litertlm)") { launchModelPicker() }
        root.addView(choose)

        // Conversation
        root.addView(label("Conversation"))
        transcript = TextView(this).apply {
            textSize = 15f
            setTextColor(textColor)
        }
        val transcriptScroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.5f
            )
            addView(transcript)
        }
        root.addView(transcriptScroll)

        val inputRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        input = EditText(this).apply {
            hint = "Message QuietOS"
            setTextColor(textColor)
            setHintTextColor(android.graphics.Color.rgb(140, 150, 170))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        send = button("Send") { sendMessage() }.apply { isEnabled = false }
        inputRow.addView(input)
        inputRow.addView(send)
        root.addView(inputRow)

        // Attention engine
        root.addView(label("Attention Engine"))
        attentionStatus = TextView(this).apply {
            textSize = 13f
            setTextColor(textColor)
        }
        root.addView(attentionStatus)
        interceptionButton = button("QuietOS Attention Mode: OFF") {
            val prefs = getSharedPreferences("quietos_attention", MODE_PRIVATE)
            val enabled = prefs.getBoolean("interception_enabled", false)
            setAttentionMode(!enabled)
        }
        root.addView(interceptionButton)

        val bucketRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        for (bucket in listOf("NOW", "SOON", "DIGEST", "QUIET")) {
            bucketRow.addView(button(bucket) { showAttentionBucket(bucket) }.apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
        }
        root.addView(bucketRow)
        root.addView(button("Summarize digest") { summarizeDigest() })

        // Spam protection
        root.addView(label("Spam Protection"))
        messageSpamStatus = TextView(this).apply {
            textSize = 13f
            setTextColor(textColor)
        }
        root.addView(messageSpamStatus)
        messageSpamButton = button("Enable Spam Message Protection") { toggleMessageSpam() }
        root.addView(messageSpamButton)
        root.addView(button("View spam message log") { showMessageSpamLog() })

        val callScreeningStatus = TextView(this).apply {
            tag = "call_screening_status"
            textSize = 13f
            setTextColor(textColor)
        }
        root.addView(callScreeningStatus)
        root.addView(button("Enable Spam Call Protection") { requestCallScreeningRole() })

        val smsRoleStatus = TextView(this).apply {
            tag = "sms_role_status"
            textSize = 13f
            setTextColor(textColor)
        }
        root.addView(smsRoleStatus)
        root.addView(button("Set QuietOS as SMS handler") { requestSmsRole() })

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            addView(root)
        }
        return scroll
    }

    // ---------- Model ----------

    private fun launchModelPicker() {
        picker.launch(arrayOf("*/*"))
    }

    private suspend fun autoLoadExistingModel() {
        val existing = withContext(Dispatchers.IO) {
            File(filesDir, "models").listFiles()
                ?.filter { it.isFile && it.extension.equals("litertlm", true) }
                ?.maxByOrNull { it.lastModified() }
        } ?: return
        choose.isEnabled = false
        send.isEnabled = false
        status.text = "Model: loading saved ${existing.name}..."
        try {
            val metrics = model.load(existing.absolutePath)
            status.text = "Model READY | load ${metrics.loadTimeMs} ms | warmup ${metrics.warmupTimeMs} ms"
            transcript.append("\nQuietOS: saved model loaded automatically.\n")
            send.isEnabled = true
        } catch (t: Throwable) {
            status.text = "Saved model load FAILED: ${t.message ?: t.javaClass.simpleName}"
            transcript.append("\nQuietOS: saved model failed to load. Choose a model file to replace it.\n")
        } finally {
            choose.isEnabled = true
        }
    }

    private suspend fun importAndLoad(uri: Uri) {
        choose.isEnabled = false
        send.isEnabled = false
        status.text = "Model: importing..."
        try {
            val displayName = queryDisplayName(uri) ?: "model.litertlm"
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
            status.text = "Model: loading ${local.name}..."
            val metrics = model.load(local.absolutePath)
            status.text = "Model READY | load ${metrics.loadTimeMs} ms | warmup ${metrics.warmupTimeMs} ms"
            transcript.append("\nQuietOS: model loaded locally.\n")
            send.isEnabled = true
        } catch (t: Throwable) {
            status.text = "Model load FAILED: ${t.message ?: t.javaClass.simpleName}"
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
        transcript.append("\nYou: $message\nQuietOS: ")
        status.text = "Model: generating..."
        lifecycleScope.launch {
            try {
                val started = System.nanoTime()
                val result = model.send(message)
                val reply = result.text
                val elapsed = (System.nanoTime() - started) / 1_000_000
                if (reply.isBlank()) {
                    transcript.append("[EMPTY RESPONSE]\n")
                    status.text = "Generation FAILED: empty response after ${elapsed} ms"
                } else {
                    transcript.append(reply + "\n[response ${elapsed} ms]\n")
                    status.text = "Model READY"
                }
            } catch (t: Throwable) {
                transcript.append("[FAILED: ${t.javaClass.simpleName}: ${t.message ?: "no message"}]\n")
                status.text = "Generation FAILED: ${t.javaClass.simpleName}"
            } finally {
                send.isEnabled = model.state == ModelState.READY
            }
        }
    }

    // ---------- Attention engine ----------

    private fun setAttentionMode(enabled: Boolean) {
        getSharedPreferences("quietos_attention", MODE_PRIVATE)
            .edit()
            .putBoolean("interception_enabled", enabled)
            .apply()
        updateInterceptionButton()
        refreshAttentionLog()
    }

    private fun summarizeDigest() {
        val digest = attentionRepository.readAll()
            .filter { it.classification == AttentionClass.DIGEST }
            .take(5)

        if (digest.isEmpty()) {
            transcript.append("\nQuietOS: no DIGEST notifications are waiting.\n")
            return
        }
        if (model.state != ModelState.READY) {
            transcript.append("\nQuietOS: model is not ready yet, so I kept the digest locally.\n")
            return
        }

        val digestContext = digest.mapIndexed { index, record ->
            val source = record.title.ifBlank { record.packageName }
            "${index + 1}. $source: ${record.text.take(120)}"
        }.joinToString("\n")

        val prompt = """These DIGEST notifications were collected. Summarize only the useful information in a calm, concise way. Do not invent details. If several items are repetitive, combine them.

$digestContext"""

        transcript.append("\nQuietOS: summarizing ${digest.size} DIGEST items locally.\n")
        status.text = "Model: summarizing digest..."
        send.isEnabled = false

        lifecycleScope.launch {
            try {
                val result = model.send(prompt)
                transcript.append(result.text + "\n")
                status.text = "Model READY"
            } catch (t: Throwable) {
                transcript.append("[DIGEST FAILED: ${t.javaClass.simpleName}: ${t.message ?: "no message"}]\n")
                status.text = "Digest FAILED: ${t.javaClass.simpleName}"
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
        val classification = runCatching { AttentionClass.valueOf(bucket) }.getOrNull() ?: return
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
        AlertDialog.Builder(this)
            .setTitle("$bucket • ${records.size} item(s)")
            .setView(ScrollView(this).apply { addView(workspace) })
            .setPositiveButton("BACK") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun refreshAttentionLog() {
        val records = attentionRepository.readAll()
        attentionStatus.text = if (records.isEmpty()) {
            "Attention Engine: no captured notifications yet"
        } else {
            val counts = records.groupingBy { it.classification }.eachCount()
            val cancelled = records.count { it.originalCancelled }
            "Attention Engine: ${records.size} captured | NOW ${counts[AttentionClass.NOW] ?: 0} | " +
                "SOON ${counts[AttentionClass.SOON] ?: 0} | DIGEST ${counts[AttentionClass.DIGEST] ?: 0} | " +
                "QUIET ${counts[AttentionClass.QUIET] ?: 0} | intercepted $cancelled"
        }
    }

    // ---------- Spam protection ----------

    private fun toggleMessageSpam() {
        val prefs = getSharedPreferences("quietos_message_spam", MODE_PRIVATE)
        val enabled = prefs.getBoolean("enabled", false)
        prefs.edit().putBoolean("enabled", !enabled).apply()
        updateMessageSpamStatus()
    }

    private fun updateMessageSpamStatus() {
        val enabled = getSharedPreferences("quietos_message_spam", MODE_PRIVATE)
            .getBoolean("enabled", false)
        val count = MessageSpamLog(this).readAll().size
        messageSpamStatus.text = "Spam Message Protection: " +
            if (enabled) "ACTIVE | logged $count" else "INACTIVE | logged $count"
        messageSpamButton.text = if (enabled) "Disable Spam Message Protection" else "Enable Spam Message Protection"
    }

    private fun showMessageSpamLog() {
        val records = MessageSpamLog(this).readAll()
        val body = if (records.isEmpty()) {
            "No SMS spam decisions logged yet."
        } else {
            records.take(40).joinToString("\n\n")
        }
        AlertDialog.Builder(this)
            .setTitle("Spam Message Log • ${records.size}")
            .setView(ScrollView(this).apply {
                addView(TextView(this@MainActivity).apply {
                    text = body
                    textSize = 16f
                    val space = (20 * resources.displayMetrics.density).toInt()
                    setPadding(space, space, space, space)
                })
            })
            .setPositiveButton("BACK") { dialog, _ -> dialog.dismiss() }
            .show()
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
        val statusView = findTaggedTextView(findViewById(android.R.id.content), "call_screening_status") ?: return
        val active = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            getSystemService(RoleManager::class.java).let {
                it.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) && it.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
            }
        statusView.text = if (active) "Spam Call Protection: ACTIVE" else "Spam Call Protection: INACTIVE"
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
        val statusView = findTaggedTextView(findViewById(android.R.id.content), "sms_role_status") ?: return
        val active = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            getSystemService(RoleManager::class.java).let {
                it.isRoleAvailable(RoleManager.ROLE_SMS) && it.isRoleHeld(RoleManager.ROLE_SMS)
            }
        statusView.text = if (active) "SMS Handler: QuietOS ACTIVE" else "SMS Handler: system messaging app"
    }

    // ---------- Helpers ----------

    private fun findTaggedTextView(view: View, wantedTag: String): TextView? {
        if (view is TextView && view.tag == wantedTag) return view
        if (view is android.view.ViewGroup) {
            for (i in 0 until view.childCount) {
                findTaggedTextView(view.getChildAt(i), wantedTag)?.let { return it }
            }
        }
        return null
    }

    private fun queryDisplayName(uri: Uri): String? {
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) return cursor.getString(0)
        }
        return null
    }

    override fun onResume() {
        super.onResume()
        if (::attentionStatus.isInitialized) refreshAttentionLog()
        updateCallScreeningStatus()
        updateSmsRoleStatus()
        if (::messageSpamStatus.isInitialized) updateMessageSpamStatus()
    }

    override fun onDestroy() {
        model.close()
        super.onDestroy()
    }
}
