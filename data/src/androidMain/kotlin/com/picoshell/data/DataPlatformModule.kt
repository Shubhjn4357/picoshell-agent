package com.picoshell.data

import android.content.Context
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.picoshell.data.local.db.PicoShellDatabase
import org.koin.dsl.module

private const val DatabaseName = "picoshell-agent.db"

fun dataPlatformModule() = module {
    single {
        provideDatabase(get())
    }
}

private fun provideDatabase(context: Context): PicoShellDatabase {
    return openValidatedDatabase(context)
        .getOrElse { firstFailure ->
            context.deleteDatabase(DatabaseName)
            openValidatedDatabase(context)
                .getOrElse { secondFailure ->
                    throw IllegalStateException(
                        "Unable to open PicoShell database even after clearing the local cache.",
                        secondFailure,
                    )
                }
        }
}

private fun openValidatedDatabase(context: Context): Result<PicoShellDatabase> {
    val driver = AndroidSqliteDriver(
        schema = PicoShellDatabase.Schema,
        context = context,
        name = DatabaseName,
    )

    return runCatching {
        PicoShellDatabase(driver = driver).also(::validateDatabase)
    }.onFailure {
        driver.close()
    }
}

private fun validateDatabase(database: PicoShellDatabase) {
    database.catalogStorageQueries
        .allStoredModels { _, _, _, _, _, _, _, _, _ -> Unit }
        .executeAsList()
    database.catalogStorageQueries
        .allAgentRuns { _, _, _, _, _, _ -> Unit }
        .executeAsList()
}
