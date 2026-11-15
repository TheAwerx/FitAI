package com.fitai.app.di

import com.fitai.app.profile.ObserveLocalUserUseCase
import com.fitai.app.profile.ObserveProfileAuthUseCase
import com.fitai.app.profile.ProfileSignInWithAppleUseCase
import com.fitai.app.profile.ProfileSignInWithGoogleUseCase
import com.fitai.app.profile.ProfileSignOutUseCase
import com.fitai.app.profile.RegeneratePlansUseCase
import com.fitai.app.profile.UpdateProfileUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val profileDomainModule = module {
    factoryOf(::ObserveLocalUserUseCase)
    factoryOf(::ObserveProfileAuthUseCase)
    factoryOf(::ProfileSignOutUseCase)
    factoryOf(::ProfileSignInWithGoogleUseCase)
    factoryOf(::ProfileSignInWithAppleUseCase)
    factoryOf(::UpdateProfileUseCase)
    factoryOf(::RegeneratePlansUseCase)
}
