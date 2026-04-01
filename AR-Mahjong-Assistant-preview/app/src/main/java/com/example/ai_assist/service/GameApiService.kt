package com.example.ai_assist.service

import com.example.ai_assist.model.AnalyzeResponse
import com.example.ai_assist.model.EndSessionRequest
import com.example.ai_assist.model.EndSessionResponse
import com.example.ai_assist.model.StartSessionRequest
import com.example.ai_assist.model.StartSessionResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface GameApiService {
    @POST("api/start-session")
    suspend fun startSession(@Body request: StartSessionRequest): StartSessionResponse

    @Multipart
    @POST("api/analyze-hand")
    suspend fun analyzeHand(
        @Part image: MultipartBody.Part,
        @Part("session_id") sessionId: RequestBody
    ): AnalyzeResponse

    @Multipart
    @POST("api/process-audio")
    suspend fun processAudio(
        @Part audio: MultipartBody.Part,
        @Part("session_id") sessionId: RequestBody
    ): ResponseBody

    @POST("api/end-session")
    suspend fun endSession(@Body request: EndSessionRequest): EndSessionResponse
}
