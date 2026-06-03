package com.example.todolistmanager

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object MockApiClient {
    // IMPORTANT: Replace this with your actual mockapi.io endpoint
    // Make sure it ends with a trailing slash "/"
    private const val BASE_URL = "https://6a20066ce96c1d13b586e171.mockapi.io/"

    val apiService: MockApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MockApiService::class.java)
    }
}