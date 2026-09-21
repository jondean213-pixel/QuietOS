package com.deanwaylabs.quietos.messages

import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Intent
import android.os.Bundle
import android.provider.Telephony
import android.telephony.SmsManager
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.text.DateFormat
import java.util.Date

class MessagesActivity : Activity() {
    private lateinit var list: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "QuietOS Messages"
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val newMessage = Button(this).apply {
            text = "New message"
            setOnClickListener { startActivity(Intent(this@MessagesActivity, SmsComposeActivity::class.java)) }
        }
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val p = dp(16)
            setPadding(p, p, p, p)
            addView(TextView(this@MessagesActivity).apply { text = "QuietOS Messages"; textSize = 24f })
            addView(TextView(this@MessagesActivity).apply {
                text = "Clean messages only. Spam Shield keeps intercepted messages in quarantine."
                textSize = 14f
            })
            addView(newMessage)
            addView(ScrollView(this@MessagesActivity).apply { addView(list) },
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        })
    }

    override fun onResume() {
        super.onResume()
        refreshThreads()
    }

    private data class SmsRow(val address: String, val body: String, val date: Long, val type: Int)

    private fun readRows(): List<SmsRow> {
        val rows = mutableListOf<SmsRow>()
        val projection = arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE, Telephony.Sms.TYPE)
        runCatching {
            contentResolver.query(Telephony.Sms.CONTENT_URI, projection, null, null, Telephony.Sms.DATE + " DESC")?.use { cursor ->
                val addressIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                val bodyIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
                val dateIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)
                val typeIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.TYPE)
                while (cursor.moveToNext()) {
                    rows += SmsRow(
                        cursor.getString(addressIndex).orEmpty(),
                        cursor.getString(bodyIndex).orEmpty(),
                        cursor.getLong(dateIndex),
                        cursor.getInt(typeIndex)
                    )
                }
            }
        }
        return rows
    }

    private fun refreshThreads() {
        list.removeAllViews()
        val latest = readRows().groupBy { it.address }.values
            .mapNotNull { group -> group.maxByOrNull { it.date } }
            .sortedByDescending { it.date }
        if (latest.isEmpty()) {
            list.addView(TextView(this).apply { text = "No clean SMS conversations yet."; textSize = 18f; setPadding(dp(20), dp(20), dp(20), dp(20)) })
            return
        }
        latest.forEach { row ->
            list.addView(Button(this).apply {
                text = row.address.ifBlank { "Unknown sender" } + "\n" + row.body.take(90)
                textAlignment = View.TEXT_ALIGNMENT_TEXT_START
                setOnClickListener { showThread(row.address) }
            })
        }
    }

    private fun showThread(address: String) {
        val rows = readRows().filter { it.address == address }.sortedBy { it.date }
        val threadText = rows.joinToString("\n\n") { row ->
            val direction = if (row.type == Telephony.Sms.MESSAGE_TYPE_SENT) "You" else address.ifBlank { "Unknown" }
            val whenText = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(row.date))
            direction + " • " + whenText + "\n" + row.body
        }
        val threadView = TextView(this).apply { text = threadText.ifBlank { "No messages." }; textSize = 17f; setPadding(dp(16), dp(16), dp(16), dp(16)) }
        AlertDialog.Builder(this)
            .setTitle(address.ifBlank { "Conversation" })
            .setView(ScrollView(this).apply { addView(threadView) })
            .setNegativeButton("Back", null)
            .setPositiveButton("Reply") { _, _ -> showReply(address) }
            .show()
    }

    private fun showReply(address: String) {
        val body = EditText(this).apply { hint = "Message"; minLines = 3 }
        AlertDialog.Builder(this)
            .setTitle("Reply to " + address)
            .setView(body)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Send") { _, _ -> sendAndStore(address, body.text.toString()) }
            .show()
    }

    private fun sendAndStore(address: String, body: String) {
        val cleanAddress = address.trim()
        val cleanBody = body.trim()
        if (cleanAddress.isBlank() || cleanBody.isBlank()) return
        runCatching {
            SmsManager.getDefault().sendTextMessage(cleanAddress, null, cleanBody, null, null)
            contentResolver.insert(Telephony.Sms.Sent.CONTENT_URI, ContentValues().apply {
                put(Telephony.Sms.ADDRESS, cleanAddress)
                put(Telephony.Sms.BODY, cleanBody)
                put(Telephony.Sms.DATE, System.currentTimeMillis())
                put(Telephony.Sms.READ, 1)
                put(Telephony.Sms.SEEN, 1)
                put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_SENT)
            })
        }.onSuccess { refreshThreads() }
         .onFailure {
             AlertDialog.Builder(this).setTitle("Send failed").setMessage(it.message ?: it.javaClass.simpleName).setPositiveButton("Back", null).show()
         }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
