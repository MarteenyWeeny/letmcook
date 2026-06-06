package com.letmcook.letmcook.services

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

data class SignUpRequest(
    val fullName: String,
    val email: String,
    val password: String
)

data class SignInRequest(
    val email: String,
    val password: String
)

data class AuthResponse(
    val success: Int,
    val message: String,
    val accessToken: String? = null,
    val userLevel: Int? = null,
    val fullName: String? = null
)

interface AuthService {
    @POST("signup")
    suspend fun signUp(@Body request: SignUpRequest): Response<AuthResponse>

    @POST("signin")
    suspend fun signIn(@Body request: SignInRequest): Response<AuthResponse>

    companion object {
        const val BASE_URL = "https://calmmove-api.vercel.app/" // Mock API base URL
    }
}
