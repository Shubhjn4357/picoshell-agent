package com.picoshell.services.integration

class VoiceControlService {
    fun resolvePrompt(
        typedPrompt: String,
        voiceEnabled: Boolean,
    ): String {
        if (typedPrompt.isNotBlank()) {
            return typedPrompt
        }

        return if (voiceEnabled) {
            "Voice checkpoint: produce a concise agent status brief for the current mobile runtime."
        } else {
            ""
        }
    }
}

