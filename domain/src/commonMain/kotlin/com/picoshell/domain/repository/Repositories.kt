package com.picoshell.domain.repository

import com.picoshell.domain.model.AgentRun
import com.picoshell.domain.model.BuildPhase
import com.picoshell.domain.model.CapabilityStatus
import com.picoshell.domain.model.ModelCard
import com.picoshell.domain.model.ModelInstallation
import com.picoshell.domain.model.RuntimeConfiguration
import kotlinx.coroutines.flow.Flow

interface ModelRepository {
    fun observeModels(): Flow<List<ModelCard>>
    suspend fun getModel(modelId: String): ModelCard?
    suspend fun upsertModel(model: ModelCard)
    suspend fun upsertInstallation(installation: ModelInstallation)
}

interface RunRepository {
    fun observeRuns(): Flow<List<AgentRun>>
    suspend fun record(run: AgentRun)
}

interface SpecRepository {
    fun buildPhases(): List<BuildPhase>
    fun capabilityStatuses(
        config: RuntimeConfiguration,
        models: List<ModelCard>,
    ): List<CapabilityStatus>
}
