package com.picoshell.data.local

import com.picoshell.domain.model.BuildPhase
import com.picoshell.domain.model.CacheStrategy
import com.picoshell.domain.model.CapabilityState
import com.picoshell.domain.model.CapabilityStatus
import com.picoshell.domain.model.InstallState
import com.picoshell.domain.model.ModelCard
import com.picoshell.domain.model.ModelCatalogItem
import com.picoshell.domain.model.RuntimeConfiguration
import com.picoshell.domain.repository.SpecRepository

internal object SeedCatalog {
    val models = listOf(
        ModelCatalogItem(
            id = "llama-3.2-3b",
            name = "Llama-3.2-3B-Instruct",
            format = "GGUF",
            category = "Text",
            sourceUrl = "https://huggingface.co/bartowski/Llama-3.2-3B-Instruct-GGUF",
            summary = "Balanced local text model tuned for prompt routing and multi-step plans.",
        ),
        ModelCatalogItem(
            id = "phi-4-mini",
            name = "Phi-4-Mini-Instruct",
            format = "GGUF",
            category = "Text",
            sourceUrl = "https://huggingface.co/bartowski/phi-4-mini-instruct-GGUF",
            summary = "Fast fallback for constrained devices when latency matters more than breadth.",
        ),
        ModelCatalogItem(
            id = "sdxl",
            name = "Stable-Diffusion-XL",
            format = "Diffusers",
            category = "Image",
            sourceUrl = "https://huggingface.co/stabilityai/stable-diffusion-xl-base-1.0",
            summary = "Reference image generation backbone for concept previews and creative output.",
        ),
        ModelCatalogItem(
            id = "flux-1-schnell",
            name = "FLUX-1-Schnell",
            format = "Diffusers",
            category = "Image",
            sourceUrl = "https://huggingface.co/black-forest-labs/FLUX.1-schnell",
            summary = "High-speed image option for instant concept passes and iterative refinement.",
        ),
    )

    val phases = listOf(
        BuildPhase("bootstrap_project", "Bootstrap Project", "Create the Compose Multiplatform shell and shared module graph."),
        BuildPhase("create_core_process_runner", "Core Process Runner", "Wrap PicoClaw command execution behind a binary bridge."),
        BuildPhase("create_domain_models", "Domain Models", "Define execution, model, service, and runtime contracts."),
        BuildPhase("setup_sqldelight_db", "SQLDelight Database", "Persist staged models and execution history."),
        BuildPhase("implement_repositories", "Repositories", "Merge spec seed data with local storage flows."),
        BuildPhase("implement_agent_executor", "Agent Executor", "Orchestrate PicoClaw, offline GGUF, and cloud fallback lanes."),
        BuildPhase("implement_download_manager", "Download Manager", "Support direct artifacts, Hugging Face repo resolution, and imported local GGUF files."),
        BuildPhase("build_design_system", "Design System", "Shape the industrial warm-tone visual system and cards."),
        BuildPhase("build_interactive_components", "Interactive Components", "Compose prompt controls, status chips, and timelines."),
        BuildPhase("build_screens", "Screens", "Deliver dashboard, models, and services surfaces."),
        BuildPhase("integrate_services", "Service Integrations", "Wire Telegram, voice, and cache strategy controls."),
        BuildPhase("setup_ci", "Continuous Integration", "Assemble debug and signed release Android artifacts."),
    )
}

class SpecRepositoryImpl : SpecRepository {
    override fun buildPhases(): List<BuildPhase> = SeedCatalog.phases

    override fun capabilityStatuses(
        config: RuntimeConfiguration,
        models: List<ModelCard>,
    ): List<CapabilityStatus> {
        val stagedLocalModel = models.any { card ->
            card.isAvailableLocally
        }

        return listOf(
            CapabilityStatus(
                title = "PicoClaw Binary Execution",
                detail = if (config.picoClawCommand.isBlank()) {
                    "Awaiting a command path or packaged binary."
                } else {
                    "Ready to delegate prompts to `${config.picoClawCommand}`."
                },
                state = if (config.picoClawCommand.isBlank()) CapabilityState.Degraded else CapabilityState.Ready,
            ),
            CapabilityStatus(
                title = "Offline LLM via GGUF",
                detail = if (stagedLocalModel) {
                    "At least one local GGUF file is available for offline routing."
                } else {
                    "Import a GGUF file from device storage or download one from a direct artifact or compatible Hugging Face repo link."
                },
                state = if (stagedLocalModel) CapabilityState.Ready else CapabilityState.Degraded,
            ),
            CapabilityStatus(
                title = "Cloud Fallback",
                detail = when {
                    !config.cloudFallbackEnabled -> "Cloud fallback is intentionally disabled."
                    config.cloudEndpoint.isBlank() -> "Enable a lightweight HTTP endpoint to activate fallback."
                    else -> "Configured to probe `${config.cloudEndpoint}` when local execution fails."
                },
                state = when {
                    !config.cloudFallbackEnabled -> CapabilityState.Disabled
                    config.cloudEndpoint.isBlank() -> CapabilityState.Degraded
                    else -> CapabilityState.Ready
                },
            ),
            CapabilityStatus(
                title = "Telegram Bot Bridge",
                detail = when {
                    !config.telegramEnabled -> "Telegram relay is disabled."
                    config.telegramChannel.isBlank() -> "Add a Telegram handle or channel to deep-link results."
                    else -> "Ready to hand off results to `${config.telegramChannel}`."
                },
                state = when {
                    !config.telegramEnabled -> CapabilityState.Disabled
                    config.telegramChannel.isBlank() -> CapabilityState.Degraded
                    else -> CapabilityState.Ready
                },
            ),
            CapabilityStatus(
                title = "Voice Control",
                detail = if (config.voiceEnabled) {
                    "Voice lane is primed for Whisper or Vosk handoff."
                } else {
                    "Voice control is paused."
                },
                state = if (config.voiceEnabled) CapabilityState.Ready else CapabilityState.Disabled,
            ),
            CapabilityStatus(
                title = "TurboQuant Cache",
                detail = when (config.cacheStrategy) {
                    CacheStrategy.Standard -> "Plain KV cache for maximum determinism."
                    CacheStrategy.PagedCache -> "Paged cache lowers memory pressure during long runs."
                    CacheStrategy.TurboQuant -> "Compressed KV cache favors mobile RAM efficiency."
                },
                state = CapabilityState.Ready,
            ),
        )
    }
}
