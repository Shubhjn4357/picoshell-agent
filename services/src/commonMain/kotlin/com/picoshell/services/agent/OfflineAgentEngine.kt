package com.picoshell.services.agent

import com.picoshell.domain.model.CacheStrategy
import com.picoshell.domain.model.ModelCard
import com.picoshell.services.agent.inference.KVCacheManager

class OfflineAgentEngine(
    private val cacheManager: KVCacheManager,
) {
    fun generate(
        prompt: String,
        selectedModel: ModelCard?,
        strategy: CacheStrategy,
    ): String {
        val modelName = selectedModel?.catalog?.name ?: "Fallback Sandbox Model"
        val cacheSummary = cacheManager.summarize(strategy, modelName, prompt)
        val installSummary = buildString {
            append(selectedModel?.installation?.notes ?: "No imported or downloaded GGUF file is available yet.")
            selectedModel?.installation?.localPath?.takeIf { it.isNotBlank() }?.let { location ->
                appendLine()
                append(if (location.startsWith("content://")) "Linked document: " else "Local file: ")
                append(location)
            }
        }

        return buildString {
            appendLine("Offline inference lane engaged with $modelName.")
            appendLine(cacheSummary)
            appendLine("Model readiness: $installSummary")
            appendLine()
            append("Response draft: ")
            append(prompt.ifBlank { "No prompt was supplied, so the agent is returning a system health brief." })
        }.trim()
    }
}
