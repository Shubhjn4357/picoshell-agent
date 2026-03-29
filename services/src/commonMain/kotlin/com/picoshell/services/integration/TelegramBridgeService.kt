package com.picoshell.services.integration

import io.ktor.http.encodeURLQueryComponent

class TelegramBridgeService {
    fun buildDeepLink(
        channel: String,
        message: String,
    ): String? {
        if (channel.isBlank()) return null
        val cleanChannel = channel.removePrefix("@").trim()
        return "https://t.me/$cleanChannel?text=${message.encodeURLQueryComponent()}"
    }
}

