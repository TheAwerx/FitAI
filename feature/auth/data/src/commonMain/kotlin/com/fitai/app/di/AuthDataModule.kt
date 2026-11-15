package com.fitai.app.di

import com.fitai.app.auth.AuthRemoteDataSource
import com.fitai.app.auth.AuthRepository
import com.fitai.app.auth.AuthRepositoryImpl
import org.koin.dsl.module

val authDataModule = module {
    single { AuthRemoteDataSource() }
    single<AuthRepository> { AuthRepositoryImpl(get()) }
}
