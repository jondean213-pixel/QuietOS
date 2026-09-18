package com.deanwaylabs.quietos

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val profile = AssistantProfile.personal()
        setContentView(TextView(this).apply {
            text = "QuietOS Alpha 0.1\nAssistant: ${profile.displayName}\nAttention Engine: foundation ready"
            textSize = 20f
            setPadding(48, 72, 48, 48)
        })
    }
}
