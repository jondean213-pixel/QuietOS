package com.deanwaylabs.quietos.messages

import android.app.Activity
import android.os.Bundle
import android.telephony.SmsManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

class SmsComposeActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val destination = intent?.data?.schemeSpecificPart.orEmpty()
        val initial = intent?.getStringExtra(android.content.Intent.EXTRA_TEXT).orEmpty()

        val to = EditText(this).apply {
            hint = "Phone number"
            setText(destination)
        }
        val body = EditText(this).apply {
            hint = "Message"
            setText(initial)
        }
        val status = TextView(this)
        val send = Button(this).apply {
            text = "Send SMS"
            setOnClickListener {
                val number = to.text.toString().trim()
                val text = body.text.toString()
                if (number.isBlank() || text.isBlank()) {
                    status.text = "Enter a phone number and message."
                } else {
                    runCatching {
                        SmsManager.getDefault().sendTextMessage(number, null, text, null, null)
                    }.onSuccess {
                        status.text = "Message sent."
                    }.onFailure {
                        status.text = "Send failed: " + (it.message ?: it.javaClass.simpleName)
                    }
                }
            }
        }

        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (20 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, pad)
            addView(to)
            addView(body)
            addView(send)
            addView(status)
        })
    }
}
