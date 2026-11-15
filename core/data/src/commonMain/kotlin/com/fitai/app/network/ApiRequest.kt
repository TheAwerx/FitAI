package com.fitai.app.network

enum class ApiMethod {
    GET,
    POST,
    PUT,
    DELETE,
    CALLABLE,
}

data class ApiRequest<T>(
    val url: String = ApiConstants.BASE_URL,
    val method: ApiMethod = ApiMethod.POST,
    val path: String,
    val requestBody: Any? = null,
    val headers: Map<String, String> = emptyMap(),
)

object ApiConstants {
    /** Placeholder for future HTTP Cloud Function endpoints; callables use [path] as function name. */
    const val BASE_URL: String = ""
}
