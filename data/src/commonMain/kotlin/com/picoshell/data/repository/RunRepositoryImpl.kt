package com.picoshell.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.picoshell.data.local.db.PicoShellDatabase
import com.picoshell.domain.model.AgentRun
import com.picoshell.domain.model.ExecutionEngine
import com.picoshell.domain.model.RunStatus
import com.picoshell.domain.repository.RunRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow

class RunRepositoryImpl(
    database: PicoShellDatabase,
) : RunRepository {
    private val queries = database.catalogStorageQueries

    override fun observeRuns(): Flow<List<AgentRun>> {
        return queries
            .allAgentRuns(::mapRun)
            .asFlow()
            .mapToList(Dispatchers.IO)
    }

    override suspend fun record(run: AgentRun) {
        queries.insertAgentRun(
            run_id = run.id,
            prompt = run.prompt,
            output = run.output,
            status = run.status.name,
            engine = run.engine.name,
            created_at_epoch_ms = run.createdAtEpochMs,
        )
    }

    private fun mapRun(
        run_id: String,
        prompt: String,
        output: String,
        status: String,
        engine: String,
        created_at_epoch_ms: Long,
    ): AgentRun {
        return AgentRun(
            id = run_id,
            prompt = prompt,
            output = output,
            status = runCatching { RunStatus.valueOf(status) }.getOrDefault(RunStatus.Failure),
            engine = runCatching { ExecutionEngine.valueOf(engine) }.getOrDefault(ExecutionEngine.OfflineLlm),
            createdAtEpochMs = created_at_epoch_ms,
        )
    }
}

