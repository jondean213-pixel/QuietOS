package com.deanwaylabs.quietos

data class AssistantProfile(val displayName: String) {
    companion object {
        fun personal() = AssistantProfile("Qwen")
        fun production(name: String = "QuietOS") =
            AssistantProfile(name.trim().ifBlank { "QuietOS" })
    }
}
