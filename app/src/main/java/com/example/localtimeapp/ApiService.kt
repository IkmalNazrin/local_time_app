package com.example.localtimeapp

import retrofit2.http.POST
import retrofit2.http.Body

interface ApiService {
    @POST("api/localtime")
    suspend fun sendLocalTime(@Body timeRequest: TimeRequest): TimeResponse
}
