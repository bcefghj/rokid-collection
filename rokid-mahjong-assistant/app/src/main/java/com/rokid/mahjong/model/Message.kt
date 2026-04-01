package com.rokid.mahjong.model

data class Message(
    val role: String,
    val content: String,
    val imageUrl: String? = null
)
