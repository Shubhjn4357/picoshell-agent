package com.picoshell.agent

import android.app.Application
import com.picoshell.data.dataPlatformModule
import com.picoshell.data.repository.dataModule
import com.picoshell.services.servicesModule
import com.picoshell.services.servicesPlatformModule
import com.picoshell.ui.state.uiModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class PicoShellApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(this@PicoShellApplication)
            modules(
                dataModule(),
                dataPlatformModule(),
                servicesModule(),
                servicesPlatformModule(),
                uiModule(),
            )
        }
    }
}

