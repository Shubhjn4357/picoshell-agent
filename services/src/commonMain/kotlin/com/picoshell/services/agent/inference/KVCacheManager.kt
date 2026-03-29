package com.picoshell.services.agent.inference

import com.picoshell.domain.model.CacheStrategy

class KVCacheManager(
    private val allocator: KVCacheAllocator,
    private val compressor: TurboQuantCompressor,
) {
    fun summarize(
        strategy: CacheStrategy,
        modelName: String,
        prompt: String,
    ): String {
        val window = allocator.estimateWindow(modelName, prompt.length)

        return when (strategy) {
            CacheStrategy.Standard -> "Standard cache keeps a $window-token working set for $modelName."
            CacheStrategy.PagedCache -> "Paged cache slices a $window-token window into reclaimable pages for $modelName."
            CacheStrategy.TurboQuant -> "${compressor.describeCompression(prompt)} Effective window: $window tokens."
        }
    }
}

