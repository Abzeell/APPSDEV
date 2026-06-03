package com.example.todolistmanager

data class ApiTask(
    val id: String = "",
    val userId: String,
    val title: String,
    val description: String,
    val completed: Boolean
)