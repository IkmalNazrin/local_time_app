package com.example.localtimeapp

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {
    @POST("api/localtime")
    suspend fun sendLocalTime(@Body timeRequest: TimeRequest): TimeResponse

    @GET("api/localtime")
    suspend fun getLocalTimes(): List<TimeResponse>
}
