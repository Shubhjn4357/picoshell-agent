package com.picoshell.ui.state

import com.picoshell.domain.model.AgentRun
import com.picoshell.domain.model.BuildPhase
import com.picoshell.domain.model.CapabilityStatus
import com.picoshell.domain.model.ModelCard
import com.picoshell.domain.model.RuntimeConfiguration

data class ShellUiState(
    val draftPrompt: String = "Outline the next mobile agent milestone with concrete engineering steps.",
    val modelUrlDraft: String = "",
    val config: RuntimeConfiguration = RuntimeConfiguration(),
    val capabilities: List<CapabilityStatus> = emptyList(),
    val phases: List<BuildPhase> = emptyList(),
    val models: List<ModelCard> = emptyList(),
    val recentRuns: List<AgentRun> = emptyList(),
    val latestResponse: String = "The orchestration lane is idle. Stage a model or run the seed prompt to inspect the pipeline.",
    val isRunning: Boolean = false,
)
