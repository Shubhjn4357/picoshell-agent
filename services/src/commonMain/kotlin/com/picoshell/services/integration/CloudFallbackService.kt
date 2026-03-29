package com.picoshell.services.integration

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

class CloudFallbackService(
    private val client: HttpClient,
) {
    suspend fun generate(
        prompt: String,
        endpoint: String,
    ): String {
        if (endpoint.isBlank()) {
            return "Cloud fallback is enabled but no endpoint is configured."
        }

        return runCatching {
            client.get(endpoint) {
                url.parameters.append("prompt", prompt)
            }.body<String>().ifBlank {
                "Cloud endpoint responded with an empty payload."
            }
        }.getOrElse { throwable ->
            "Cloud fallback probe failed: ${throwable.message ?: "unknown error"}"
        }
    }
}

