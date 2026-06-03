package com.example.todolistmanager

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface MockApiService {
    // --- USER ENDPOINTS ---
    @GET("users")
    suspend fun login(@Query("username") username: String): List<User>

    @POST("users")
    suspend fun register(@Body user: User): User

    // --- TASK ENDPOINTS ---
    // The @Query filters the mockAPI so it only returns tasks for the logged-in user!
    @GET("tasks")
    suspend fun getTasks(@Query("userId") userId: String): List<ApiTask>

    @POST("tasks")
    suspend fun createTask(@Body task: ApiTask): ApiTask

    @PUT("tasks/{id}")
    suspend fun updateTask(@Path("id") id: String, @Body task: ApiTask): ApiTask

    @DELETE("tasks/{id}")
    suspend fun deleteTask(@Path("id") id: String)
}