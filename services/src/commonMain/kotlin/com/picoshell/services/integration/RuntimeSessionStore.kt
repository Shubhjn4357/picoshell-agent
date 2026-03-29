package com.picoshell.services.integration

import com.picoshell.domain.model.CacheStrategy
import com.picoshell.domain.model.RuntimeConfiguration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class RuntimeSessionStore {
    private val mutableConfig = MutableStateFlow(RuntimeConfiguration())

    val config: StateFlow<RuntimeConfiguration> = mutableConfig.asStateFlow()

    fun updateSelectedModel(modelId: String) {
        mutableConfig.update { it.copy(selectedModelId = modelId) }
    }

    fun updateCacheStrategy(strategy: CacheStrategy) {
        mutableConfig.update { it.copy(cacheStrategy = strategy) }
    }

    fun updateVoiceEnabled(enabled: Boolean) {
        mutableConfig.update { it.copy(voiceEnabled = enabled) }
    }

    fun updateTelegramEnabled(enabled: Boolean) {
        mutableConfig.update { it.copy(telegramEnabled = enabled) }
    }

    fun updateCloudFallbackEnabled(enabled: Boolean) {
        mutableConfig.update { it.copy(cloudFallbackEnabled = enabled) }
    }

    fun updatePicoClawCommand(command: String) {
        mutableConfig.update { it.copy(picoClawCommand = command) }
    }

    fun updateTelegramChannel(channel: String) {
        mutableConfig.update { it.copy(telegramChannel = channel) }
    }

    fun updateCloudEndpoint(endpoint: String) {
        mutableConfig.update { it.copy(cloudEndpoint = endpoint) }
    }
}

