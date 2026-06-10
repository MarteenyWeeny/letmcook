package com.letmcook.letmcook.services

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

@Keep
data class SignUpRequest(
    @SerializedName("fullName") val fullName: String,
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

@Keep
data class SignInRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

@Keep
data class AuthResponse(
    @SerializedName("success") val success: Int,
    @SerializedName("message") val message: String,
    @SerializedName("accessToken") val accessToken: String? = null,
    @SerializedName("userLevel") val userLevel: Int? = null,
    @SerializedName("fullName") val fullName: String? = null
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
