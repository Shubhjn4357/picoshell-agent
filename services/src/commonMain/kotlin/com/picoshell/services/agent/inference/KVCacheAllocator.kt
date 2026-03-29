package com.picoshell.services.agent.inference

class KVCacheAllocator {
    fun estimateWindow(
        modelName: String,
        promptLength: Int,
    ): Int {
        val baseWindow = when {
            "phi" in modelName.lowercase() -> 2048
            "llama" in modelName.lowercase() -> 4096
            else -> 3072
        }

        return (baseWindow - promptLength.coerceAtMost(baseWindow / 2)).coerceAtLeast(1024)
    }
}

