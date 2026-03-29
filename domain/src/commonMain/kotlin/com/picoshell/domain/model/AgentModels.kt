package com.picoshell.domain.model

enum class CacheStrategy {
    Standard,
    PagedCache,
    TurboQuant,
}

enum class CapabilityState {
    Ready,
    Degraded,
    Disabled,
}

enum class InstallState {
    Missing,
    Syncing,
    Staged,
    Ready,
    Error,
}

enum class RunStatus {
    Success,
    Failure,
}

enum class ExecutionEngine {
    PicoClaw,
    OfflineLlm,
    CloudFallback,
}

enum class ModelOrigin {
    Catalog,
    Imported,
}

data class CapabilityStatus(
    val title: String,
    val detail: String,
    val state: CapabilityState,
)

data class BuildPhase(
    val key: String,
    val title: String,
    val detail: String,
)

data class ModelCatalogItem(
    val id: String,
    val name: String,
    val format: String,
    val category: String,
    val sourceUrl: String,
    val summary: String,
    val origin: ModelOrigin = ModelOrigin.Catalog,
)

data class ModelInstallation(
    val modelId: String,
    val installState: InstallState,
    val progress: Float = 0f,
    val localPath: String? = null,
    val notes: String? = null,
)

data class ModelCard(
    val catalog: ModelCatalogItem,
    val installation: ModelInstallation? = null,
) {
    val installState: InstallState
        get() = installation?.installState ?: InstallState.Missing

    val progress: Float
        get() = installation?.progress ?: 0f

    val isImported: Boolean
        get() = catalog.origin == ModelOrigin.Imported

    val isAvailableLocally: Boolean
        get() = installation?.localPath?.isNotBlank() == true &&
            installState == InstallState.Ready
}

data class RuntimeConfiguration(
    val selectedModelId: String = "llama-3.2-3b",
    val cacheStrategy: CacheStrategy = CacheStrategy.TurboQuant,
    val voiceEnabled: Boolean = true,
    val telegramEnabled: Boolean = false,
    val cloudFallbackEnabled: Boolean = true,
    val picoClawCommand: String = "",
    val telegramChannel: String = "",
    val cloudEndpoint: String = "",
)

data class ExecutionRequest(
    val prompt: String,
    val config: RuntimeConfiguration,
)

data class AgentRun(
    val id: String,
    val prompt: String,
    val output: String,
    val status: RunStatus,
    val engine: ExecutionEngine,
    val createdAtEpochMs: Long,
)
