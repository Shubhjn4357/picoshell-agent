package com.picoshell.data.repository

import com.picoshell.data.local.SpecRepositoryImpl
import com.picoshell.domain.repository.ModelRepository
import com.picoshell.domain.repository.RunRepository
import com.picoshell.domain.repository.SpecRepository
import org.koin.dsl.module

fun dataModule() = module {
    single<SpecRepository> { SpecRepositoryImpl() }
    single<ModelRepository> { ModelRepositoryImpl(get()) }
    single<RunRepository> { RunRepositoryImpl(get()) }
}

