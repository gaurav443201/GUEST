package com.example.guardianhealth.network

import com.example.guardianhealth.data.Alert
import com.example.guardianhealth.data.ApiResponse
import com.example.guardianhealth.data.HealthData
import com.example.guardianhealth.data.ServiceRequest
import com.example.guardianhealth.data.Stats
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

import com.example.guardianhealth.data.VitalsAnalysisRequest
import com.example.guardianhealth.data.VitalsAnalysisResponse

interface ApiService {

    @POST("health-data")
    suspend fun postHealthData(@Body data: HealthData): Response<ApiResponse>

    @POST("emergency")
    suspend fun postEmergency(@Body data: HealthData): Response<ApiResponse>

    @GET("alerts")
    suspend fun getAlerts(): Response<List<Alert>>

    @POST("service-request")
    suspend fun requestService(@Body request: ServiceRequest): Response<ApiResponse>

    @POST("resolve/{alert_id}")
    suspend fun resolveAlert(@Path("alert_id") alertId: Int): Response<ApiResponse>

    @GET("stats")
    suspend fun getStats(): Response<Stats>

    @POST("ai/analyze")
    suspend fun postAiAnalyze(@Body data: VitalsAnalysisRequest): Response<VitalsAnalysisResponse>
}
