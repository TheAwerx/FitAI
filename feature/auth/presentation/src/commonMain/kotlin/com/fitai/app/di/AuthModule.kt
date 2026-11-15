package com.fitai.app.di

import com.fitai.app.viewmodel.AuthViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val authPresentationModule = module {
    includes(authDomainModule)
    viewModelOf(::AuthViewModel)
}
