package com.picoshell.services.download

import com.picoshell.domain.model.ModelCard
import com.picoshell.domain.model.ModelInstallation

interface ModelDownloadManager {
    suspend fun stageModel(modelId: String): Result<ModelInstallation>
    suspend fun importLocalModel(uriString: String): Result<ModelCard>
    suspend fun downloadModelFromUrl(url: String): Result<ModelCard>
}
