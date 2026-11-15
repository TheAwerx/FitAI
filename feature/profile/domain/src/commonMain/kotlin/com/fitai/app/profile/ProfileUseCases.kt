package com.fitai.app.profile

import com.fitai.app.auth.AuthUser
import com.fitai.app.auth.ObserveAuthStateUseCase
import com.fitai.app.auth.SignInWithAppleUseCase
import com.fitai.app.auth.SignInWithGoogleUseCase
import com.fitai.app.auth.SignOutUseCase
import com.fitai.app.domain.GenerateDietPlanUseCase
import com.fitai.app.domain.GenerateWorkoutPlanUseCase
import com.fitai.app.domain.UseCaseState
import com.fitai.app.domain.useCaseCatching
import com.fitai.app.model.LocalUserProfile
import com.fitai.app.repository.LocalUserRepository
import com.fitai.app.util.nowIsoTimestamp
import kotlinx.coroutines.flow.Flow

class ObserveLocalUserUseCase(
    private val localUserRepository: LocalUserRepository,
) {
    operator fun invoke(): Flow<LocalUserProfile?> = localUserRepository.observeLatestUser()
}

class ObserveProfileAuthUseCase(
    private val observeAuthStateUseCase: ObserveAuthStateUseCase,
) {
    operator fun invoke(): Flow<AuthUser?> = observeAuthStateUseCase()
}

class ProfileSignOutUseCase(
    private val signOutUseCase: SignOutUseCase,
) {
    suspend operator fun invoke(): UseCaseState<Unit> = signOutUseCase()
}

class ProfileSignInWithGoogleUseCase(
    private val signInWithGoogleUseCase: SignInWithGoogleUseCase,
) {
    suspend operator fun invoke(idToken: String): UseCaseState<Unit> =
        signInWithGoogleUseCase(idToken).asUnit()
}

class ProfileSignInWithAppleUseCase(
    private val signInWithAppleUseCase: SignInWithAppleUseCase,
) {
    suspend operator fun invoke(idToken: String, rawNonce: String): UseCaseState<Unit> =
        signInWithAppleUseCase(idToken, rawNonce).asUnit()
}

private fun UseCaseState<*>.asUnit(): UseCaseState<Unit> = when (this) {
    is UseCaseState.Success -> UseCaseState.Success(Unit)
    is UseCaseState.Error -> this
}

data class UpdateProfileInput(
    val id: String,
    val name: String,
    val email: String,
    val age: Long,
    val heightCm: Double,
    val weightKg: Double,
    val goal: String,
    val activityLevel: String,
    val equipment: String,
    val dietRestrictions: String,
)

class UpdateProfileUseCase(
    private val localUserRepository: LocalUserRepository,
) {
    suspend operator fun invoke(input: UpdateProfileInput): UseCaseState<Unit> = useCaseCatching {
        localUserRepository.updateProfile(
            id = input.id,
            name = input.name.trim(),
            email = input.email.trim(),
            age = input.age,
            heightCm = input.heightCm,
            weightKg = input.weightKg,
            goal = input.goal.trim(),
            activityLevel = input.activityLevel.trim(),
            equipment = input.equipment.trim(),
            dietRestrictions = input.dietRestrictions.trim(),
            updatedAt = nowIsoTimestamp(),
        )
    }
}

class RegeneratePlansUseCase(
    private val generateWorkoutPlanUseCase: GenerateWorkoutPlanUseCase,
    private val generateDietPlanUseCase: GenerateDietPlanUseCase,
) {
    suspend operator fun invoke(): UseCaseState<Unit> {
        when (val workout = generateWorkoutPlanUseCase()) {
            is UseCaseState.Error -> return workout
            is UseCaseState.Success -> Unit
        }
        return when (val diet = generateDietPlanUseCase()) {
            is UseCaseState.Error -> diet
            is UseCaseState.Success -> UseCaseState.Success(Unit)
        }
    }
}
