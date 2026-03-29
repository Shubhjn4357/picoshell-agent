package com.picoshell.services.agent

import com.picoshell.core.BinaryExecutor
import com.picoshell.domain.model.AgentRun
import com.picoshell.domain.model.ExecutionEngine
import com.picoshell.domain.model.ExecutionRequest
import com.picoshell.domain.model.RunStatus
import com.picoshell.domain.repository.ModelRepository
import com.picoshell.services.integration.CloudFallbackService
import com.picoshell.services.integration.TelegramBridgeService
import com.picoshell.services.integration.VoiceControlService
import kotlin.random.Random
import kotlin.time.Clock

class AgentExecutor(
    private val binaryExecutor: BinaryExecutor,
    private val offlineAgentEngine: OfflineAgentEngine,
    private val cloudFallbackService: CloudFallbackService,
    private val telegramBridgeService: TelegramBridgeService,
    private val voiceControlService: VoiceControlService,
    private val modelRepository: ModelRepository,
) {
    suspend fun execute(request: ExecutionRequest): AgentRun {
        val resolvedPrompt = voiceControlService.resolvePrompt(
            typedPrompt = request.prompt,
            voiceEnabled = request.config.voiceEnabled,
        )

        val selectedModel = modelRepository.getModel(request.config.selectedModelId)
        val timestamp = Clock.System.now().toEpochMilliseconds()

        if (request.config.picoClawCommand.isNotBlank()) {
            val processResult = binaryExecutor.execute(
                commandLine = request.config.picoClawCommand,
                stdin = resolvedPrompt,
            )

            if (processResult.exitCode == 0 && processResult.stdout.isNotBlank()) {
                return createRun(
                    prompt = resolvedPrompt,
                    output = appendTelegramLink(
                        baseOutput = processResult.stdout,
                        telegramChannel = request.config.telegramChannel,
                    ),
                    engine = ExecutionEngine.PicoClaw,
                    status = RunStatus.Success,
                    timestamp = timestamp,
                )
            }
        }

        val offlineOutput = offlineAgentEngine.generate(
            prompt = resolvedPrompt,
            selectedModel = selectedModel,
            strategy = request.config.cacheStrategy,
        )

        val finalOutput = if (
            request.config.cloudFallbackEnabled &&
            request.config.cloudEndpoint.isNotBlank()
        ) {
            val cloudOutput = cloudFallbackService.generate(
                prompt = resolvedPrompt,
                endpoint = request.config.cloudEndpoint,
            )
            buildString {
                appendLine(offlineOutput)
                appendLine()
                appendLine("Cloud fallback note:")
                append(cloudOutput)
            }
        } else {
            offlineOutput
        }

        return createRun(
            prompt = resolvedPrompt,
            output = appendTelegramLink(
                baseOutput = finalOutput,
                telegramChannel = request.config.telegramChannel,
            ),
            engine = if (request.config.cloudFallbackEnabled && request.config.cloudEndpoint.isNotBlank()) {
                ExecutionEngine.CloudFallback
            } else {
                ExecutionEngine.OfflineLlm
            },
            status = RunStatus.Success,
            timestamp = timestamp,
        )
    }

    private fun appendTelegramLink(
        baseOutput: String,
        telegramChannel: String,
    ): String {
        val link = telegramBridgeService.buildDeepLink(
            channel = telegramChannel,
            message = baseOutput.take(180),
        ) ?: return baseOutput

        return buildString {
            appendLine(baseOutput)
            appendLine()
            append("Telegram relay: ")
            append(link)
        }
    }

    private fun createRun(
        prompt: String,
        output: String,
        engine: ExecutionEngine,
        status: RunStatus,
        timestamp: Long,
    ): AgentRun {
        return AgentRun(
            id = "run-${timestamp}-${Random.nextInt(1000, 9999)}",
            prompt = prompt,
            output = output,
            status = status,
            engine = engine,
            createdAtEpochMs = timestamp,
        )
    }
}

