package com.picoshell.services

import com.picoshell.core.BinaryExecutor
import com.picoshell.core.JvmBinaryExecutor
import com.picoshell.services.download.ModelDownloadManager
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.dsl.module

fun servicesPlatformModule() = module {
    single<HttpClient> {
        HttpClient(OkHttp) {
            expectSuccess = false
        }
    }
    single<BinaryExecutor> { JvmBinaryExecutor() }
    single<ModelDownloadManager> { AndroidModelDownloadManager(get(), get(), get()) }
}
