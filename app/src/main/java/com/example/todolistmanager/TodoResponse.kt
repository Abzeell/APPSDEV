package com.example.todolistmanager

data class TodoResponse(
    val id: Int = 0,
    val userId: Int = 1,
    val title: String = "",
    val completed: Boolean = false
)
