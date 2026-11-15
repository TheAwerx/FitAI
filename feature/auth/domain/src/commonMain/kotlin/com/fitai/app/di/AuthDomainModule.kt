package com.fitai.app.di

import com.fitai.app.auth.AuthenticateWithEmailUseCase
import com.fitai.app.auth.IsFirebaseAvailableUseCase
import com.fitai.app.auth.LinkExistingSessionUseCase
import com.fitai.app.auth.LinkSignedInAccountUseCase
import com.fitai.app.auth.ObserveAuthStateUseCase
import com.fitai.app.auth.SendPasswordResetUseCase
import com.fitai.app.auth.SignInWithAppleUseCase
import com.fitai.app.auth.SignInWithGoogleUseCase
import com.fitai.app.auth.SignOutUseCase
import com.fitai.app.auth.SyncPlansToFirestoreUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val authDomainModule = module {
    factoryOf(::IsFirebaseAvailableUseCase)
    factoryOf(::LinkSignedInAccountUseCase)
    factoryOf(::LinkExistingSessionUseCase)
    factoryOf(::AuthenticateWithEmailUseCase)
    factoryOf(::SendPasswordResetUseCase)
    factoryOf(::SyncPlansToFirestoreUseCase)
    factoryOf(::SignInWithGoogleUseCase)
    factoryOf(::SignInWithAppleUseCase)
    factoryOf(::SignOutUseCase)
    factoryOf(::ObserveAuthStateUseCase)
}
