package com.example.todolistmanager

import retrofit2.Response
import retrofit2.http.*

interface TodoApiService {

    @GET("todos")
    suspend fun getTodos(@Query("_limit") limit: Int = 10): List<TodoResponse>

    @POST("todos")
    suspend fun createTodo(@Body todo: TodoResponse): TodoResponse

    @PUT("todos/{id}")
    suspend fun updateTodo(@Path("id") id: Int, @Body todo: TodoResponse): TodoResponse

    @DELETE("todos/{id}")
    suspend fun deleteTodo(@Path("id") id: Int): Response<Unit>
}
