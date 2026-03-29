package com.picoshell.data

import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.picoshell.data.local.db.PicoShellDatabase
import org.koin.dsl.module

fun dataPlatformModule() = module {
    single {
        AndroidSqliteDriver(
            schema = PicoShellDatabase.Schema,
            context = get(),
            name = "picoshell-agent.db",
        )
    }
    single {
        PicoShellDatabase(driver = get())
    }
}

