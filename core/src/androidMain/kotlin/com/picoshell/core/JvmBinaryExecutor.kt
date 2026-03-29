package com.picoshell.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class JvmBinaryExecutor : BinaryExecutor {
    override suspend fun execute(
        commandLine: String,
        stdin: String?,
    ): BinaryExecutionResult = withContext(Dispatchers.IO) {
        if (commandLine.isBlank()) {
            return@withContext BinaryExecutionResult(
                exitCode = 127,
                stdout = "",
                stderr = "PicoClaw command is empty.",
            )
        }

        runCatching {
            val process = ProcessBuilder(tokenize(commandLine))
                .redirectErrorStream(false)
                .start()

            if (!stdin.isNullOrBlank()) {
                process.outputStream.bufferedWriter().use { writer ->
                    writer.write(stdin)
                    writer.flush()
                }
            } else {
                process.outputStream.close()
            }

            val stdout = process.inputStream.bufferedReader().use { it.readText() }
            val stderr = process.errorStream.bufferedReader().use { it.readText() }
            val exitCode = process.waitFor()

            BinaryExecutionResult(
                exitCode = exitCode,
                stdout = stdout.trim(),
                stderr = stderr.trim(),
            )
        }.getOrElse { throwable ->
            BinaryExecutionResult(
                exitCode = 1,
                stdout = "",
                stderr = throwable.message ?: "Binary execution failed.",
            )
        }
    }

    private fun tokenize(commandLine: String): List<String> {
        val tokenPattern = Regex("""[^\s"']+|"([^"]*)"|'([^']*)'""")
        return tokenPattern.findAll(commandLine).map { match ->
            match.groups[1]?.value
                ?: match.groups[2]?.value
                ?: match.value
        }.toList()
    }
}
