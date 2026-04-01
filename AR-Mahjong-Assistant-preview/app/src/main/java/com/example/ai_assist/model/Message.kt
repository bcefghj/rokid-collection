package com.example.ai_assist.model

data class Message(
    val role: String, // "user" or "assistant"
    val content: String,
    val imageUrl: String? = null
)