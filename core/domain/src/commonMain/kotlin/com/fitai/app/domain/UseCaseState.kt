package com.fitai.app.domain

sealed class UseCaseState<out T> {
    data class Success<T>(val data: T) : UseCaseState<T>()
    data class Error(val message: String) : UseCaseState<Nothing>()
}

inline fun <T> useCaseCatching(block: () -> T): UseCaseState<T> =
    try {
        UseCaseState.Success(block())
    } catch (e: Exception) {
        UseCaseState.Error(e.message ?: "Unknown error")
    }

fun <T> Result<T>.toUseCaseState(): UseCaseState<T> =
    fold(
        onSuccess = { UseCaseState.Success(it) },
        onFailure = { UseCaseState.Error(it.message ?: "Unknown error") },
    )
