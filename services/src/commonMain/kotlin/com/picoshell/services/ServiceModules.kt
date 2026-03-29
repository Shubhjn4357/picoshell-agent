package com.picoshell.services

import com.picoshell.services.agent.AgentExecutor
import com.picoshell.services.agent.OfflineAgentEngine
import com.picoshell.services.agent.inference.KVCacheAllocator
import com.picoshell.services.agent.inference.KVCacheManager
import com.picoshell.services.agent.inference.TurboQuantCompressor
import com.picoshell.services.integration.CloudFallbackService
import com.picoshell.services.integration.RuntimeSessionStore
import com.picoshell.services.integration.TelegramBridgeService
import com.picoshell.services.integration.VoiceControlService
import org.koin.dsl.module

fun servicesModule() = module {
    single { KVCacheAllocator() }
    single { TurboQuantCompressor() }
    single { KVCacheManager(get(), get()) }
    single { RuntimeSessionStore() }
    single { VoiceControlService() }
    single { TelegramBridgeService() }
    single { CloudFallbackService(get()) }
    single { OfflineAgentEngine(get()) }
    single { AgentExecutor(get(), get(), get(), get(), get(), get()) }
}

