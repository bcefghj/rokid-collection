package com.example.ai_assist.model

import com.google.gson.annotations.SerializedName

data class EndSessionRequest(
    @SerializedName("session_id") val sessionId: String
)

data class EndSessionResponse(
    val status: String,
    val message: String
)

data class StartSessionRequest(
    @SerializedName("session_id") val sessionId: String
)

data class StartSessionResponse(
    val status: String,
    @SerializedName("session_id") val sessionId: String
)
