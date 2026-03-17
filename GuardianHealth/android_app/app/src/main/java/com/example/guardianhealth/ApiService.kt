package com.example.guardianhealth

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

data class HealthData(
    val heart_rate: Int,
    val spo2: Int,
    val timestamp: String,
    val location: String
)

data class ServiceRequest(
    val location: String,
    val type: String,
    val timestamp: String
)

data class ApiResponse(
    val status: String,
    val message: String?
)

interface ApiService {
    @POST("/health-data")
    suspend fun sendHealthData(@Body data: HealthData): ApiResponse

    @POST("/emergency")
    suspend fun triggerEmergency(@Body data: HealthData): ApiResponse

    @POST("/request-service")
    suspend fun requestService(@Body data: ServiceRequest): ApiResponse
}

object RetrofitClient {
    // Render endpoint
    private const val BASE_URL = "https://guest-65oe.onrender.com" // Update this for real device or Render deployment

    val instance: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
