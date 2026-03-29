package com.picoshell.core

data class BinaryExecutionResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
)

interface BinaryExecutor {
    suspend fun execute(
        commandLine: String,
        stdin: String? = null,
    ): BinaryExecutionResult
}

fun String.toSlug(): String = buildString {
    lowercase().forEach { char ->
        when {
            char.isLetterOrDigit() -> append(char)
            isEmpty() || last() == '-' -> Unit
            else -> append('-')
        }
    }
}.trim('-')

