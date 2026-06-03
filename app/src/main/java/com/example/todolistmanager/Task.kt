package com.example.todolistmanager

data class Task(
    var id: Int = 0,
    var apiId: String = "", // Changed to String to support mockAPI
    var title: String,
    var description: String,
    var isCompleted: Boolean = false
)