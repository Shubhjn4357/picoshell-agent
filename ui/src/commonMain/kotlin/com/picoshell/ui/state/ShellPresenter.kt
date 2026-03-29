package com.picoshell.ui.state

import com.picoshell.domain.model.AgentRun
import com.picoshell.domain.model.CacheStrategy
import com.picoshell.domain.model.ExecutionRequest
import com.picoshell.domain.model.InstallState
import com.picoshell.domain.model.ModelCard
import com.picoshell.domain.model.RuntimeConfiguration
import com.picoshell.domain.repository.ModelRepository
import com.picoshell.domain.repository.RunRepository
import com.picoshell.domain.repository.SpecRepository
import com.picoshell.services.agent.AgentExecutor
import com.picoshell.services.download.ModelDownloadManager
import com.picoshell.services.integration.RuntimeSessionStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.koin.dsl.module

class ShellPresenter(
    private val modelRepository: ModelRepository,
    private val runRepository: RunRepository,
    private val specRepository: SpecRepository,
    private val runtimeSessionStore: RuntimeSessionStore,
    private val agentExecutor: AgentExecutor,
    private val modelDownloadManager: ModelDownloadManager,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val promptDraft = MutableStateFlow(ShellUiState().draftPrompt)
    private val modelUrlDraft = MutableStateFlow(ShellUiState().modelUrlDraft)
    private val latestResponse = MutableStateFlow(ShellUiState().latestResponse)
    private val isRunning = MutableStateFlow(false)
    private val mutableState = MutableStateFlow(ShellUiState())

    val uiState: StateFlow<ShellUiState> = mutableState.asStateFlow()

    init {
        scope.launch {
            combine(
                modelRepository.observeModels(),
                runRepository.observeRuns(),
            ) { models, runs ->
                models to runs
            }.combine(runtimeSessionStore.config) { (models, runs), config ->
                Triple(models, runs, config)
            }.combine(promptDraft) { (models, runs, config), prompt ->
                PromptSeed(models, runs, config, prompt)
            }.combine(modelUrlDraft) { seed, urlDraft ->
                StateSeed(
                    models = seed.models,
                    runs = seed.runs,
                    config = seed.config,
                    prompt = seed.prompt,
                    modelUrlDraft = urlDraft,
                )
            }.combine(latestResponse) { seed, response ->
                seed.copy(response = response)
            }.combine(isRunning) { seed, running ->
                ShellUiState(
                    draftPrompt = seed.prompt,
                    modelUrlDraft = seed.modelUrlDraft,
                    config = seed.config,
                    capabilities = specRepository.capabilityStatuses(seed.config, seed.models),
                    phases = specRepository.buildPhases(),
                    models = seed.models,
                    recentRuns = seed.runs.take(6),
                    latestResponse = seed.response,
                    isRunning = running,
                )
            }.collect { state ->
                mutableState.value = state
            }
        }
    }

    fun updatePrompt(prompt: String) {
        promptDraft.value = prompt
    }

    fun updateModelUrlDraft(url: String) {
        modelUrlDraft.value = url
    }

    fun selectModel(modelId: String) {
        runtimeSessionStore.updateSelectedModel(modelId)
    }

    fun updateCacheStrategy(strategy: CacheStrategy) {
        runtimeSessionStore.updateCacheStrategy(strategy)
    }

    fun updateVoiceEnabled(enabled: Boolean) {
        runtimeSessionStore.updateVoiceEnabled(enabled)
    }

    fun updateTelegramEnabled(enabled: Boolean) {
        runtimeSessionStore.updateTelegramEnabled(enabled)
    }

    fun updateCloudFallbackEnabled(enabled: Boolean) {
        runtimeSessionStore.updateCloudFallbackEnabled(enabled)
    }

    fun updatePicoClawCommand(command: String) {
        runtimeSessionStore.updatePicoClawCommand(command)
    }

    fun updateTelegramChannel(channel: String) {
        runtimeSessionStore.updateTelegramChannel(channel)
    }

    fun updateCloudEndpoint(endpoint: String) {
        runtimeSessionStore.updateCloudEndpoint(endpoint)
    }

    fun stageModel(modelId: String) {
        scope.launch {
            latestResponse.value = "Preparing model source for $modelId."
            modelDownloadManager.stageModel(modelId)
                .onSuccess { installation ->
                    latestResponse.value = when (installation.installState) {
                        InstallState.Ready -> {
                            "Model source is ready at ${installation.localPath ?: "unknown location"}."
                        }
                        else -> {
                            installation.notes ?: "Model source prepared."
                        }
                    }
                }
                .onFailure { throwable ->
                    latestResponse.value = throwable.message ?: "Model staging failed."
                }
        }
    }

    fun importLocalModel(uriString: String?) {
        if (uriString.isNullOrBlank()) {
            return
        }

        scope.launch {
            latestResponse.value = "Linking local GGUF file."
            modelDownloadManager.importLocalModel(uriString)
                .onSuccess { model ->
                    runtimeSessionStore.updateSelectedModel(model.catalog.id)
                    latestResponse.value = buildString {
                        append("Linked ")
                        append(model.catalog.name)
                        append(" from device storage without copying it into app storage.")
                        model.installation?.localPath?.let { location ->
                            append(" Source: ")
                            append(location)
                        }
                    }
                }
                .onFailure { throwable ->
                    latestResponse.value = throwable.message ?: "Local model import failed."
                }
        }
    }

    fun downloadModelFromUrl() {
        val url = modelUrlDraft.value.trim()
        if (url.isBlank()) {
            latestResponse.value = "Paste a GGUF file link or Hugging Face model repo link before starting the download."
            return
        }

        scope.launch {
            latestResponse.value = "Resolving and downloading the pasted model link."
            modelDownloadManager.downloadModelFromUrl(url)
                .onSuccess { model ->
                    runtimeSessionStore.updateSelectedModel(model.catalog.id)
                    modelUrlDraft.value = ""
                    latestResponse.value = buildString {
                        append("Downloaded ")
                        append(model.catalog.name)
                        append(" from the pasted link.")
                        model.installation?.localPath?.let { location ->
                            append(" Stored at: ")
                            append(location)
                        }
                    }
                }
                .onFailure { throwable ->
                    latestResponse.value = throwable.message ?: "Model download failed."
                }
        }
    }

    fun runAgent() {
        scope.launch {
            isRunning.value = true
            runCatching {
                agentExecutor.execute(
                    ExecutionRequest(
                        prompt = promptDraft.value,
                        config = runtimeSessionStore.config.value,
                    ),
                )
            }.onSuccess { run ->
                runRepository.record(run)
                latestResponse.value = run.output
            }.onFailure { throwable ->
                latestResponse.value = throwable.message ?: "Execution failed."
            }
            isRunning.value = false
        }
    }
}

private data class PromptSeed(
    val models: List<ModelCard>,
    val runs: List<AgentRun>,
    val config: RuntimeConfiguration,
    val prompt: String,
)

private data class StateSeed(
    val models: List<ModelCard>,
    val runs: List<AgentRun>,
    val config: RuntimeConfiguration,
    val prompt: String,
    val modelUrlDraft: String,
    val response: String = ShellUiState().latestResponse,
)

fun uiModule() = module {
    single { ShellPresenter(get(), get(), get(), get(), get(), get()) }
}
